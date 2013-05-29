package com.wanikani.wklib;

import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class Radical extends Item {

	URL image;
	
	public Radical (JSONObject obj)
		throws JSONException
	{
		super (obj, Item.Type.RADICAL);
		
		image = Util.getURL (obj, "image");
	}	
}
