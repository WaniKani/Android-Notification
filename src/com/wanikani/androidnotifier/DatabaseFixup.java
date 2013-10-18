package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.preference.PreferenceManager;

import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.UserInformation;

public class DatabaseFixup {

	Context ctxt;
	
	Connection conn;
	
	private static final int MIN_DAYS = 3;
	
	private static final String PREFIX = DatabaseFixup.class + ".";
	
	private static final String LEVEL_DONE = PREFIX + "LEVEL_DONE_";
	
	private DatabaseFixup (Context ctxt, Connection conn)
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
		
		prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);
		
		hdb = new HistoryDatabase (ctxt);
		try {
			hdb.openW ();
			ui = conn.getUserInformation ();
			levelups = hdb.getLevelups ();
			slup = getSuspectLevelups (levelups, ui.level);
			for (Integer l : slup)
				fixLevel (hdb, ui, prefs, l);
		} finally {
			hdb.close ();
		}
	}
	
	private void fixLevel (HistoryDatabase hdb, UserInformation ui, 
						   SharedPreferences prefs, int level)
		throws IOException, SQLException
	{
		ItemLibrary<Item> lib;
		String lpref;
		int day;
		
		lpref = LEVEL_DONE + level;
		
		if (prefs.getBoolean (lpref, false))
			return;
		
		lib = new ItemLibrary<Item> ();
		lib.add (conn.getRadicals (level));
		lib.add (conn.getKanji (level));
		Collections.sort (lib.list, Item.SortByTime.INSTANCE_ASCENDING);

		if (lib.list.size () >= 2) {
			day = ui.getDay (lib.list.get (1).getUnlockedDate ());
			hdb.updateLevelup (level, day);
		}
		
		prefs.edit ().putBoolean (lpref, true).commit ();
	}
	
	private List<Integer> getSuspectLevelups (Map<Integer, Integer> levelups, int levels)
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
					if (delta < MIN_DAYS)
						ans.add (i - 1);
				}
				lday = cday;
			}
		}
		
		return ans;
	}
	
	public static void run (Context ctxt, Connection conn)
		throws IOException, SQLException
	{
		new DatabaseFixup (ctxt, conn).go ();
	}
	
}
