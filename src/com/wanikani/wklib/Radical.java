package com.wanikani.wklib;

import java.net.URL;

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

	public final static Factory FACTORY = new Factory ();

	URL image;
	
	public Radical (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.RADICAL);
		
		image = Util.getURL (obj, "image");
	}	

	@Override
	protected boolean hasReading ()
	{
		return false;
	}
}
