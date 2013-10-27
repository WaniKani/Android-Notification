package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
	
	private static final int KANJI_MIN_DAYS = 3;
	
	private static final int RADICALS_MIN_DAYS = 7;
	
	public static final int SUSPECT_DAYS = 4;

	private static final String PREFIX = DatabaseFixup.class + ".";
	
	private static final String SHOULD_RUN = PREFIX + "SHOUD_RUN";
	
	public DatabaseFixup (Context ctxt, Connection conn)
	{
		this.ctxt = ctxt;
		this.conn = conn;
	}
	
	private void go ()
		throws IOException, SQLException
	{
		SharedPreferences prefs;
		UserInformation ui;
		Map<Integer, Integer> levelups;
		List<Integer> slup;
		HistoryDatabase hdb;
		boolean change;
		int i;
		
		hdb = new HistoryDatabase (ctxt);
		prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);

		/* Just in case it gets interrupted ... */
		prefs.edit ().putBoolean (SHOULD_RUN, true).commit ();
		try {
			hdb.openW ();
			ui = conn.getUserInformation ();
			levelups = hdb.getLevelups ();
			for (i = 0; i < 2 * ui.level; i++) {
				change = false;
				slup = getSuspectLevelups (levelups, ui.level);			
				for (Integer l : slup)
					change |= fixLevel (hdb, ui, l, levelups);
				if (!change)
					break;
			}
			
			prefs.edit ().putBoolean (SHOULD_RUN, false).commit ();
		} finally {
			hdb.close ();
		}
	}
	
	private boolean tryFix (HistoryDatabase hdb, UserInformation ui, ItemLibrary<Item> lib, 
							Map<Integer, Integer> levelups, int level, int minday)
	{
		int day;
		
		Collections.sort (lib.list, Item.SortByTime.INSTANCE_ASCENDING);
		for (Item i : lib.list) {
			day = ui.getDay (i.getUnlockedDate ());
			if (day >= minday) {
				hdb.updateLevelup (level, day);
				levelups.put (level, day);
				return true;
			}
		}		
		
		return false;
	}
	
	private boolean fixLevel (HistoryDatabase hdb, UserInformation ui, int level, 
						      Map<Integer, Integer> levelups)
		throws IOException, SQLException
	{
		Integer plday;
		
		plday = levelups.get (level - 1);
		if (plday == null)
			return false;
		
		if (tryFix (hdb, ui, new ItemLibrary<Item> (conn.getRadicals (level)), 
					levelups, level, plday + RADICALS_MIN_DAYS))
			return true;
				
		if (tryFix (hdb, ui, new ItemLibrary<Item> (conn.getKanji (level)), 
				levelups, level, plday + KANJI_MIN_DAYS))
			return true;

		return false;
	}
	
	private static List<Integer> getSuspectLevelups (Map<Integer, Integer> levelups, int levels)
	{
		List<Integer> ans;
		Integer lday, cday;
		int i, delta;
		
		lday = null;
		ans = new Vector<Integer> ();
		for (i = 1; i <= levels; i++) {
			cday = levelups.get (i);
			if (cday != null) {
				if (lday != null) {
					delta = cday - lday;
					if (delta < SUSPECT_DAYS)
						ans.add (i);
				}
				lday = cday;
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
		
		slups = getSuspectLevelups (cs.levelups, level);
		
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
