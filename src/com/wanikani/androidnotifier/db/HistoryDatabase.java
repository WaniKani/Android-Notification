package com.wanikani.androidnotifier.db;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.UserInformation;
import com.wanikani.wklib.Vocabulary;

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
		
		public int maxRadicals;
		
		public int maxKanji;
		
		public int maxVocab;
		
		public Map<Integer, Integer> levelups;
		
		public CoreStats (int maxUnlockedRadicals, 
						  int maxUnlockedKanji, 
						  int maxUnlockedVocab,
						  int maxRadicals,
						  int maxKanji,
						  int maxVocab,
						  Map<Integer, Integer> levelups)
		{
			this.maxUnlockedRadicals = maxUnlockedRadicals;
			this.maxUnlockedKanji = maxUnlockedKanji;
			this.maxUnlockedVocab = maxUnlockedVocab;
			this.maxRadicals = maxRadicals;
			this.maxKanji = maxKanji;
			this.maxVocab = maxVocab;
			this.levelups = levelups;
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
		
		private static final String SQL_MISSING_DAYS =
				"SELECT " + C_DAY + " FROM " + TABLE + 
				" WHERE " + C_GURU_KANJI + " IS NULL";
		
		private static final String SQL_INSERT_DAY =
				"INSERT OR IGNORE INTO " + TABLE + " (" + C_DAY + ") VALUES (?)";
		
		private static final String WHERE_DAY_BETWEEN = 
				C_DAY + " BETWEEN ? AND ? ";
		
		private static final String WHERE_DAY_LTE =
				C_DAY + " <= ?";
		
		private static final String WHERE_DAY_IS =
				C_DAY + " = ?";
		
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
		
		public static void fillGapsThoroughly (SQLiteDatabase db, int day)
			throws SQLException
		{
			SQLiteStatement stmt;
			String cols [], args [];
			Cursor c;
			int i;
						
			cols = new String [] { " COUNT (*)" };
			args = new String [] { Integer.toString (day) };
		 	
			/* First of all we must make sure that gaps do exist */
			c = db.query (TABLE, cols, WHERE_DAY_LTE, args, null, null, null);
			if (c.moveToNext () && !c.isNull (0)) {
				if (c.getInt (0) == day + 1)
					return;
			}
			
			/* Yeah, I know, using rowcount I could identify where gaps lie, 
			 * but this should never happen, so we can afford being brutal */
			stmt = db.compileStatement (SQL_INSERT_DAY);
			for (i = 0; i <= day; i++) {
				stmt.bindLong (1, i);
				stmt.executeInsert ();
			}
		}
		
		public static void insert (SQLiteDatabase db, int day, SRSDistribution srs)
			throws SQLException
		{
			ContentValues cv;
			String deleteArgs [];
			
			cv = new ContentValues ();
			cv.put (C_DAY, day);
			cv.put (C_GURU_RADICALS, srs.guru.radicals);
			cv.put (C_MASTER_RADICALS, srs.master.radicals);
			cv.put (C_ENLIGHTEN_RADICALS, srs.enlighten.radicals);
			cv.put (C_BURNED_RADICALS, srs.burned.radicals);
			cv.put (C_UNLOCKED_RADICALS,
					srs.apprentice.radicals + srs.guru.radicals +
					srs.master.radicals + srs.enlighten.radicals);

			cv.put (C_GURU_KANJI, srs.guru.kanji);
			cv.put (C_MASTER_KANJI, srs.master.kanji);
			cv.put (C_ENLIGHTEN_KANJI, srs.enlighten.kanji);
			cv.put (C_BURNED_KANJI, srs.burned.kanji);
			cv.put (C_UNLOCKED_KANJI, 
					srs.apprentice.kanji + srs.guru.kanji +
					srs.master.kanji + srs.enlighten.kanji);

			cv.put (C_GURU_VOCAB, srs.guru.vocabulary);
			cv.put (C_MASTER_VOCAB, srs.master.vocabulary);
			cv.put (C_ENLIGHTEN_VOCAB, srs.enlighten.vocabulary);
			cv.put (C_BURNED_VOCAB, srs.burned.vocabulary);
			cv.put (C_UNLOCKED_VOCAB,
					srs.apprentice.vocabulary + srs.guru.vocabulary +
					srs.master.vocabulary + srs.enlighten.vocabulary);
			
			deleteArgs = new String [] { Integer.toString (day) };

			/* What I'd like to do here is a REPLACE operation, and receive an
			 * SQLException if it fails. I could not find any way to implement 
			 * this, however, using this API level. 
			 * So I do it manually */						
			db.beginTransaction ();
			try {
				db.delete (TABLE, WHERE_DAY_IS, deleteArgs);
				db.insertOrThrow (TABLE, null, cv);
				db.setTransactionSuccessful ();
			} finally {
				db.endTransaction ();
			}
		}

		public static Cursor select (SQLiteDatabase db, int from, int to)
				throws SQLException
		{
			String args [];
			
			args = new String [] { Integer.toString (from), Integer.toString (to) };
			
			return db.query (TABLE, null, WHERE_DAY_BETWEEN, args, null, null, C_DAY);
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
				"MAX(" + C_UNLOCKED_VOCAB + ")",
				"MAX(" + C_UNLOCKED_RADICALS + " + " + C_BURNED_RADICALS + ")",
				"MAX(" + C_UNLOCKED_KANJI + " + " + C_BURNED_KANJI + ")",
				"MAX(" + C_UNLOCKED_VOCAB + " + " + C_BURNED_VOCAB + ")"
			};
			c = null;
			cs = null;
			try {
				c = db.query(TABLE, cols, null, null, null, null, null);
				c.moveToFirst ();
				cs = new CoreStats (getIntOrZero (c, 0),
									getIntOrZero (c, 1),
									getIntOrZero (c, 2),
									getIntOrZero (c, 3),
									getIntOrZero (c, 4),
									getIntOrZero (c, 5),
									Levels.getLevelups (db));
			} catch (SQLException e) {
				cs = new CoreStats (0, 0, 0, 0, 0, 0, null);
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
	
	public static class ReconstructTable {

		private static final String TABLE = "reconstruct";
		
		private static final String C_DAY = "_id";
		
		private static final String C_BURNED_RADICALS = "burned_radicals";
		
		private static final String C_UNLOCKED_RADICALS = "unlocked_radicals";

		private static final String C_BURNED_KANJI = "burned_kanji";

		private static final String C_UNLOCKED_KANJI = "unlocked_kanji";

		private static final String C_BURNED_VOCAB = "burned_vocab";
		
		private static final String C_UNLOCKED_VOCAB = "unlocked_vocab";
		
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +

						C_DAY + " INTEGER PRIMARY KEY," +
						
						C_BURNED_RADICALS + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_RADICALS + " INTEGER DEFAULT 0, " +

						C_BURNED_KANJI + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_KANJI + " INTEGER DEFAULT 0, " +

						C_BURNED_VOCAB + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_VOCAB + " INTEGER DEFAULT 0)";
		
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		private static final String SQL_COPY_FROM_FACTS =
				"INSERT INTO " + TABLE +  "(" + C_DAY + ") " + 
						Facts.SQL_MISSING_DAYS;
		
		private static final String SQL_COPY_TO_FACTS =
				"REPLACE INTO " + Facts.TABLE + "( " +
						Facts.C_DAY + ", " +
						Facts.C_UNLOCKED_RADICALS + ", " +
						Facts.C_BURNED_RADICALS + ", " +
						Facts.C_UNLOCKED_KANJI + ", " +
						Facts.C_BURNED_KANJI + ", " +
						Facts.C_UNLOCKED_VOCAB + ", "+
						Facts.C_BURNED_VOCAB + " ) " +
				"SELECT " +
						C_DAY + ", " +
						C_UNLOCKED_RADICALS + ", " +
						C_BURNED_RADICALS + ", " +
						C_UNLOCKED_KANJI + ", " +
						C_BURNED_KANJI + ", " +
						C_UNLOCKED_VOCAB + ", "+
						C_BURNED_VOCAB + " " +
				"FROM " + TABLE;

		private static String SQL_ABOVE =
				"UPDATE " + TABLE + " SET %1$s = %1$s + 1 WHERE " + C_DAY + " >= ?";
		
		private static String SQL_BETWEEN =
				"UPDATE " + TABLE + " SET %1$s = %1$s + 1 WHERE " + 
						C_DAY + " BETWEEN ? AND ?";
		
		private SQLiteStatement radicalStmtU;
		
		private SQLiteStatement radicalStmtB1;
		
		private SQLiteStatement radicalStmtB2;

		private SQLiteStatement kanjiStmtU;
		
		private SQLiteStatement kanjiStmtB1;
		
		private SQLiteStatement kanjiStmtB2;

		private SQLiteStatement vocabStmtU;
		
		private SQLiteStatement vocabStmtB1;
				
		private SQLiteStatement vocabStmtB2;

		private SQLiteDatabase db;
		
		private UserInformation ui;
		
		private ReconstructTable (UserInformation ui, SQLiteDatabase db, int day)
		{			
			this.db = db;
			this.ui = ui;
			
			db.execSQL (SQL_DROP);
			db.execSQL (SQL_CREATE);
			db.execSQL (SQL_COPY_FROM_FACTS);			
			
			radicalStmtU = db.compileStatement 
					(String.format (SQL_ABOVE, C_UNLOCKED_RADICALS));
			radicalStmtB1 = db.compileStatement
					(String.format (SQL_BETWEEN, C_UNLOCKED_RADICALS));
			radicalStmtB2 = db.compileStatement
					(String.format (SQL_ABOVE, C_BURNED_RADICALS));

			kanjiStmtU = db.compileStatement 
					(String.format (SQL_ABOVE, C_UNLOCKED_KANJI));
			kanjiStmtB1 = db.compileStatement
					(String.format (SQL_BETWEEN, C_UNLOCKED_KANJI));
			kanjiStmtB2 = db.compileStatement
					(String.format (SQL_ABOVE, C_BURNED_KANJI));

			vocabStmtU = db.compileStatement 
					(String.format (SQL_ABOVE, C_UNLOCKED_VOCAB));
			vocabStmtB1 = db.compileStatement
					(String.format (SQL_BETWEEN, C_UNLOCKED_VOCAB));
			vocabStmtB2 = db.compileStatement
					(String.format (SQL_ABOVE, C_BURNED_VOCAB));
		}
		
		public void close ()
		{
			radicalStmtU.close ();
			radicalStmtB1.close ();
			radicalStmtB2.close ();
			
			kanjiStmtU.close ();
			kanjiStmtB1.close ();
			kanjiStmtB2.close ();
			
			vocabStmtU.close ();
			vocabStmtB1.close ();
			vocabStmtB2.close ();
		}
		
		public void load (Radical radical)
		{
			Date from, to;
			
			from = radical.getUnlockedDate ();
			if (from == null)
				return;

			to = radical.stats.burned ? radical.stats.burnedDate : null; 
			
			if (to != null) {
				radicalStmtB1.bindLong (1, ui.getDay (from));
				radicalStmtB1.bindLong (2, ui.getDay (to));
				radicalStmtB1.execute ();
				
				radicalStmtB2.bindLong (1, ui.getDay (to));
				radicalStmtB2.execute ();				
			} else {
				radicalStmtU.bindLong (1, ui.getDay (from));
				radicalStmtU.execute ();				
			}
		}
		
		public void load (Kanji kanji)
		{
			Date from, to;
			
			from = kanji.getUnlockedDate ();
			if (from == null)
				return;

			to = kanji.stats.burned ? kanji.stats.burnedDate : null; 
			
			if (to != null) {
				kanjiStmtB1.bindLong (1, ui.getDay (from));
				kanjiStmtB1.bindLong (2, ui.getDay (to));
				kanjiStmtB1.execute ();
				
				kanjiStmtB2.bindLong (1, ui.getDay (to));
				kanjiStmtB2.execute ();				
			} else {
				kanjiStmtU.bindLong (1, ui.getDay (from));
				kanjiStmtU.execute ();				
			}
		}
		
		public void load (Vocabulary vocab)
		{
			Date from, to;
			
			from = vocab.getUnlockedDate ();
			if (from == null)
				return;
			
			to = vocab.stats.burned ? vocab.stats.burnedDate : null; 
			
			if (to != null) {
				vocabStmtB1.bindLong (1, ui.getDay (from));
				vocabStmtB1.bindLong (2, ui.getDay (to));
				vocabStmtB1.execute ();
				
				vocabStmtB2.bindLong (1, ui.getDay (to));
				vocabStmtB2.execute ();				
			} else {
				vocabStmtU.bindLong (1, ui.getDay (from));
				vocabStmtU.execute ();				
			}
		}
		
		private void merge ()
		{
			db.execSQL (SQL_COPY_TO_FACTS);
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
		
		public static Map<Integer, Integer> getLevelups (SQLiteDatabase db)
			throws SQLException
		{
			Map<Integer, Integer> ans;
			String cols [];
			Cursor c;
			
			cols = new String [] { C_LEVEL, C_DAY };
			ans = new Hashtable<Integer, Integer> ();
			
			c = db.query (TABLE, cols, null, null, null, null, null);
			while (c.moveToNext ())
				ans.put (c.getInt (0), c.getInt (1));
					
			return ans;
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
	
	public ReconstructTable startReconstructing (UserInformation ui)
	{
		int yesterday;
		
		yesterday = ui.getDay () - 1;
		if (yesterday < 0)
			return null;
		
		Facts.fillGapsThoroughly (db, yesterday);
		
		return new ReconstructTable (ui, db, yesterday);
	}
	
	public void endReconstructing (ReconstructTable rt)
	{
		rt.merge ();
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
