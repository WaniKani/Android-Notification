package com.wanikani.androidnotifier.db;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

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
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.Item.Stats;
import com.wanikani.wklib.SRSDistribution.Level;
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

public class ItemsDatabase {

	public abstract static class ItemsTable {
		
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
		private static final String C_BURNED_DATE = "available_date";
		
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
						C_CHARACTER + " TEXT NOT NULL, " +
						C_MEANING + " TEXT NOT NULL, " +
						C_LEVEL + " INTEGER NOT NULL, " +
						C_SRS + " INTEGER NULL, " +
						C_UNLOCKED_DATE +  " INTEGER NULL, " +
						C_AVAILABLE_DATE +  " INTEGER NULL, " +
						C_BURNED_DATE +  " INTEGER NULL, " +
						
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
		
		private static final String SQL_IDX_1 =
					 	"CREATE INDEX %1$s_LEVEL ON %1$s (" + C_LEVEL + ")";
		private static final String SQL_IDX_2 =
			 			"CREATE INDEX %1$s_AVAILABLE ON %1$s (" + C_AVAILABLE_DATE + ")";
		
		private static final String SQL_DROP =
						"DROP TABLE IF EXISTS %s";
		
		private static final String SQL_DROP_IDX_1 =
						"DROP INDEX IF EXISTS %s_LEVEL";

		private static final String SQL_DROP_IDX_2 =
				"DROP INDEX IF EXISTS %s_AVAILABLE";

		
		public abstract String getTable ();
		
		public abstract String getCreateStatement ();
				
		public void onCreate (SQLiteDatabase db)
		{
			boolean ok;
			
			ok = false;
			db.execSQL ("BEGIN");
			try {
				db.execSQL (getCreateStatement ());
				db.execSQL (String.format (SQL_IDX_1, getTable ()));
				db.execSQL (String.format (SQL_IDX_2, getTable ()));
				ok = true;
			} finally {
				if (ok)
					db.execSQL ("COMMIT");
				else
					db.execSQL ("ROLLBACK");
			}
		}

		public void onDrop (SQLiteDatabase db)
		{
			db.execSQL (String.format (SQL_DROP_IDX_1, getTable ()));
			db.execSQL (String.format (SQL_DROP_IDX_2, getTable ()));
			db.execSQL (String.format (SQL_DROP, getTable ()));
		}
	};
	
	public static class RadicalsTable extends ItemsTable {

		/** Table name */
		private static final String TABLE = "radicals";
		
		/** Image */
		private static final String C_IMAGE = "image";
		
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						SQL_CREATE_COLUMNS + ", " +
						C_IMAGE + " TEXT NULL) "; 
						
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
	};
	
	public static class KanjiTable extends ItemsTable {

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

		public String getTable ()
		{
			return TABLE;
		}
		
		@Override
		public String getCreateStatement ()
		{
			return SQL_CREATE;
		}		
	};
		
	public static class VocabTable extends ItemsTable {

		/** Table name */
		private static final String TABLE = "vocab";
		
		/** Reading */
		private static final String C_KANA = "kana";
				
		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						SQL_CREATE_COLUMNS + ", " +
						C_KANA + " TEXT NOT NULL)"; 

		public String getTable ()
		{
			return TABLE;
		}
		
		@Override
		public String getCreateStatement ()
		{
			return SQL_CREATE;
		}		
	};
	
	/**
	 * The DB open helper.  
	 */
	static class OpenHelper extends SQLiteOpenHelper {
		
		/** DB Version */
		private static final int VERSION = 1;
		
		/** The db file */
		private static final String NAME = "items.db";
		
		/** The radicals table */
		private static final RadicalsTable RADICALS = new RadicalsTable ();

		/** The kanji table */
		private static final KanjiTable KANJI = new KanjiTable ();

		/** The vocab table */
		private static final VocabTable VOCAB = new VocabTable ();

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
			RADICALS.onCreate (db);
			KANJI.onCreate (db);
			VOCAB.onCreate (db);
		}
		
		public void onDrop (SQLiteDatabase db)
		{
			RADICALS.onDrop (db);
			KANJI.onDrop (db);
			VOCAB.onDrop (db);
		}

		@Override
		public void onUpgrade (SQLiteDatabase db, int oldv, int newv)
		{
			onDrop (db);
			onCreate (db);
		}
		
	}

	/** The DB helper */
	private OpenHelper helper;
	
	/** The database */
	SQLiteDatabase db;
	
	/** Synchronization */
	public static final Object MUTEX = new Object ();
		
	/**
	 * Cosntructor
	 * @param ctxt the context
	 */
	public ItemsDatabase (Context ctxt)
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

}
