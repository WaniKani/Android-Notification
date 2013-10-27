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
	
	private static final String DONE = PREFIX + "DONE";
	
	private static final int VERSION = 1;
	
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
		boolean change;
		int i;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);
		if (prefs.getInt (DONE, 0) >= VERSION)
			return;
		
		hdb = new HistoryDatabase (ctxt);
		try {
			hdb.openW ();
			ui = conn.getUserInformation ();
			levelups = hdb.getLevelups ();
			for (i = 0; i < 2 * ui.level; i++) {
				change = false;
				slup = getSuspectLevelups (levelups, ui.level);			
				for (Integer l : slup)
					change |= fixLevel (hdb, ui, prefs, l, levelups);
				if (!change)
					break;
			}
			prefs.edit ().putInt (DONE, VERSION).commit ();
		} finally {
			hdb.close ();
		}
	}
	
	private boolean fixLevel (HistoryDatabase hdb, UserInformation ui, 
							  SharedPreferences prefs, int level, 
						      Map<Integer, Integer> levelups)
		throws IOException, SQLException
	{
		ItemLibrary<Item> lib;
		Integer plday;
		int day, minday;
		
		plday = levelups.get (level - 1);
		if (plday == null)
			return false;
		minday = plday + MIN_DAYS;
		
		lib = new ItemLibrary<Item> ();
		lib.add (conn.getRadicals (level));
		lib.add (conn.getKanji (level));
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
						ans.add (i);
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
