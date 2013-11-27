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

import com.wanikani.wklib.Item;
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

/**
 * The database helper that gives access to the user facts tables.
 * The schema of this database is quite simple, since it comprises just two 
 * (permanent) tables:
 * <ul>
 * <li>The facts table, that records all the SRS distribution state for each day
 * <li>The levels table, associating each level to the the day the user levelled up
 * </ul>   
 * In addition to these tables, this class creates an auxilary (and temporary) table
 * to perform item reconstruction. We do this to minimize the consequences of
 * an item reconstruction going wrong: the actual update of the facts table is
 * performed using a single SQL statement, and this makes error management
 * a lot easier.<p>
 * Access to the three tables is performed through three classes ({@link Facts},
 * {@link Level} and {@link ReconstructTable respectively}). We don't follow
 * the DAO pattern, since it seems quite unfit to the objects rows represent.<p>
 * This class exposes both static and non-static methods. Static methods should
 * be used for simple operations, because they take care of opening the DB, 
 * doing what's needed, and closing it. Multiple operations require 
 * the caller to create an instance, calling {@link #openR()} or {@link #openW()}, 
 * the appropriate methods, and finally {@link #close()}.
 */
public class HistoryDatabase {

	/**
	 * How complete is the information stored on the facts table row.
	 */
	public static enum FactType {
		
		/** No information at all */
		MISSING,
		
		/** Just number of unlocked and burned items */
		PARTIAL,
		
		/** Information is complete */
		COMPLETE
		
	}
	
	/**
	 * Some overall information, useful to setup TY plots.
	 * @see Facts#getCoreStats(SQLiteDatabase)
	 */
	public static class CoreStats {
		
		/** Maximum number of unlocked radicals so far */
		public int maxUnlockedRadicals;
		
		/** Maximum number of unlocked kanji so far */
		public int maxUnlockedKanji;
	
		/** Maximum number of unlocked vocab items so far */
		public int maxUnlockedVocab;
		
		/** Maximum number of unlocked/burned radicals so far */
		public int maxRadicals;
		
		/** Maximum number of unlocked/burned kanji so far */
		public int maxKanji;
		
		/** Maximum number of unlocked/burned vocab items so far */
		public int maxVocab;
		
		/** Contents of the levels table */
		public Map<Integer, LevelInfo> levelInfo;
		
		/**
		 * Constructor.
		 * @param maxUnlockedRadicals maximum number of unlocked radicals so far
		 * @param maxUnlockedKanji maximum number of unlocked kanji so far
		 * @param maxUnlockedVocab maximum number of unlocked vocab items so far
		 * @param maxRadicals maximum number of unlocked/burned radicals so far
		 * @param maxKanji maximum number of unlocked/burned kanji so far
		 * @param maxVocab maximum number of unlocked/burned vocab items so far
		 * @param levelInfo contents of the levels table
		 */
		public CoreStats (int maxUnlockedRadicals, 
						  int maxUnlockedKanji, 
						  int maxUnlockedVocab,
						  int maxRadicals,
						  int maxKanji,
						  int maxVocab,
						  Map<Integer, LevelInfo> levelInfo)
		{
			this.maxUnlockedRadicals = maxUnlockedRadicals;
			this.maxUnlockedKanji = maxUnlockedKanji;
			this.maxUnlockedVocab = maxUnlockedVocab;
			this.maxRadicals = maxRadicals;
			this.maxKanji = maxKanji;
			this.maxVocab = maxVocab;
			this.levelInfo = levelInfo;
		}
	}
	
	public static class LevelInfo {
		
		public int day;
		
		public int vacation;
		
		public LevelInfo (int day, int vacation)
		{
			this.day = day;
			this.vacation = vacation;
		}
		
	}
	
	/**
	 * The facts table. Each line represents a day, and the primary key is 
	 * the number of days since subscription. An healthy database always
	 * contains all the rows from day zero up to today, because when a new
	 * record is added, this class takes care of filling all the rows from
	 * the last record to the current day.
	 * Depending on the amount of information stored in a row, we have three
	 * types of facts (@see FactType):
	 * <ul>
	 * 	<li>A regular record, containing all the information
	 *  <li>A "filler" record, where there is no information at all. These records
	 *  	are added when e.g. day 42 we add a regular record and the facts table
	 *  	stops at day 39
	 *  <li>A partial record, created by the reconstruction process. These records
	 *      only contain the number of unlocked and burned radicals, kanji and vocab.
	 * </ul>
	 * To save space, there is no apprentice column, since it can be obtained
	 * by subtracting guru, master and enlightened values from the number of unlocked
	 * items.
	 */
	public static class Facts {

		/** Table name */
		private static final String TABLE = "facts";
		
		/** Primary key. Number of days since subscription (encoded as an integer) */
		private static final String C_DAY = "_id";
		
		/** Number or guru radicals */
		private static final String C_GURU_RADICALS = "guru_radicals";

		/** Number of master radicals */
		private static final String C_MASTER_RADICALS = "master_radicals";

		/** Number of enlightened radicals */
		private static final String C_ENLIGHTEN_RADICALS = "enlighten_radicals";

		/** Number of burned radicals */
		private static final String C_BURNED_RADICALS = "burned_radicals";

		/** Number of unlocked radicals */
		private static final String C_UNLOCKED_RADICALS = "unlocked_radicals";

		/** Number of guru kanji */
		private static final String C_GURU_KANJI = "guru_kanji";
		
		/** Number of master kanji */
		private static final String C_MASTER_KANJI = "master_kanji";

		/** Number of enlightened kanji */
		private static final String C_ENLIGHTEN_KANJI = "enlighten_kanji";

		/** Number of burned kanji */
		private static final String C_BURNED_KANJI = "burned_kanji";

		/** Number of unlocked kanji */
		private static final String C_UNLOCKED_KANJI = "unlocked_kanji";

		/** Number of guru vocab items */
		private static final String C_GURU_VOCAB = "guru_vocab";
		
		/** Number of master vocab items */
		private static final String C_MASTER_VOCAB = "master_vocab";

		/** Number of enlightened vocab items */
		private static final String C_ENLIGHTEN_VOCAB = "enlighten_vocab";

		/** Number of burned vocab items */
		private static final String C_BURNED_VOCAB = "burned_vocab";
		
		/** Number of unlocked vocab items */
		private static final String C_UNLOCKED_VOCAB = "unlocked_vocab";
		
		/** Day column index */
		private static final int CX_DAY = 0;
		
		/** Guru radicals column index */
		private static final int CX_GURU_RADICALS = 1;		
		/** Master radicals column index */
		private static final int CX_MASTER_RADICALS = 2;
		/** Enlightened radicals column index */
		private static final int CX_ENLIGHTEN_RADICALS = 3;
		/** Burned radicals column index */
		private static final int CX_BURNED_RADICALS = 4;
		/** Unlocked radicals column index */
		private static final int CX_UNLOCKED_RADICALS = 5;
		
		/** Guru kanji column index */
		private static final int CX_GURU_KANJI = 6;		
		/** Master kanji column index */
		private static final int CX_MASTER_KANJI = 7;
		/** Enlightened kanji column index */
		private static final int CX_ENLIGHTEN_KANJI = 8;
		/** Burned kanji column index */
		private static final int CX_BURNED_KANJI = 9;
		/** Unlocked kanji column index */
		private static final int CX_UNLOCKED_KANJI = 10;

		/** Guru vocab items column index */
		private static final int CX_GURU_VOCAB = 11;		
		/** Master vocab items column index */
		private static final int CX_MASTER_VOCAB = 12;
		/** Enlightened vocab items column index */
		private static final int CX_ENLIGHTEN_VOCAB = 13;
		/** Burned vocab items column index */
		private static final int CX_BURNED_VOCAB = 14;
		/** Unlocked vocab items column index */
		private static final int CX_UNLOCKED_VOCAB = 15;

		/** The SQL create statement */
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

		/** The SQL drop statement */
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		/** An SQL query returning all the rows carrying partial or no information */
		private static final String SQL_MISSING_DAYS =
				"SELECT " + C_DAY + " FROM " + TABLE + 
				" WHERE " + C_GURU_KANJI + " IS NULL";
		
		/** The SQL statement that creates a new record */
		private static final String SQL_INSERT_DAY =
				"INSERT OR IGNORE INTO " + TABLE + " (" + C_DAY + ") VALUES (?)";
		
		/** Where condition, selecting records between two days */
		private static final String WHERE_DAY_BETWEEN = 
				C_DAY + " BETWEEN ? AND ? ";
		
		/** Where condition, selecting records before a given day */
		private static final String WHERE_DAY_LTE =
				C_DAY + " <= ?";
		
		/** Where condition, selecting the record of a specific day */
		private static final String WHERE_DAY_IS =
				C_DAY + " = ?";
		
		/**
		 * Creates the table
		 * @param db the database
		 */
		public static void onCreate (SQLiteDatabase db)
		{
			db.execSQL (SQL_CREATE);
		}

		/**
		 * Drops the table
		 * @param db the database
		 */		
		public static void onDrop (SQLiteDatabase db)
		{
			db.execSQL (SQL_DROP);
		}
		
		/**
		 * Makes sure that adding a new record does not create a gap.
		 * This operation must be called before adding a record that will 
		 * describe the most recent day in the table. If the last record is not
		 * the day before, this method takes care of "bridging" the gap
		 * by adding empty records. Of course if gaps exist before the last
		 * record, they are left untouched.
		 * @param db the database
		 * @param day the day that will be added
		 */
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
		
		/**
		 * Fills all the gaps in the database. This (maintenance) method 
		 * goes through all the table and, if some day is missing in the sequence,
		 * it adds it as an empty record
		 * @param db the database
		 * @param day the last day that should be present in the sequence
		 * @throws SQLException
		 */
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
			c = null;
			try {
				c = db.query (TABLE, cols, WHERE_DAY_LTE, args, null, null, null);
				if (c.moveToNext () && !c.isNull (0)) {
					if (c.getInt (0) == day + 1)
						return;
				}
			} finally {
				if (c != null)
					c.close ();
			}
			
			/* Yeah, I know, using rowcount I could identify where gaps lie, 
			 * but this should never happen, so we can afford being brutal */
			stmt = db.compileStatement (SQL_INSERT_DAY);
			for (i = 0; i <= day; i++) {
				stmt.bindLong (1, i);
				stmt.executeInsert ();
			}
		}
		
		/**
		 * Adds a new fact to the table
		 * @param db the database
		 * @param day the day
		 * @param srs the data to store 
		 * @throws SQLException
		 */
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

		/**
		 * Returns a cursor referring to the whole db.
		 * @param db the sql database
		 * @return the cursor
		 * @throws SQLException
		 */
		public static Cursor select (SQLiteDatabase db)
				throws SQLException
		{
			return db.query (TABLE, null, null, null, null, null, C_DAY);
		}

		/**
		 * Returns a cursor referring to an interval of rows.
		 * @param db the sql database
		 * @param from the first day to be selected 
		 * @param to the last day to be selected
		 * @return the cursor
		 * @throws SQLException
		 */
		public static Cursor select (SQLiteDatabase db, int from, int to)
				throws SQLException
		{
			String args [];
			
			args = new String [] { Integer.toString (from), Integer.toString (to) };
			
			return db.query (TABLE, null, WHERE_DAY_BETWEEN, args, null, null, C_DAY);
		}
		
		/**
		 * Returns a {@link CoreStats} object, containing some overall info regarding
		 * this database.
		 * @param db the database
		 * @param ui user information
		 * @return the overall info
		 * @throws SQLException
		 */
		public static CoreStats getCoreStats (SQLiteDatabase db, UserInformation ui)
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
									Levels.getLevelInfo (db, ui));
			} catch (SQLException e) {
				cs = new CoreStats (0, 0, 0, 0, 0, 0, null);
			} finally {
				if (c != null)
					c.close ();
			}
			
			return cs;
		}

		/**
		 * Returns the type of row currently selected by the cursor.
		 * @param c the cursor
		 * @return the type of row
		 */
		public static FactType getType (Cursor c)
		{
			return  !c.isNull (CX_GURU_RADICALS) ? FactType.COMPLETE :
					!c.isNull (CX_UNLOCKED_RADICALS) ? FactType.PARTIAL : FactType.MISSING;
		}
		
		/**
		 * Return the day currently selected by the cursor
		 * @param c the cursor
		 * @return the day number
		 */
		public static int getDay (Cursor c)
		{
			return c.getInt (CX_DAY);
		}
		
		/**
		 * Returns a column integer value, defaulting to zero
		 * @param c the cursor
		 * @param column the column index
		 * @return its contents
		 */
		private static int getIntOrZero (Cursor c, int column)
		{
			return c.isNull (column) ? 0 : c.getInt (column);
		}
		
		/**
		 * Returns the SRS distribution of the day selected by the cursor.
		 * In case of partial information, the total number of unlocked items
		 * is in the apprentice level (SRS distribution have no "unlocked" field).  
		 * @param c the cursor
		 * @return the distribution
		 */
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
		
		/**
		 * Given a SRS distribution object, as built from the database, if fixes
		 * the "total" fields up.
		 * @param srs an SRS distribution objects
		 */
		private static void fixupTotals (SRSDistribution srs)
		{
			srs.apprentice.total = srs.apprentice.radicals + srs.apprentice.kanji + srs.apprentice.vocabulary;
			srs.guru.total = srs.guru.radicals + srs.guru.kanji + srs.guru.vocabulary;
			srs.master.total = srs.master.radicals + srs.master.kanji + srs.master.vocabulary;
			srs.enlighten.total = srs.enlighten.radicals + srs.enlighten.kanji + srs.enlighten.vocabulary;
			srs.burned.total = srs.burned.radicals + srs.burned.kanji + srs.burned.vocabulary;			
		}
	}
	
	/**
	 * A temporary table used to reconstruct SRS distribution from item information.
	 * This is actually implemented by creating a subset of the facts table
	 * (it contains just the unlocked and burned columns for each item type).
	 * An instance of this class is fed with all the items from level one to
	 * the user's current level, so it can update the rows of this temporary table.
	 * At the end of the process, we update the master table using a single
	 * sql statement.
	 * This class also takes care of updating the levelup table. Here, however,
	 * the rows are comparatively few, so we store everything in an hashtable
	 * and update the master levels table row by row (the operation is idempotent).
	 */
	public static class ReconstructTable {

		/** The table name */
		private static final String TABLE = "reconstruct";
		
		/** Primary key. The same as the master facts table */
		private static final String C_DAY = "_id";
		
		/** Number of burned radicals */
		private static final String C_BURNED_RADICALS = "burned_radicals";
		
		/** Number of unlocked radicals */
		private static final String C_UNLOCKED_RADICALS = "unlocked_radicals";

		/** Number of burned kanji */
		private static final String C_BURNED_KANJI = "burned_kanji";

		/** Number of unlocked kanji */
		private static final String C_UNLOCKED_KANJI = "unlocked_kanji";

		/** Number of burned vocab items */
		private static final String C_BURNED_VOCAB = "burned_vocab";
		
		/** Number of unlocked vocab items */
		private static final String C_UNLOCKED_VOCAB = "unlocked_vocab";
		
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +

						C_DAY + " INTEGER PRIMARY KEY," +
						
						C_BURNED_RADICALS + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_RADICALS + " INTEGER DEFAULT 0, " +

						C_BURNED_KANJI + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_KANJI + " INTEGER DEFAULT 0, " +

						C_BURNED_VOCAB + " INTEGER DEFAULT 0, " +
						C_UNLOCKED_VOCAB + " INTEGER DEFAULT 0)";
		
		/** The SQL drop statement */
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		/** Loads the day sequence from the master table to the reconstruct table.
		 *  It is important to select only the rows containing partial or no data,
		 *  because at the end of the process all those rows will be overwritten
		 *  on the master table  */
		private static final String SQL_COPY_FROM_FACTS =
				"INSERT INTO " + TABLE +  "(" + C_DAY + ") " + 
						Facts.SQL_MISSING_DAYS;
		
		/** Puts the reconstructed data onto the master table */
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

		/** Increase by one the values of a field from one day up to the end of the table */
		private static String SQL_ABOVE =
				"UPDATE " + TABLE + " SET %1$s = %1$s + 1 WHERE " + C_DAY + " >= ?";
		
		/** Increase by one the values of a field from one day to another */
		private static String SQL_BETWEEN =
				"UPDATE " + TABLE + " SET %1$s = %1$s + 1 WHERE " + 
						C_DAY + " BETWEEN ? AND ?";
		
		/** A prepared statement to update the table when an unlocked radical
		 *  is encountered. It increments the unlocked radicals counter from
		 *  the unlock day up to the end of the table */
		private SQLiteStatement radicalStmtU;
		
		/** The first prepared statement to update the table when a burned radical
		 *  is encountered. It increments the unlocked radicals counter from
		 *  the unlock day up to the day it is burned */
		private SQLiteStatement radicalStmtB1;
		
		/** The second prepared statement to update the table when a burned radical
		 *  is encountered. It increments the burned radicals counter from the
		 *  day it is burned */
		private SQLiteStatement radicalStmtB2;

		/** A prepared statement to update the table when an unlocked kanji
		 *  is encountered. It increments the unlocked kanji counter from
		 *  the unlock day up to the end of the table */
		private SQLiteStatement kanjiStmtU;
		
		/** The first prepared statement to update the table when a burned kanji
		 *  is encountered. It increments the unlocked kanji counter from
		 *  the unlock day up to the day it is burned */
		private SQLiteStatement kanjiStmtB1;
		
		/** The second prepared statement to update the table when a burned kanji
		 *  is encountered. It increments the burned kanji counter from the
		 *  day it is burned */
		private SQLiteStatement kanjiStmtB2;

		/** A prepared statement to update the table when an unlocked vocab items
		 *  is encountered. It increments the unlocked vocab items counter from
		 *  the unlock day up to the end of the table */
		private SQLiteStatement vocabStmtU;
		
		/** The first prepared statement to update the table when a burned vocab item
		 *  is encountered. It increments the unlocked vocab items counter from
		 *  the unlock day up to the day it is burned */
		private SQLiteStatement vocabStmtB1;
				
		/** The second prepared statement to update the table when a burned vocab item
		 *  is encountered. It increments the burned vocab items counter from the
		 *  day it is burned */
		private SQLiteStatement vocabStmtB2;

		/** The database */
		private SQLiteDatabase db;
		
		/** User information data. Needed to convert dates into days */
		private UserInformation ui;
		
		/** The levels table, mapping levels to levelup and vacation days */
		private Map<Integer, LevelInfo> levelups;
		
		/**
		 * Constructor.
		 * @param ui the user information 
		 * @param db the database
		 * @param day the last day (today)
		 */
		private ReconstructTable (UserInformation ui, SQLiteDatabase db, int day)
		{			
			this.db = db;
			this.ui = ui;
			
			db.execSQL (SQL_DROP);
			db.execSQL (SQL_CREATE);
			db.execSQL (SQL_COPY_FROM_FACTS);			
			
			levelups = Levels.getLevelInfo (db);
			
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
		
		/**
		 * Ends the reconstruction process. Closes the statements and 
		 * drops the table.
		 */
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
			
			db.execSQL (SQL_DROP);
		}
		
		/**
		 * Updates the levelup hashtable by checking if an item has been
		 * unlocked before the levelup day of this item's level.
		 * If so, the levelup day is brought back.
		 * @param i an item   
		 */
		private void checkLevelup (Item i)
		{
			Date unlock;
			LevelInfo ci;
			int day;
			
			unlock = i.getUnlockedDate ();
			if (unlock == null)
				return;
			
			day = ui.getDay (unlock);
			ci = levelups.get (i.level);
			if (ci == null)
				levelups.put (i.level, new LevelInfo (day, 0));
			else if (ci.day > day)
				ci.day = day;
		}
		
		/**
		 * Updates table and levlups hashtable using the info contained
		 * in a radical. This method must be called for each unlocked radical.
		 * @param radical a radical
		 */
		public void load (Radical radical)
		{
			Date from, to;
			
			checkLevelup (radical);
			
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
		
		/**
		 * Updates table and levlups hashtable using the info contained
		 * in a kanji. This method must be called for each unlocked kanji.
		 * @param kanji a kanji
		 */
		public void load (Kanji kanji)
		{
			Date from, to;
			
			checkLevelup (kanji);

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
		
		/**
		 * Updates table and levlups hashtable using the info contained
		 * in a vocab item. This method must be called for each unlocked 
		 * vocab item.
		 * @param radical a radical
		 */
		public void load (Vocabulary vocab)
		{
			Date from, to;
			
			checkLevelup (vocab);
			
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
		
		/**
		 * Updates the master tables using the information collected
		 * during the reconstruction process. Must be called only 
		 * after all the items have been successfully evaluated.
		 */
		private void merge ()
		{
			Levels.setLevelInfo (db, levelups);
			db.execSQL (SQL_COPY_TO_FACTS);
		}
		
	}
	
	/**
	 * The levelup table. A simple mapping between level number (which
	 * is the primary key) and day number. Gaps in the level sequence
	 * are legitimate, especially when the app is installed when the
	 * user is already at level two or higher.
	 * Of course if the app is turned off, a leveup occurs and some days
	 * later the app is turned on again, this table will not be accurate.
	 * This can be fixed by a reconstruction process, however.
	 */
	static class Levels {
		
		/** The table name */
		private static final String TABLE = "levels";
		
		/** The level column name (which is the primary key) */
		private static final String C_LEVEL = "_id";
		
		/** The levelup day. Not nullable */
		private static final String C_DAY = "day";
				
		/** The vacation days on that level. Not nullable */
		private static final String C_VACATION = "vacation";

		/** The create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						C_LEVEL + " INTEGER PRIMARY KEY," +						
						C_DAY + " INTEGER NOT NULL," +
						C_VACATION + " INTEGER NOT NULL DEFAULT 0)";
		
		/** The alter statement needed to upgrade from version 1 */
		private static final String SQL_UPGRADE_FROM_V1 = 
				"ALTER TABLE " + TABLE + " ADD COLUMN " +
						C_VACATION + " INTEGER NOT NULL DEFAULT 0";		

		/** The drop statement */
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
		/** Tries to insert a new row into the table. If a row at the
		 *  same level already exists, it is retained. */
		private static final String SQL_INSERT_OR_IGNORE =
				"INSERT OR IGNORE INTO " + TABLE + " VALUES (?, ?, ?)";
		
		/** Tries to insert a new row into the table. If a row at the
		 *  same level already exists, it is updated. */
		private static final String SQL_INSERT_OR_UPDATE =
				"INSERT OR REPLACE INTO " + TABLE + " VALUES (?, ?, ?)";

		/** Inserts a new row into the table. If a row at the same level
		 *  already exists, it is replaced */
		private static final String SQL_REPLACE =
				"REPLACE INTO " + TABLE + " VALUES (?, ?, ?)";
		
		/** Adds some more vacation days to a given level */
		private static final String SQL_ADD_VACATION =
				"UPDATE " + TABLE + 
					" SET " + C_VACATION + " = " + C_VACATION + " + ? " +
					" WHERE " + C_LEVEL + " = ?";

		/**
		 * Creates the table
		 * @param db the database
		 */
		public static void onCreate (SQLiteDatabase db)
		{
			db.execSQL (SQL_CREATE);
		}
		
		/**
		 * Upgrade from version 1
		 * @param db the database
		 */
		public static void upgradeFromV1 (SQLiteDatabase db)
		{
			db.execSQL (SQL_UPGRADE_FROM_V1);
		}
		
		/**
		 * Drops the table
		 * @param db the database
		 */
		public static void onDrop (SQLiteDatabase db)
		{
			db.execSQL (SQL_DROP);
		}
		
		/**
		 * Inserts a row into the database. If the row already exists, it
		 * is retained. This method is the one to call daily, mapping the
		 * current user level with the day number. If no row exists at this
		 * level, a leveup has occurred. 
		 * @param db the database
		 * @param level the level number
		 * @param day the day number
		 * @param vacation the number of vacation days
		 * @throws SQLException if something goes wrong. May indicate the db is broken
		 */
		public static void insertOrIgnore (SQLiteDatabase db, int level, int day, int vacation)
			throws SQLException
		{
			String values [];
			
			values = new String [] {
				Integer.toString (level), 
				Integer.toString (day),
				Integer.toString (vacation)
			};

			db.execSQL (SQL_INSERT_OR_IGNORE, values);		
		}
		
		/**
		 * Inserts a row into the database. If the row already exists, it
		 * is updated.  
		 * @param db the database
		 * @param level the level number
		 * @param day the day number
		 * @param vacation the number of vacation days
		 * @throws SQLException if something goes wrong. May indicate the db is broken
		 */
		public static void insertOrUpdate (SQLiteDatabase db, int level, int day, int vacation)
			throws SQLException
		{
			String values [];
			
			values = new String [] {
				Integer.toString (level), 
				Integer.toString (day),
				Integer.toString (vacation)
			};

			db.execSQL (SQL_INSERT_OR_UPDATE, values);		
		}

		/**
		 * Returns the entire contents of the table.
		 * @param db the database
		 * @return the level to day mapping
		 */
		public static Map<Integer, LevelInfo> getLevelInfo (SQLiteDatabase db)
		{
			Map<Integer, LevelInfo> ans;
			String cols [];
			Cursor c;
			
			cols = new String [] { C_LEVEL, C_DAY, C_VACATION };
			ans = new Hashtable<Integer, LevelInfo> ();
			
			c = null;
			try {
				c = db.query (TABLE, cols, null, null, null, null, null);
				while (c.moveToNext ())
					ans.put (c.getInt (0), new LevelInfo (c.getInt (1), c.getInt(2)));
			} catch (SQLException e) {
				/* return empty hashtable */
			} finally {
				if (c != null)
					c.close ();
			}
					
			return ans;
		}
		
		/**
		 * Returns the entire contents of the table, making sure also the last levelup
		 * is part of the returned map. The last one may have not been recorded yet,
		 * if we have levelled up today
		 * @param db the database
		 * @param ui the user information
		 * @return the level to day mapping
		 * @throws SQLException if something goes wrong. May indicate the db is broken
		 */
		public static final Map<Integer, LevelInfo> getLevelInfo (SQLiteDatabase db, UserInformation ui)
		{
			Map<Integer, LevelInfo> ans;
			
			ans = getLevelInfo (db);
			/* The last test is an heuristic way to tell whether no reconstruction process
			 * has been done yet */
			if (ui != null && ui.level > 1 && ans.get (ui.level) == null && ans.get (ui.level - 1) != null)
				ans.put (ui.level, new LevelInfo (ui.getDay () + 1, 0));

			return ans;
		}
		
		/**
		 * Replaces the contents of the table. If a level is not present 
		 * in the map, the corresponding table row (if present) is left
		 * untouched. The operation is not atomic (I can't see a reason why
		 * it should be).
		 * @param db the database
		 * @param map a map
		 */
		public static void setLevelInfo (SQLiteDatabase db, Map<Integer, LevelInfo> map)
		{
			SQLiteStatement stmt;
			
			stmt = db.compileStatement (SQL_REPLACE);			
			
			for (Map.Entry<Integer, LevelInfo> e : map.entrySet ()) {
				stmt.bindLong (1, e.getKey ());
				stmt.bindLong (2, e.getValue().day);
				stmt.bindLong (3, e.getValue ().vacation);
				stmt.executeInsert ();
			}
		}
		
		/**
		 * Adds some more vacation days to a given level
		 * @param level the level to update
		 * @param days the extra days
		 */
		public static void addVacation (SQLiteDatabase db, long level, long days)
		{
			Object args [];
			
			args = new Object [] { days, level }; 
			
			db.execSQL (SQL_ADD_VACATION, args);
		}
		
	}
	
	/**
	 * The DB open helper.  
	 */
	static class OpenHelper extends SQLiteOpenHelper {
		
		/** DB Version. Hope I'll never need to change it */
		private static final int VERSION = 2;
		
		/** The db file */
		private static final String NAME = "history.db";

		/**
		 * Constructor
		 * @param ctxt the context
		 */
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
			if (oldv < 2)
				Levels.upgradeFromV1 (db);			
			
		}
		
	}

	/** The DB helper */
	private OpenHelper helper;
	
	/** The database */
	private SQLiteDatabase db;
	
	/** Synchronization */
	public static final Object MUTEX = new Object ();
		
	/**
	 * Cosntructor
	 * @param ctxt the context
	 */
	public HistoryDatabase (Context ctxt)
	{
		helper = new OpenHelper (ctxt);		
	}	
	
	/**
	 * Opens the database in r/w mode. This method may be called multiple times
	 * on the same instance, provided that {@link #close()} is called
	 * beforehand.
	 */	
	public synchronized void openW ()
		throws SQLException
	{
		if (db == null)
			db = helper.getWritableDatabase ();	
	}
	
	/**
	 * Opens the database in r/o mode. This method may be called multiple times
	 * on the same instance, provided that {@link #close()} is called
	 * beforehand.
	 */	
	public synchronized void openR ()
		throws SQLException
	{
		if (db == null)
			db = helper.getReadableDatabase ();	
	}
	
	/**
	 * Closes the DB.
	 */
	public void close ()
		throws SQLException
	{
		helper.close ();
	}

	/**
	 * Returns a cursor on the facts table, returning all the rows
	 * between two given days. Rows are ordered by day, in ascending order.
	 * Callers should deserialize the contents of each row through
	 * {@link Facts}' convenience methods
	 * @param from the first day to return
	 * @param to the last day to return
	 * @return a cursor
	 */
	public Cursor selectFacts (int from, int to)
		throws SQLException
	{
		return Facts.select (db, from, to);
	}

	/**
	 * Returns a cursor on the facts table, returning all the rows.
	 * Rows are ordered by day, in ascending order.
	 * Callers should deserialize the contents of each row through
	 * {@link Facts}' convenience methods
	 * @return a cursor
	 */
	public Cursor selectFacts ()
		throws SQLException
	{
		return Facts.select (db);
	}

	/**
	 * Starts the reconstruction process. If successful, the
	 * process must be ended by calling {@link #endReconstructing(ReconstructTable)}.
	 * Note that all the methods involve I/O activity, so should be
	 * performed on a non-UI thread. 
	 * @param ui the user info
	 * @return a reconstruct control object, to feed with all the relevant items
	 */
	public ReconstructTable startReconstructing (UserInformation ui)
	{
		int yesterday;
		
		yesterday = ui.getDay () - 1;
		if (yesterday < 0)
			return null;
		
		Facts.fillGapsThoroughly (db, yesterday);
		
		return new ReconstructTable (ui, db, yesterday);
	}
	
	/**
	 * Completes the reconstruction process.
	 * @param rt the reconstruct control object
	 */
	public void endReconstructing (ReconstructTable rt)
	{
		rt.merge ();
	}
		
	/**
	 * Inserts a new row into the facts table. If a row already exists,
	 * it is replaced. This seems the sensible thing to do since we
	 * assume that this information is fresh. The old row may also
	 * arise from a reconstruction process and therefore be partial.
	 * When this method is called, the DB must have already been opened
	 * in R/W mode.
	 * @param ui the user info
	 * @param srs the information to store into the table
	 */
	public void insert (UserInformation ui, SRSDistribution srs)
		throws SQLException
	{
		int today;
		
		today = ui.getDay ();
		
		Facts.fillGap (db, today);
		Levels.insertOrIgnore (db, ui.level, today, 0);
		
		Facts.insert (db, today, srs);		
	}

	/**
	 * Inserts a new row into the facts table. If a row already exists,
	 * it is replaced. This seems the sensible thing to do since we
	 * assume that this information is fresh. The old row may also
	 * arise from a reconstruction process and therefore be partial.
	 * @param ctxt the context
	 * @param ui the user info
	 * @param srs the information to store into the table
	 */
	public static void insert (Context ctxt, UserInformation ui, SRSDistribution srs)
		throws SQLException
	{
		HistoryDatabase hdb;
		
		synchronized (MUTEX) {
			hdb = new HistoryDatabase (ctxt);
			hdb.openW ();
			try {
				hdb.insert (ui, srs);
			} finally {
				hdb.close ();
			}
		}
	}
	
	/**
	 * Retrieve some overall statistical data from the facts table.
	 * When this method is called, the DB must have already been opened
	 * in R/O (or R/W) mode.
	 * @param ui the user information
	 * @return the info
	 */	
	public CoreStats getCoreStats (UserInformation ui)
		throws SQLException
	{
		return Facts.getCoreStats (db, ui);
	}
	
	/**
	 * Retrieve some overall statistical data from the facts table.
	 * @param ctxt the context
	 * @param ui the user information
	 * @return the info
	 */	
	public static CoreStats getCoreStats (Context ctxt, UserInformation ui)
	{
		HistoryDatabase hdb;
		
		synchronized (MUTEX) {
			hdb = new HistoryDatabase (ctxt);
			hdb.openW ();
			try {
				return hdb.getCoreStats (ui);
			} finally {
				hdb.close ();
			}
		}
	}
	
	/**
	 * Returns the levelups hashtable
	 * @return the levels-day mapping
	 */
	public Map<Integer, LevelInfo> getLevelInfo ()
	{
		return getLevelInfo (null);
	}

	/**
	 * Returns the levelups hashtable
	 * @param ui the user information
	 * @return the levels-day mapping
	 */
	public Map<Integer, LevelInfo> getLevelInfo (UserInformation ui)
	{
		return Levels.getLevelInfo (db, ui);		
	}
	
	/**
	 * Updates the leveup table. To be done only when a serious inconsistency
	 * has been detected
	 * @param level the level
	 * @param daty the day
	 * @param vacation the vacation days
	 */
	public void updateLevelup (int level, int day, int vacation)
	{
		Levels.insertOrUpdate (db, level, day, vacation);
	}
	
	/**
	 * Add more vacation days to a given level
	 * @param ctxt the context
	 * @param level the level
	 * @param vacation number of vacation days
	 */
	public static void addVacation (Context ctxt, int level, int vacation)
	{
		HistoryDatabase hdb;
		
		synchronized (MUTEX) {
			hdb = new HistoryDatabase (ctxt);
			hdb.openW ();
			try {
				Levels.addVacation (hdb.db, level, vacation);
			} finally {
				hdb.close ();
			}
		}
	}
}
