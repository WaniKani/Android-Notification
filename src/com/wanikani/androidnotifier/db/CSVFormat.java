package com.wanikani.androidnotifier.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import com.wanikani.androidnotifier.db.HistoryDatabase.FactType;
import com.wanikani.androidnotifier.db.HistoryDatabase.Levels;
import com.wanikani.wklib.SRSDistribution;

/* 
 *  Copyright (c) 2013 Alberto Cuda
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class CSVFormat implements Format {
	
	public static class ImportState extends Format.ImportResult {
		
		int level;
		
		public ImportState ()
		{
			level = -1;
		}
		
	}

	private static final String SEPARATOR = ";";
	
	private static final String DATE_TAG = "Date";
	
	private static final String VERSION_TAG = "Version";
	
	private static final String DAY_TAG = "Day";
	
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
	
	@Override
	public ImportResult importFile (File file)
		throws IOException, SQLException
	{
		int day;
		
		day = passOne (file);
		
		return passTwo (file, day);
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
	
	protected int passOne (File file)
		throws IOException
	{
		BufferedReader is;
		int version, line, day, i;
		String tab [];

		line = 1;
		is = new BufferedReader (new FileReader (file));
		try {
			version = Integer.parseInt (rdVar (is, VERSION_TAG));
			if (version < 0 || version > VERSION)
				throw new IOException ("Unsupported dump version: " + version);

			line++;
			rdVar (is, DATE_TAG);
			
			line++;
			rdRow (is);
			
			day = -1;
			while (true) {
				line++;
				tab = rdRow (is);
				if (tab == null)
					break;
				
				/* Assume EOF */
				if (tab.length == 0)
					break;
				
				if (tab.length < 2 || tab [0].length () == 0 || tab [1].length () == 0)
					throw new IOException ("Expecting at least two columns at line " + line);
				
				if (Integer.parseInt (tab [0]) != ++day)
					throw new IOException ("Bad day sequence at line " + line);
								
				for (i = 0; i < tab.length; i++)
					if (tab [i].length () != 0)
						Integer.parseInt (tab [i]);				
			}
			
			while (tab != null) {
				if (tab.length != 0)
					throw new IOException ("Trailing stuff at line " + line);
				line++;
				tab = rdRow (is);
			}
			
		} catch (NumberFormatException e) {
			throw new IOException ("Bad integer at line " + line);
		} finally {
			try {
				is.close ();
			} catch (IOException e) {
				/* empty */
			}
		}
		
		return day;
	}
		
	protected ImportState passTwo (File file, int day)
			throws IOException, SQLException
	{
		HistoryDatabase hdb;
		BufferedReader is;
		Set<Integer> recl;
		String tab [];
		ImportState istate;
			
		istate = new ImportState ();
		
		if (day < 0)
			return istate;
		
		is = new BufferedReader (new FileReader (file));
		try {
			rdVar (is, VERSION_TAG);
			rdVar (is, DATE_TAG);
			rdRow (is);
			synchronized (HistoryDatabase.MUTEX) {
				hdb = new HistoryDatabase (ctxt);
				hdb.openW ();
				try {
					HistoryDatabase.Facts.fillGapsThoroughly (hdb.db, day);
					recl = HistoryDatabase.Levels.getReconstructedLevels (hdb.db);
					while (true) {
						tab = rdRow (is);
						if (tab == null || tab.length == 0)
							break;
						insert (hdb, recl, tab, istate);
					}
				} finally {
					hdb.close ();
				}
			}
		} finally {
			try {
				is.close ();
			} catch (IOException e) {
					/* empty */
			}
		}
		
		return istate;
	}		
	
	protected void var (PrintStream os, String tag, Object value)
		throws IOException
	{
		row (os, tag, value);
	}
	
	protected String rdVar (BufferedReader is, String tag)
			throws IOException
	{
		String tab [];
	
		tab = rdRow (is);
		if (tab == null || tab.length < 2 || !tab [0].equals (tag))
			throw new IOException ("Bad format: expecting " + tag);
		
		return tab [1];
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
	
	protected void insert (HistoryDatabase hdb, Set<Integer> recl, String tab [], ImportState istate)
	{	
		int day, level, vacation;
		SRSDistribution srs;
		HistoryDatabase.FactType type;
		int i;

		srs = new SRSDistribution ();
		if (tab.length >= 20) {
			type = FactType.COMPLETE;
			for (i = 2; i < 20; i++)
				if (tab [i].length () == 0) {
					type = FactType.PARTIAL;
					break;
				}			
			if (tab [6].length () == 0 ||
				tab [7].length () == 0 ||
				tab [12].length () == 0 ||
				tab [13].length () == 0 ||
				tab [18].length () == 0 ||
				tab [19].length () == 0)
				type = FactType.MISSING;
		} else
			type = FactType.MISSING;
		
		day = Integer.parseInt (tab [0]);
		level = Integer.parseInt (tab [1]);
		vacation = getInt (tab, 20);

		srs = new SRSDistribution ();
		loadStats (tab, 2, srs.apprentice);
		loadStats (tab, 3, srs.guru);
		loadStats (tab, 4, srs.master);
		loadStats (tab, 5, srs.enlighten);
		loadStats (tab, 6, srs.burned);
		if (type == FactType.PARTIAL)
			loadStats (tab, 7, srs.apprentice);
		
		if (istate.level != level) {
			istate.level = level;
			Levels.insertOrUpdate (hdb.db, level, day, vacation);
		}
		istate.updated += HistoryDatabase.Facts.importDay (hdb.db, day, srs, type);
		istate.read++;
	}
	
	protected void loadStats (String tab [], int col, SRSDistribution.Level stats)
	{
		stats.radicals = getInt (tab, col);
		stats.kanji = getInt (tab, col + 6);
		stats.vocabulary = getInt (tab, col + 12);
	}
	
	protected int getInt (String tab [], int col)
	{
		if (tab.length < col + 1 || tab [col].length () == 0)
			return 0;
		
		return Integer.parseInt (tab [col]);
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
		
		os.print ("\r\n");
	}	

	protected String [] rdRow (BufferedReader is)
		throws IOException
	{
		String line;
		
		line = is.readLine ();
		if (line == null)
			return null;
		
		return line.split ("\\s*" + SEPARATOR + "\\s*");
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
