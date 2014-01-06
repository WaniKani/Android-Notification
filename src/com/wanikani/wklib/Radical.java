package com.wanikani.wklib;

import org.json.JSONException;
import org.json.JSONObject;

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

	public static final long serialVersionUID = 1L;
	
	public static final Item.Factory<Radical> FACTORY = new Factory ();

	public static final Item.Factory<Item> ITEM_FACTORY = new ItemFactory ();
	
	public String image;
	
	private String hyphenatedMeaning;
	
	public Radical (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.RADICAL);
		
		image = Util.getString (obj, "image");
		
		fixup ();
	}	
	
	public Radical ()
	{
		super (Item.Type.RADICAL);
	}
	
	@Override
	protected boolean hasReading ()
	{
		return false;
	}

	public String getItemURLComponent ()
	{
		return hyphenatedMeaning;
	}

	protected String getClassURLComponent ()
	{
		return "radicals";
	}
	
	@Override
	public void fixup ()
	{
		hyphenatedMeaning = meaning;
		meaning = meaning.replace ('-', ' ');
	}
}
