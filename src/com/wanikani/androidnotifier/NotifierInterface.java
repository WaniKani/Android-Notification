package com.wanikani.androidnotifier;

import java.util.Date;

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
 * A notification interface used by {@link NotifierStateMachine} to control
 * rescheduling of events and notifications to be shown. 
 */
public interface NotifierInterface {

	/**
	 * Request the interface retrieve data from WaniKani and
	 * feed {@link NotifierStateMachine#next(com.wanikani.wklib.StudyQueue)} with it
	 * at a given time.
	 *  @param fsm the state machine requesting this scheduling
	 *	@param date when the timer should be tiggered
	 */
	public void schedule (NotifierStateMachine fsm, Date date);
	
	/**
	 * Called when the notification icon should be shown
	 * @param reviews the number of reviews in the study queue
	 */
	public void showNotification (int reviews);
	
	/**
	 * Called when the notification icon should be hidden
	 */
	public void hideNotification ();
}
