package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Date;

import android.graphics.Bitmap;
import android.os.Bundle;

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
class DashboardData {
	
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
		public LevelProgression lp;
		
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
		 * @param lp the level progression
		 * @param lpStatus level progression status
		 * @param criticalItems number of critical items
		 * @param ciStatus critical items status
		 */
		public OptionalData (SRSDistribution srs, DashboardData.OptionalDataStatus srsStatus, 
							 LevelProgression lp, DashboardData.OptionalDataStatus lpStatus,
							 int criticalItems, DashboardData.OptionalDataStatus ciStatus)
		{
			this.srs = srs;
			this.lp = lp;
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
				lp = od.lp;
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
					srsStatus != DashboardData.OptionalDataStatus.RETRIEVED;
		}
	};
	
	private static final String PREFIX = "com.wanikani.wanikaninotifier.DashboardData.";

	private static final String KEY_USERNAME = PREFIX + "username";
	private static final String KEY_TITLE = PREFIX + "title";
	private static final String KEY_LEVEL = PREFIX + "level";
	
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
	private static final String KEY_RADICALS_TOTAL = PREFIX + "radicals_total";
	private static final String KEY_KANJI_PROGRESS = PREFIX + "kanji_progress";
	private static final String KEY_KANJI_TOTAL = PREFIX + "kanji_total";
	
	private static final String KEY_CRITICAL_ITEMS = PREFIX + "critical_items";
	
	public int lessonsAvailable;
	
	public int reviewsAvailable;
	
	public Date nextReviewDate;
	
	public String username;
	
	public String title;
	
	public int level;
	
	public Bitmap gravatar;
	
	public int reviewsAvailableNextHour;
	
	public int reviewsAvailableNextDay;
	
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
		bundle.putString (KEY_USERNAME, username);
		bundle.putString (KEY_TITLE, title);
		bundle.putInt(KEY_LEVEL, level);
		
		bundle.putInt (KEY_LESSONS_AVAILABLE, lessonsAvailable);
		bundle.putInt (KEY_REVIEWS_AVAILABLE, reviewsAvailable);
		if (nextReviewDate != null)
			bundle.putLong (KEY_NEXT_REVIEW_DATE, nextReviewDate.getTime ());
		bundle.putInt (KEY_REVIEWS_AVAILABLE_NEXT_HOUR, reviewsAvailableNextHour);
		bundle.putInt (KEY_REVIEWS_AVAILABLE_NEXT_DAY, reviewsAvailableNextDay);
		
		if (od.srs != null) {
			bundle.putInt (KEY_APPRENTICE, od.srs.apprentice.total);
			bundle.putInt (KEY_GURU, od.srs.guru.total);
			bundle.putInt (KEY_MASTER, od.srs.master.total);
			bundle.putInt (KEY_ENLIGHTEN, od.srs.enlighten.total);
			bundle.putInt (KEY_BURNED, od.srs.burned.total);
		}		
		
		if (od.lp != null) {
			bundle.putInt(KEY_RADICALS_PROGRESS, od.lp.radicalsProgress);
			bundle.putInt(KEY_RADICALS_TOTAL, od.lp.radicalsTotal);
			bundle.putInt(KEY_KANJI_PROGRESS, od.lp.kanjiProgress);
			bundle.putInt(KEY_KANJI_TOTAL, od.lp.kanjiTotal);
			
		}
		
		if (od.ciStatus == OptionalDataStatus.RETRIEVED)
			bundle.putInt (KEY_CRITICAL_ITEMS, od.criticalItems);
		
		if (e != null)
			bundle.putSerializable (KEY_EXCEPTION, e);
	}
	
	/**
	 * Deserialize data from a bundle
	 * @param bundle the source bundle
	 */
	private void deserialize (Bundle bundle)
	{		
		username = bundle.getString (KEY_USERNAME);
		title = bundle.getString (KEY_TITLE);
		level = bundle.getInt (KEY_LEVEL);
		
		lessonsAvailable = bundle.getInt (KEY_LESSONS_AVAILABLE);
		reviewsAvailable = bundle.getInt (KEY_REVIEWS_AVAILABLE);
		if (bundle.containsKey (KEY_NEXT_REVIEW_DATE))
			nextReviewDate = new Date (bundle.getLong (KEY_NEXT_REVIEW_DATE));
		reviewsAvailableNextHour = bundle.getInt (KEY_REVIEWS_AVAILABLE_NEXT_HOUR);
		reviewsAvailableNextDay = bundle.getInt (KEY_REVIEWS_AVAILABLE_NEXT_DAY);

		if (bundle.containsKey (KEY_APPRENTICE)) {
			od.srs = new SRSDistribution ();

			od.srsStatus = OptionalDataStatus.RETRIEVED;
			od.srs.apprentice.total = bundle.getInt (KEY_APPRENTICE);
			od.srs.guru.total = bundle.getInt (KEY_GURU);
			od.srs.master.total = bundle.getInt (KEY_MASTER);
			od.srs.enlighten.total = bundle.getInt (KEY_ENLIGHTEN);
			od.srs.burned.total = bundle.getInt (KEY_BURNED);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
			 * will do right after calling this method */
			od.srsStatus = OptionalDataStatus.RETRIEVING;
			od.srs = null;
		}
		
		if (bundle.containsKey (KEY_RADICALS_PROGRESS)) {
			od.lp = new LevelProgression ();

			od.lpStatus = OptionalDataStatus.RETRIEVED;
			od.lp.radicalsProgress = bundle.getInt (KEY_RADICALS_PROGRESS);
			od.lp.radicalsTotal = bundle.getInt (KEY_RADICALS_TOTAL);
			od.lp.kanjiProgress = bundle.getInt (KEY_KANJI_PROGRESS);
			od.lp.kanjiTotal = bundle.getInt (KEY_KANJI_TOTAL);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
			 * will do right after calling this method */
			od.lpStatus = OptionalDataStatus.RETRIEVING;
			od.lp = null;
		}
		
		if (bundle.containsKey (KEY_CRITICAL_ITEMS)) {
			od.ciStatus = OptionalDataStatus.RETRIEVED;
			od.criticalItems = bundle.getInt (KEY_CRITICAL_ITEMS);
		} else {
			/* RETRIEVING is correct, because this is what DashboardActivity
			 * will do right after calling this method */
			od.ciStatus = OptionalDataStatus.RETRIEVING;
			od.criticalItems = 0;
		}

		if (bundle.containsKey (KEY_EXCEPTION))
			e = (IOException) bundle.getSerializable (KEY_EXCEPTION);
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