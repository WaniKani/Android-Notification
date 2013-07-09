package com.wanikani.wklib;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

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

public class UserInformation {

	public String username;
	
	public String gravatar;
	
	public Bitmap gravatarBitmap;
	
	public int level;
	
	public String title;
	
	public String about;
	
	public String website;
	
	public String twitter;
	
	public int topicsCount;
	
	public int postsCount;
	
	public Date creationDate;
	
	private static final long ONE_DAY = 24 * 60 * 60 * 1000;
	
	UserInformation (JSONObject obj)
		throws JSONException
	{
		username = Util.getString (obj, "username");
		gravatar = Util.getString (obj, "gravatar");
		level = Util.getInt (obj, "level");
		title = Util.getString (obj, "title");
		about = Util.getString (obj, "about");
		website = Util.getString (obj, "website");
		twitter = Util.getString (obj, "twitter");
		topicsCount = Util.getInt (obj, "topics_count");
		postsCount = Util.getInt (obj, "posts_count");
		creationDate = Util.getDate (obj, "creation_date");
	}
	
	public int getDay ()
	{
		return getDay (new Date ());
	}
	
	public int getDay (Date d)
	{
		return (int) ((d.getTime () - creationDate.getTime ()) / ONE_DAY);
	}
}