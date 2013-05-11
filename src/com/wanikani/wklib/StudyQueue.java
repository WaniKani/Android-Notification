package com.wanikani.wklib;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

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


public class StudyQueue {
	
	public int lessonsAvailable;
	
	public int reviewsAvailable;
	
	public Date nextReviewDate;
	
	public int reviewsAvailableNextHour;
	
	public int reviewsAvailableNextDay;
	
	StudyQueue (JSONObject obj)
		throws JSONException
	{
		lessonsAvailable = Util.getInt (obj, "lessons_available");
		reviewsAvailable = Util.getInt (obj, "reviews_available");
		nextReviewDate = Util.getDate (obj, "next_review_date");
		reviewsAvailableNextHour = Util.getInt (obj, "reviews_available_next_hour");
		reviewsAvailableNextDay = Util.getInt (obj, "reviews_available_next_day");
	}
}
