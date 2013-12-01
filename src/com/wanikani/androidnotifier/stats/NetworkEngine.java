package com.wanikani.androidnotifier.stats;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import android.os.AsyncTask;
import android.view.View;

import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.Vocabulary;

public class NetworkEngine {

	public static interface Chart {

		public void bind (NetworkEngine netwe, MainActivity main, View view);
		
		public void unbind ();
		
		public void startUpdate (int levels);
		
		public void update (EnumSet<Item.Type> types);
	
		public void newRadical (ItemLibrary<Radical> radicals);

		public void newKanji (ItemLibrary<Kanji> kanji);

		public void newVocab (ItemLibrary<Vocabulary> vocabs);
		
		public boolean scrolling ();
	}
	
	private class DataSource implements IconizableChart.DataSource {

		Connection.Meter meter;
		
		EnumSet<Item.Type> types;
		
		PendingTask task;
		
		public DataSource (Connection.Meter meter, EnumSet<Item.Type> types)
		{
			this.meter = meter;
			this.types = types;
		}
		
		@Override
		public void loadData () 
		{
			if (task == null || !task.completed)
				task = refresh (meter, types);
		}
	}
	
	/**
	 * The asynch task that loads all the info from WK, feeds the database and
	 * publishes the progress.
	 */
	private class Task extends AsyncTask<Void, Integer, Boolean> {

		/// The connection
		private Connection conn;

		/// The task description
		private PendingTask task;
		
		/// Number of levels to load at once
		private static final int BUNCH_SIZE = 50;
		
		public Task (Connection conn, PendingTask task)
		{
			this.conn = conn;
			this.task = task;
		}
				
		/**
		 * The reconstruction process itself. It opens a DB reconstruction object,
		 * loads all the items, and retrieves the new core stats 
		 * @return true if everything is ok
		 */
		@Override
		protected Boolean doInBackground (Void... v)
		{
			ItemLibrary<Radical> rlib;
			ItemLibrary<Kanji> klib;
			ItemLibrary<Vocabulary> vlib;
			int i, j, levels, bunch [];

			if (task.types.isEmpty ())
				return true;
			
			try {
				levels = conn.getUserInformation (task.meter).level;
			} catch (IOException e) {
				return false;
			}
			
			for (Chart c : charts)
				c.startUpdate (levels);

			publishProgress ((100 * 1) / (levels + 2));

			try {
				if (task.types.contains (Item.Type.RADICAL)) {
					rlib = conn.getRadicals (task.meter, false);
					for (Chart c : charts)
						c.newRadical (rlib);
				}
			} catch (IOException e) {
				return false;
			} 

			try {
				if (task.types.contains (Item.Type.KANJI)) {
					klib = conn.getKanji (task.meter, false);
					for (Chart c : charts)
						c.newKanji (klib);
				}
			} catch (IOException e) {
				return false;
			} 

			publishProgress ((100 * 2) / (levels + 2));
			
			try {
				if (task.types.contains (Item.Type.VOCABULARY)) {
					i = 1;
					while (i <= levels) {
						bunch = new int [Math.min (BUNCH_SIZE, levels - i + 1)];
						for (j = 0; j < BUNCH_SIZE && i <= levels; j++)
							bunch [j] = i++;
						vlib = conn.getVocabulary (task.meter, bunch, false);
						for (Chart c : charts)
							c.newVocab (vlib);
						publishProgress ((100 * (i - 1)) / (levels + 2));
					}
				}
			} catch (IOException e) {
				return false;
			} 

			return true;
		}	
				
		@Override
		protected void onProgressUpdate (Integer... i)
		{
			/* Not used yet */
		}
		
		/**
		 * Ends the reconstruction process by telling everybody how it went.
		 * @param ok if everything was ok
		 */
		@Override
		protected void onPostExecute (Boolean ok)
		{
			if (ok)
				for (Chart c : charts)
					c.update (task.types);
			
			completed (task, ok);
		}
	}
	
	private static class PendingTask {
		
		Connection.Meter meter;
		
		EnumSet <Item.Type> types;
		
		boolean completed;
		
		public PendingTask (Connection.Meter meter, EnumSet<Item.Type> types)
		{
			this.meter = meter;
			this.types = types;
		}
		
		public boolean clear (EnumSet<Item.Type> atypes)
		{
			for (Item.Type t : atypes)
				types.remove (t);
			
			return !types.isEmpty ();
		}
		
	}

	private List<PendingTask> tasks;
	
	private Connection conn;
	
	private List<Chart> charts;
	
	private EnumSet<Item.Type> availableTypes; 

	public NetworkEngine ()
	{
		charts = new Vector<Chart> ();
		
		availableTypes = EnumSet.noneOf (Item.Type.class);
		tasks = new Vector<PendingTask> ();
	}
	
	public IconizableChart.DataSource getDataSource (Connection.Meter meter, EnumSet<Item.Type> types)
	{
		return new DataSource (meter, types);
	}
	
	private PendingTask refresh (Connection.Meter meter, EnumSet<Item.Type> types)
	{
		PendingTask task;
		boolean empty;
		
		empty = tasks.isEmpty ();
		task = new PendingTask (meter, types);
		tasks.add (task);
		if (empty)
			runQueue ();
		
		return task;
	}

	private void completed (PendingTask task, boolean ok)
	{
		task.completed = true;
		availableTypes.addAll (task.types);
		tasks.remove (0);
		runQueue ();
	}
	
	private void runQueue ()
	{
		PendingTask task;
				
		if (!tasks.isEmpty ()) {
			task = tasks.get (0);
			task.clear (availableTypes);
			new Task (conn, task).execute ();				
		}		
	}
	
	public void add (Chart chart)
	{
		charts.add (chart);
	}
		
	public void bind (MainActivity main, View view)
	{
		conn = main.getConnection ();
		
		for (Chart chart : charts)
			chart.bind (this, main, view);
	}
	
	public void unbind ()
	{
		for (Chart chart : charts)
			chart.unbind ();
	}
	
	public boolean scrolling ()
	{
		for (Chart chart : charts)
			if (chart.scrolling ())
				return true;
		
		return false;
	}
}
