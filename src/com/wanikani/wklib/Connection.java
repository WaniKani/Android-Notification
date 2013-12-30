package com.wanikani.wklib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.wanikani.wklib.ItemsCacheInterface.Quality;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

public class Connection {
	
	public interface Meter {
		
		public void count (int data);
		
		public void sync ();
		
	}
	
	class Response {
		
		UserInformation ui;
		
		Date lastModified;
		
		JSONObject infoAsObj;

		JSONArray infoAsArray;

		public Response (JSONObject obj, Date lastModified, boolean isArray)
			throws JSONException, IOException
		{
			this.lastModified = lastModified;
			
			if (!obj.isNull("user_information")) {
				ui = new UserInformation (obj.getJSONObject ("user_information"));
				if (!obj.isNull ("requested_information")) {
					if (isArray)
						infoAsArray = obj.getJSONArray ("requested_information");
					else
						infoAsObj = obj.getJSONObject ("requested_information");
				}
			} else {
				throw ApplicationException.buildFromJSON (obj);
			}
		}
	}
	
	class NotModifiedException extends IOException {
		
		private static final long serialVersionUID = 1L;
		
		public NotModifiedException ()
		{
			super ("Document not modified");
		}
		
	}
	
	public static final int CONNECT_TIMEOUT = 20000;
	
	public static final int READ_TIMEOUT = 60000;
	
	private static final int CACHE_DISPERSION_GROUPS = 7;
	
	private static final long CACHE_STALE_DISPERSION = 12 * 3600 * 1000;
	
	private static final long CACHE_STALE_TIME = 7 * 24 * 3600 * 1000;
	
	UserLogin login;
	
	Config config;
	
	Authenticator auth;
	
	UserInformation ui;
	
	public ItemsCacheInterface cache;
	
	public Connection (UserLogin login, Config config)
	{
		this.login = login;
		this.config = config;
		cache = new ItemsCache ();
	}
	
	public Connection (UserLogin ulogin)
	{
		this (ulogin, Config.DEFAULT);
		cache = new ItemsCache ();
	}
	
	public void flush ()
	{
		cache = new ItemsCache ();
	}
	
	public UserInformation getUserInformation (Meter meter)
		throws IOException
	{
		if (ui == null)
			ui = call (meter, "user-information", false).ui;
		
		return ui;
	}	

	public StudyQueue getStudyQueue (Meter meter)
			throws IOException
	{
		Response res;
		
		try {
			res = call (meter, "study-queue", false);
			ui = res.ui;

			return new StudyQueue (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}	

	public SRSDistribution getSRSDistribution (Meter meter)
			throws IOException
	{
		Response res;
		
		try {
			res = call (meter, "srs-distribution", false);
			ui = res.ui;

			return new SRSDistribution (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}	

	public LevelProgression getLevelProgression (Meter meter)
			throws IOException
	{
		Response res;
		
		try {
			res = call (meter, "level-progression", false);
			ui = res.ui;

			return new LevelProgression (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	private int [] getAllLevels (Meter meter)
		throws IOException
	{
		UserInformation ui;
		int ans [];
		int i;
		
		ui = getUserInformation (meter);
		
		ans = new int [ui.level];
		for (i = 0; i < ans.length; i++)
			ans [i] = i + 1;
		
		return ans;
	}
	
	private static String levelList (List<Integer> level)
	{
		StringBuffer sb;
		int i;
		
		sb = new StringBuffer ();
		sb.append (level.get (0));		
		for (i = 1; i < level.size (); i++)
			sb.append (',').append (level.get (i));
		
		return sb.toString ();
	}
	
	private static<T extends Item> boolean isDataStale (ItemsCacheInterface.LevelData<T> ld, int level)
	{		
		Date now;
		long age;
		
		now = new Date ();
		age = now.getTime () - ld.date.getTime ();
		if (age > CACHE_STALE_TIME + CACHE_STALE_DISPERSION * (level % CACHE_DISPERSION_GROUPS))
			return true;
		
		for (T item : ld.lib.list) 
			if (item.getAvailableDate () != null &&
			    item.getAvailableDate ().before (now))
				return true;
		
		return false;
	}
	
	public ItemLibrary<Radical> getRadicals (Meter meter, int level)
			throws IOException
	{
		return getItems (meter, level, "radicals", Item.Type.RADICAL, Radical.FACTORY);
	}

	public ItemLibrary<Radical> getRadicals (Meter meter)
			throws IOException
	{		
		return getRadicals (meter, getAllLevels (meter));
	}
		
	public ItemLibrary<Radical> getRadicals (Meter meter, int levels [])
			throws IOException
	{
		return getItems (meter, levels, "radicals", Item.Type.RADICAL, Radical.FACTORY);
	}

	public ItemLibrary<Kanji> getKanji (Meter meter, int level)
			throws IOException
	{
		return getItems (meter, level, "kanji", Item.Type.KANJI, Kanji.FACTORY);
	}	

	public ItemLibrary<Kanji> getKanji (Meter meter)
			throws IOException
	{		
		return getKanji (meter, getAllLevels (meter));
	}
		
	public ItemLibrary<Kanji> getKanji (Meter meter, int levels [])
			throws IOException
	{
		return getItems (meter, levels, "kanji", Item.Type.KANJI, Kanji.FACTORY);
	}
	
	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, int level)
			throws IOException
	{
		return getItems (meter, level, "vocabulary", Item.Type.VOCABULARY, Vocabulary.FACTORY);
	}

	public ItemLibrary<Vocabulary> getVocabulary (Meter meter)
			throws IOException
	{		
		return getVocabulary (meter, getAllLevels (meter));
	}
		
	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, int levels [])
			throws IOException
	{
		return getItems (meter, levels, "vocabulary", Item.Type.VOCABULARY, Vocabulary.FACTORY);
	}
	
	protected<T extends Item> ItemLibrary<T> getItems (Meter meter, int level, String resource, 
													   Item.Type type, Item.Factory<T> factory)
			throws IOException
	{
		ItemsCacheInterface.LevelData<T> data;
		ItemsCacheInterface.Cache<T> ic;
		ItemLibrary<T> lib;
		Response res;
			
		ic = cache.get (type);
		data = ic.get (level);				
		if (data.quality == ItemsCacheInterface.Quality.GOOD &&	!isDataStale (data, level))
			return data.lib;
		
		try {
			res = call (meter, resource, true, Integer.toString (level), data.date);

			lib = new ItemLibrary<T> (factory, res.infoAsArray);
			
			data = new ItemsCacheInterface.LevelData<T> (res.lastModified, lib);
			ic.put (data);
			
			return lib;
			
		} catch (NotModifiedException e) {	
			return data.lib;
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}

	protected<T extends Item> ItemLibrary<T> getItems (Meter meter, int levels [], String resource, 
			  										   Item.Type type, Item.Factory<T> factory)
			throws IOException
	{
		Map<Integer, ItemsCacheInterface.LevelData<T>> map;
		ItemsCacheInterface.LevelData <T> ld;
		ItemsCacheInterface.Cache<T> ic;
		List<Integer> badl, missingl;
		ItemLibrary<T> ans, lib;
		Response res;
		Date cachedt;

		ic = cache.get (type);
		map = ItemsCacheInterface.LevelData.createMap (levels);
		ic.get (map);

		cachedt = new Date ();
		ans = new ItemLibrary<T> ();
		badl = new Vector<Integer> ();
		missingl = new Vector<Integer> ();
		for (Map.Entry<Integer, ItemsCacheInterface.LevelData<T>> e : map.entrySet ()) {
			ld = e.getValue ();
			switch (ld.quality) {
			case GOOD:
				if (isDataStale (ld, e.getKey ())) {
					if (ld.date.before (cachedt))
						cachedt = ld.date;
					badl.add (e.getKey ());					
				} else				
					ans.add (ld.lib);
				break;
				
			case MISSING:
				missingl.add (e.getKey ());
			}
		}

		try {
			if (!badl.isEmpty ()) {
				res = call (meter, resource, true, levelList (badl), cachedt);
				lib = new ItemLibrary<T> (factory, res.infoAsArray);
				ic.put (new ItemsCacheInterface.LevelData<T> (res.lastModified, lib));
				ans.add (lib);
			} 
		} catch (NotModifiedException e) {
			for (Integer i : badl)
				ans.add (map.get (i).lib);
		} catch (JSONException e) {
			throw new ParseException ();
		}
		
		try {
			if (!missingl.isEmpty ()) {
				res = call (meter, resource, true, levelList (missingl), null);
				lib = new ItemLibrary<T> (factory, res.infoAsArray);
				ic.put (new ItemsCacheInterface.LevelData<T> (res.lastModified, lib));
				ans.add (lib);
			} 
		} catch (JSONException e) {
			throw new ParseException ();
		}
		
		return ans;
	}
	
	public ItemLibrary<Item> getRecentUnlocks (Meter meter, int count)
		throws IOException
	{
		Response res;
		
		try {
			res = call (meter, "recent-unlocks", true, Integer.toString (count));

			return new ItemLibrary<Item> (Item.FACTORY, res.infoAsArray);
			
		} catch (JSONException e) {
			throw new ParseException ();
		}		
	}

	public ItemLibrary<Item> getCriticalItems (Meter meter)
			throws IOException
	{
		Response res;
			
		try {
			res = call (meter, "critical-items", true);
				return new ItemLibrary<Item> (Item.FACTORY, res.infoAsArray);
			
		} catch (JSONException e) {
			throw new ParseException ();
		}		
	}

	public ItemLibrary<Item> getItems (Meter meter, int level)
			throws IOException
	{
		ItemLibrary<Radical> radicals;
		ItemLibrary<Kanji> kanji; 
		ItemLibrary<Vocabulary> vocab;
		
		radicals = getRadicals (meter, level);
		kanji = getKanji (meter, level);
		vocab = getVocabulary (meter, level);
		
		return new ItemLibrary<Item> ().
					add (radicals).add (kanji).add(vocab);
	}		

	private static String readStream (Meter meter, InputStream is)
		throws IOException
	{
		InputStreamReader ir;
		StringBuffer sb;
		char buf [];
		int rd;
		
		buf = new char [1024];
		sb = new StringBuffer ();
		ir = new InputStreamReader (is, "UTF-8");
		while (true) {
			rd = ir.read (buf, 0, buf.length);
			if (rd < 0)
				break;
			meter.count (rd);
			sb.append (buf, 0, rd);
		}
		
		meter.sync ();
		return sb.toString ();
	}
	
	protected Response call (Meter meter, String resource, boolean isArray)
			throws IOException
	{
			return call (meter, resource, isArray, null);
	}

	protected Response call (Meter meter, String resource, boolean isArray, String arg)
		throws IOException
	{
		return call (meter, resource, isArray, arg, null);
	}

	protected Response call (Meter meter, String resource, boolean isArray, String arg, Date ifModified)
		throws IOException
	{
		HttpURLConnection conn;
		JSONTokener tok;
		InputStream is;
		Date lastModified;
		URL url;
		
		url = new URL (makeURL (resource, arg));
		conn = null;
		tok = null;
		try {
			conn = (HttpURLConnection) url.openConnection ();
			if (ifModified != null)
				conn.setIfModifiedSince (ifModified.getTime ());
			setTimeouts (conn);
			if (ifModified != null && conn.getResponseCode () == HttpURLConnection.HTTP_NOT_MODIFIED)
				throw new NotModifiedException ();
			is = conn.getInputStream ();
			measureHeaders (meter, conn, false);
			tok = new JSONTokener (readStream (meter, is));
		} finally {
			if (conn != null)
				conn.disconnect ();
		}
				
		lastModified = new Date ();
		if (conn.getDate () > 0)
			lastModified = new Date (conn.getDate ());
		if (conn.getLastModified () > 0)
			lastModified = new Date (conn.getLastModified ());
		
		try {
			return new Response (new JSONObject (tok), lastModified, isArray);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
		
	public void resolve (Meter meter, UserInformation ui, int size, Bitmap defAvatar)
	{
			HttpURLConnection conn;
			InputStream is;
			URL url;
			int code;
			
			conn = null;
			try {
				url = new URL (config.gravatarUrl + "/" + ui.gravatar + 
							   "?s=" + size + "&d=404");
				conn = (HttpURLConnection) url.openConnection ();
				setTimeouts (conn);
				code = conn.getResponseCode ();
				if (code == 200) {
					is = conn.getInputStream ();
					ui.gravatarBitmap = BitmapFactory.decodeStream (is);
				} else if (code == 404)
					ui.gravatarBitmap = defAvatar;
				measureHeaders (meter, conn, true);
			} catch (IOException e) {
				/* empty */
			} finally {
				if (conn != null)
					conn.disconnect ();
			}
	}
	
	protected void measureHeaders (Meter meter, URLConnection conn, boolean clen)
	{
		Map<String, List<String>> hdrs;
		
		hdrs = conn.getHeaderFields ();
		if (hdrs == null)
			return;
		for (Map.Entry<String, List<String>> e : hdrs.entrySet ()) {
			if (e.getKey () != null) 
				meter.count (e.getKey ().length () + 1);
			for (String s : e.getValue ())
				meter.count (s.length () + 3);
			if (clen && e.getKey () != null && 
			    e.getKey ().equals ("Content-Length") && !e.getValue ().isEmpty ()) {
				try {
					meter.count (Integer.parseInt (e.getValue ().get (0)));
				} catch (NumberFormatException x) {
					/* empty */
				}
			}
		}
		
		meter.sync ();
	}
		
	private String makeURL (String resource, String arg)
	{
		String ans;
		
		ans = String.format ("%s/user/%s/%s", 
							 config.url, login.userkey, resource);
		if (arg != null)
			ans += "/" + arg;
							 
		return ans;
	}
	
	private void setTimeouts (HttpURLConnection conn)
	{
		conn.setConnectTimeout (CONNECT_TIMEOUT);
		conn.setReadTimeout (READ_TIMEOUT);
	}
}
