package com.wanikani.wklib;

import java.io.Serializable;

public interface ItemsCacheInterface extends Serializable {

	public interface Cache<T extends Item> extends Serializable {
		
		public ItemLibrary<T> get (int level);

		public int [] get (ItemLibrary<T> lib, int level []);
		
		public ItemLibrary<T> put (ItemLibrary<T> lib);		
	}
	
	public Cache<Radical> getRadicals ();
	
	public Cache<Kanji> getKanji ();
	
	public Cache<Vocabulary> getVocab ();
	
}
