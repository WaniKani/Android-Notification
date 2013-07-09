package com.wanikani.androidnotifier.db;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.UserInformation;

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

public class HistoryDatabase {

	public static enum FactType {
		
		MISSING,
		
		PARTIAL,
		
		COMPLETE
		
	}
	
	public static class CoreStats {
		
		public int maxUnlockedRadicals;
		
		public int maxUnlockedKanji;
	
		public int maxUnlockedVocab;
		
		public CoreStats (int maxUnlockedRadicals, 
						  int maxUnlockedKanji, 
						  int maxUnlockedVocab)
		{
			this.maxUnlockedRadicals = maxUnlockedRadicals;
			this.maxUnlockedKanji = maxUnlockedKanji;
			this.maxUnlockedVocab = maxUnlockedVocab;
		}
	}
	
	public static class Facts {
		
		private static final String TABLE = "facts";
		
		private static final String C_DAY = "_id";
		
		private static final String C_GURU_RADICALS = "guru_radicals";
		
		private static final String C_MASTER_RADICALS = "master_radicals";

		private static final String C_ENLIGHTEN_RADICALS = "enlighten_radicals";

		private static final String C_BURNED_RADICALS = "burned_radicals";
		
		private static final String C_UNLOCKED_RADICALS = "unlocked_radicals";

		private static final String C_GURU_KANJI = "guru_kanji";
		
		private static final String C_MASTER_KANJI = "master_kanji";

		private static final String C_ENLIGHTEN_KANJI = "enlighten_kanji";

		private static final String C_BURNED_KANJI = "burned_kanji";

		private static final String C_UNLOCKED_KANJI = "unlocked_kanji";

		private static final String C_GURU_VOCAB = "guru_vocab";
		
		private static final String C_MASTER_VOCAB = "master_vocab";

		private static final String C_ENLIGHTEN_VOCAB = "enlighten_vocab";

		private static final String C_BURNED_VOCAB = "burned_vocab";
		
		private static final String C_UNLOCKED_VOCAB = "unlocked_vocab";
		
		private static final int CX_DAY = 0;
		
		private static final int CX_GURU_RADICALS = 1;		
		private static final int CX_MASTER_RADICALS = 2;
		private static final int CX_ENLIGHTEN_RADICALS = 3;
		private static final int CX_BURNED_RADICALS = 4;
		private static final int CX_UNLOCKED_RADICALS = 5;
		
		private static final int CX_GURU_KANJI = 6;		
		private static final int CX_MASTER_KANJI = 7;
		private static final int CX_ENLIGHTEN_KANJI = 8;
		private static final int CX_BURNED_KANJI = 9;
		private static final int CX_UNLOCKED_KANJI = 10;

		private static final int CX_GURU_VOCAB = 11;		
		private static final int CX_MASTER_VOCAB = 12;
		private static final int CX_ENLIGHTEN_VOCAB = 13;
		private static final int CX_BURNED_VOCAB = 14;
		private static final int CX_UNLOCKED_VOCAB = 15;

		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +

						C_DAY + " INTEGER PRIMARY KEY," +
						
						C_GURU_RADICALS + " INTEGER DEFAULT NULL, " +
						C_MASTER_RADICALS + " INTEGER DEFAULT NULL, " +
						C_ENLIGHTEN_RADICALS + " INTEGER DEFAULT NULL, " +
						C_BURNED_RADICALS + " INTEGER DEFAULT NULL, " +
						C_UNLOCKED_RADICALS + " INTEGER DEFAULT NULL, " +

						C_GURU_KANJI + " INTEGER DEFAULT NULL, " +
						C_MASTER_KANJI + " INTEGER DEFAULT NULL, " +
						C_ENLIGHTEN_KANJI + " INTEGER DEFAULT NULL, " +
						C_BURNED_KANJI + " INTEGER DEFAULT NULL, " +
						C_UNLOCKED_KANJI + " INTEGER DEFAULT NULL, " +

						C_GURU_VOCAB + " INTEGER DEFAULT NULL, " +
						C_MASTER_VOCAB + " INTEGER DEFAULT NULL, " +
						C_ENLIGHTEN_VOCAB + " INTEGER DEFAULT NULL, " +
						C_BURNED_VOCAB + " INTEGER DEFAULT NULL, " +
						C_UNLOCKED_VOCAB + " INTEGER DEFAULT NULL)";
		
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		private static final String SQL_INSERT_DAY =
				"REPLACE INTO " + TABLE + " (" + C_DAY + ") VALUES (?)";
		
		private static final String SQL_INSERT =
				"REPLACE INTO " + TABLE +  " VALUES (" +
						"?," +
						"?, ?, ?, ?, ?, " +
						"?, ?, ?, ?, ?, " +
						"?, ?, ?, ?, ?)";
		
		private static final String SQL_SELECT = 
				C_DAY + " BETWEEN ? AND ? ";
		
		public static void onCreate (SQLiteDatabase db)
		{
			db.execSQL (SQL_CREATE);
		}
		
		public static void onDrop (SQLiteDatabase db)
		{
			db.execSQL (SQL_DROP);
		}
		
		public static void fillGap (SQLiteDatabase db, int day)
		{
			SQLiteStatement stmt;
			String cols [];
			Cursor c;
			int i, n;
			
			cols = new String [] { "MAX(" + C_DAY + ")" };
			
			c = db.query (TABLE, cols, null, null, null, null, null);
			if (c == null)
				return;
			try {
				if (c.moveToNext ()) {
					n = c.isNull (0) ? -1 : (int) c.getLong (0);
					stmt = db.compileStatement (SQL_INSERT_DAY);
					for (i = n + 1; i < day; i++) {
						stmt.bindLong (1, i);
						stmt.executeInsert ();
					}
				}
			} finally {
				c.close ();
			}
		}
		
		public static void insert (SQLiteDatabase db, int day, SRSDistribution srs)
			throws SQLException
		{
			String values [];

			values = new String [] {
					Integer.toString (day),

					Integer.toString (srs.guru.radicals),
					Integer.toString (srs.master.radicals),
					Integer.toString (srs.enlighten.radicals),
					Integer.toString (srs.burned.radicals),
					Integer.toString (srs.apprentice.radicals + srs.guru.radicals +
									  srs.master.radicals + srs.enlighten.radicals),

					Integer.toString (srs.guru.kanji),
					Integer.toString (srs.master.kanji),
					Integer.toString (srs.enlighten.kanji),
					Integer.toString (srs.burned.kanji),
					Integer.toString (srs.apprentice.kanji + srs.guru.kanji +
									  srs.master.kanji + srs.enlighten.kanji),

					Integer.toString (srs.guru.vocabulary),
					Integer.toString (srs.master.vocabulary),
					Integer.toString (srs.enlighten.vocabulary),
					Integer.toString (srs.burned.vocabulary),
					Integer.toString (srs.apprentice.vocabulary + srs.guru.vocabulary +
									  srs.master.vocabulary + srs.enlighten.vocabulary),									  
			};
			
			db.execSQL (SQL_INSERT, values);		
		}

		public static Cursor select (SQLiteDatabase db, int from, int to)
				throws SQLException
		{
			String args [];
			
			args = new String [] { Integer.toString (from), Integer.toString (to) };
			
			return db.query (TABLE, null, SQL_SELECT, args, null, null, C_DAY);
		}
		
		public static CoreStats getCoreStats (SQLiteDatabase db)
				throws SQLException
		{
			CoreStats cs;
			String cols [];
			Cursor c;
			
			cols = new String [] {
				"MAX(" + C_UNLOCKED_RADICALS + ")",
				"MAX(" + C_UNLOCKED_KANJI + ")",
				"MAX(" + C_UNLOCKED_VOCAB + ")"
			};
			c = null;
			cs = null;
			try {
				c = db.query(TABLE, cols, null, null, null, null, null);
				c.moveToFirst ();
				cs = new CoreStats (getIntOrZero (c, 0),
									getIntOrZero (c, 1),
									getIntOrZero (c, 2));
			} catch (SQLException e) {
				cs = new CoreStats (0, 0, 0);
			} finally {
				c.close ();
			}
			
			return cs;
		}

		public static FactType getType (Cursor c)
		{
			return  !c.isNull (CX_GURU_RADICALS) ? FactType.COMPLETE :
					!c.isNull (CX_UNLOCKED_RADICALS) ? FactType.PARTIAL : FactType.MISSING;
		}
		
		public static int getDay (Cursor c)
		{
			return c.getInt (CX_DAY);
		}
		
		private static int getIntOrZero (Cursor c, int column)
		{
			return c.isNull (column) ? 0 : c.getInt (column);
		}
		
		public static SRSDistribution getSRSDistribution (Cursor c)
		{
			SRSDistribution srs;
			
			srs = new SRSDistribution ();

			srs.guru.radicals = getIntOrZero (c, CX_GURU_RADICALS);
			srs.master.radicals = getIntOrZero (c, CX_MASTER_RADICALS);
			srs.enlighten.radicals = getIntOrZero (c, CX_ENLIGHTEN_RADICALS);
			srs.burned.radicals = getIntOrZero (c, CX_BURNED_RADICALS);
			srs.apprentice.radicals = getIntOrZero (c, CX_UNLOCKED_RADICALS) -
					srs.guru.radicals - srs.master.radicals - srs.enlighten.radicals;

			srs.guru.kanji = getIntOrZero (c, CX_GURU_KANJI);
			srs.master.kanji = getIntOrZero (c, CX_MASTER_KANJI);
			srs.enlighten.kanji = getIntOrZero (c, CX_ENLIGHTEN_KANJI);
			srs.burned.kanji = getIntOrZero (c, CX_BURNED_KANJI);
			srs.apprentice.kanji = getIntOrZero (c, CX_UNLOCKED_KANJI) -
					srs.guru.kanji - srs.master.kanji - srs.enlighten.kanji;
			
			srs.guru.vocabulary = getIntOrZero (c, CX_GURU_VOCAB);
			srs.master.vocabulary = getIntOrZero (c, CX_MASTER_VOCAB);
			srs.enlighten.vocabulary = getIntOrZero (c, CX_ENLIGHTEN_VOCAB);
			srs.burned.vocabulary = getIntOrZero (c, CX_BURNED_VOCAB);
			srs.apprentice.vocabulary = getIntOrZero (c, CX_UNLOCKED_VOCAB) -
					srs.guru.vocabulary - srs.master.vocabulary - srs.enlighten.vocabulary;
			
			fixupTotals (srs);
						
			return srs;
		}
		
		private static void fixupTotals (SRSDistribution srs)
		{
			srs.apprentice.total = srs.apprentice.radicals + srs.apprentice.kanji + srs.apprentice.vocabulary;
			srs.guru.total = srs.guru.radicals + srs.guru.kanji + srs.guru.vocabulary;
			srs.master.total = srs.master.radicals + srs.master.kanji + srs.master.vocabulary;
			srs.enlighten.total = srs.enlighten.radicals + srs.enlighten.kanji + srs.enlighten.vocabulary;
			srs.burned.total = srs.burned.radicals + srs.burned.kanji + srs.burned.vocabulary;			
		}
	}
	
	static class Levels {
		
		private static final String TABLE = "levels";
		
		private static final String C_LEVEL = "_id";
		
		private static final String C_DAY = "day";
				
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +

						C_LEVEL + " INTEGER PRIMARY KEY," +
						
						C_DAY + " INTEGER NOT NULL)";

		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		private static final String SQL_INSERT_OR_IGNORE =
				"INSERT OR IGNORE INTO " + TABLE + " VALUES (?, ?)";
	
		public static void onCreate (SQLiteDatabase db)
		{
			db.execSQL (SQL_CREATE);
		}
		
		public static void onDrop (SQLiteDatabase db)
		{
			db.execSQL (SQL_DROP);
		}
		
		public static void insertOrIgnore (SQLiteDatabase db, int level, int day)
			throws SQLException
		{
			String values [];
			
			values = new String [] {
				Integer.toString (level), 
				Integer.toString (day)
			};

			db.execSQL (SQL_INSERT_OR_IGNORE, values);		
		}
		
	}
	
	static class OpenHelper extends SQLiteOpenHelper {
		
		private static final int VERSION = 1;
		
		private static final String NAME = "history.db";

		
		OpenHelper (Context ctxt)
		{
			super (ctxt, NAME, null, VERSION);
		}
		
		@Override
		public void onCreate (SQLiteDatabase db)
		{
			Facts.onCreate (db);
			Levels.onCreate (db);
		}
		
		@Override
		public void onUpgrade (SQLiteDatabase db, int oldv, int newv)
		{
			/* Hope I'll never need it */
		}
		
	}
	
	OpenHelper helper;
	
	SQLiteDatabase db;
		
	public HistoryDatabase (Context ctxt)
	{
		helper = new OpenHelper (ctxt);		
	}	
	
	public synchronized void openW ()
		throws SQLException
	{
		if (db == null)
			db = helper.getWritableDatabase ();	
	}
	
	public synchronized void openR ()
		throws SQLException
	{
		if (db == null)
			db = helper.getReadableDatabase ();	
	}
	
	public void close ()
		throws SQLException
	{
		helper.close ();
	}
	
	public Cursor selectFacts (int from, int to)
		throws SQLException
	{
		return Facts.select (db, from, to);
	}
	
	public void insert (UserInformation ui, SRSDistribution srs)
		throws SQLException
	{
		int today;
		
		today = ui.getDay ();
		
		Facts.fillGap (db, today);
		Levels.insertOrIgnore (db, ui.level, today);
		
		/* This is the only non-idempotent operation */
		Facts.insert (db, today, srs);		
	}
	
	public static void insert (Context ctxt, UserInformation ui, SRSDistribution srs)
		throws SQLException
	{
		HistoryDatabase hdb;
		
		hdb = new HistoryDatabase (ctxt);
		hdb.openW ();
		try {
			hdb.insert (ui, srs);
		} finally {
			hdb.close ();
		}
	}
	
	public CoreStats getCoreStats ()
		throws SQLException
	{
		return Facts.getCoreStats (db);
	}
	
	public static CoreStats getCoreStats (Context ctxt)
		throws SQLException
	{
		HistoryDatabase hdb;
		
		hdb = new HistoryDatabase (ctxt);
		hdb.openW ();
		try {
			return hdb.getCoreStats ();
		} finally {
			hdb.close ();
		}		
	}
}
