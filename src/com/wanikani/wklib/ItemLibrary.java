package com.wanikani.wklib;

import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;

public class ItemLibrary<T extends Item> {

	public List<T> list;
	
	public ItemLibrary (Item.Factory<T> f, JSONArray array)
		throws JSONException
	{
		int i;
		
		list = new Vector<T> (array.length ());
		for (i = 0; i < array.length (); i++)
			list.add (f.deserialize (array.getJSONObject (i)));
	}
	
	public ItemLibrary (ItemLibrary<? extends T>... libs)
	{
		list = new Vector<T> ();
		for (ItemLibrary<? extends T> lib : libs)
			list.addAll (lib.list);
	}
	
}
