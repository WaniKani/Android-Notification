package com.wanikani.androidnotifier.notification;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;

import com.wanikani.androidnotifier.DashboardData;
import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.MeterSpec;
import com.wanikani.androidnotifier.SettingsActivity;
import com.wanikani.androidnotifier.WebReviewActivity;
import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.androidnotifier.notification.NotificationInterface.ChangeType;
import com.wanikani.androidnotifier.notification.NotifierStateMachine.Event;
import com.wanikani.wklib.Connection;
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
 * This service that show and hides the notification icon.
 * In other words, this class implements the main object of this app.
 * This service is started on boot by {@see NotificationService.BootReceiver} or
 * by {@link DashboardActivity} when notifications are enabled.
 * Conversely, it is stopped by {@link DashboardActivity} when 
 * notifications are disabled.
 * Since the notification logic is not straightforward,
 * we implement it in a separate class ({@link NotifierStateMachine})
 * while here is just the glue code that interacts with the operating system. 
 */
public class NotificationService 
	extends IntentService implements NotifierStateMachine.Interface {
	
	public static class StateData {
		
		public boolean hasReviews;
		
		public boolean hasLessons;
		
		public int reviews;
		
		public int lessons;
		
		public DashboardData dd;
		
		private static final String PREF_REVIEWS = PREFIX + "sd.reviews";
		
		private static final String PREF_LESSONS = PREFIX + "sd.lessons";

		private StateData (SharedPreferences prefs)
		{
			reviews = prefs.getInt (PREF_REVIEWS, 0);
			lessons = prefs.getInt (PREF_LESSONS, 0);
			hasReviews = reviews > 0;
			hasLessons = lessons > 0;
		}
		
		private void serialize (SharedPreferences prefs)
		{
			prefs.edit ().
				putInt (PREF_REVIEWS, hasReviews ? reviews : 0).
				putInt (PREF_LESSONS, hasLessons ? lessons : 0).commit ();
		}
	}
	
	/**
	 * This event receiver gets all the notifications needed by
	 * this class. Currently they are:
	 * <ul>
	 * 	<li><code>BOOT_COMPLETED</code> to start the notification
	 * 			state machine.
	 *  <li><code>CONNECTIVITY_ACTION</code> when the connectivity
	 * 			state changes.  This is useful to quickly fix IO errors,
	 * 			bypassing the exponential backoff algorithm the FSM would
	 * 			use.
	 *  <li><code>PACKAGE_REPLACED</code> to restart the notifier after
	 *  		our package gets updated
	 */
	public static class Receiver extends BroadcastReceiver {
	 
		/**
		 * Starts the {@link NotificationService} service.
		 *	@param context the context on which to send notifications
		 *	@param intent the <code>BOOT_COMPLETED</code> intent  
		 */
	    @Override
	    public void onReceive (Context context, Intent intent) 
	    {
			String action, sAction;
			Intent sIntent;
						
			action = intent.getAction ();
			if (action.equals (Intent.ACTION_BOOT_COMPLETED))
				sAction = ACTION_BOOT_COMPLETED;
			else if (action.equals (ConnectivityManager.CONNECTIVITY_ACTION))
				sAction = ACTION_CONNECTIVITY_CHANGE;
			else if (action.equals (Intent.ACTION_PACKAGE_REPLACED))
				sAction = ACTION_BOOT_COMPLETED;
			else
				return;
			
			sIntent = new Intent (context, NotificationService.class);
			sIntent.setAction (sAction);
			context.startService (sIntent);
	    }
	}
	
	/// Local prefix
	private static final String PREFIX = "com.wanikani.wanikaninotifier.NotificationService.";
	
	/// FSM bundle key
	private static final String KEY_FSM = PREFIX + "fsm";
	
	/// Dashboard data key
	public static final String KEY_DD = PREFIX + "dd";
	
	/// Current Notification Interface
	private NotificationInterface nifc;
	
	/// The autocancel notification implementation
	private AutoCancelNotification acn;
	
	/// The persistent notification implementation
	private PersistentNotification pn;
	
	/// The current data
	private StateData sd;

	/* The actions this class supports. We keep the same name of
	 * the standard actions, when a mapping is possible. However we
	 * don't use the official name because data is different */

	/** Boot action, called at boot time or when notifications are enabled */
	public static final String ACTION_BOOT_COMPLETED = 
			PREFIX + "BOOT_COMPLETED";
	
	/** Called when network connectivity changes. Used to try and fix 
	 * connection errors */
	public static final String ACTION_CONNECTIVITY_CHANGE = 
			PREFIX + "CONNECTIVITY_CHANGE";

	/** Called when a state machine or the cron alarm goes off.
	 *  The state machine is reconstructed from intent extra data
	 *  and we proceed to the next step */
	public static final String ACTION_ALARM = 
			PREFIX + "ALARM";

	/** Called when the user taps the reviews notification.
	 *  We start an external browser, but also reset the state machine
	 *  that may be polling with large intervals */
	public static final String ACTION_TAP = 
			PREFIX + "TAP";

	/** Called when the user taps the persistent notification and there are no reviews nor lessons. */
	public static final String ACTION_NULL_TAP = 
			PREFIX + "NULL_TAP";

	/** Called when the user taps the lessons notification.
	 *  We start an external browser, but also reset the state machine
	 *  that may be polling with large intervals */
	public static final String ACTION_LESSONS_TAP = 
			PREFIX + "LESSONS_TAP";
	
	/** Called by @link DashboardActivity when the notification icon needs to
	 *  be hidden. It is similar to @link {@link #ACTION_TAP}, however it
	 *  does not start the browser, because this is done at activity level  */
	public static final String ACTION_HIDE_NOTIFICATION = 
			PREFIX + "HIDE_NOTIFICATION";

	/** Called by @link DashboardActivity when it has obtained some fresh
	 *  dashboard data. This avoids having the dashboard displaying different
	 *  information from notification bar. 
	 */
	public static final String ACTION_NEW_DATA = 
			PREFIX + "NEW_DATA";
	
	/** The preferences file */
	private static final String PREFERENCES_FILE = "notification.xml";
	
	/** The cron schedule alarm time shared preferences key */
	private static final String PREFS_CRON_NEXT = PREFIX + "CRON_NEXT";
	
	/** Last known vacation day. Defaults to -1 */
	public static final String PREFS_LAST_VACATION = PREFIX + "LAST_VACATION";
	
	/** The cron interval. Default is one day */
	private static final long CRON_INTERVAL = 24 * 3600 * 1000;
	
	/** The cron retry interval. Default is half an hour */
	private static final long CRON_RETRY = 1800 * 1000;	
	
	/**
	 * Constructor. 
	 */
	public NotificationService () 
	{
		super("NotificationService");
	}
	
	private SharedPreferences prefs ()
	{
		int flags;
		
		flags = Context.MODE_PRIVATE;

		return getSharedPreferences (PREFERENCES_FILE, flags);		
	}
	
	private void updateNotificationInterface ()
	{
		NotificationInterface nnifc;
		
		nnifc = SettingsActivity.getPersistent (this) ? pn : acn;
		if (nifc != nnifc) {
			if (nifc != null)
				nifc.disable ();
			nifc = nnifc;
			nifc.enable (sd);
		}
	}
	
	/**
	 * Handles incoming intents, dispatching them to the appropriate methods.
	 * Before doing this, we check whether notifications are enabled; if they
	 * are not, this methods returns. This provides a simple method to
	 * stop notifications without explicitly canceling pending alarms.
	 * @see #bootCompleted(Intent)
	 * @see #connectivityChange(Intent)
	 * @see #alarm(Intent)
	 * 	@param intent the intent
	 */
	@Override
	public void onHandleIntent (Intent intent)
	{
		String action;
		boolean enabled;
		
		sd = new StateData (prefs ());

		enabled = SettingsActivity.getEnabled (this);
		action = intent.getAction ();
				
		if (nifc == null) {
			acn = new AutoCancelNotification (this);
			pn = new PersistentNotification (this);
		
			updateNotificationInterface ();
		}
		
		/* ACTION_HIDE_NOTIFICATION and ACTION_(LESSONS_)TAP are special, 
		 * because we must call it even if notifications
		 * are disabled */
		if (action.equals (ACTION_HIDE_NOTIFICATION)) {
			hideNotification (intent, enabled);
			return;
		} else if (action.equals (ACTION_TAP)) {
			tap (intent, enabled);
			return;
		} else if (action.equals (ACTION_LESSONS_TAP)) {
			lessonsTap (intent);
			return;
		} else if (action.equals (ACTION_NULL_TAP)) {
			nullTap (intent);
			return;
		}
		
		cronDaily (enabled);
		
		if (!enabled)
			return;
		
		if (action.equals (ACTION_BOOT_COMPLETED))
			bootCompleted (intent);
		else if (action.equals (ACTION_CONNECTIVITY_CHANGE))
			connectivityChange (intent);
		else if (action.equals (ACTION_ALARM))
			alarm (intent);
		else if (action.equals (ACTION_NEW_DATA))
			newData (intent);

		sd.serialize (prefs ());
	}
	
	/**
	 * Checks whether there it is time to run daily jobs.
	 * Admittedly, this has nothing to do with the notification service,
	 * however this class already handles alarms and gets boot notifications, so 
	 * it's quite natural to put it here. In addition, since alarms are somehow a precious
	 * resource, we merge the FSM alarsm with the cron alarms.
	 * @param enabled if notifications are enabled 
	 */
	private void cronDaily (boolean enabled)
	{
		SharedPreferences prefs;
		long next, now;
		boolean ok;
		
		now = System.currentTimeMillis ();
		prefs = prefs ();

		next = prefs.getLong (PREFS_CRON_NEXT, normalize (now));
		if (now >= next) {
			ok = false;
			try {				
				ok = runDailyJobs (prefs);
			} finally {
				if (ok) {
					next = normalize (now + CRON_INTERVAL);
					prefs.edit ().putLong (PREFS_CRON_NEXT, next).commit ();
				} else
					next = now + CRON_RETRY;
				
				if (!enabled)
					schedule (null, new Date (next));
			}			
		}
	}
	
	/**
	 * Given a timestamp, finds the best time of that day when to run the cron jobs.
	 * It may also be earlier that the input timestamp.
	 * 	@param timestamp a timestamp
	 *  @return a normalized timestamp
	 */
	protected long normalize (long timestamp)
	{
		Calendar cal;

		/* Set time = 1am. This gives us a lot of time to retry if something goes bad */
		cal = Calendar.getInstance ();
		cal.setTimeInMillis (timestamp);
		cal.set (Calendar.HOUR_OF_DAY, 1);
		cal.set (Calendar.MINUTE, 0);
		
		return cal.getTimeInMillis ();
	}
	
	/**
	 * This is the method that is guaranteed to be called exactly once a day, if 
	 * the phone is turned on. If it is not turned on, it is called as soon as possible.
	 * @param prefs the preferences
	 * @param <code>true</code> if completed successfully 
	 */
	protected boolean runDailyJobs (SharedPreferences prefs)
	{
		SRSDistribution srs;
		UserInformation ui;
		Connection conn;
		Connection.Meter meter;
		int vday, lvday, nvdays;
		
		try {
			meter = MeterSpec.T.NOTIFY_DAILY_JOBS.get (this);
			conn = SettingsActivity.newConnection (this);
			
			srs = conn.getSRSDistribution (meter);
			ui = conn.getUserInformation (meter);
			
			HistoryDatabase.insert (this, ui, srs);
			
			/* Update vacation mode */
			if (ui.vacationDate != null) {
				vday = ui.getDay (ui.vacationDate);
				lvday = prefs.getInt (PREFS_LAST_VACATION, -1);
				vday = Math.max (vday, lvday);
				nvdays = ui.getDay () - vday;
				if (nvdays > 0) {
					HistoryDatabase.addVacation (this, ui.level, nvdays);
					prefs.edit ().putInt (PREFS_LAST_VACATION, ui.getDay ()).commit ();
				}
			}
			
		} catch (IOException e) {
			return false;
		} catch (SQLException e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Handler of the {@link #ACTION_BOOT_COMPLETED} intent, called at boot time
	 * or when notifications are enabled. This creates an empty state machine,
	 * feeding it with the current data.
	 * @param intent the intent
	 */
	protected void bootCompleted (Intent intent)
	{
		NotifierStateMachine fsm;
		
		/* This handles notification type (persistent vs. nonpersistent) changes */
		updateNotificationInterface ();
		
		fsm = new NotifierStateMachine (this);
		
		feed (fsm, NotifierStateMachine.Event.E_INITIAL);
	}
	
	/**
	 * Handler of the {@link #ACTION_CONNECTIVITY_CHANGE} intent, called when
	 * connectivity changes. This creates an empty state machine,
	 * feeding it with the current data (if they can be retrieved successfully).
	 * If we can't contact the server, this event is simply ignored
	 * keeping the state machine in the pending intent.
	 * @param intent the intent
	 */
	protected void connectivityChange (Intent intent)
	{
		NotifierStateMachine fsm;
		
		fsm = new NotifierStateMachine (this);
		
		feed (fsm, NotifierStateMachine.Event.E_UNSOLICITED);
	}
	
	/**
	 * Handler of the {@link #ACTION_HIDE_NOTIFICATION} intent, called when
	 * the user taps the "Review now" link in the dashboard. 
	 * If notifications are enabled,
	 * we reset the state machine, to make it more responsive.
	 * In addition, we hide the notification icon.  
	 * @param intent the intent
	 * @param enabled set if notifications are enabled 
	 */
	protected void hideNotification (Intent intent, boolean enabled)
	{
		NotifierStateMachine fsm;
		
		hideNotification ();
		if (enabled) {
			fsm = new NotifierStateMachine (this);

			feed (fsm, NotifierStateMachine.Event.E_TAP);
		}
	}

	/**
	 * Handler of the {@link #ACTION_TAP} intent, called when
	 * the user taps the notification. If notifications are enabled,
	 * we reset the state machine, to make it more responsive.
	 * In addition, we start the browser. .  
	 * @param intent the intent
	 * @param enabled set if notifications are enabled 
	 */
	protected void tap (Intent intent, boolean enabled)
	{
		NotifierStateMachine fsm;
		
		if (openDashboard ())
			return;
		
		openBrowser (SettingsActivity.getURL (this));
		if (enabled) {
			fsm = new NotifierStateMachine (this);

			feed (fsm, NotifierStateMachine.Event.E_TAP);
		}
	}

	/**
	 * Handler of the {@link #ACTION_LESSONS_TAP} intent, called when
	 * the user taps the lessons notification. 
	 * @param intent the intent
	 */
	protected void lessonsTap (Intent intent)
	{
		if (openDashboard ())
			return;

		openBrowser (SettingsActivity.getLessonURL (this));
	}
	
	protected void nullTap (Intent intent)
	{
		if (openDashboard ())
			return;

		openBrowser (SettingsActivity.getChatURL (this));
	}
	
	private boolean openDashboard ()
	{
		Intent i;
		
		if (SettingsActivity.getPersistentHome (this)) {
			
			i = new Intent (this, MainActivity.class);
			i.setAction (Intent.ACTION_MAIN);
			i.addFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
			
			startActivity (i);
		
			return true;
		} else
			return false;
	}

	/**
	 * Handler of the {@link #ACTION_ALARM} intent, called when
	 * a timeout expires. This deserializes the current state machine,
	 * that was saved into the intent by @link #schedule(NotifierStateMachine, Date). 
	 * @param intent the intent
	 */
	protected void newData (Intent intent)
	{
		NotifierStateMachine fsm;
		
		fsm = new NotifierStateMachine (this);
		if (intent.hasExtra (KEY_DD)) {
			sd.dd = new DashboardData (intent.getBundleExtra (KEY_DD));
			nifc.update (sd, ChangeType.DATA);
			fsm.next (NotifierStateMachine.Event.E_UNSOLICITED, 
					  SettingsActivity.getReviewThreshold (this), sd.dd);
		} else
			feed (fsm, NotifierStateMachine.Event.E_UNSOLICITED);
	}
	
	/**
	 * Handler of the {@link #ACTION_NEW_DATA} intent, called when
	 * the dashboard data would like us to update the information.
	 * Attached to the intent we can find an (incomplete) 
	 * @link DashboardData object, so we deserialize it and pretend that
	 * an alarm has gone off. 
	 * @param intent the intent
	 */
	protected void alarm (Intent intent)
	{
		NotifierStateMachine fsm;
		Bundle b;
		
		/* Normally the ALARM event is equipped with a state machine. 
		 * It is not, only if this is a chron event AND notifications are
		 * disabled. In this case, however, this method should not have
		 * been called. Better being tolerant, anyway */
		if (intent.hasExtra (KEY_FSM)) {
			b = intent.getBundleExtra (KEY_FSM);
			fsm = new NotifierStateMachine (this, b);
		} else
			fsm = new NotifierStateMachine (this);
		
		feed (fsm, NotifierStateMachine.Event.E_SOLICITED);
	}

	/**
	 * Called when it is time to feed the state machine with
	 * fresh state information. This happens either because
	 * the state machine explicitly set a timeout or because
	 * connectivity state changes. The latter case is useful
	 * to fix temporary errors.
	 *  @param fsm the state machine
	 *	@param event the event that triggered this call
	 */
	private void feed (NotifierStateMachine fsm, Event event)
	{
		UserInformation ui;
		Connection.Meter meter;
		DashboardData dd;
		Connection conn;
		StudyQueue sq;
		
		meter = event.meter ().get (this);
		
		conn = SettingsActivity.newConnection (this);
		try {
			sq = conn.getStudyQueue (meter);
			/* This call does not cause network traffic */
			ui = conn.getUserInformation (meter);
			sd.dd = new DashboardData (ui, sq);
			sd.lessons = sd.dd.lessonsAvailable;
			sd.hasLessons = sd.lessons > 0 && SettingsActivity.getLessonsEnabled (this); 
			nifc.update (sd, ChangeType.LESSONS);
			sd.dd.serialize (this, DashboardData.Source.NOTIFICATION_SERVICE);
			
			dd = sd.dd;
		} catch (IOException e) {
			if (event == Event.E_UNSOLICITED)
				return;
			
			dd = new DashboardData (e);
		}
		
		fsm.next (event, SettingsActivity.getReviewThreshold (this), dd);
	}	
	
	/**
	 * Shows the notification icon.
	 *	@param reviews the number of pending reviews
	 */
	@Override
	public void showNotification (int reviews)
	{
		sd.hasReviews = true;
		sd.reviews = reviews;		
		nifc.update (sd, ChangeType.REVIEWS);
	}
	
	/**
	 * Hides the notification icon and resets the state machine.
	 */
	@Override
	public void hideNotification ()
	{
		sd.hasReviews = false;
		nifc.update (sd, ChangeType.REVIEWS);
	}

	/**
	 * Used by the state machine when it wants to be notified
	 * at some time elapses. We serialize the contents of 
	 * the FSM into an intent and set an alarm.
	 * This method is also called to schedule the chron event
	 * (and in that case the state machine parameter is null).
	 *  @param fsm the state machine
	 *	@param date when the timer should be tiggered
	 */
	public void schedule (NotifierStateMachine fsm, Date date)
	{		
		SharedPreferences prefs;
		AlarmManager alarm;
		PendingIntent pi;
		long next, chron;
		Bundle b;
		Intent i;
		
		i = new Intent (this, getClass ());
		i.setAction (ACTION_ALARM);
		if (fsm != null) {
			b = new Bundle ();
			fsm.serialize (b);
			i.putExtra (KEY_FSM, b);
		}
		
		next = date.getTime ();
		prefs = prefs ();
		chron = prefs.getLong (PREFS_CRON_NEXT, next);
		if (next > chron)
			next = chron;
		
		pi = PendingIntent.getService (this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		
		alarm = (AlarmManager) getSystemService (Context.ALARM_SERVICE);
		alarm.set (AlarmManager.RTC, next, pi);
	}
	
	/**
	 * Open the browser.
	 * 	@param url the url to open
	 */
	protected void openBrowser (String url)
	{		
		Intent intent;
		
		if (SettingsActivity.getUseIntegratedBrowser (this)) {
			intent = SettingsActivity.getWebViewIntent (this);
			intent.setAction (WebReviewActivity.OPEN_ACTION);
		} else
			intent = new Intent (Intent.ACTION_VIEW);
		intent.setData (Uri.parse (url));
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		
		startActivity (intent);
	}
}
