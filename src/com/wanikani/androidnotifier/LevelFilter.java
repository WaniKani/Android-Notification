package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import android.os.AsyncTask;

import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;

public class LevelFilter implements Filter {
	
	private class Task extends AsyncTask<Void, ItemLibrary<Item>, Boolean > {
		
		Connection conn;
		
		int level;
		
		List<Item> allItems;
		
		boolean apprentice;
		
		public Task (Connection conn, int level, boolean apprentice)
		{
			this.conn = conn;
			this.level = level;
			this.apprentice = apprentice;
			
			allItems = new Vector<Item> ();
		}
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			ItemLibrary<Item> lib;
			List<Radical> imgrad;
			Radical rad;
			Iterator<Item> i;
			boolean ok;
			
			ok = true;
			lib = new ItemLibrary<Item> ();
			imgrad = new Vector<Radical> ();
			try {
				lib.addAll (conn.getRadicals (level));
				i = lib.list.iterator ();
				while (i.hasNext ()) {
					rad = (Radical) i.next ();
					if (rad.character == null) {
						imgrad.add (rad);
						i.remove ();
					}
				}
				publishProgress (new ItemLibrary<Item> (lib));
			} catch (IOException e) {
				ok = false;
			}
			
			for (Radical r : imgrad) {
				try {
					conn.loadImage (r);
				} catch (IOException e) {
					r.character = "?";
					ok = false;
				}				
				publishProgress (new ItemLibrary<Item> (r));
			}
			
			lib = new ItemLibrary<Item> ();
			try {
				lib.addAll (conn.getKanji (level));
				publishProgress (lib);
			} catch (IOException e) {
				ok = false;
			}
			
			lib = new ItemLibrary<Item> ();
			try {
				lib.addAll (conn.getVocabulary (level));
				publishProgress (lib);
			} catch (IOException e) {
				ok = false;
			}			

			return ok;
		}	
		
		@Override
		protected void onProgressUpdate (ItemLibrary<Item>... lib)
		{
			allItems.addAll (lib [0].list);
			update (this, lib [0].list, apprentice);
		}
						
		@Override
		protected void onPostExecute (Boolean ok)
		{
			done (this, allItems, level, ok);
		}
	}

	ItemsFragment itemf;
	
	Hashtable<Integer, Task> pending;
	
	Hashtable<Integer, List<Item>> ht;
	
	Task task;
	
	public LevelFilter (ItemsFragment itemf)	
	{
		this.itemf = itemf;
		
		pending = new Hashtable<Integer, Task> ();
		ht = new Hashtable<Integer, List<Item>> ();		
	}
	
	public void select (int level, boolean apprentice)
	{
		List<Item> ans;
		Task ptask;
		
		ptask = pending.get (level);
		ans = ht.get (level);
		if (ans != null) {
			itemf.setData (filter (ans, apprentice), true);
			itemf.selectLevel (this, level, false);
		} else if (ptask == null) {
			itemf.clearData ();
			itemf.selectLevel (this, level, true);

			task = new Task (itemf.getConnection (), level, apprentice);
			task.execute ();
			pending.put (level, task);
		} else
			ptask = task;
	}
	
	private void update (Task stask, List<Item> items, boolean apprentice)
	{
		if (stask == task)
			itemf.addData (this, filter (items, apprentice));
	}
	
	private void done (Task stask, List<Item> allItems, int level, boolean ok)
	{
		if (ok)
			ht.put (level, allItems);
		pending.remove (level);
		
		if (stask == task) 
			itemf.selectLevel (this, level, false);
	}
	
	public void stopTask ()
	{
		task = null;
	}
	
	private static List<Item> filter (List<Item> items, boolean apprentice)
	{
		Iterator<Item> i;
		Item item;
		
		if (apprentice) {
			items = new Vector<Item> (items);
			i = items.iterator ();
			while (i.hasNext ()) {
				item = i.next ();
				if (item.stats != null && item.stats.srs != SRSLevel.APPRENTICE)
					i.remove ();
			}
		}
		
		return items;
	}
}
