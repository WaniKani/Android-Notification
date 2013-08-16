package com.wanikani.androidnotifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.wanikani.wklib.Radical;

/* 
 *  Copyright (c) 2013 Alberto Cuda
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This object retrieves and caches radical images. 
 * The cache is implemented at two levels:
 * <ul>
 * <li>A L1 memory cache, keeping the last {@link #L1_CACHE_SIZE} bitmaps
 * <li>A L2 disk cache
 * </ul>
 * Of course this internal structure is hidden to user classes, that should
 * just call {@link #getImage(Radical)} to retrieve the bitmap. Calling this
 * method makes also sure that the radical is moved to the L1 cache.
 */
public class RadicalImages {

	/**
	 * An entry in the L1 cache. It implements the comparable interface,
	 * and the natural ordering puts the latest accessed objects at the head.
	 */
	static class Entry implements Comparable<Entry> {

		/// Last access time
		Date timestamp;
		
		/// Bitmap
		Bitmap bmp;
		
		/**
		 * Constructor
		 * @param bmp the bitmap
		 */
		public Entry (Bitmap bmp)
		{
			this.bmp = bmp;
			
			timestamp = new Date ();
		}
		
		@Override
		public int compareTo (Entry e)
		{
			return -timestamp.compareTo (e.timestamp);
		}		
	}
	
	/**
	 * A general purpose comparator that uses the natural ordering of values
	 * to compare two map entries.
	 * @param <K> the entry key
	 * @param <V> the entry map
	 */
	static class MapEntryComparator<K,V extends Comparable<V>> 
		implements Comparator<Map.Entry<K, V>> {
		
		@Override
		public int compare (Map.Entry<K, V> e1, Map.Entry<K, V> e2)
		{
			return e1.getValue ().compareTo (e2.getValue ());
		}
		
	}
	
	/// The L1 cache size
	public static final int L1_CACHE_SIZE = 128;

	/// Number of entries to retain after cleaning the l1 cache because
	///  the entries are more than #L1_CACHE_SIZE
	private static final int L1_CACHE_LO = 100;
	
	/// Subdirectory containing the (l2 cached) radical image files
	private static final String SUBDIRECTORY = "radicalimgs";
	
	/// The l1 cache	
	public Hashtable<String, Entry> ht;
	
	/// A global mutex that serializes access to the L2 cache
	private static Object FILE_MUTEX = new Object ();
	
	/// The L1 entry comparator, used when cleaning up the cache	
	private static final MapEntryComparator<String, Entry> COMPARATOR = 
			new	MapEntryComparator<String, Entry> ();

	/**
	 * Constructor.
	 */
	public RadicalImages ()
	{
		ht = new Hashtable<String, Entry> ();
	}
	
	/**
	 * Loads an image, also placing it at the beginning of the L1 cache
	 * @param ctxt the application context
	 * @param r the radical whose image is to be loaded
	 * @return the bitmap
	 * @throws IOException if some network or disk exception arises
	 */
	public Bitmap getImage (Context ctxt, Radical r)
		throws IOException
	{
		Bitmap ans;
		
		ans = loadMemory (r);
		if (ans != null)
			return ans;
		
		ans = loadDisk (ctxt, r);
		if (ans != null) {
			storeMemory (r, ans);
			return ans;
		}
		
		ans = loadNet (r);
		if (ans == null)
			throw new IOException ("Failed to load image from network");
	
		storeDisk (ctxt, r, ans);
		storeMemory (r, ans);
		return ans;
	}
	
	/**
	 * Retrieves an image from the l1 cache
	 * @param r the radical
	 * @return the result, or <tt>null</tt> if not found
	 */
	private Bitmap loadMemory (Radical r)
	{
		Entry e;
		
		synchronized (ht) {
			e = ht.get (r.meaning);
			if (e != null) {
				e.timestamp = new Date ();
				return e.bmp;
			}
		}
		
		return null;
	}
	
	/**
	 * Stores an object into the l1 cache
	 * @param r the radical
	 * @param bmp its image
	 */
	private void storeMemory (Radical r, Bitmap bmp)
	{
		Entry e;
		
		synchronized (ht) {
			ensureCapacity ();
			
			e = new Entry (bmp);
			
			ht.put (r.meaning, e);
		}
	}
	
	/**
	 * Makes sure that the l1 cache is not overcrowded. If so, older entries
	 * are removed.
	 */
	private void ensureCapacity ()
	{
		List<Map.Entry<String, Entry>> l;
		Hashtable<String, Entry> nht;
		int i;
		
		if (ht.size () > L1_CACHE_SIZE) {
			nht = new Hashtable<String, Entry> ();
			l = new Vector<Map.Entry<String, Entry>> (ht.entrySet ());
			Collections.sort (l, COMPARATOR);
			for (i = 0; i < L1_CACHE_LO; i++)
				nht.put (l.get (i).getKey (), l.get (i).getValue ());

			ht = nht;
		}		
	}

	/**
	 * Returns the file object associated to a given radical
	 * @param ctxt the application context
	 * @param r a radical
	 * @return a file object
	 */
	private File getFile (Context ctxt, Radical r)
	{
		File dir;		
		
		dir = ctxt.getDir (SUBDIRECTORY, Context.MODE_PRIVATE);
		
		return new File (dir, r.getItemURLComponent ());
	}
	
	/**
	 * Retrieves an image from the l2 cache
	 * @param r the radical
	 * @return the result, or <tt>null</tt> if not found
	 */
	private Bitmap loadDisk (Context ctxt, Radical r)
	{		
		InputStream is;
		
		if (ctxt == null)
			return null;
		
		is = null;
		try {
			is = new FileInputStream (getFile (ctxt, r));
			return BitmapFactory.decodeStream (is);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				if (is != null)
					is.close ();
			} catch (IOException e) {
				/* empty */
			}
		}
	}
	
	/**
	 * Stores an object into the l1 cache
	 * @param r the radical
	 * @param bmp its image
	 */
	private void storeDisk (Context ctxt, Radical r, Bitmap bmp)
	{		
		OutputStream os;
		File file;		
		boolean ok;
		
		if (ctxt == null)
			return;
		
		file = getFile (ctxt, r);
		os = null;
		ok =  false;
		synchronized (FILE_MUTEX) {
			try {
				
				os = new FileOutputStream (getFile (ctxt, r), false);
				ok = bmp.compress (CompressFormat.PNG, 100, os);
					
			} catch (IOException e) {
				/* empty */
			} finally {
				try {
					if (os != null)
						os.close ();
				} catch (IOException e) {
					ok = false;
				}
				if (!ok)
					file.delete ();
			}
		}
	}

	/**
	 * Loads the radical image from the network. Used as a last resort
	 * @param r the radical
	 * @return the bitmap
	 * @throws IOException
	 */
	private Bitmap loadNet (Radical r)
			throws IOException
	{
		HttpURLConnection conn;
		InputStream is;
		URL url;
		int code;

		conn = null;

		try {
			url = new URL (r.image);
			conn = (HttpURLConnection) url.openConnection ();
			code = conn.getResponseCode ();
			if (code / 100 == 2) {
				is = conn.getInputStream ();
					return BitmapFactory.decodeStream (is);
			} else
				throw new IOException ("Response code is " + code);
		} finally {
			if (conn != null)
				conn.disconnect ();
		}
	}
}
