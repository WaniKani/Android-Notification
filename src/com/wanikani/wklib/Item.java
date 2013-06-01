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

public abstract class Item {

	public interface Factory<T extends Item> {
		
		public T deserialize (JSONObject obj)
			throws JSONException;
		
	};
	
	public static enum Type {
		
		RADICAL,	
		KANJI,
		VOCABULARY
	}
	
	public static class Performance {
		
		public int correct;
		
		public int incorrect;
		
		public int maxStreak;
		
		public int currentStreak;
		
		Performance (JSONObject obj, String prefix)
			throws JSONException
		{
			correct = Util.getInt (obj, prefix + "_correct");
			incorrect = Util.getInt (obj, prefix + "_incorrect");
			maxStreak = Util.getInt (obj, prefix + "_max_streak");
			currentStreak = Util.getInt (obj, prefix + "_current_streak");
		}
		
	};
	
	public static class Stats {
		
		public SRSLevel srs;
		
		public Date unlockedDate;
		
		public Date availableDate;
		
		public Date burnedDate;
		
		public boolean burned;
				
		public Item.Performance reading;
		
		public Item.Performance meaning;		

		Stats (JSONObject obj, boolean hasReading)
			throws JSONException
		{
			String s;
			
			s = obj.getString ("srs");
			srs = SRSLevel.fromString (s);
			if (srs == null)
				throw new JSONException ("Bad SRS: " + s);
			
			unlockedDate = Util.getDate (obj, "unlocked_date"); 
			availableDate = Util.getDate (obj, "available_date"); 
			burnedDate = Util.getDate (obj, "burned_date");
			burned = Util.getBoolean (obj, "burned");
			
			if (hasReading)
				reading = new Item.Performance (obj, "reading");
			meaning = new Item.Performance (obj, "meaning");
		}
	};
	
	public Type type;
	
	public String character;
	
	public String meaning;
	
	public int level;
	
	public Stats stats;
	
	protected Item (JSONObject obj, Type type)
		throws JSONException
	{
		this.type = type;
		
		character = Util.getString (obj, "character");
		meaning = Util.getString (obj, "meaning");
		level = Util.getInt (obj, "level");
		
		stats = new Stats (obj.getJSONObject ("stats"), hasReading ());
	}
	
	protected boolean hasReading ()
	{
		return true;
	}
}
