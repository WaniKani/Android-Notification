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

public class Kanji extends Item {
	
	static class Factory implements Item.Factory<Kanji> {

		public Kanji deserialize (JSONObject obj)
			throws JSONException
		{
			return new Kanji (obj);
		}
	}
	
	static class ItemFactory implements Item.Factory<Item> {

		public Item deserialize (JSONObject obj)
			throws JSONException
		{
			return new Kanji (obj);
		}
	}

	public static final Item.Factory<Kanji> FACTORY = new Factory ();

	public static final Item.Factory<Item> ITEM_FACTORY = new ItemFactory ();

	public static enum Reading {
		
		ONYOMI, 
		
		KUNYOMI,
		
		NANORI; 
		
		public static Reading fromString (String s)
		{
			if (s.equals ("onyomi"))
				return ONYOMI;
			else if (s.equals ("kunyomi"))
				return KUNYOMI;
			else if (s.equals ("nanori"))
				return NANORI;
			else
				return null;
		}
	};
	
	public static final long serialVersionUID = 1L;

	public String onyomi;
	
	public String kunyomi;
	
	public String nanori;
	
	public Reading importantReading;
		
	public Kanji (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.KANJI);

		String s;
		
		onyomi = Util.getString (obj, "onyomi");
		kunyomi = Util.getString (obj, "kunyomi");
		nanori = Util.getString (obj, "nanori");
		s = Util.getString (obj, "important_reading");
		importantReading = Reading.fromString (s);
		if (importantReading == null)
			importantReading = Reading.ONYOMI;
	}
	
	public Kanji ()
	{
		super (Item.Type.KANJI);
	}
		
	public Item deserialize (JSONObject obj)
		throws JSONException
	{
		return new Kanji (obj);
	}

	@Override
	protected String getClassURLComponent ()
	{
		return "kanji";
	}
	
	public boolean matches (String s)
	{
		return super.matches (s) ||
				(onyomi != null && onyomi.contains (s)) ||
				(kunyomi != null && kunyomi.contains (s));
	}
	
}
;