package com.wanikani.wklib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;

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
	
	UserLogin login;
	
	Config config;
	
	Authenticator auth;
	
	UserInformation ui;
	
	public Connection (UserLogin login, Config config)
	{
		this.login = login;
		this.config = config;
	}
	
	public Connection (UserLogin ulogin)
	{
		this (ulogin, Config.DEFAULT);
	}
	
	public UserInformation getUserInformation ()
		throws IOException
	{
		if (ui == null)
			ui = call ("user-information", false).ui;
		
		return ui;
	}	

	public StudyQueue getStudyQueue ()
			throws IOException
	{
		Response res;
		
		try {
			res = call ("study-queue", false);
			ui = res.ui;

			return new StudyQueue (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}	

	public SRSDistribution getSRSDistribution ()
			throws IOException
	{
		Response res;
		
		try {
			res = call ("srs-distribution", false);
			ui = res.ui;

			return new SRSDistribution (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}	

	public LevelProgression getLevelProgression ()
			throws IOException
	{
		Response res;
		
		try {
			res = call ("level-progression", false);
			ui = res.ui;

			return new LevelProgression (res.infoAsObj);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Radical> getRadicals (int level)
		throws IOException
	{
		Response res;
		
		try {
			res = call ("radicals", true, Integer.toString (level));

			return new ItemLibrary<Radical> (Radical.FACTORY, res.infoAsArray);
			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public void loadImage (Radical r)
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
			if (code == 200) {
				is = conn.getInputStream ();
				r.bitmap = BitmapFactory.decodeStream (is);
			} 
		} finally {
			if (conn != null)
				conn.disconnect ();
		}
}


	public ItemLibrary<Kanji> getKanji (int level)
		throws IOException
	{
		Response res;
		
		try {
			res = call ("kanji", true, Integer.toString (level));

			return new ItemLibrary<Kanji> (Kanji.FACTORY, res.infoAsArray);
			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
	
	public ItemLibrary<Vocabulary> getVocabulary (int level)
		throws IOException
	{
		Response res;
		
		try {
			res = call ("vocabulary", true, Integer.toString (level));

			return new ItemLibrary<Vocabulary> (Vocabulary.FACTORY, res.infoAsArray);
			
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}

	public ItemLibrary<Item> getItems (int level)
			throws IOException
	{
		ItemLibrary<Radical> radicals;
		ItemLibrary<Kanji> kanji; 
		ItemLibrary<Vocabulary> vocab;
		
		radicals = getRadicals (level);
		kanji = getKanji (level);
		vocab = getVocabulary (level);
		
		return new ItemLibrary<Item> (radicals, kanji, vocab);
	}		

	private static String readStream (InputStream is)
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
			sb.append (buf);
		}
		return sb.toString ();
	}
	
	protected Response call (String resource, boolean isArray)
			throws IOException
	{
			return call (resource, isArray, null);
	}
		
	protected Response call (String resource, boolean isArray, String arg)
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
			is = conn.getInputStream ();
			tok = new JSONTokener (readStream (is));
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
		
	public void resolve (UserInformation ui, int size, Bitmap defAvatar)
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
				code = conn.getResponseCode ();
				if (code == 200) {
					is = conn.getInputStream ();
					ui.gravatarBitmap = BitmapFactory.decodeStream (is);
				} else if (code == 404)
					ui.gravatarBitmap = defAvatar;
			} catch (IOException e) {
				/* empty */
			} finally {
				if (conn != null)
					conn.disconnect ();
			}
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
}
