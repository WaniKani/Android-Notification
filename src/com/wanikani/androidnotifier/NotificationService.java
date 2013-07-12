package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.wanikani.androidnotifier.NotifierStateMachine.Event;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.StudyQueue;
import com.wanikani.wklib.UserInformation;
import com.wanikani.wklib.UserLogin;

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
	extends IntentService implements NotifierInterface {
	
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

	/** Called when a state machine or the chron alarm goes off.
	 *  The state machine is reconstructed from intent extra data
	 *  and we proceed to the next step */
	public static final String ACTION_ALARM = 
			PREFIX + "ALARM";

	/** Called when the user taps the notification.
	 *  We start an external browser, but also reset the state machine
	 *  that may be polling with large intervals */
	public static final String ACTION_TAP = 
			PREFIX + "TAP";

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

	/** The ID associated to the notification icon. Since we can
	 *  only display one notification at a time, this is a
	 *  constant */
	private static final int NOT_ID = 1;
	
	/** The chron schedule alarm time shared preferences key */
	private static final String PREFS_CHRON_NEXT = PREFIX + "CHRON_NEXT";
	
	/** The chron interval. Default is one day */
	private static final long CHRON_INTERVAL = 24 * 3600 * 1000;
	
	/**
	 * Constructor. 
	 */
	public NotificationService () 
	{
		super("NotificationService");
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
		SharedPreferences prefs;
		String action;
		boolean enabled;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		enabled = SettingsActivity.getEnabled (prefs);
		action = intent.getAction ();
		
		/* ACTION_HIDE_NOTIFICATION and ACTION_TAP are special, 
		 * because we must call it even if notifications
		 * are disabled */
		if (action.equals (ACTION_HIDE_NOTIFICATION)) {
			hideNotification (intent, enabled);
			return;
		} else if (action.equals (ACTION_TAP)) {
			tap (intent, enabled);
			return;
		}
		
		chronDaily (enabled);
		
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
	}
	
	/**
	 * Checks whether there it is time to run dailiy jobs.
	 * Admittedly, this has nothing to do with the notification service,
	 * however this class already handles alarms and gets boot notifications, so 
	 * it's quite natural to put it here. In addition, since alarms are somehow a precious
	 * resource, we merge the FSM alarsm with the cron alarms.
	 * @param enabled if notifications are enabled 
	 */
	private void chronDaily (boolean enabled)
	{
		SharedPreferences prefs;
		long next, now;
		
		now = System.currentTimeMillis ();
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		next = prefs.getLong (PREFS_CHRON_NEXT, now);
		
		if (now >= next) {
			next = now + CHRON_INTERVAL;
			prefs.edit ().putLong (PREFS_CHRON_NEXT, next).commit ();			
			if (!enabled)
				schedule (null, new Date (next));
			
			runDailyJobs ();
		}
	}
	
	/**
	 * This is the method that is guaranteed to be called exactly once a day, if 
	 * the phone is turned on. If it is not turned on, it is called as soon as possible.
	 */
	protected void runDailyJobs ()
	{
		
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
		
		openBrowser ();
		if (enabled) {
			fsm = new NotifierStateMachine (this);

			feed (fsm, NotifierStateMachine.Event.E_TAP);
		}
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
		DashboardData dd;
		
		fsm = new NotifierStateMachine (this);
		if (intent.hasExtra (KEY_DD)) {
			dd = new DashboardData (intent.getBundleExtra (KEY_DD));
			fsm.next (NotifierStateMachine.Event.E_UNSOLICITED, dd);
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
		SharedPreferences prefs;
		UserLogin login;
		UserInformation ui;
		Connection conn;
		DashboardData dd;
		StudyQueue sq;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		login = SettingsActivity.getLogin (prefs);
		
		conn = new Connection (login);
		try {
			sq = conn.getStudyQueue ();
			/* This call does not cause network traffic */
			ui = conn.getUserInformation ();
			dd = new DashboardData (ui, sq);
		} catch (IOException e) {
			if (event == Event.E_UNSOLICITED)
				return;
			
			dd = new DashboardData (e);
		}
		
		fsm.next (event, dd);
	}	
	
	/**
	 * Shows the notification icon.
	 *	@param reviews the number of pending reviews
	 */
	public void showNotification (int reviews)
	{
		NotificationManager nmanager;
		NotificationCompat.Builder builder;
		SharedPreferences prefs;
		Notification not;
		PendingIntent pint;
		Intent intent;
		String text;

		prefs = PreferenceManager.getDefaultSharedPreferences (this); 
			
		intent = new Intent (this, NotificationService.class);
		intent.setAction (ACTION_TAP);
		
		pint = PendingIntent.getService (this, 0, intent, 0);

		builder = new NotificationCompat.Builder (this);
		builder.setSmallIcon (R.drawable.not_icon);
		
		if (SettingsActivity.get42plus (prefs) && reviews > DashboardFragment.LESSONS_42P)
			text = getString (R.string.new_reviews_42plus, DashboardFragment.LESSONS_42P);
		else
			text = getString (reviews == 1 ? 
						      R.string.new_review : R.string.new_reviews, reviews);
		builder.setContentTitle (getString (R.string.app_name));
								 
		builder.setContentText (text);
		builder.setContentIntent (pint);
		
		nmanager = (NotificationManager) 
			getSystemService (Context.NOTIFICATION_SERVICE);
		
		not = builder.build ();
		not.flags |= Notification.FLAG_AUTO_CANCEL;
		
		nmanager.notify (NOT_ID, not);
	}
	
	/**
	 * Hides the notifi
	 * cation icon and resets the state machine.
	 */
	public void hideNotification ()
	{
		NotificationManager nmanager;
		
		nmanager = (NotificationManager) 
			getSystemService (Context.NOTIFICATION_SERVICE);
		
		nmanager.cancel (NOT_ID);
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
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		chron = prefs.getLong (PREFS_CHRON_NEXT, next);
		if (next > chron)
			next = chron;
		
		pi = PendingIntent.getService (this, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		
		alarm = (AlarmManager) getSystemService (Context.ALARM_SERVICE);
		alarm.set (AlarmManager.RTC, next, pi);
	}
	
	/**
	 * Open the browser, showing the reviews page.
	 */
	protected void openBrowser ()
	{		
		SharedPreferences prefs;
		Intent intent;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		if (SettingsActivity.getUseIntegratedBrowser (prefs)) {
			intent = new Intent (this, WebReviewActivity.class);
			intent.setAction (WebReviewActivity.OPEN_ACTION);
		} else
			intent = new Intent (Intent.ACTION_VIEW);
		intent.setData (Uri.parse (SettingsActivity.getURL (prefs)));
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		
		startActivity (intent);
	}
}
