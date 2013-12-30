package com.wanikani.wklib;

import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.wanikani.wklib.ItemsCacheInterface.LevelData;

public class ItemsCache implements ItemsCacheInterface {
	
	static final long serialVersionUID = 2L; 

	public class LevelCache<T extends Item> implements ItemsCacheInterface.Cache<T> {
		
		static final long serialVersionUID = 2L; 
		
		Map<Integer, ItemsCacheInterface.LevelData<T>> ht;
		
		public LevelCache ()
		{
			ht = new Hashtable<Integer, LevelData<T>> ();
		}
		
		public LevelData<T> get (int level)
		{
			LevelData<T> ans;
			
			ans = ht.get (level);
			
			return ans != null ? ans : new LevelData<T> ();
		}

		public void get (Map<Integer, LevelData <T>> data)
		{
			for (int level : data.keySet ())
				data.put (level, get (level));
		}
		
		public void put (LevelData <T> data)
		{
			Map<Integer, LevelData <T>> map;
			LevelData<T> ld;
			
			map = new Hashtable<Integer, LevelData <T>> ();
			for (T t : data.lib.list) {
				ld = map.get (t.level);
				if (ld == null) {
					ld = new LevelData<T> (data.date, new ItemLibrary<T> ());
					map.put (t.level, ld);
				}
				ld.lib.add (t);
			}
				
			ht.putAll (map);
		}		
	}

	LevelCache<Radical> radicals;
	
	LevelCache<Kanji> kanji;
	
	LevelCache<Vocabulary> vocab;
	
	public ItemsCache ()
	{
		radicals = new LevelCache<Radical> ();
		kanji = new LevelCache<Kanji> ();
		vocab = new LevelCache<Vocabulary> ();
	}
	
	@Override
	public<T extends Item> Cache<T> get (Item.Type type)
	{
		switch (type) {
		case RADICAL:
			return (Cache<T>) radicals;
			
		case KANJI:
			return (Cache <T>) kanji;
			
		case VOCABULARY:
			return (Cache <T>) vocab;
		}
		
		return null;
	}
	
}
