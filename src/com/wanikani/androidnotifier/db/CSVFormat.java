package com.wanikani.androidnotifier.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import com.wanikani.wklib.SRSDistribution;

public class CSVFormat implements Format {

	private static final String SEPARATOR = ";";
	
	private static final String DATE_TAG = "Date";
	
	private static final String VERSION_TAG = "Version";
	
	private static final String DAY_TAG = "Day";
	
	private static final int COLUMNS = 17;
	
	private static final int VERSION = 1;
	
	File file;
	
	Context ctxt;
	
	private CSVFormat (Context ctxt)
	{
		this.ctxt = ctxt;
	}
	
	public static Format newInstance (Context ctxt)
	{
		return new CSVFormat (ctxt);
	}
	
	public void export (OutputStream os)
		throws IOException, SQLException
	{
		HistoryDatabase db;
		
		synchronized (HistoryDatabase.MUTEX) {
			db = new HistoryDatabase (ctxt);

			try {
				db.openR ();
			
				doExport (new PrintStream (os), db);			
			} finally {
				db.close ();
			}
		}
	}
	
	protected void doExport (PrintStream os, HistoryDatabase db)
		throws IOException, SQLException
	{
		Map<Integer, HistoryDatabase.LevelInfo> levelInfo;
		HistoryDatabase.LevelInfo li;
		Map<Integer, Integer> levelupDays;
		SRSDistribution srs;
		Integer newLevel;
		int level, day;
		Cursor c;		

		levelInfo = db.getLevelInfo ();
		levelupDays = invert (db.getLevelInfo ());
		
		var (os, VERSION_TAG, VERSION);
		var (os, DATE_TAG, new Date ());

		heading (os);
		level = 1;
		c = null;
		try {			
			c = db.selectFacts ();

			while (c.moveToNext ()) {				
				day = HistoryDatabase.Facts.getDay (c);
				srs = HistoryDatabase.Facts.getSRSDistribution (c);

				newLevel = levelupDays.get (day);
				if (newLevel != null) {
					level = newLevel;
					li = levelInfo.get (level);
				} else
					li = null;

				switch (HistoryDatabase.Facts.getType (c)) {
				case COMPLETE:
					dumpComplete (os, day, level, li, srs);
					break;
					
				case PARTIAL:
					dumpPartial (os, day, level, li, srs);
					break;
					
				case MISSING:
					dumpMissing (os, day, level);
				}
			}
				
		} finally { 
			if (c != null)
				c.close ();
		}
	}
	
	protected void var (PrintStream os, String tag, Object value)
		throws IOException
	{
		row (os, tag, value);
	}
	
	protected void heading (PrintStream os)
	{
		row (os, DAY_TAG, "Level",
			 "Apprentice Radicals", "Guru Radicals", "Master Radicals", 
			 	"Enlightened Radicals", "Burned Radicals", "Unlocked Radicals",
			 "Apprentice Kanji", "Guru Kanji", "Master Kanji", 
			 	"Enlightened Kanji", "Burned Kanji", "Unlocked Kanji",
			 "Apprentice Vocab", "Guru Vocab", "Master Vocab", 
			 	"Enlightened Vocab", "Burned Vocab", "Unlocked Vocab",
			 "Vacation days");			 
	}
	
	protected void dumpComplete (PrintStream os, int day, int level, HistoryDatabase.LevelInfo li, SRSDistribution srs)
	{
		row (os, day, level,
			 srs.apprentice.radicals, srs.guru.radicals, srs.master.radicals,
			 srs.enlighten.radicals, srs.burned.radicals,
			 srs.apprentice.radicals + srs.guru.radicals + srs.master.radicals + srs.enlighten.radicals,
			 
			 srs.apprentice.kanji, srs.guru.kanji, srs.master.kanji,
			 srs.enlighten.kanji, srs.burned.kanji,
			 srs.apprentice.kanji + srs.guru.kanji + srs.master.kanji + srs.enlighten.kanji,
			 
			 srs.apprentice.vocabulary, srs.guru.vocabulary, srs.master.vocabulary,
			 srs.enlighten.vocabulary, srs.burned.vocabulary,
			 srs.apprentice.vocabulary + srs.guru.vocabulary + srs.master.vocabulary + srs.enlighten.vocabulary,
			 li != null ? li.vacation : "");
	}
	
	protected void dumpPartial (PrintStream os, int day, int level, HistoryDatabase.LevelInfo li, SRSDistribution srs)
	{
		row (os, day, level,
			 null, null, null, null, srs.burned.radicals, srs.apprentice.radicals,
			 null, null, null, null, srs.burned.kanji, srs.apprentice.kanji,
			 null, null, null, null, srs.burned.vocabulary, srs.apprentice.vocabulary,
			 li != null ? li.vacation : "");				
	}
	
	protected void dumpMissing (PrintStream os, int day, int level)
	{
		row (os, day, level);
	}
	
	protected void row (PrintStream os, Object... data)
	{
		int i;
		
		for (i = 0; i < data.length; i++) {
			if (i > 0)
				os.print (SEPARATOR);
			if (data [i] != null)
				os.print (wrap (data [i]));
		}
		
		while (i++ < COLUMNS)
			os.print (SEPARATOR);
		
		os.print ("\r\n");
	}	
	
	private String wrap (Object o)
	{
		String s;
		int i;
		
		s = o.toString ();
		for (i = s.indexOf ('"'); i >= 0; i = s.indexOf ('"', i + 2))
			s = s.substring (0, i) + '"' + s.substring (i);
		
		if (s.contains (" "))
			s = "\"" + s + "\"";
	
		return s;
	}
	
	protected static Map<Integer, Integer> invert (Map<Integer, HistoryDatabase.LevelInfo> levelInfo)
	{
		Map<Integer, Integer> ans;
		HistoryDatabase.LevelInfo li;
		int i;
		
		ans = new Hashtable<Integer, Integer> ();
		ans.put (0, 1);	/* Want to make sure at least this entry exists */
		i = 2;
		while (true) {
			li = levelInfo.get (i);
			if (li == null)
				break;
			ans.put (li.day, i);
			i++;
		}
		
		return ans;
	}
	
	public String getType ()
	{
		return "text/csv";
	}
	
}
