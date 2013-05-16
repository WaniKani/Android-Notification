package com.wanikani.androidnotifier;

import android.os.Handler;

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
 * A class that can generate a notification at a given point in time.
 * This is a little bit tricky, because the cheapest way to do this
 * (i.e. through an {@link com.os.Handler}) has a big drawback:
 * the countdown is frozen when the device enters deep sleep, and 
 * resumes when it is awaken.
 * We deal with this problem by providing the {@link #screenOn()}
 * method, to be called when it is probable that the screen has
 * been turned on. As a result, if the timeout expires when in deep 
 * sleep mode, the notification will be delivered as soon as the screen is
 * powered on (which is the sensible behaviour in our case, anyway).
 * An instance of this class can handle at most one pending event 
 * at a time.  
 */
public class Alarm {

	/** 
	 * This class wraps the runnable to be called at scheduled
	 * time.
	 */
	class Event implements Runnable {

		/// The runnable to be run
		Runnable r;
		
		/**
		 * Constructor.
		 * @param r the runnable this class wraps
		 */
		public Event (Runnable r)
		{
			this.r = r;			
		}
		
		/**
		 * Called by the handler when the timeout expires.
		 * It relays the call the originating class after
		 * making sure this instance is the one currently
		 * active. If so, it clears the pointer in the
		 * enclosing class.
		 */
		public void run ()
		{
			if (event == this) {
				// First clear event, /then/, run the code, otherwise
				// funny things happen :)
				event = null;
				r.run ();
			}
		}
			
		
	}
	
	/** The handler that awakes us */
	Handler handler;

	/** The class to notify, or <code>null</code> if no notifications
	 * are pending */
	Event event;
	
	/** When the timeout expires, as a timestamp */
	long target;
	
	/**
	 * Constructor. Calls of this method must be matched by calls to
	 * {@link #stopAlarm} to properly unregister the receiver.
	 * @see #stopAlarm
	 * @param ctxt the context this object is called from
	 */
	public Alarm ()
	{
		handler = new Handler ();		
	}
	
	/**
	 * Must be called when the object is not needed anymore.
	 */
	public void stopAlarm ()
	{
		cancel ();
	}
		
	/**
	 * Must be called when the screen is powered on. 
	 * Adjusts the timeout (or relays the notification).
	 * Spurious calls to this method (i.e. calls made when
	 * there is no real transition from deep sleep mode)
	 * do not cause malfunctions.  
	 */
	public void screenOn ()
	{
		long remain; 

		if (event == null)
			return;
				
		/* The timeout is no longer accurate */
		handler.removeCallbacks (event);
		
		remain = target - System.currentTimeMillis ();
 		if (remain > 0)
			handler.postDelayed (event, remain);
		else
			handler.post (event);
	}
	
	/**
	 * Schedules a notification at a point in time, canceling
	 * the previous one if any. The notification will be delivered
	 * on the first instant after the delay is expired <i>and</i>
	 * the screen is powered on.  
	 */
	public void schedule (Runnable r, long delay)
	{
		cancel ();
		target = System.currentTimeMillis () + delay;
		event = new Event (r);
		screenOn ();
	}
	
	/**
	 * Cancels the pending notification, if any.
	 */
	public void cancel ()
	{
		if (event != null) {
			handler.removeCallbacks (event);
			event = null;
		}
	}
	
	/**
	 * Returns the number seconds to the next event.
	 * 	@return the number of seconds (or null, if no timeout
	 * 		scheduled
	 */
	public Long remaining ()
	{
		if (event == null)
			return null;
		
		return (target - System.currentTimeMillis ()) / 1000; 
	}

}
