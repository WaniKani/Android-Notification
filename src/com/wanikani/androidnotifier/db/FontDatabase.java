package com.wanikani.androidnotifier.db;

import java.util.List;
import java.util.Vector;

import com.wanikani.androidnotifier.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

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

public class FontDatabase {

	public static class FontEntry {
		
		public String name;
		
		public String filename;
		
		public Typeface face;
		
		public boolean enabled;
				
		public FontEntry (String name, String filename, boolean enabled)
		{
			this.name = name;
			this.filename = filename;
			this.enabled = enabled;
		}
		
		public boolean load ()
		{
			try {
				face = Typeface.createFromFile (name);
			} catch (Exception e) {
				/* empty */
			}
			
			return face != null;
		}
	}
	
	public static class FontTable {

		private static final String TABLE = "font";
		
		private static final String C_ID = "_id";
		
		private static final String C_NAME = "name";

		private static final String C_FILENAME = "filename";
		
		private static final String C_ENABLED = "enabled";

		/** The SQL create statement */
		private static final String SQL_CREATE = 
				"CREATE TABLE " + TABLE + " (" +
						C_ID + " INTEGER PRIMARY KEY," +
						C_NAME + " TEXT UNIQUE," +
						C_FILENAME + " TEXT" +				
						C_ENABLED + " INTEGER DEFAULT 1" +
				")";

		/** The SQL drop statement */
		private static final String SQL_DROP = 
				"DROP TABLE IF EXISTS " + TABLE;
		
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
		
		public static List<FontEntry> getFonts (SQLiteDatabase db)
			throws SQLException
		{
			List<FontEntry> ans;
			String cols [];
			Cursor c;
			
			cols = new String [] { C_NAME, C_FILENAME, C_ENABLED };
			ans = new Vector<FontEntry> ();
			
			c = null;
			try {
				c = db.query (TABLE, cols, null, null, null, null, C_ID);
				while (c.moveToNext ())
					ans.add (new FontEntry (c.getString (0), c.getString (1), c.getInt (2) > 0));
			} finally {
				if (c != null)
					c.close ();
			}
					
			return ans;
		}		
	}
	
	/**
	 * The DB open helper.  
	 */
	static class OpenHelper extends SQLiteOpenHelper {
		
		/** DB Version */
		private static final int VERSION = 1;
		
		/** The db file */
		private static final String NAME = "font.db";

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
			FontTable.onCreate (db);
		}
		
		@Override
		public void onUpgrade (SQLiteDatabase db, int oldv, int newv)
		{
			/* Hope I'll never need it */
		}
		
	}

	/** The DB helper */
	private OpenHelper helper;
	
	/** The database */
	private SQLiteDatabase db;
		
	/// The japanese typeface path
	private static final String JAPANESE_TYPEFACE_FONT = "/system/fonts/MTLmr3m.ttf";
	
	private static final String PREFIX = FontDatabase.class.getName () + ".";
	
	private static final String PREF_SYS_ENABLED = PREFIX + "SYS_ENABLED";

	private static final String PREF_MOTOYA_ENABLED = PREFIX + "MOTOYA_ENABLED";

	/**
	 * Cosntructor
	 * @param ctxt the context
	 */
	public FontDatabase (Context ctxt)
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
	
	public static List<FontEntry> getFonts (Context ctxt)
	{
		List<FontEntry> ans;
		FontDatabase fdb;
		
		fdb = new FontDatabase (ctxt);
		fdb.openR ();
		try {
			ans = FontTable.getFonts (fdb.db); 
		} finally {
			if (fdb != null)
				fdb.close ();
		}
		
		return ans;
	}
	
	public static List<FontEntry> getAllFonts (Context ctxt)
	{
		SharedPreferences prefs;
		List<FontEntry> ans, dbfe;
		boolean haveMotoya;
		FontEntry fe;
		
		ans = new Vector<FontEntry> ();
		
		prefs = PreferenceManager.getDefaultSharedPreferences (ctxt);
		fe = new FontEntry (ctxt.getString (R.string.tag_motoya_font), JAPANESE_TYPEFACE_FONT,
							prefs.getBoolean (PREF_MOTOYA_ENABLED, true));
		haveMotoya = fe.load ();
		
		ans.add (new FontEntry (ctxt.getString (R.string.tag_system_font), null, 
				 prefs.getBoolean (PREF_SYS_ENABLED, !haveMotoya)));
		if (haveMotoya = fe.load ())
			ans.add (fe);
		
		try {
			dbfe = getFonts (ctxt);
		} catch (SQLException e) {
			dbfe = new Vector<FontEntry> ();
		}
		
		for (FontEntry ife : dbfe)
			if (ife.load ())
				ans.add (ife);
		
		return ans;
	}

}
