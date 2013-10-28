package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Date;

import android.os.Bundle;

import com.wanikani.wklib.AuthenticationException;

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
 * A state machine that decides when to display and to hide the
 * notification icon. 
 * The idea behind this implementation is to avoid bothering the user
 * too much, and generate as little traffic as possible.
 * The drawback is that status changes may be quite slow to be
 * detected; this does not affect much the "show icon" part, since
 * the WaniKani API provides the exact time when reviews will be pending. 
 * On the other hand, if a notification icon is shown and the user
 * starts consuming the reviews, it may take a really long time before
 * it will be removed. This should not be an issue because usually
 * users will tap on the event and make the icon disappear.
 * <p>
 * This FSM features three states:
 * <ul>
 *	<li>No reviews pending: this is simply handled by sleeping
 *		till the next review will be available (and this
 *		is published by the study queue API)
 *	<li>Reviews pending: here we start polling the API using 
 *		a capped exponential backoff scheme, and hide the icon
 *		as soon as we detect some activity (i.e. the number
 *		of pending reviews diminishes)
 *   	<li>Error: again, we apply an exponential backoff algorithm
 *		and start polling the site. Since timeouts grow
 *		quickly large, feeding the state machine with connectivity 
 *		changes might prove useful. This applies particularly 
 *		to boot time because the application may be started
 *		before networking is completely set up.
 * </ul>
 */
public class NotifierStateMachine {

	/** Polling time when the user reviewing (number of reviews
	 *  decreasing) */
	private static int T_INT_REVIEWING = 4;
	
	/** Initial polling timeout after new reviews become available.
	 *  This interval is doubled and capped by {@link #T_CAP_REVIEWS} */
	private static int T_INT_REVIEWS = T_INT_REVIEWING;
	
	/** Timeout for an user who has no reviews pending */
	private static int T_NO_REVIEWS = 60;

	/** Cap polling timeout when reviews are availble (one hour) */
	private static int T_CAP_REVIEWS = 60;
	
	/** Timeout when waiting for reviews */
	private static int T_INT_WAITING_FOR_REVIEWS = 1; 
	
	/** Timeout when waiting for reviews */
	private static int T_CAP_WAITING_FOR_REVIEWS = 30; 

	/** Initial polling timeout when we can't contact the server.
	 *  This interval is doubled and capped by {@link #T_CAP_ERROR} */
	private static int T_INT_ERROR = 1;
	
	/** Cap polling timeout when we can't contact the server (one day) */
	private static int T_CAP_ERROR = 24 * 60;
	
	/** Retry timeout when it is review time but no reviews are in the
	 *  queue (this means clock disalignment between the terminal and WaniKani */
	private static int T_INT_CLOCK_COMPENSATION = 3;
	
	/**
	 * This enum conveys additional info to @link {@link NotifierStateMachine#next(DashboardData)},
	 * in order to let the state machine know what event triggered the state machine.
	 */
	public enum Event {
		
		/** The state machine boots */ 
		E_INITIAL {
			public MeterSpec.T meter () {
				return MeterSpec.T.NOTIFY_TIMEOUT;
			}
		},
		
		/** A timeout explicitly set by the state machine */
		E_SOLICITED {
			public MeterSpec.T meter () {
				return MeterSpec.T.NOTIFY_TIMEOUT;
			}
		},
		
		/** Any event of interest to the state machine (connectivity change) */
		E_UNSOLICITED {
			public MeterSpec.T meter () {
				return MeterSpec.T.NOTIFY_CHANGE_CONNECTIVITY;
			}
		},
		
		/** The user tapping the notification icon */
		E_TAP {
			public MeterSpec.T meter () {
				return MeterSpec.T.NOTIFY_TIMEOUT;
			}
		};
		
		public abstract MeterSpec.T meter ();
	};

	static enum State {

 		/**
		 * The state machine enters this state when no reviews are 
		 * available.
		 */
		S_NO_REVIEWS {
			public void enter (NotifierStateMachine fsm, Event event, 
								State prev, DashboardData ldd, DashboardData cdd) 
				{
					fsm.ifc.hideNotification ();
					if (cdd.nextReviewDate == null)
						fsm.schedule (T_NO_REVIEWS);
					else if (cdd.nextReviewDate.after (new Date ()))
						fsm.schedule (cdd.nextReviewDate, 10000);
					else
						fsm.schedule (T_INT_CLOCK_COMPENSATION); 
				}
		},

 		/**
		 * The state machine enters this state when there are reviews, but 
		 * they have not reached the threshold
		 */
		S_TOO_FEW_REVIEWS {
			public void enter (NotifierStateMachine fsm, Event event, 
							   State prev, DashboardData ldd, DashboardData cdd) 
				{
					fsm.ifc.hideNotification ();
					if (prev != this)
						fsm.schedule (NotifierStateMachine.T_INT_WAITING_FOR_REVIEWS);
					else
						fsm.schedule (NotifierStateMachine.T_INT_WAITING_FOR_REVIEWS,
									  NotifierStateMachine.T_CAP_WAITING_FOR_REVIEWS);
				}
		},

		/**
		 * The state machine enters this state when there are
		 * pending reviews. Here we have to poll.
		 */
		S_REVIEWS_AVAILABLE {			
				public void enter (NotifierStateMachine fsm, Event event, 
									State prev, DashboardData ldd, DashboardData cdd) 
				{
					if (event == Event.E_TAP ||
						(prev == this && detectActivity (ldd, cdd))) {
						fsm.ifc.hideNotification ();
						fsm.schedule (NotifierStateMachine.T_INT_REVIEWING);
					} else if (prev != this) {
						fsm.ifc.showNotification (cdd.reviewsAvailable);
						fsm.schedule (NotifierStateMachine.T_INT_REVIEWS);
					} else {
						fsm.ifc.showNotification (cdd.reviewsAvailable);
						fsm.schedule (NotifierStateMachine.T_INT_REVIEWS,
									  NotifierStateMachine.T_CAP_REVIEWS);
					}
				}			
		},

 		/**
		 * The state machine enters this state when we can't contact
		 * the server anymore. Start polling.
		 */
		S_ERROR {
			public void enter (NotifierStateMachine fsm, Event event, 
							   State prev, DashboardData ldd, DashboardData cdd) 
				{
					if (cdd.e instanceof AuthenticationException)
						fsm.schedule (NotifierStateMachine.T_CAP_ERROR);
					else if (prev != this || 
							 ldd != null && ldd.e != null &&  
							 ldd.e.getClass () != cdd.e.getClass ())  
						fsm.schedule (NotifierStateMachine.T_INT_ERROR);
					else
						fsm.schedule (NotifierStateMachine.T_INT_ERROR,
							      	  NotifierStateMachine.T_CAP_ERROR);
				}
		};

		/**
		 * Called on entering this state. Each state implementation
		 * decides how long to wait before triggering a new
		 * timeout event.
		 *	@param fsm the state machine
		 *  @param e the event that triggered the call
		 *	@param prev the previous state (may be self)
		 *	@param ldd the previous study queue data (or null)
		 *	@param cdd the current study queue data
		 */
		public abstract void enter (NotifierStateMachine fsm, Event e, State prev,
						   			DashboardData ldd, DashboardData cdd);	 

		/**
		 * Tells whether the user is currently reviewing.
		 * This is done by comparing the new study queue
		 * with the previous one.
		 *	@param ldd the previous study queue data (or null)
		 *	@param cdd the current study queue data
		 */
		protected boolean detectActivity (DashboardData ldd, 
						  				  DashboardData cdd)
		{
			if (ldd == null || ldd.e != null)
				return false;
			
			/* Might add also a similar check for 
			 * lessonsAvailable. 
			 * Don't know if that would be better or worse */
			return (cdd.reviewsAvailable < ldd.reviewsAvailable);
		}
	}	
		
	/// The callback interface
	NotifierInterface ifc;
	
	/// Last state
	State lstate;
	
	/// Last study queue sample
	DashboardData ldd;
	
	/// Last timeout interval (needed for exponential backoff)
	int ldelta;

	/// Bundle data prefix
	private static final String PREFIX = "com.wanikani.wanikaninotifier.NotifierStateMachine.";

	/// Last state bundle key
	private static final String KEY_LAST_STATE = PREFIX + "last_state";
	
	/// Last delta bundle key
	private static final String KEY_LAST_DELTA = PREFIX + "last_delta";
	
	/// Last sample present bundle key
	private static final String KEY_LAST_DATA = PREFIX + "last_data";

	/**
	 * Constructor.
	 *	@param ifc callback interface
	 */
	public NotifierStateMachine (NotifierInterface ifc)
	{
		this.ifc = ifc;
		lstate = null;
		ldd = null;
	}
	
	/**
	 * Constructor. Deserializes its state a bundle enriched
	 * by {@link #serialize}.
	 *	@param ifc callback interface
	 *	@param bundle a bundle containing
	 */
	public NotifierStateMachine (NotifierInterface ifc, Bundle bundle)
	{
		this.ifc = ifc;
		if (bundle.containsKey (KEY_LAST_STATE))
			lstate = State.valueOf (State.class, bundle.getString (KEY_LAST_STATE));
		ldelta = bundle.getInt (KEY_LAST_DELTA);
		if (bundle.containsKey (KEY_LAST_DATA))
			ldd = new DashboardData (bundle);
	}

	/**
	 * Serializes the state of a this instance into a bundle.
	 *	@param bundle a bundle containing
	 */
	public void serialize (Bundle bundle)
	{
		if (lstate != null)
			bundle.putString (KEY_LAST_STATE, lstate.name ());		
		bundle.putInt (KEY_LAST_DELTA, ldelta);
		if (ldd != null) {
			bundle.putBoolean (KEY_LAST_DATA, true);
			ldd.serialize (bundle);
		}
	}

	/**
	 * Called when a timeout (or network connectivity change) event
	 * is triggered <i>and</i> study queue data is available
	 *  @param event the kind of event
	 *  @param threshold the number of reviews needed to show a notification 
	 *	@param dd the study queue
	 */
	public void next (Event event, int threshold, DashboardData dd)
	{
		State cstate, llstate;
		DashboardData lldd;
		
		try {
			dd.wail ();
			if (dd.reviewsAvailable >= threshold)
				cstate = State.S_REVIEWS_AVAILABLE;
			else if (dd.reviewsAvailable > 0)
				cstate = State.S_TOO_FEW_REVIEWS;
			else
				cstate = State.S_NO_REVIEWS;
		
		} catch (IOException e) {
			cstate = State.S_ERROR;			
		}

		/* Must set this before calling enter, so schedule will
		 * serialize all the stuff correctly */
		llstate = lstate;
		lldd = ldd;
		
		lstate = cstate;
		ldd = dd;
		
		cstate.enter (this, event, llstate, lldd, dd);
	}
	
 	/**
	 * Schedule a timeout at a given point in time, resetting
	 * the exponential backoff algorithm.
	 *	@param int the delay in minutes
	 */
	void schedule (int delta)
	{
		ldelta = 0;
		
		schedule (delta, delta);
	}
	
 	/**
	 * Schedule a timeout at a given point in time, using
	 * the exponential backoff algorithm.
	 *	@param delta the initial delay in minutes
	 *	@param cal the backoff in minutes
	 */
	void schedule (int delta, int cap)
	{
		long now;
		
		if (ldelta == 0)
			ldelta = delta;
		else {
			ldelta <<= 1;
			if (ldelta > cap)
				ldelta = cap;
		}
	
		now = System.currentTimeMillis ();
		ifc.schedule (this, new Date (now + ldelta * 60 * 1000));
	}

	/**
	 * Schedule a timeout at a given point in time.
	 *	@param date when to trigger it
	 *	@param tolerance an extra delay (in milliseconds), to
	 *		take into consideration small differences between terminal
	 *		and server clock
	 */
	void schedule (Date date, long tolerance)
	{
		ldelta = 0;
		
		ifc.schedule (this, new Date (tolerance + date.getTime ()));
	}

}
