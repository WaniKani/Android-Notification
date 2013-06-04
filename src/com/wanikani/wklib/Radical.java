package com.wanikani.wklib;

import org.json.JSONException;
import org.json.JSONObject;

import com.wanikani.wklib.Kanji.Factory;
import com.wanikani.wklib.Kanji.ItemFactory;

import android.graphics.Bitmap;

public class Radical extends Item {

	private static class Factory implements Item.Factory<Radical> {

		public Radical deserialize (JSONObject obj)
			throws JSONException
		{
			return new Radical (obj);
		}
	}

	private static class ItemFactory implements Item.Factory<Item> {

		public Radical deserialize (JSONObject obj)
			throws JSONException
		{
			return new Radical (obj);
		}
	}

	public static final Item.Factory<Radical> FACTORY = new Factory ();

	public static final Item.Factory<Item> ITEM_FACTORY = new ItemFactory ();

	public Bitmap bitmap;
	
	public String image;
	
	public Radical (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.RADICAL);
		
		image = Util.getString (obj, "image");
	}	

	@Override
	protected boolean hasReading ()
	{
		return false;
	}
}
