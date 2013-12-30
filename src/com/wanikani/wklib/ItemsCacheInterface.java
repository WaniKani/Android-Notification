package com.wanikani.wklib;

import java.io.Serializable;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

public interface ItemsCacheInterface extends Serializable {

	public enum Quality {
		
		GOOD,
		
		MISSING
		
	}
	
	public static class LevelData<T extends Item> implements Serializable {
		
		static final long serialVersionUID = 1L; 
		
		public Quality quality;
		
		public Date date;
		
		public ItemLibrary<T> lib;
		
		public LevelData (Date date, ItemLibrary<T> lib)
		{
			quality = Quality.GOOD;
			this.date = date;
			this.lib = lib;
		}
		
		public LevelData ()
		{
			quality = Quality.MISSING;
		}
		
		public static<U extends Item> Map<Integer, LevelData <U>> createMap (int levels [])
		{
			Map<Integer, LevelData <U>> ans;
			int i;
			
			ans = new Hashtable<Integer, LevelData <U>> ();
			for (i = 0; i < levels.length; i++)
				ans.put (levels [i], new LevelData<U> ());
			
			return ans;
		}
		
	}
	
	public interface Cache<T extends Item> extends Serializable {
		
		public LevelData<T> get (int level);

		public void get (Map<Integer, LevelData <T>> data);
		
		public void put (LevelData <T> data);		
	}
	
	
	public<T extends Item> Cache<T> get (Item.Type type);	
}
