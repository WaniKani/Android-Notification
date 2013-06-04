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

public class CriticalFilter implements Filter {

	private class Task extends AsyncTask<Void, ItemLibrary<Item>, Boolean > {
		
		Connection conn;

		List<Item> allItems;
		
		public Task (Connection conn)
		{
			this.conn = conn;

			allItems = new Vector<Item> ();
		}
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			ItemLibrary<Item> lib;
			List<Radical> imgrad;
			Item item;
			Iterator<Item> i;
			boolean ok;
			
			ok = true;
			lib = new ItemLibrary<Item> ();
			imgrad = new Vector<Radical> ();
			try {
				lib = conn.getCriticalItems ();
				i = lib.list.iterator ();
				while (i.hasNext ()) {
					item = i.next ();
					if (item.character == null &&
						item.type == Item.Type.RADICAL) {
						imgrad.add ((Radical) item);
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
			
			return ok;
		}	
		
		@Override
		protected void onProgressUpdate (ItemLibrary<Item>... lib)
		{
			allItems.addAll (lib [0].list);
			update (this, lib [0].list);
		}
						
		@Override
		protected void onPostExecute (Boolean ok)
		{
			done (this, allItems, ok);
		}
	}

	ItemsFragment itemf;
	
	List<Item> citems;
	
	Task task;
	
	public CriticalFilter (ItemsFragment itemf)	
	{
		this.itemf = itemf;
	}
	
	public void select ()
	{
		if (citems == null) {
			itemf.clearData ();
			itemf.selectOtherFilter (this, true, true);			

			task = new Task (itemf.getConnection ());
			task.execute ();
		} else {
			itemf.setData (citems, true);
			itemf.selectOtherFilter (this, true, false);
		}
	}
	
	private void update (Task stask, List<Item> items)
	{
		if (stask == task)
			itemf.addData (this, items);
	}
	
	private void done (Task stask, List<Item> allItems, boolean ok)
	{
		if (ok)
			citems = allItems;

		if (stask == task)
			itemf.selectOtherFilter (this, true, false);
	}
	
	public void stopTask ()
	{
		task = null;
	}
}
