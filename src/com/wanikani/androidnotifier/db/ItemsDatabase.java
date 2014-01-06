package com.wanikani.androidnotifier.db;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.wanikani.wklib.Item;
import com.wanikani.wklib.Item.Type;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.ItemsCacheInterface;
import com.wanikani.wklib.ItemsCacheInterface.LevelData;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
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

public class ItemsDatabase {

	public abstract class ItemsTable<T extends Item> implements ItemsCacheInterface.Cache<T> {
		
		private static final long serialVersionUID = 1;
		
		/** Primary key */
		private static final String C_ID = "_id";
		
		/** Character */
		private static final String C_CHARACTER = "character";
		
		/** Meaning */
		private static final String C_MEANING ="meaning";

		/** Level */
		private static final String C_LEVEL = "level";
		
		/** SRS level */
		private static final String C_SRS = "srs";

		/** Unlocked date */
		private static final String C_UNLOCKED_DATE = "unlocked_date";
		
		/** Available date */
		private static final String C_AVAILABLE_DATE = "available_date";
		
		/** Burned date */
		private static final String C_BURNED_DATE = "burned_date";
		
		/** Is item burned */
		private static final String C_BURNED = "burned";

		/** Correct readings */
		private static final String C_READING_CORRECT = "reading_correct";
		
		/** Incorrect readings */
		private static final String C_READING_INCORRECT = "reading_incorrect";
		
		/** Max streak readings */
		private static final String C_READING_MAX_STREAK = "reading_max_streak";
		
		/** Current streak readings */
		private static final String C_READING_CURRENT_STREAK = "reading_current_streak";

		/** Correct meaning answers */
		private static final String C_MEANING_CORRECT = "meaning_correct";
		
		/** Incorrect meaning answers */
		private static final String C_MEANING_INCORRECT = "meaning_incorrect";
		
		/** Max streak meaning answers */
		private static final String C_MEANING_MAX_STREAK = "meaning_max_streak";
		
		/** Current streak meaning answers */
		private static final String C_MEANING_CURRENT_STREAK = "meaning_current_streak";
		
		/** Reading note */
		private static final String C_READING_NOTE = "reading_note";

		/** Meaning note */
		private static final String C_MEANING_NOTE = "meaning_note";
		
		/** User synonyms */
		private static final String C_USER_SYNONYMS = "user_synonyms";
		
		/** The SQL create statement */
		protected static final String SQL_CREATE_COLUMNS = 
						C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
						C_CHARACTER + " TEXT NULL, " + 			/* Image radicals can set character == NULL */
						C_MEANING + " TEXT NOT NULL, " +
						C_LEVEL + " INTEGER NOT NULL, " +
						C_SRS + " INTEGER NULL, " +
						C_UNLOCKED_DATE +  " INTEGER NULL, " +
						C_AVAILABLE_DATE +  " INTEGER NULL, " +
						C_BURNED_DATE +  " INTEGER NULL, " +
						C_BURNED + " INTEGER NOT NULL," +
						
						C_READING_CORRECT + " INTEGER NULL, " +
						C_READING_INCORRECT + " INTEGER NULL, " +
						C_READING_MAX_STREAK + " INTEGER NULL, " +
						C_READING_CURRENT_STREAK + " INTEGER NULL, " +
						
						C_MEANING_CORRECT + " INTEGER NULL, " +
						C_MEANING_INCORRECT + " INTEGER NULL, " +
						C_MEANING_MAX_STREAK + " INTEGER NULL, " +
						C_MEANING_CURRENT_STREAK + " INTEGER NULL, " +
						
						C_READING_NOTE + " TEXT NULL, " +
						C_MEANING_NOTE + " TEXT NULL, " +
						C_USER_SYNONYMS + " TEXT NULL";
		
		protected static final String SQL_INSERT_COLUMNS = 
					C_CHARACTER + ", " +
					C_MEANING + ", " +
					C_LEVEL + ", " +
					C_SRS + ", " +
					C_UNLOCKED_DATE + ", " +
					C_AVAILABLE_DATE + ", " +
					C_BURNED_DATE + ", " +
					C_BURNED + ", " +
					
					C_READING_CORRECT + ", " +
					C_READING_INCORRECT + ", " +
					C_READING_MAX_STREAK + ", " +
					C_READING_CURRENT_STREAK + ", " +

					C_MEANING_CORRECT + ", " +
					C_MEANING_INCORRECT + ", " +
					C_MEANING_MAX_STREAK + ", " +
					C_MEANING_CURRENT_STREAK + ", " +

					C_READING_NOTE + ", " +
					C_MEANING_NOTE + ", " +
					C_USER_SYNONYMS;
		
		protected static final String SQL_INSERT_ARGS =
					"?, ?, ?, ?, ?, ?, ?, ?,"+
				    "?, ?, ?, ?, " +
				    "?, ?, ?, ?, " +
				    "?, ?, ?";

		private static final String I_TABLE_PFX = "inv_";
		
		private static final String C_I_LEVEL = C_LEVEL;

		private static final String C_I_DATE = "date";
		
		private static final String C_I_ETAG = "etag";
		
		private static final String SQL_CREATE_INVENTORY =
					    "CREATE TABLE %s (" +
					    C_I_LEVEL + " INTEGER PRIMARY KEY NOT NULL, " +
					    C_I_DATE +  " INTEGER NOT NULL, " +
					    C_I_ETAG + " TEXT NULL) ";		
		
		private static final String SQL_IDX_1 =
					 	"CREATE INDEX %1$s_LEVEL ON %1$s (" + C_LEVEL + ")";
		
		private static final String SQL_DROP =
						"DROP TABLE IF EXISTS %s";
		
		private static final String SQL_DROP_IDX_1 =
						"DROP INDEX IF EXISTS %s_LEVEL";

		private static final String SQL_DROP_IDX_2 =
				"DROP INDEX IF EXISTS %s_AVAILABLE";
		
		private static final String WHERE_LEVEL_IS =
					C_LEVEL + " =  ?";
		
		private static final String WHERE_LEVEL_IN =
				C_LEVEL + " IN (%s)";

		private static final String SEPARATOR = "/";
		
		private static final String SQL_UPDATE_INVENTORY =
					"INSERT OR REPLACE INTO %s (" + 
						C_I_LEVEL + ", " +
						C_I_DATE + ", " +
						C_I_ETAG + ") VALUES (?, ?, ?)";
		
		public abstract String getTable ();
		
		public abstract String getCreateStatement ();
		
		public abstract String getInsertStatement ();
		
		public void onCreate (SQLiteDatabase db)
		{
			db.execSQL (getCreateStatement ());
			db.execSQL (String.format (SQL_IDX_1, getTable ()));
			db.execSQL (String.format (SQL_CREATE_INVENTORY, inventory ()));
		}

		public void onDrop (SQLiteDatabase db)
		{
			db.execSQL (String.format (SQL_DROP_IDX_1, getTable ()));
			db.execSQL (String.format (SQL_DROP_IDX_2, getTable ()));
			db.execSQL (String.format (SQL_DROP, getTable ()));
			db.execSQL (String.format (SQL_DROP, inventory ()));
		}
		
		private String inventory ()
		{
			return I_TABLE_PFX + getTable ();
		}

		@Override
		public LevelData<T> get (int level)
		{	
			String columns [], args [];			
			ItemLibrary<T> lib;
			LevelData<T> ld;
			Cursor c;
			T item;
			
			columns = new String [] { C_I_DATE, C_I_ETAG };
			args = new String [] { Integer.toString (level) };

			synchronized (MUTEX) {
				/* Must use openW to allow db upgrade */
				openW ();
				c = null;
				try {
					c = db.query (inventory (), columns, WHERE_LEVEL_IS, args, null, null, null);
					if (!c.moveToFirst ())
						return new LevelData<T> ();
					lib = new ItemLibrary<T> ();
					ld = new LevelData<T> (new Date (c.getLong (0)), c.getString (1), lib);
					c.close ();
					
					c = null;
					c = db.query (getTable (), null, WHERE_LEVEL_IS, args, null, null, null);
					while (c.moveToNext ()) {
						item = buildItem (c);
						item.fixup ();
						lib.list.add (item);
					}
					
				} finally {
					if (c != null)
						c.close ();
					close ();
				}
				
			}
			
			return ld;
		}
		
		private String getLSet (Set<Integer> set)
		{
			StringBuffer sb;
			String lset;
			
			sb = new StringBuffer ();
			for (Integer i : set)
				sb.append (i).append (",");
			lset = sb.toString ();
			lset = lset.substring (0, lset.length () - 1);

			return lset;
		}
		
		@Override
		public void get (Map<Integer, LevelData <T>> data)
		{	
			String columns [];			
			ItemLibrary<T> lib;
			LevelData<T> ld;
			String lset;
			Cursor c;
			T item;
			
			if (data.isEmpty ())
				return;
			
			lset = getLSet (data.keySet ());
			
			columns = new String [] { C_I_DATE, C_I_ETAG, C_I_LEVEL };

			synchronized (MUTEX) {
				/* Must use openW to allow db upgrade */
				openW ();
				c = null;
				try {
					c = db.query (inventory (), columns, String.format (WHERE_LEVEL_IN, lset), null, null, null, null);
					while (c.moveToNext ()) {
						lib = new ItemLibrary<T> ();
						ld = new LevelData<T> (new Date (c.getLong (0)), c.getString (1), lib);
						data.put (c.getInt (2), ld);
					}
					c.close ();
					
					c = null;
					c = db.query (getTable (), null, String.format (WHERE_LEVEL_IN, lset), null, null, null, null);
					while (c.moveToNext ()) {
						item = buildItem (c);
						item.fixup ();
						ld = data.get (item.level);
						if (ld.lib != null)	/* May happen if inventory is not synch'd */
							ld.lib.add (item);
					}
				} finally {
					if (c != null)
						c.close ();
					close ();
				}				
			}
		}
		
		@Override
		public void put (LevelData <T> data)
		{
			SQLiteStatement stmt;
			Set<Integer> levels;
			String lset;
			
			levels = new HashSet<Integer> ();
			for (T item : data.lib.list)
				levels.add (item.level);

			lset = getLSet (levels);
			
			synchronized (MUTEX) {
				stmt = null;
				openW ();
				db.beginTransaction ();
				try {
					stmt = db.compileStatement (String.format (SQL_UPDATE_INVENTORY, inventory ()));
					stmt.bindLong (2, data.date.getTime ());
					if (data.etag != null)
						stmt.bindString (3, data.etag);
					else
						stmt.bindNull (3);
					for (Integer level : levels) {
						stmt.bindLong (1, level);
						stmt.execute ();
					}
					stmt.close ();
					stmt = null;
										
					db.delete (getTable (), String.format (WHERE_LEVEL_IN, lset), null);
					stmt = db.compileStatement (getInsertStatement ());
					for (T item : data.lib.list) {
						fillStatement (stmt, item);
						stmt.executeInsert ();
					}
						
					
					db.setTransactionSuccessful ();
				} finally {
					if (stmt != null)
						stmt.close ();
					db.endTransaction ();
					close ();
				}				
			}
			
		}
		
		protected abstract T buildItem (Cursor c);
		
		protected Date getDate (Cursor c, String col)
		{
			int idx;
			
			idx = c.getColumnIndex (col);
			return !c.isNull (idx) ? new Date (c.getLong (idx)) : null;
		}
		
		protected String [] parseStringArray (String s)
		{
			StringTokenizer st;
			List<String> l;
			String tok;
			
			if (s == null || s.equals (""))
				return null;
			
			l = new Vector<String> ();
			st = new StringTokenizer (s, SEPARATOR);
			while (st.hasMoreTokens ()) {
				tok = st.nextToken ().trim ();
				if (!tok.equals (""))
					l.add (tok);
			}
			
			return l.toArray (new String [l.size ()]);
		}
		
		protected String encodeStringArray (String s [])
		{
			StringBuffer sb;
			int i;
			
			if (s == null || s.length == 0)
				return null;
			
			sb = new StringBuffer ();
			i = 0;
			sb.append (s [i++]);
			while (i < s.length)
				sb.append (SEPARATOR).append (s [i++]);
			
			return sb.toString ();
		}
		
		private Item.Performance loadPerformance (Cursor c, String correct, String incorrect,
									  			  String maxStreak, String currentStreak)
		{
			Item.Performance ans;
			
			if (c.isNull (c.getColumnIndex (correct)))
				return null;
				
			ans = new Item.Performance ();
			ans.correct = c.getInt (c.getColumnIndex (correct));
			ans.incorrect = c.getInt (c.getColumnIndex (incorrect));
			ans.maxStreak = c.getInt (c.getColumnIndex (maxStreak));
			ans.currentStreak = c.getInt (c.getColumnIndex (currentStreak));
			
			return ans;
		}
		
		private int fillPerformance (SQLiteStatement stmt, Item.Performance perf, int idx)
		{
			if (perf != null) {
				stmt.bindLong (idx++, perf.correct);
				stmt.bindLong (idx++, perf.incorrect);
				stmt.bindLong (idx++, perf.maxStreak);
				stmt.bindLong (idx++, perf.currentStreak);
			} else {
				stmt.bindNull (idx++);
				stmt.bindNull (idx++);
				stmt.bindNull (idx++);
				stmt.bindNull (idx++);
			}
			
			return idx;
		}

		protected void setFields (Cursor c, T i)
		{
			Item.Stats stats;
			
			i.character = c.getString (c.getColumnIndex (C_CHARACTER));
			i.meaning = c.getString (c.getColumnIndex (C_MEANING));
			i.level = c.getInt (c.getColumnIndex (C_LEVEL));
			
			if (!c.isNull (c.getColumnIndex (C_SRS))) {
				stats = new Item.Stats ();
				stats.srs = SRSLevel.fromOrdinal (c.getInt (c.getColumnIndex (C_SRS)));
				stats.availableDate = getDate (c, C_AVAILABLE_DATE);
				stats.burnedDate = getDate (c, C_BURNED_DATE);
				stats.burned = c.getInt (c.getColumnIndex (C_BURNED)) != 0;

				stats.reading = loadPerformance (c, C_READING_CORRECT, C_READING_INCORRECT,
								 				 C_READING_MAX_STREAK, C_READING_CURRENT_STREAK);
				stats.meaning = loadPerformance (c, C_MEANING_CORRECT, C_MEANING_INCORRECT,
						 		 			     C_MEANING_MAX_STREAK, C_READING_CURRENT_STREAK);
				
				stats.readingNote = c.getString (c.getColumnIndex (C_READING_NOTE));
				stats.meaningNote = c.getString (c.getColumnIndex (C_MEANING_NOTE));
				stats.userSynonyms = parseStringArray (c.getString (c.getColumnIndex (C_USER_SYNONYMS)));
				
				i.setStats (stats);
			}
					
			i.setUnlockedDate (getDate (c, C_UNLOCKED_DATE));
		}
		
		protected int fillStatement (SQLiteStatement stmt, T item)
		{
			String s;
			int idx;
			
			idx = 1;
			if (item.character != null)
				stmt.bindString (idx++, item.character);
			else
				stmt.bindNull (idx++);
			stmt.bindString (idx++, item.meaning);
			stmt.bindLong (idx++, item.level);
			if (item.stats != null && item.stats.srs != null)
				stmt.bindLong (idx++, item.stats.srs.ordinal ());
			else
				stmt.bindNull (idx++);

			if (item.getUnlockedDate () != null)
				stmt.bindLong (idx++, item.getUnlockedDate ().getTime ());
			else
				stmt.bindNull (idx++);
			
			if (item.stats != null && item.stats.availableDate != null)
				stmt.bindLong (idx++, item.stats.availableDate.getTime ());
			else
				stmt.bindNull (idx++);
			
			if (item.stats != null && item.stats.burnedDate != null)
				stmt.bindLong (idx++, item.stats.burnedDate.getTime ());
			else
				stmt.bindNull (idx++);
			
			stmt.bindLong (idx++, item.stats != null && item.stats.burned ? 1 : 0);

			idx = fillPerformance (stmt, item.stats != null ? item.stats.reading : null, idx);
			idx = fillPerformance (stmt, item.stats != null ? item.stats.meaning : null, idx);
			
			if (item.stats != null && item.stats.readingNote != null)
				stmt.bindString (idx++, item.stats.readingNote);
			else
				stmt.bindNull (idx++);
			
			if (item.stats != null && item.stats.meaningNote != null)
				stmt.bindString (idx++, item.stats.meaningNote);
			else
				stmt.bindNull (idx++);
			
			if (item.stats != null && item.stats.userSynonyms != null)
				s = encodeStringArray (item.stats.userSynonyms);
			else
				s = null;
			if (s != null)
				stmt.bindString (idx++, s);
			else
				stmt.bindNull (idx++);

			return idx;
		}		
	};
	
	public class RadicalsTable extends ItemsTable<Radical> {

		public static final long serialVersionUID = 1;
		
		/** Table name */
		private static final String TABLE = "radicals";
		
		/** Image */
		private static final String C_IMAGE = "image";
		
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						SQL_CREATE_COLUMNS + ", " +
						C_IMAGE + " TEXT NULL) ";
		
		private static final String SQL_INSERT =
				"INSERT INTO " + TABLE + "(" + 
						SQL_INSERT_COLUMNS + ", " +
						C_IMAGE + ") VALUES (" +
						SQL_INSERT_ARGS + ", ?)";
										
		@Override
		public String getTable ()
		{
			return TABLE;
		}
		
		@Override
		public String getCreateStatement ()
		{
			return SQL_CREATE;
		}
		
		@Override
		public String getInsertStatement ()
		{
			return SQL_INSERT;
		}

		@Override
		protected Radical buildItem (Cursor c)
		{
			Radical ans;
			
			ans = new Radical ();
			
			super.setFields (c, ans);			
			ans.image = c.getString (c.getColumnIndex (C_IMAGE));
			
			return ans;
		}

		@Override
		protected int fillStatement (SQLiteStatement stmt, Radical item)
		{
			int idx;
			
			idx = super.fillStatement (stmt, item);
			if (item.image != null)
				stmt.bindString (idx++, item.image);
			else
				stmt.bindNull (idx++);
			
			return idx;
		}		
	}
		
	public class KanjiTable extends ItemsTable<Kanji> {

		private static final long serialVersionUID = 1;
		
		/** Table name */
		private static final String TABLE = "kanji";
		
		/** On'yomi reading */
		private static final String C_ONYOMI = "onyomi";
		
		/** Kun'yomi reading */
		private static final String C_KUNYOMI = "kunyomi";
		
		/** Is on'yomi the important reading */
		private static final String C_IMPORTANT_IS_ON = "important_is_on";
		
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						SQL_CREATE_COLUMNS + ", " +
						C_ONYOMI + " TEXT NULL, " + 
						C_KUNYOMI + " TEXT NULL, " + 
						C_IMPORTANT_IS_ON + " INTEGER NOT NULL)";
		
		private static final String SQL_INSERT =
				"INSERT INTO " + TABLE + "(" + 
						SQL_INSERT_COLUMNS + ", " +
						C_ONYOMI + ", " + 
						C_KUNYOMI + ", " + 
						C_IMPORTANT_IS_ON + ") VALUES (" +
						SQL_INSERT_ARGS + ", ?, ?, ?)";		

		public String getTable ()
		{
			return TABLE;
		}
		
		@Override
		public String getCreateStatement ()
		{
			return SQL_CREATE;
		}
		
		@Override
		public String getInsertStatement ()
		{
			return SQL_INSERT;
		}

		@Override
		protected Kanji buildItem (Cursor c)
		{
			Kanji ans;
			
			ans = new Kanji ();
			
			super.setFields (c, ans);			
			ans.onyomi = c.getString (c.getColumnIndex (C_ONYOMI));
			ans.kunyomi = c.getString (c.getColumnIndex (C_KUNYOMI));
			ans.importantReading = c.getInt (c.getColumnIndex (C_IMPORTANT_IS_ON)) == 1 ?
							Kanji.Reading.ONYOMI : Kanji.Reading.KUNYOMI;
			
			return ans;
		}

		@Override
		protected int fillStatement (SQLiteStatement stmt, Kanji item)
		{
			int idx;
			
			idx = super.fillStatement (stmt, item);
			if (item.onyomi != null)
				stmt.bindString (idx++, item.onyomi);
			else
				stmt.bindNull (idx++);
			
			if (item.kunyomi != null)
				stmt.bindString (idx++, item.kunyomi);
			else
				stmt.bindNull (idx++);
			
			stmt.bindLong (idx++, item.importantReading == Kanji.Reading.ONYOMI ? 1 : 0);
			
			return idx;
		}		
	};
		
	public class VocabTable extends ItemsTable<Vocabulary> {

		private static final long serialVersionUID = 1;
				
		/** Table name */
		private static final String TABLE = "vocab";
		
		/** Reading */
		private static final String C_KANA = "kana";
				
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						SQL_CREATE_COLUMNS + ", " +
						C_KANA + " TEXT NOT NULL)"; 

		private static final String SQL_INSERT =
				"INSERT INTO " + TABLE + "(" + 
						SQL_INSERT_COLUMNS + ", " +
						C_KANA + ") VALUES (" +
						SQL_INSERT_ARGS + ", ?)";		

		public String getTable ()
		{
			return TABLE;
		}
		
		@Override
		public String getCreateStatement ()
		{
			return SQL_CREATE;
		}		
		
		@Override
		public String getInsertStatement ()
		{
			return SQL_INSERT;
		}

		@Override
		protected Vocabulary buildItem (Cursor c)
		{
			Vocabulary ans;
			
			ans = new Vocabulary ();
			
			super.setFields (c, ans);			
			ans.kana = c.getString (c.getColumnIndex (C_KANA));
			
			return ans;
		}

		@Override
		protected int fillStatement (SQLiteStatement stmt, Vocabulary item)
		{
			int idx;
			
			idx = super.fillStatement (stmt, item);
			
			stmt.bindString (idx++, item.kana);
			
			return idx;
		}				
	};
	
	/**
	 * The DB open helper.  
	 */
	class OpenHelper extends SQLiteOpenHelper {
		
		/** DB Version */
		private static final int VERSION = 1;
		
		/** The db file */
		private static final String NAME = "items.db";
		
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
			new RadicalsTable ().onCreate (db);
			new KanjiTable ().onCreate (db);
			new VocabTable ().onCreate (db);
		}
		
		public void onDrop (SQLiteDatabase db)
		{
			new RadicalsTable ().onDrop (db);
			new KanjiTable ().onDrop (db);
			new VocabTable ().onDrop (db);
		}

		@Override
		public void onUpgrade (SQLiteDatabase db, int oldv, int newv)
		{
			onDrop (db);
			onCreate (db);
		}
		
	}
	
	private class ItemsCacheImpl implements ItemsCacheInterface {

		private static final long serialVersionUID = 1;
		
		RadicalsTable radicals;
		
		KanjiTable kanji;
		
		VocabTable vocab;
		
		public ItemsCacheImpl ()
		{
			radicals = new RadicalsTable ();
			kanji = new KanjiTable ();
			vocab = new VocabTable ();
		}
		
		@Override
		public <T extends Item> Cache<T> get (Type type) 
		{
			switch (type) {
			case RADICAL:
				return (ItemsCacheInterface.Cache<T>) radicals;

			case KANJI:
				return (ItemsCacheInterface.Cache<T>) kanji;

			case VOCABULARY:
				return (ItemsCacheInterface.Cache<T>) vocab;
			}
			
			return null;
		}				
		
		public void flush ()
		{
			/* empty */
		}
	}

	/** The DB helper */
	private OpenHelper helper;
	
	/** The database */
	SQLiteDatabase db;
	
	/** The cache */
	ItemsCacheImpl cache;
	
	/** Synchronization */
	public static final Object MUTEX = new Object ();
		
	/**
	 * Cosntructor
	 * @param ctxt the context
	 */
	public ItemsDatabase (Context ctxt)
	{		
		helper = new OpenHelper (ctxt);
		cache = new ItemsCacheImpl ();
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
		db = null;
	}
	
	public ItemsCacheInterface getCache ()
	{
		return cache;
	}
	
}
