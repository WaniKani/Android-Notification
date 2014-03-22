package com.wanikani.androidnotifier;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.wanikani.wklib.ExtendedLevelProgression;
import com.wanikani.wklib.LevelProgression;
import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.StudyQueue;
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

/**
 * This class holds all the stats displayed on the dashboard.
 * In addition to the mere collection of data, it features some utility methods
 * to serialize/deserialize on a bundle.  
 */
public class DashboardData {
	
	/**
	 * An additional description of the contents of {@link DashboardData.OptionalData} 
	 * fields. We need this information because the dashboard is displayed even if
	 * one of these optional query is underway, so we need to distinguish between the different
	 * cases.
	 */
	public enum OptionalDataStatus {
		
		/** The query is being performed */
		RETRIEVING, 
		
		/** The query completed successfully */
		RETRIEVED, 
		
		/** The query failed, and the field contains no useful data */
		FAILED
		
	};
	
	public enum Source {
		
		MAIN_ACTIVITY {
			public String getPrefix () 
			{
				return "ma";
			}
		},
		
		NOTIFICATION_SERVICE {
			public String getPrefix ()
			{
				return "ns"; 
			}
		};
		
		public abstract String getPrefix ();
		
	}
	
	/**
	 * A container of all the fields that may not (yet) been available
	 * when the dashboard is displayed. The object may be partially populated.
	 */
	public static class OptionalData {
		
		/** The SRS Distribution */
		public SRSDistribution srs;
		
		/** SRS Distribution status */
		public DashboardData.OptionalDataStatus srsStatus;
		
		/** The level progression */
		public ExtendedLevelProgression elp;
		
		/** Level progression status */
		public DashboardData.OptionalDataStatus lpStatus;
		
		/** Number of critical items */
		public int criticalItems;
		
		/** Critical items status */
		public DashboardData.OptionalDataStatus ciStatus;
		
		/**
		 * Constructor. Input parameters may be null.
		 * In order to provide consistent behaviour:
		 * <ul>
		 * 	<li>If a parameter is not null, its status should be 
		 * 		@link {@link DashboardData.OptionalDataStatus#RETRIEVED}
		 * 	<li>If a parameter is null, its status should be 
		 * 		either @link {@link DashboardData.OptionalDataStatus#RETRIEVING}
		 * 		or @link {@link DashboardData.OptionalDataStatus#FAILED}
		 * </ul>
		 * 
		 * @param srs SRS Distribution
		 * @param srsStatus SRS Distribution status
		 * @param elp the level progression
		 * @param lpStatus level progression status
		 * @param criticalItems number of critical items
		 * @param ciStatus critical items status
		 */
		public OptionalData (SRSDistribution srs, DashboardData.OptionalDataStatus srsStatus, 
							 ExtendedLevelProgression elp, DashboardData.OptionalDataStatus lpStatus,
							 int criticalItems, DashboardData.OptionalDataStatus ciStatus)
		{
			this.srs = srs;
			this.elp = elp;
			this.criticalItems = criticalItems;
			
			this.srsStatus = srsStatus;
			this.lpStatus = lpStatus;			
			this.ciStatus = ciStatus;
		}
		
		/**
		 * Empty constructor. We assume that queries are underway, so
		 * the statuses are set to {@link DashboardData.OptionalDataStatus#RETRIEVING}
		 */
		public OptionalData ()
		{
			srsStatus = DashboardData.OptionalDataStatus.RETRIEVING;
			lpStatus = DashboardData.OptionalDataStatus.RETRIEVING;
			ciStatus = DashboardData.OptionalDataStatus.RETRIEVING;
		}
				
		/**
		 * Merge the data contained in this object with an external (<i>older</i>) 
		 * object.
		 * 	@param od the new data
		 */
		private void merge (DashboardData.OptionalData od)
		{
			if (srsStatus != DashboardData.OptionalDataStatus.RETRIEVED &&
				od.srsStatus == DashboardData.OptionalDataStatus.RETRIEVED) {
				srs = od.srs;
				srsStatus = od.srsStatus;				
			}

			if (lpStatus != DashboardData.OptionalDataStatus.RETRIEVED &&
				od.lpStatus == DashboardData.OptionalDataStatus.RETRIEVED) {
				elp = od.elp;
				lpStatus = od.lpStatus;				
			}

			if (ciStatus != DashboardData.OptionalDataStatus.RETRIEVED &&
				od.ciStatus == DashboardData.OptionalDataStatus.RETRIEVED) {
				criticalItems = od.criticalItems;
				ciStatus = od.ciStatus;				
			}
		}

		/**
		 * Tells whether some data is still missing.
		 * @return true if a refresh of optional data is adivsable
		 */
		public boolean isIncomplete ()
		{
			return 	lpStatus != DashboardData.OptionalDataStatus.RETRIEVED ||
					srsStatus != DashboardData.OptionalDataStatus.RETRIEVED ||
					ciStatus != DashboardData.OptionalDataStatus.RETRIEVED;
		}
	};
	
	private interface Storage {
		
		public void write ();
		
		public void commit ();
		
		public void putInt (String key, int value);
		
		public void putLong (String key, long value);

		public void putBoolean (String key, boolean value);

		public void putString (String key, String value);
		
		public void putSerializable (String key, Serializable value);

		public boolean containsKey (String key);
		
		public void removeKey (String key);

		public int getInt (String key);
		
		public String getString (String key);
		
		public long getLong (String key);
		
		public boolean getBoolean (String key);
		
		public Serializable getSerializable (String key);
	}
	
	private static class BundleStorage implements Storage {
		
		Bundle bundle;				
		
		public BundleStorage (Bundle bundle)
		{
			this.bundle = bundle;
		}
		
		@Override
		public void write ()
		{
			/* empty */
		}

		@Override
		public void commit ()
		{
			/* empty */
		}

		@Override
		public void putInt (String key, int value)
		{
			bundle.putInt (key, value);
		}
		
		@Override
		public void putString (String key, String value)
		{
			bundle.putString (key, value);
		}
		
		@Override
		public void putLong (String key, long value)
		{
			bundle.putLong (key, value);
		}

		@Override
		public void putBoolean (String key, boolean value)
		{
			bundle.putBoolean (key, value);
		}

		@Override
		public void putSerializable (String key, Serializable value)
		{
			bundle.putSerializable (key, value);
		}

		@Override
		public boolean containsKey (String key)
		{
			return bundle.containsKey (key);
		}
		
		@Override
		public void removeKey (String key)
		{
			/* No need to do this, since we are not persisted */
		}

		@Override
		public int getInt (String key)
		{
			return bundle.getInt (key);
		}
		
		@Override
		public String getString (String key)
		{
			return bundle.getString (key);
		}
		
		@Override
		public boolean getBoolean (String key)
		{
			return bundle.getBoolean (key);
		}		

		@Override
		public long getLong (String key)
		{
			return bundle.getLong (key);
		}		
		
		@Override
		public Serializable getSerializable (String key)
		{
			return bundle.getSerializable (key);
		}
	}
	
	private static class PreferencesStorage implements Storage {
		
		SharedPreferences prefs;
		
		Editor editor;
		
		String pfx;
		
		public PreferencesStorage (SharedPreferences prefs, String pfx)
		{
			this.prefs = prefs;
			this.pfx = pfx;
		}
		
		private String key (String key)
		{
			return pfx + "." + key;
		}
		
		@Override
		public void write ()
		{
			editor = prefs.edit ();
		}

		@Override
		public void commit ()
		{
			editor.commit ();
			editor = null;
		}
		
		@Override
		public void putInt (String key, int value)
		{
			editor.putInt (key (key), value);
		}
		
		@Override
		public void putString (String key, String value)
		{
			editor.putString (key (key), value);
		}
		
		@Override
		public void putLong (String key, long value)
		{
			editor.putLong (key (key), value);
		}

		@Override
		public void putBoolean (String key, boolean value)
		{
			editor.putBoolean (key (key), value);
		}

		@Override
		public void putSerializable (String key, Serializable value)
		{
			/* ignored */
		}
		
		@Override
		public boolean containsKey (String key)
		{
			return prefs.contains (key (key));
		}
		
		@Override
		public void removeKey (String key)
		{
			editor.remove (key (key));
		}

		@Override
		public int getInt (String key)
		{
			return prefs.getInt (key (key), -1);
		}
		
		@Override
		public String getString (String key)
		{
			return prefs.getString (key (key), null);
		}
		
		@Override
		public boolean getBoolean (String key)
		{
			return prefs.getBoolean (key (key), false);
		}		

		@Override
		public long getLong (String key)
		{
			return prefs.getLong (key (key), -1);
		}		
		
		@Override
		public Serializable getSerializable (String key)
		{
			return null;
		}
	}

	private static final String PREFIX = "com.wanikani.wanikaninotifier.DashboardData.";

	private static final String KEY_USERNAME = PREFIX + "username";
	private static final String KEY_TITLE = PREFIX + "title";
	private static final String KEY_LEVEL = PREFIX + "level";
	private static final String KEY_CREATION = PREFIX + "creation";
	private static final String KEY_VACATION = PREFIX + "vacation";
	
	private static final String KEY_LESSONS_AVAILABLE = PREFIX + "lessons_available";
	private static final String KEY_REVIEWS_AVAILABLE = PREFIX + "reviews_available";
	private static final String KEY_NEXT_REVIEW_DATE = PREFIX + "next_review_date";
	private static final String KEY_REVIEWS_AVAILABLE_NEXT_HOUR = PREFIX + "reviews_available_next_hour";
	private static final String KEY_REVIEWS_AVAILABLE_NEXT_DAY = PREFIX + "reviews_available_next_day";
	private static final String KEY_EXCEPTION = PREFIX + "exception";	
	
	private static final String KEY_APPRENTICE = PREFIX + "apprentice";
	private static final String KEY_GURU = PREFIX + "guru";
	private static final String KEY_MASTER = PREFIX + "master";
	private static final String KEY_ENLIGHTEN = PREFIX + "enlighten";
	private static final String KEY_BURNED = PREFIX + "burned";
	
	private static final String KEY_RADICALS_PROGRESS = PREFIX + "radicals_progress";
	private static final String KEY_KANJI_PROGRESS = PREFIX + "kanji_progress";

	private static final String KEY_RADICALS_UNLOCKED = PREFIX + "radicals_unlocked";
	private static final String KEY_KANJI_UNLOCKED = PREFIX + "kanji_unlocked";

	private static final String KEY_RADICALS_TOTAL = PREFIX + "radicals_total";
	private static final String KEY_KANJI_TOTAL = PREFIX + "kanji_total";

	private static final String KEY_KANJI_APPRENTICE = PREFIX + "kanji_apprentice";
	private static final String KEY_KANJI_GURU = PREFIX + "kanji_guru";
	private static final String KEY_KANJI_MASTER = PREFIX + "kanji_master";
	private static final String KEY_KANJI_ENLIGHTEN = PREFIX + "kanji_enlighten";
	private static final String KEY_KANJI_BURNED = PREFIX + "kanji_burned";
	
	private static final String KEY_VOCAB_APPRENTICE = PREFIX + "vocab_apprentice";
	private static final String KEY_VOCAB_GURU = PREFIX + "vocab_guru";
	private static final String KEY_VOCAB_MASTER = PREFIX + "vocab_master";
	private static final String KEY_VOCAB_ENLIGHTEN = PREFIX + "vocab_enlighten";
	private static final String KEY_VOCAB_BURNED = PREFIX + "vocab_burned";
	
	private static final String KEY_CRITICAL_ITEMS = PREFIX + "critical_items";
	
	private static final String KEY_CURRENT_LEVEL_RADICALS = PREFIX + "current_level_radicals";
	private static final String KEY_CURRENT_LEVEL_KANJI = PREFIX + "current_level_kanji";
	
	private static final String PREFERENCES_FILE = "dd.xml";

	public int lessonsAvailable;
	
	public int reviewsAvailable;
	
	public Date nextReviewDate;
	
	public String username;
	
	public String title;
	
	public int level;
	
	public Date creation;
	
	public Bitmap gravatar;
	
	public int reviewsAvailableNextHour;
	
	public int reviewsAvailableNextDay;
	
	public boolean vacation;
	
	public OptionalData od;

	public IOException e;
		
	/**
	 * Constructor. An instance is created from the information returned
	 * by the WaniKani API. Any of the arguments may be null; in that
	 * case its corresponding field values will be unspecified
	 * @param ui the user information
	 * @param sq the study queue
	 * @param srs the SRS distribution info
	 */
	public DashboardData (UserInformation ui, StudyQueue sq)
	{
		od = new OptionalData ();
		
		username = ui.username;
		title = ui.title;
		level = ui.level;
		gravatar = ui.gravatarBitmap;
		creation = ui.creationDate;
		vacation = ui.vacationDate != null;

		reviewsAvailable = sq.reviewsAvailable;
		lessonsAvailable = sq.lessonsAvailable;
		nextReviewDate = sq.nextReviewDate;
		reviewsAvailableNextHour = sq.reviewsAvailableNextHour;
		reviewsAvailableNextDay = sq.reviewsAvailableNextDay;		
	}
		
	/**
	 * Constructor. An instance is created by unmarshalling a bundle.
	 * @param bundle the bundle where to read the data from
	 */
	public DashboardData (Bundle bundle)
	{
		od = new OptionalData ();
		
		deserialize (bundle);	
	}
	
	/**
	 * Constructor. An instance is created by unmarshalling a generic storage
	 * @param storage the storage where to read the data from
	 */
	private DashboardData (Storage storage)
	{
		od = new OptionalData ();
		
		doDeserialize (storage);	
	}

	/**
	 * Updates this object with optional data.
	 * 	@param od the optional data
	 */
	public void setOptionalData (OptionalData od)
	{
		this.od = od;
	}

	/**
	 * Merge the data contained in this object with an external (<i>older</i>) 
	 * object. This isn't much intuitive, but the implementation
	 * is safer (look at the code if you don't believe it :).
	 * 	@param dd the new data
	 */
	public void merge (DashboardData dd)
	{
		od.merge (dd.od);
	}

	/**
	 * A constructor to be used when information retrieval fails 
	 * @param e the exception that occurred
	 */
	public DashboardData (IOException e)
	{
		this.e = e;
	
		od = new OptionalData ();
	}
		
	/**
	 * If information retrieval fails, this method throws the an exception
	 * describing the failure event. Otherwise, this method does nothing
	 * @throws IOException 
	 */
	public void wail ()
		throws IOException
	{
		if (e != null)
			throw e;
	}
	
	/**
	 * Serialize data into a bundle
	 * @param bundle the destination bundle
	 */
	public void serialize (Bundle bundle)
	{
		doSerialize (new BundleStorage (bundle));
	}
	
	private static SharedPreferences prefs (Context ctxt)
	{
		return ctxt.getSharedPreferences (PREFERENCES_FILE, Context.MODE_PRIVATE);
	}
	
	public void serialize (Context ctxt, Source src)
	{
		doSerialize (new PreferencesStorage (prefs (ctxt), src.getPrefix ()));
	}
	
	protected void doSerialize (Storage storage)
	{
		boolean ok;
		
		ok = false;
		storage.write ();
		
		try {
		
			storage.putString (KEY_USERNAME, username);
			storage.putString (KEY_TITLE, title);
			storage.putInt(KEY_LEVEL, level);
			/* It could be null, if e != null */
			if (creation != null)
				storage.putLong (KEY_CREATION, creation.getTime ());
			else
				storage.removeKey (KEY_CREATION);
			storage.putBoolean (KEY_VACATION, vacation);
		
			storage.putInt (KEY_LESSONS_AVAILABLE, lessonsAvailable);
			storage.putInt (KEY_REVIEWS_AVAILABLE, reviewsAvailable);
			if (nextReviewDate != null)
				storage.putLong (KEY_NEXT_REVIEW_DATE, nextReviewDate.getTime ());
			else
				storage.removeKey (KEY_NEXT_REVIEW_DATE);
			storage.putInt (KEY_REVIEWS_AVAILABLE_NEXT_HOUR, reviewsAvailableNextHour);
			storage.putInt (KEY_REVIEWS_AVAILABLE_NEXT_DAY, reviewsAvailableNextDay);
		
			if (od.srs != null) {
				storage.putInt (KEY_APPRENTICE, od.srs.apprentice.total);
				storage.putInt (KEY_GURU, od.srs.guru.total);
				storage.putInt (KEY_MASTER, od.srs.master.total);
				storage.putInt (KEY_ENLIGHTEN, od.srs.enlighten.total);
				storage.putInt (KEY_BURNED, od.srs.burned.total);

				storage.putInt (KEY_KANJI_APPRENTICE, od.srs.apprentice.kanji);
				storage.putInt (KEY_KANJI_GURU, od.srs.guru.kanji);
				storage.putInt (KEY_KANJI_MASTER, od.srs.master.kanji);
				storage.putInt (KEY_KANJI_ENLIGHTEN, od.srs.enlighten.kanji);
				storage.putInt (KEY_KANJI_BURNED, od.srs.burned.kanji);
			
				storage.putInt (KEY_VOCAB_APPRENTICE, od.srs.apprentice.vocabulary);
				storage.putInt (KEY_VOCAB_GURU, od.srs.guru.vocabulary);
				storage.putInt (KEY_VOCAB_MASTER, od.srs.master.vocabulary);
				storage.putInt (KEY_VOCAB_ENLIGHTEN, od.srs.enlighten.vocabulary);
				storage.putInt (KEY_VOCAB_BURNED, od.srs.burned.vocabulary);
			} else
				storage.removeKey (KEY_APPRENTICE);
		
			if (od.elp != null) {
				storage.putInt(KEY_RADICALS_PROGRESS, od.elp.radicalsProgress);
				storage.putInt (KEY_RADICALS_UNLOCKED, od.elp.radicalsUnlocked);
				storage.putInt(KEY_RADICALS_TOTAL, od.elp.radicalsTotal);
				storage.putInt(KEY_KANJI_PROGRESS, od.elp.kanjiProgress);
				storage.putInt (KEY_KANJI_UNLOCKED, od.elp.kanjiUnlocked);
				storage.putInt (KEY_KANJI_TOTAL, od.elp.kanjiTotal);
				storage.putInt (KEY_CURRENT_LEVEL_RADICALS, od.elp.currentLevelRadicalsAvailable);
				storage.putInt (KEY_CURRENT_LEVEL_KANJI, od.elp.currentLevelKanjiAvailable);
			
			} else
				storage.removeKey (KEY_RADICALS_PROGRESS);
		
			if (od.ciStatus == OptionalDataStatus.RETRIEVED)
				storage.putInt (KEY_CRITICAL_ITEMS, od.criticalItems);
			else
				storage.removeKey (KEY_CRITICAL_ITEMS);
		
			if (e != null)
				storage.putSerializable (KEY_EXCEPTION, e);
			ok = true;
		} finally {
			if (!ok)
				storage.removeKey (KEY_USERNAME);
			
			storage.commit ();
		}
	}
	
	/**
	 * Deserialize data from a bundle
	 * @param bundle the source bundle
	 */
	private void deserialize (Bundle bundle)
	{
		doDeserialize (new BundleStorage (bundle));
	}
	
	public static DashboardData fromPreferences (Context ctxt, Source src)
	{
		Storage storage;
		
		storage = new PreferencesStorage (prefs (ctxt), src.getPrefix ());
		
		return storage.containsKey (KEY_USERNAME) ? new DashboardData (storage) : null;
	}
	
	protected void doDeserialize (Storage storage)
	{		
		username = storage.getString (KEY_USERNAME);
		title = storage.getString (KEY_TITLE);
		level = storage.getInt (KEY_LEVEL);
		if (storage.containsKey (KEY_CREATION))
			creation = new Date (storage.getLong (KEY_CREATION));
		vacation = storage.getBoolean (KEY_VACATION);
		
		lessonsAvailable = storage.getInt (KEY_LESSONS_AVAILABLE);
		reviewsAvailable = storage.getInt (KEY_REVIEWS_AVAILABLE);
		if (storage.containsKey (KEY_NEXT_REVIEW_DATE))
			nextReviewDate = new Date (storage.getLong (KEY_NEXT_REVIEW_DATE));
		reviewsAvailableNextHour = storage.getInt (KEY_REVIEWS_AVAILABLE_NEXT_HOUR);
		reviewsAvailableNextDay = storage.getInt (KEY_REVIEWS_AVAILABLE_NEXT_DAY);

		if (storage.containsKey (KEY_APPRENTICE)) {
			od.srs = new SRSDistribution ();

			od.srsStatus = OptionalDataStatus.RETRIEVED;
			od.srs.apprentice.total = storage.getInt (KEY_APPRENTICE);
			od.srs.guru.total = storage.getInt (KEY_GURU);
			od.srs.master.total = storage.getInt (KEY_MASTER);
			od.srs.enlighten.total = storage.getInt (KEY_ENLIGHTEN);
			od.srs.burned.total = storage.getInt (KEY_BURNED);
				
			od.srs.apprentice.kanji = storage.getInt (KEY_KANJI_APPRENTICE);
			od.srs.guru.kanji = storage.getInt (KEY_KANJI_GURU);
			od.srs.master.kanji = storage.getInt (KEY_KANJI_MASTER);
			od.srs.enlighten.kanji = storage.getInt (KEY_KANJI_ENLIGHTEN);
			od.srs.burned.kanji = storage.getInt (KEY_KANJI_BURNED);
				
			od.srs.apprentice.vocabulary = storage.getInt (KEY_VOCAB_APPRENTICE);
			od.srs.guru.vocabulary = storage.getInt (KEY_VOCAB_GURU);
			od.srs.master.vocabulary = storage.getInt (KEY_VOCAB_MASTER);
			od.srs.enlighten.vocabulary = storage.getInt (KEY_VOCAB_ENLIGHTEN);
			od.srs.burned.vocabulary = storage.getInt (KEY_VOCAB_BURNED);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
		     * will do right after calling this method */
			od.srsStatus = OptionalDataStatus.RETRIEVING;
			od.srs = null;
		}
		
		if (storage.containsKey (KEY_RADICALS_PROGRESS)) {
			od.elp = new ExtendedLevelProgression ();
				
			od.lpStatus = OptionalDataStatus.RETRIEVED;
			od.elp.radicalsProgress = storage.getInt (KEY_RADICALS_PROGRESS);
			od.elp.radicalsUnlocked = storage.getInt (KEY_RADICALS_UNLOCKED);
			od.elp.radicalsTotal = storage.getInt (KEY_RADICALS_TOTAL);
			od.elp.kanjiProgress = storage.getInt (KEY_KANJI_PROGRESS);
			od.elp.kanjiUnlocked = storage.getInt (KEY_KANJI_UNLOCKED);
			od.elp.kanjiTotal = storage.getInt (KEY_KANJI_TOTAL);
			od.elp.currentLevelRadicalsAvailable = storage.getInt (KEY_CURRENT_LEVEL_RADICALS);
			od.elp.currentLevelKanjiAvailable = storage.getInt (KEY_CURRENT_LEVEL_KANJI);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
			 * will do right after calling this method */
			od.lpStatus = OptionalDataStatus.RETRIEVING;
			od.elp = null;
		}
			
		if (storage.containsKey (KEY_CRITICAL_ITEMS)) {
			od.ciStatus = OptionalDataStatus.RETRIEVED;
			od.criticalItems = storage.getInt (KEY_CRITICAL_ITEMS);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
			 * will do right after calling this method */
			od.ciStatus = OptionalDataStatus.RETRIEVING;
			od.criticalItems = 0;
		}
			
		if (storage.containsKey (KEY_EXCEPTION))
			e = (IOException) storage.getSerializable (KEY_EXCEPTION);
		else
			e = null;
		}
	
	/**
	 * Tells whether some optional data is still missing.
	 * @return true if a refresh of optional data is advisable
	 */
	public boolean isIncomplete ()
	{
		return od.isIncomplete ();
	}
}