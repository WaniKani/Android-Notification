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
	
	public int lessonsAvailable;
	
	public int reviewsAvailable;
	
	public Date nextReviewDate;
	
	public String username;
	
	public String title;
	
	public int level;
	
	public Bitmap gravatar;
	
	public int reviewsAvailableNextHour;
	
	public int reviewsAvailableNextDay;
	
	public int apprentice;
	
	public int guru;
	
	public int master;
	
	public int enlighten;
	
	public int burned;
	
	public LevelProgression lp;
	
	public IOException e;
		
	/**
	 * Constructor. An instance is created from the information returned
	 * by the WaniKani API. Any of the arguments may be null; in that
	 * case its corresponding field values will be unspecified
	 * @param ui the user information
	 * @param sq the study queue
	 * @param srs the SRS distribution info
	 * @param lp the level progression
	 */
	public DashboardData (UserInformation ui, StudyQueue sq, SRSDistribution srs)
	{
		if (ui != null) {
			username = ui.username;
			title = ui.title;
			level = ui.level;
			gravatar = ui.gravatarBitmap;
		}		
		if (sq != null) {
			reviewsAvailable = sq.reviewsAvailable;
			lessonsAvailable = sq.lessonsAvailable;
			nextReviewDate = sq.nextReviewDate;
			reviewsAvailableNextHour = sq.reviewsAvailableNextHour;
			reviewsAvailableNextDay = sq.reviewsAvailableNextDay;
		}		
		if (srs != null) {
			apprentice = srs.apprentice.total;
			guru = srs.guru.total;
			master = srs.master.total;
			enlighten = srs.enlighten.total;
			burned = srs.burned.total;
		}
	}
		
	/**
	 * Constructor. An instance is created by unmarshalling a bundle.
	 * @param bundle the bundle where to read the data from
	 */
	public DashboardData (Bundle bundle)
	{
		deserialize (bundle);	
	}
	
	/**
	 * Updates this object, setting level progression data.
	 * 	@param lp a populated level progression object
	 */
	public void setLevelProgression (LevelProgression lp)
	{
		this.lp = lp;
	}
		
	/**
	 * A constructor to be used when information retrieval fails 
	 * @param e the exception that occurred
	 */
	public DashboardData (IOException e)
	{
		this.e = e;
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
		
		bundle.putInt (KEY_APPRENTICE, apprentice);
		bundle.putInt (KEY_GURU, guru);
		bundle.putInt (KEY_MASTER, master);
		bundle.putInt (KEY_ENLIGHTEN, enlighten);
		bundle.putInt (KEY_BURNED, burned);
		
		if (lp != null) {
			bundle.putInt(KEY_RADICALS_PROGRESS, lp.radicalsProgress);
			bundle.putInt(KEY_RADICALS_TOTAL, lp.radicalsTotal);
			bundle.putInt(KEY_KANJI_PROGRESS, lp.kanjiProgress);
			bundle.putInt(KEY_KANJI_TOTAL, lp.kanjiTotal);
			
		}
		
		if (e != null)
			bundle.putSerializable (KEY_EXCEPTION, e);
	}
	
	/**
	 * Deserialize data from a bundle
	 * @param bundle the source bundle
	 */
	public void deserialize (Bundle bundle)
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

		apprentice = bundle.getInt (KEY_APPRENTICE);
		guru = bundle.getInt (KEY_GURU);
		master = bundle.getInt (KEY_MASTER);
		enlighten = bundle.getInt (KEY_ENLIGHTEN);
		burned = bundle.getInt (KEY_BURNED);
		
		if (bundle.containsKey (KEY_RADICALS_PROGRESS)) {
			lp = new LevelProgression ();
			lp.radicalsProgress = bundle.getInt (KEY_RADICALS_PROGRESS);
			lp.radicalsTotal = bundle.getInt (KEY_RADICALS_TOTAL);
			lp.kanjiProgress = bundle.getInt (KEY_KANJI_PROGRESS);
			lp.kanjiTotal = bundle.getInt (KEY_KANJI_TOTAL);
		} else
			lp = null;
		
		if (bundle.containsKey (KEY_EXCEPTION))
			e = (IOException) bundle.getSerializable (KEY_EXCEPTION);
		else
			e = null;
		
	}
}