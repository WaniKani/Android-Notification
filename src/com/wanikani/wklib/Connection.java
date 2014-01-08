package com.wanikani.wklib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
		
		JSONObject infoAsObj;

		JSONArray infoAsArray;

		public Response (JSONObject obj, boolean isArray)
			throws JSONException, IOException
		{
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
	
	public static final int CONNECT_TIMEOUT = 20000;
	
	public static final int READ_TIMEOUT = 60000;
	
	UserLogin login;
	
	Config config;
	
	Authenticator auth;
	
	UserInformation ui;
	
	public ItemsCache cache;
	
	public Connection (UserLogin login, Config config)
	{
		this.login = login;
		this.config = config;
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
	
	private static String levelList (int level [])
	{
		StringBuffer sb;
		int i;
		
		sb = new StringBuffer ();
		sb.append (level [0]);		
		for (i = 1; i < level.length; i++)
			sb.append (',').append (level [i]);
		
		return sb.toString ();
	}
	
	public ItemLibrary<Radical> getRadicals (Meter meter, int level)
			throws IOException
	{
		ItemLibrary<Radical> ans;
		Response res;
			
		ans = cache.radicals.get (level);
		if (ans != null)
			return ans;
		
		try {
			res = call (meter, "radicals", true, Integer.toString (level));

			return cache.radicals.put 
					(new ItemLibrary<Radical> (Radical.FACTORY, res.infoAsArray));
				
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}

	public ItemLibrary<Radical> getRadicals (Meter meter, int levels [])
			throws IOException
	{
		return getRadicals (meter, levels, true);
	}

	public ItemLibrary<Radical> getRadicals (Meter meter, int levels [], boolean cacher)
		throws IOException
	{
		ItemLibrary<Radical> ans;
		Response res;
		
		ans = new ItemLibrary<Radical> ();
		levels = cache.radicals.get (ans, levels);
		if (levels.length == 0)
			return ans;

		try {
			res = call (meter, "radicals", true, levelList (levels));
			ans.add (new ItemLibrary<Radical> (Radical.FACTORY, res.infoAsArray));
			if (cacher)
				cache.radicals.put (ans);
			return ans;			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Radical> getRadicals (Meter meter, boolean cacher)
			throws IOException
	{		
		return getRadicals (meter, getAllLevels (meter), cacher);
	}
		
	public ItemLibrary<Kanji> getKanji (Meter meter, int level)
		throws IOException
	{
		ItemLibrary<Kanji> ans;
		Response res;
		
		ans = cache.kanji.get (level);
		if (ans != null)
			return ans;
		
		try {
			res = call (meter, "kanji", true, Integer.toString (level));

			return cache.kanji.put 
				(new ItemLibrary<Kanji> (Kanji.FACTORY, res.infoAsArray));
			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Kanji> getKanji (Meter meter, int level [])
		throws IOException
	{
		return getKanji (meter, level, true);
	}

	public ItemLibrary<Kanji> getKanji (Meter meter, int level [], boolean cacher)
		throws IOException
	{
		ItemLibrary<Kanji> ans;
		Response res;
			
		ans = new ItemLibrary<Kanji> ();
		level = cache.kanji.get (ans, level);
		if (level.length == 0)
			return ans;
		
		try {
			res = call (meter, "kanji", true, levelList (level));

			ans.add (new ItemLibrary<Kanji> (Kanji.FACTORY, res.infoAsArray));
			if (cacher)
				cache.kanji.put (ans);
			
			return ans;
				
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
		
	public ItemLibrary<Kanji> getKanji (Meter meter, boolean cacher)
			throws IOException
	{		
		return getKanji (meter, getAllLevels (meter), cacher);
	}
	
	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, int level)
		throws IOException
	{
		ItemLibrary<Vocabulary> ans;
		Response res;
		
		ans = cache.vocab.get (level);
		if (ans != null)
			return ans;
		
		try {
			res = call (meter, "vocabulary", true, Integer.toString (level));

			return cache.vocab.put
					(new ItemLibrary<Vocabulary> (Vocabulary.FACTORY, res.infoAsArray));
			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, int level [])
		throws IOException
	{
		return getVocabulary (meter, level, true);
	}

	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, int level [], boolean cacher)
			throws IOException
	{
		ItemLibrary<Vocabulary> ans;
		Response res;		
		
		ans = new ItemLibrary<Vocabulary> ();
		level = cache.vocab.get (ans, level);
		if (level.length == 0)
			return ans;
		
		try {
			res = call (meter, "vocabulary", true, levelList (level));
			ans.add (new ItemLibrary<Vocabulary> (Vocabulary.FACTORY, res.infoAsArray));
			if (cacher)
				cache.vocab.put (ans);
			
			return ans;			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Vocabulary> getVocabulary (Meter meter, boolean cacher)
			throws IOException
	{		
		return getVocabulary (meter, getAllLevels (meter), cacher);
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
		HttpURLConnection conn;
		JSONTokener tok;
		InputStream is;
		URL url;
		
		url = new URL (makeURL (resource, arg));
		conn = null;
		tok = null;
		try {
			conn = (HttpURLConnection) url.openConnection ();
			setTimeouts (conn);
			is = conn.getInputStream ();
			measureHeaders (meter, conn, false);
			tok = new JSONTokener (readStream (meter, is));
		} finally {
			if (conn != null)
				conn.disconnect ();
		}
		
		try {
			return new Response (new JSONObject (tok), isArray);
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
