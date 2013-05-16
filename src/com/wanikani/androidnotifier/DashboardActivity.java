package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.wklib.AuthenticationException;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.StudyQueue;
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
 * The main activity, which is displayed on application launch.  We
 * display a simple GUI that show some stats (need some work on here)
 * and (most important) allow the user change his/her preferences.
 * <p> 
 * The stats are refreshed automatically every 
 * {@link #T_INT_AUTOREFRESH} milliseconds.  This value should be kept quite
 * large to avoid needless traffic and power consumption; if the user
 * really needs to update the stats, we provide also a "refresh" menu.
 */
public class DashboardActivity extends Activity implements Runnable { 

	/*** The auto-refresh time (milliseconds) */
	public static final int T_INT_AUTOREFRESH = 5 * 60 * 1000;
		
	/**
	 * A receiver that gets notifications when 
	 * {@link SettingsActivity} updates the credentials. We need this
	 * both to update the credentials used to fill the stats
	 * screen, and to enable notifications if the user chose so.
	 */
	private class CredentialsReceiver extends BroadcastReceiver {
		
		/**
		 * Handles credentials change notifications
		 * 	@param ctxt local context
		 * 	@param i the intent		
		 */
		@Override
		public void onReceive (Context ctxt, Intent i)
		{
			UserLogin ul;
			
			ul = new UserLogin (i.getStringExtra (SettingsActivity.E_USERKEY));
			
			updateCredentials (ul);
			enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));
		}
		
	}
	
	/**
	 * A receiver that gets notifications when {@link SettingsActivity} 
	 * updates the "enable notif" flag.
	 */
	private class NotifyReceiver extends BroadcastReceiver {
		
		/**
		 * Handles enable/disable notification notifications.
		 */
		@Override
		public void onReceive (Context ctxt, Intent i)
		{
			enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));
		}
		
	}

	/**
	 * A task that gets called whenever the stats need to be refreshed.
	 * In order to keep the GUI responsive, we do this through an AsyncTask
	 */
	private class RefreshTask extends AsyncTask<Connection, Void, DashboardData > {
		
		/**
		 * Called before starting the task, inside the activity thread.
		 * Updates the status message.
		 */
		@Override
		protected void onPreExecute ()
		{
			if (dd != null) {
				spin (true);
				status (R.string.status_msg_retrieving);
			} else
				spinTheWorld (true);
		}
		
		/**
		 * Performs the real job, by using the provided
		 * connection to obtain the study queue and the SRS
		 * distribution.
		 * 	@param conn a connection to the WaniKani API site
		 */
		@Override
		protected DashboardData doInBackground (Connection... conn)
		{
			DashboardData dd;
			StudyQueue sq;
			SRSDistribution srs;
			
			try {
				sq = conn [0].getStudyQueue ();
				srs = conn [0].getSRSDistribution ();
				dd = new DashboardData (sq, srs);
			} catch (IOException e) {
				dd = new DashboardData (e);
			}
			
			return dd;
		}	
						
		/**
		 * Called at completion of the job, inside the Activity thread.
		 * Updates the stats and the status page.
		 * 	@param dd the results encoded by 
		 *		{@link #doInBackground(Connection...)}
		 */
		@Override
		protected void onPostExecute (DashboardData dd)
		{
			try {
				dd.wail ();
				status (R.string.status_msg_success);
				refreshComplete (dd);
			} catch (AuthenticationException e) {
				status (R.string.status_msg_unauthorized);
			} catch (IOException e) {
				status (R.string.status_msg_error);
			} finally {
				spin (false);
				spinTheWorld (false);
			}
		}
	}

	/** The key checked by {@link #onCreate} to make 
	 *  sure that the statistics contained in the bundle are valid
	 * @see #onCreate
	 * @see #onSaveInstanceState */
	private static final String BUNDLE_VALID = "bundle_valid";

	/** The broadcast receiver that informs this activity of
	 * changes in the credentials */
	private CredentialsReceiver credentialsRecv;

	/** The broadcast receiver that informs this activity when
	 * notifications are enabled or disabled */
	private NotifyReceiver notifyRecv;
	
	/** The object that implements the WaniKani API client */
	private Connection conn;
	
	/** The information displayed on the dashboard. It is built
	 * from the objects returned by the WaniKani API*/
	private DashboardData dd;
	
	/** The asynchronous task that performs the actual queries */
	RefreshTask rtask;
	
	/** The object that notifies us when the refresh timeout expires */
	Alarm alarm;
	
	/**
	 * Constructor.
	 */
	public DashboardActivity ()
	{
		credentialsRecv = new CredentialsReceiver ();
		notifyRecv = new NotifyReceiver ();
	}
	
	/** 
	 * Called when the activity is first created.  We register the
	 * listeners and create the 
	 * {@link com.wanikani.wklib.Connection} object that will perform the
	 * queries for us.  In some cases (e.g. a change in the
	 * orientation of the display), the activity is destroyed and
	 * immediately recreated: to avoid querying the website, we
	 * use the bundle to retains the stats. To make sure that the
	 * bundle is actually valid (e.g. the previous instance of the
	 * activity actually succeeded in retrieving the data), we
	 * look for the {@link #BUNDLE_VALID} key.
	 * 	@see #onSaveInstanceState()
	 *  @param bundle the bundle, or <code>null</code>
	 */
	@Override
	public void onCreate (Bundle bundle) 
	{	
		SharedPreferences prefs;
		
	    super.onCreate (bundle);
	    		
	    registerIntents ();
	    setContentView(R.layout.dashboard);
	    alarm = new Alarm ();

	    prefs = PreferenceManager.getDefaultSharedPreferences (this);
	    if (!SettingsActivity.credentialsAreValid (prefs))
	    	settings ();
	    
	    conn = new Connection (SettingsActivity.getLogin (prefs));
	    if (bundle == null || !bundle.containsKey (BUNDLE_VALID))
	    	refresh ();
	    else
	    	refreshComplete (new DashboardData (bundle));
	}
	
	/**
	 * The OS calls this method when destroying the view.
	 * This implementation stores the stats into the bundle, if they
	 * have been successfully retrieved.
	 * 	@see #onCreate(Bundle)
	 *  @param bundle the bundle where to store the stats
	 */
	@Override
	public void onSaveInstanceState (Bundle bundle)
	{
		if (dd != null) {
			dd.serialize (bundle);
			bundle.putBoolean (BUNDLE_VALID, true);
		}
	}
	
	/**
	 * Called when the application resumes.
	 * We notify this the alarm to adjust its timers in case
	 * they were tainted by a deep sleep mode transition.
	 */
	@Override
	public void onResume ()
	{
		super.onResume ();
		
		alarm.screenOn ();
	}
	
	/**
	 * Called on dashboard destruction. 
	 * Destroys the listeners of the changes in the settings, and the 
	 * refresher thread.
	 */
	@Override
	public void onDestroy ()
	{	
		super.onDestroy ();
		
		unregisterIntents ();
		alarm.stopAlarm ();
	}

	/**
	 * Registers the intent listeners.
	 * Currently the intents we listen to are:
	 * <ul>
	 * 	<li>{@link SettingsActivity#ACT_CREDENTIALS}, when the credentials are changed
	 *  <li>{@link SettingsActivity#ACT_NOTIFY}, when notifications are enabled or disabled
	 * </ul>
	 * Both intents are triggered by {@link SettingsActivity}
	 */
	private void registerIntents ()
	{
		IntentFilter filter;
		LocalBroadcastManager lbm;
				
		lbm = LocalBroadcastManager.getInstance (this);
		
		filter = new IntentFilter (SettingsActivity.ACT_CREDENTIALS);
		lbm.registerReceiver (credentialsRecv, filter);

		filter = new IntentFilter (SettingsActivity.ACT_NOTIFY);
		lbm.registerReceiver (notifyRecv, filter);
	}
	
	/**
	 * Unregister the intent listeners that were registered by {@link #registerIntent}. 
	 */
	private void unregisterIntents ()
	{
		LocalBroadcastManager lbm;
		
		lbm = LocalBroadcastManager.getInstance (this);
		
		lbm.unregisterReceiver (credentialsRecv);
		lbm.unregisterReceiver (notifyRecv);
	}
	
	/**
	 * Associates the menu description to the menu key (or action bar).
	 * The XML description is <code>main.xml</code>
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Menu handler. Calls either the {@link #refresh} or the
	 * {@link #settings} method, depending on the selected item.
	 * 	@param item the item that has been seleted
	 */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId ()) {
		case R.id.em_refresh:
			refresh ();
			break;
			
		case R.id.em_settings:
			settings ();
			break;
		
		default:
			return super.onOptionsItemSelected (item);
		}
		
		return true;
	}

	/**
	 * Settings menu implementation. Actually, it simply stacks the 
	 * {@link SettingsActivity} activity.
	 */
	private void settings ()
	{
		Intent intent;
		
		intent = new Intent (this, SettingsActivity.class);
		startActivity (intent);
	}
	
	/**
	 * Called to update the credentials. It also triggers a refresh of 
	 * the GUI.
	 * @param login the new credentials
	 */
	private void updateCredentials (UserLogin login)
	{
		conn = new Connection (login);
		
		refresh ();
	}

	/**
	 * Called when the refresh timeout expires.
	 * We call {@link #refresh()}.
	 */
	public void run ()
	{
		refresh ();
	}

	/**
	 * Called when the GUI needs to be refreshed.
	 * It starts an asynchrous task that actually performs the job.
	 */
	private void refresh ()
	{
			if (rtask != null)
				rtask.cancel (false);
			rtask = new RefreshTask ();
			rtask.execute (conn);			
	}
	
	/**
	 * Called by {@link RefreshTask} when asynchronous data 
	 * retrieval is completed.
	 * @param dd the retrieved data
	 */
	private void refreshComplete (DashboardData dd)
	{
		TextView tw;

		this.dd = dd;
		
		rtask = null;
		tw = (TextView) findViewById (R.id.reviews_val);
		tw.setText (Integer.toString (dd.reviewsAvailable));
		
		tw = (TextView) findViewById (R.id.tv_next_review);
		tw.setText (R.string.tag_next_review);
		
		tw = (TextView) findViewById (R.id.tv_next_review_val);
		tw.setText (niceInterval (dd.nextReviewDate));
		
		tw = (TextView) findViewById (R.id.lessons_val);
		tw.setText (Integer.toString (dd.lessonsAvailable));

		tw = (TextView) findViewById (R.id.next_hour_val);
		tw.setText (Integer.toString (dd.reviewsAvailableNextHour));
		
		tw = (TextView) findViewById (R.id.next_day_val);
		tw.setText (Integer.toString (dd.reviewsAvailableNextDay));
		
		tw = (TextView) findViewById (R.id.apprentice_val);
		tw.setText (Integer.toString (dd.apprentice));
		
		tw = (TextView) findViewById (R.id.guru_val);
		tw.setText (Integer.toString (dd.guru));

		tw = (TextView) findViewById (R.id.master_val);
		tw.setText (Integer.toString (dd.master));

		tw = (TextView) findViewById (R.id.enlightened_val);
		tw.setText (Integer.toString (dd.enlighten));

		tw = (TextView) findViewById (R.id.burned_val);
		tw.setText (Integer.toString (dd.burned));
		
		alarm.schedule (this, T_INT_AUTOREFRESH);
	}
	
	/**
	 * Called when notifications are enabled or disabled.
	 * It actually starts or terminates the {@link NotificationService}. 
	 * @param enable <code>true</code> if should be started; <code>false</code>
	 * if should be stopped
	 */
	private void enableNotifications (boolean enable)
	{
		Intent intent;
		
		if (enable) {
			intent = new Intent (this, NotificationService.class);
			intent.setAction (NotificationService.ACTION_BOOT_COMPLETED);
			startService (intent);
		}
	}

	/**
	 * Updates the status line.
	 * @param id resource string ID
	 */
	private void status (int id)
	{
		TextView tw;
		
		tw = (TextView) findViewById (R.id.tv_alert);
		tw.setText (id);		
	}

	/**
	 * Show or hide the spinner.
	 * @param enable true if should be shown
	 */
	private void spin (boolean enable)
	{
		ProgressBar pb;
		
		pb = (ProgressBar) findViewById (R.id.pb_status);
		pb.setVisibility (enable ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
	}
	
	/**
	 * Show or hide the spinners placed "above" the data.
	 * @param enable true if they should be shown
	 */
	private void spinTheWorld (boolean enable)
	{
		spinField (enable, R.id.pb_lessons, R.id.lessons_val);
		spinField (enable, R.id.pb_reviews, R.id.reviews_val);
		
		spinField (enable, R.id.pb_next_hour, R.id.lessons_val);
		spinField (enable, R.id.pb_next_day, R.id.lessons_val);

		spinField (enable, R.id.pb_apprentice, R.id.apprentice_val);
		spinField (enable, R.id.pb_guru, R.id.guru_val);
		spinField (enable, R.id.pb_master, R.id.master_val);
		spinField (enable, R.id.pb_enlightened, R.id.enlightened_val);
		spinField (enable, R.id.pb_burned, R.id.burned_val);
	}
	
	/**
	 * Show or hide a single spinner place above a single field.
	 * @param enable true if it should be shown
	 * @param pbid spinner on the field
	 * @param fid  the field 
	 */
	private void spinField (boolean enable, int pbid, int fid)
	{
		ProgressBar pb; 
		TextView tv;
		
		pb = (ProgressBar) findViewById (pbid);
		pb.setVisibility (enable ? ProgressBar.VISIBLE : ProgressBar.GONE);

		tv = (TextView) findViewById (fid);
		tv.setVisibility (enable ? ProgressBar.GONE : ProgressBar.VISIBLE);
	}

	/**
	 * Pretty-prints a date. This implementation tries to mimic the WaniKani website,
	 * by returning an approximate interval. 
	 * @param date the date to format
	 * @return a string to be displayed
	 */
	private String niceInterval (Date date)
	{
		float days, hours, minutes;
		boolean forward;
		Resources res;
		long delta;
		int x;
			
		res = this.getResources ();
		delta = date.getTime () - new Date ().getTime ();
		forward = delta > 0;
		if (!forward)
			return res.getString (R.string.fmt_available_now);
		
		minutes = delta / (60 * 1000);
		hours = minutes / 60;
		days = hours / 24;
		
		x = Math.round (days);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_days), x);
		else if (x == 1)
			return String.format (res.getString (R.string.fmt_one_day));

		x = Math.round (hours);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_hours), x);
		else if (x == 1 && hours >= 1)
			return String.format (res.getString (R.string.fmt_one_hour));

		x = Math.round (minutes);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_minutes), x);
		else if (x == 1)
			return String.format (res.getString (R.string.fmt_one_minute));
		
		return String.format (res.getString (R.string.fmt_seconds));	
	}
	
}
	
