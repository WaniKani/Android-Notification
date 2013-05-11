package com.wanikani.wklib;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

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


public class Util {

	public static String getString (JSONObject obj, String key)
		throws JSONException
	{
		return obj.isNull (key) ? null : obj.getString (key);
	}

	public static int getInt (JSONObject obj, String key)
			throws JSONException
	{
			return obj.getInt (key);
	}

	public static Date getDate (JSONObject obj, String key)
			throws JSONException
	{		
			return obj.isNull (key) ? null : new Date (obj.getLong (key) * 1000);
	}
}
