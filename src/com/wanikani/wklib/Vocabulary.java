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

public class Vocabulary extends Item {

	private static class Factory implements Item.Factory<Vocabulary> {

		public Vocabulary deserialize (JSONObject obj)
			throws JSONException
		{
			return new Vocabulary (obj);
		}
	}

	private static class ItemFactory implements Item.Factory<Item> {

		public Vocabulary deserialize (JSONObject obj)
			throws JSONException
		{
			return new Vocabulary (obj);
		}
	}

	public final static Item.Factory<Vocabulary> FACTORY = new Factory ();
	
	public final static Item.Factory<Item> ITEM_FACTORY = new ItemFactory ();

	public String kana;
	
	public Vocabulary (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.VOCABULARY);
		
		kana = Util.getString (obj, "kana");
	}
	
	protected String getClassURLComponent ()
	{
		return "vocabulary";
	}
	
	public boolean matches (String s)
	{
		return super.matches (s) ||
				kana.contains (s);
	}
}
