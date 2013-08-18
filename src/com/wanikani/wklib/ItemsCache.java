package com.wanikani.wklib;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ItemsCache implements Serializable {
	
	public static final long serialVersionUID = 1L;
	
	public class LevelCache<T extends Item> implements Serializable {
		
		public static final long serialVersionUID = 1L;		
		
		Map<Integer, ItemLibrary<T>> ht;
		
		public LevelCache ()
		{
			ht = new Hashtable<Integer, ItemLibrary<T>> ();
		}
		
		public synchronized ItemLibrary<T> get (int level)
		{
			return ht.get (level);
		}
		
		
		public synchronized int [] get (ItemLibrary<T> lib, int level [])
		{
			List<Integer> missing;
			ItemLibrary<T> clib;
			int i;
			
			missing = new Vector<Integer> (level.length);
			for (i = 0; i < level.length; i++) {
				clib = ht.get (level [i]);
				if (clib != null)
					lib.add (clib);
				else
					missing.add (level [i]);
			}
			
			level = new int [missing.size ()];
			for (i = 0; i < level.length; i++)
				level [i] = missing.get (i);
			
			return level;
		}
		
		public synchronized ItemLibrary<T> put (ItemLibrary<T> lib)
		{
			Map<Integer, ItemLibrary<T>> map;
			ItemLibrary<T> tlib;
			
			map = new Hashtable<Integer, ItemLibrary<T>> ();
			for (T t : lib.list) {
				tlib = map.get (t.level);
				if (tlib == null)
					map.put (t.level, tlib = new ItemLibrary<T> ());
				tlib.list.add (t);
			}
			
			ht.putAll (map);
			
			return lib;
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
}
