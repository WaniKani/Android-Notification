package com.wanikani.wklib;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
		
		JSONObject info;
		
		public Response (JSONObject obj)
			throws JSONException, IOException
		{
			if (!obj.isNull("user_information")) {
				ui = new UserInformation (obj.getJSONObject ("user_information"));
				if (!obj.isNull ("requested_information"))
					info = obj.getJSONObject ("requested_information");
			} else {
				throw ApplicationException.buildFromJSON (obj);
			}
		}
	}
	
	UserLogin login;
	
	Config config;
	
	Authenticator auth;
	
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
		return getUserInformation (false);		
	}
	
	public UserInformation getUserInformation (boolean resolveGravatar)
		throws IOException
	{
		UserInformation ui;
		
		ui = call ("user-information").ui;
		if (resolveGravatar)
			resolve (ui);
		
		return ui;
	}	

	public StudyQueue getStudyQueue ()
			throws IOException
	{
		Response res;
		
		try {
			res = call ("study-queue");
			return new StudyQueue (res.info);
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}	

	public SRSDistribution getSRSDistribution ()
			throws IOException
	{
		Response res;
		
		try {
			res = call ("srs-distribution");
			return new SRSDistribution (res.info);
		} catch (JSONException e) {
			throw new ParseException ();
		}
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
	
	protected Response call (String resource)
			throws IOException
	{
			return call (resource, null);
	}
		
	protected Response call (String resource, String arg)
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
			return new Response (new JSONObject (tok));
		} catch (JSONException e) {
			throw new ParseException ();
		}
	}
		
	protected void resolve (UserInformation ui)
	{
			HttpURLConnection conn;
			InputStream is;
			URL url;
			
			conn = null;
			try {
				url = new URL (config.gravatarUrl + "/" + ui.gravatar);
				conn = (HttpURLConnection) url.openConnection ();
				is = conn.getInputStream ();
				ui.gravatarBitmap = BitmapFactory.decodeStream (is);
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
