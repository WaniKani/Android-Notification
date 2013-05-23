package com.wanikani.wklib;

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


public class SRSDistribution {

	public class Level {
	
		public int radicals;
		
		public int kanji;
		
		public int vocabulary;
		
		public int total;
		
		Level (JSONObject obj)
			throws JSONException
		{
			radicals = Util.getInt (obj, "radicals");
			kanji = Util.getInt (obj, "kanji");
			vocabulary = Util.getInt (obj, "vocabulary");
			total = Util.getInt (obj, "total");
		}
	}
	
	public Level apprentice;
	public Level guru;
	public Level master;
	public Level enlighten;
	public Level burned;

	SRSDistribution (JSONObject obj)
		throws JSONException
	{
		apprentice = new Level (obj.getJSONObject ("apprentice"));
		guru = new Level (obj.getJSONObject ("guru"));
		master = new Level (obj.getJSONObject ("master"));
		enlighten = new Level (obj.getJSONObject ("enlighten"));
		burned = new Level (obj.getJSONObject ("burned"));
	}

	public SRSDistribution ()
	{
		/* empty */
	}
}
