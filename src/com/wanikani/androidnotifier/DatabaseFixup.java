package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.androidnotifier.db.HistoryDatabase.CoreStats;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.UserInformation;


public class DatabaseFixup {

	public interface Listener {
		
		public void done (boolean ok);
		
	}
	
	private class Task extends AsyncTask<Void, Void, Boolean> {
		
		Listener listener;
		
		Task (Listener listener)
		{
			this.listener = listener;
		}
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			try {
				go ();
			} catch (SQLException e) {
				return false;
			} catch (IOException e) {
				return false; 
			}
			
			return true;
		}	
						
		@Override
		protected void onPostExecute (Boolean ok)

		{
			listener.done (ok);
		}
	}

	Context ctxt;
	
	Connection conn;
	
	Connection.Meter meter;

	private static final int KANJI_MIN_DAYS = 3;
	
	private static final int RADICALS_MIN_DAYS = 7;
	
	public static final int SUSPECT_DAYS = 4;

	private static final String PREFIX = DatabaseFixup.class + ".";
	
	private static final String SHOULD_RUN = PREFIX + "SHOUD_RUN";
	
	public DatabaseFixup (Context ctxt, Connection conn)
	{
		this.ctxt = ctxt;
		this.conn = conn;
		meter = MeterSpec.T.RECONSTRUCT_DIALOG.get (ctxt);
	}
	
	private void go ()
		throws IOException, SQLException
	{
		SharedPreferences prefs;
		UserInformation ui;
		Map<Integer, HistoryDatabase.LevelInfo> levelInfo;
		List<Integer> slup;
		HistoryDatabase hdb;
		boolean change;
		int i;
		
		synchronized (HistoryDatabase.MUTEX) {
			hdb = new HistoryDatabase (ctxt);
			prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);

			/* Just in case it gets interrupted ... */
			prefs.edit ().putBoolean (SHOULD_RUN, true).commit ();
			try {
				hdb.openW ();
				ui = conn.getUserInformation (meter);
				levelInfo = hdb.getLevelInfo ();
				for (i = 0; i < 2 * ui.level; i++) {
					change = false;
					slup = getSuspectLevelups (levelInfo, ui.level);			
					for (Integer l : slup)
						change |= fixLevel (hdb, ui, l, levelInfo);
					if (!change)
						break;
				}
			
				prefs.edit ().putBoolean (SHOULD_RUN, false).commit ();
			} finally {
				hdb.close ();
			}
		}
	}
	
	private boolean tryFix (HistoryDatabase hdb, UserInformation ui, ItemLibrary<Item> lib, 
							Map<Integer, HistoryDatabase.LevelInfo> levelInfo, int level, int minday)
	{
		HistoryDatabase.LevelInfo li;
		int day, vacation;
		
		Collections.sort (lib.list, Item.SortByTime.INSTANCE_ASCENDING);
		li = levelInfo.get (level);
		if (li != null)
			vacation = li.vacation;
		else
			vacation = 0;
		
		for (Item i : lib.list) {
			day = ui.getDay (i.getUnlockedDate ());
			if (day >= minday) {
				hdb.updateLevelup (level, day, vacation);
				levelInfo.put (level, new HistoryDatabase.LevelInfo (day, vacation));
				return true;
			}
		}		

		return false;
	}
	
	private boolean fixLevel (HistoryDatabase hdb, UserInformation ui, int level, 
						      Map<Integer, HistoryDatabase.LevelInfo> levelInfo)
		throws IOException, SQLException
	{
		HistoryDatabase.LevelInfo pli;
		
		pli = levelInfo.get (level - 1);
		if (pli == null)
			return false;
		
		if (tryFix (hdb, ui, new ItemLibrary<Item> (conn.getRadicals (meter, level)), 
					levelInfo, level, pli.day + RADICALS_MIN_DAYS))
			return true;
				
		if (tryFix (hdb, ui, new ItemLibrary<Item> (conn.getKanji (meter, level)), 
					levelInfo, level, pli.day + KANJI_MIN_DAYS))
			return true;

		return false;
	}
	
	private static List<Integer> getSuspectLevelups (Map<Integer, HistoryDatabase.LevelInfo> levelInfo, int levels)
	{
		HistoryDatabase.LevelInfo li;
		List<Integer> ans;
		Integer lday;
		int i, delta;
		
		lday = null;
		ans = new Vector<Integer> ();
		for (i = 1; i <= levels; i++) {
			li = levelInfo.get (i);
			if (li != null) {
				if (lday != null) {
					delta = li.day - lday;
					if (delta < SUSPECT_DAYS)
						ans.add (i);
				}
				lday = li.day;
			}
		}
		
		return ans;
	}
	
	public static int getSuspectLevels (Context ctxt, int level, CoreStats cs)
	{
		SharedPreferences prefs;
		List<Integer> slups;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);
		if (!prefs.getBoolean (SHOULD_RUN, true))
			return 0;
		
		slups = getSuspectLevelups (cs.levelInfo, level);
		
		return slups != null ? slups.size () : 0;
	}
	
	public static void run (Context ctxt, Connection conn)
		throws IOException, SQLException
	{
		new DatabaseFixup (ctxt, conn).go ();
	}
	
	public void asyncRun (Listener listener)
	{		
		new Task (listener).execute ();
	}
}

