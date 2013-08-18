package com.wanikani.wklib;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;

public class ItemLibrary<T extends Item> implements Serializable {

	public static final long serialVersionUID = 1L;
	
	public List<T> list;
	
	public ItemLibrary (Item.Factory<T> f, JSONArray array)
		throws JSONException
	{
		int i;
		
		list = new Vector<T> (array.length ());
		for (i = 0; i < array.length (); i++)
			list.add (f.deserialize (array.getJSONObject (i)));
	}
	
	public ItemLibrary (ItemLibrary<? extends T> lib)
	{
		list = new Vector<T> ();
		
		add (lib);
	}

	public ItemLibrary (T item)
	{
		list = new Vector<T> ();
		
		add (item);
	}
	
	public ItemLibrary ()
	{
		list = new Vector<T> ();
	}
	
	public ItemLibrary<T> add (ItemLibrary<? extends T> lib)
	{
		list.addAll (lib.list);
		
		return this;
	}

	public ItemLibrary<T> add (T item)
	{
		list.add (item);
		
		return this;
	}
}
