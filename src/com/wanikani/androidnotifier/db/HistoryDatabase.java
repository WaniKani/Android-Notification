package com.wanikani.androidnotifier.db;

import android.content.ContentValues;
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

	static class Facts {
		
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
					n = c.isNull (0) ? 0 : (int) c.getLong (0);
					if (n < day) {
						stmt = db.compileStatement (SQL_INSERT_DAY);
						for (i = n; i < day; i++) {
							stmt.bindLong (1, i);
							stmt.executeInsert ();
						}
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
									  srs.enlighten.radicals),

					Integer.toString (srs.guru.kanji),
					Integer.toString (srs.master.kanji),
					Integer.toString (srs.enlighten.kanji),
					Integer.toString (srs.burned.kanji),
					Integer.toString (srs.apprentice.kanji + srs.guru.kanji +
									  srs.enlighten.kanji),

					Integer.toString (srs.guru.vocabulary),
					Integer.toString (srs.master.vocabulary),
					Integer.toString (srs.enlighten.vocabulary),
					Integer.toString (srs.burned.vocabulary),
					Integer.toString (srs.apprentice.vocabulary + srs.guru.vocabulary +
									  srs.enlighten.vocabulary),									  
			};
			
			db.execSQL (SQL_INSERT, values);		
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
	
	private static final long ONE_DAY = 24 * 60 * 60 * 1000;
	
	public HistoryDatabase (Context ctxt)
	{
		helper = new OpenHelper (ctxt);		
	}	
	
	public void openW ()
		throws SQLException
	{
		db = helper.getWritableDatabase ();	
	}
	
	public void openR ()
		throws SQLException
	{
		db = helper.getReadableDatabase ();	
	}

	public void close ()
		throws SQLException
	{
		helper.close ();
	}
	
	private static int days (UserInformation ui, long ts)
	{
		return (int) ((ts - ui.creationDate.getTime ()) / ONE_DAY);
	}
	
	private static int days (UserInformation ui)
	{
		return days (ui, System.currentTimeMillis ());
	}

	public void insert (UserInformation ui, SRSDistribution srs)
		throws SQLException
	{
		int today;
		
		today = days (ui);
		
		Levels.insertOrIgnore (db, ui.level, today);
		Facts.fillGap (db, today);
		
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
}
