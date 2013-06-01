package com.wanikani.androidnotifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.wklib.AuthenticationException;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.LevelProgression;
import com.wanikani.wklib.SRSDistribution;
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

	/*** The avatar bitmap filename */
	private static final String AVATAR_FILENAME = "avatar.png";
		
	/**
	 * A receiver that gets notifications on several occasions:
	 * <ul>
	 * 	<li>@link {@link SettingsActivity#ACT_CREDENTIALS}: We need this
	 * 			both to update the credentials used to fill the stats
	 * 			screen, and to enable notifications if the user chose so.
	 * 	<li>@link {@link SettingsActivity#ACT_NOTIFY}: Enable the notification
	 * 			flag
	 * 	<li>@link {@link #ACTION_REFRESH}: force a refresh onto the dashboard
	 */
	private class Receiver extends BroadcastReceiver {
		
		/**
		 * Handles credentials change notifications
		 * 	@param ctxt local context
		 * 	@param i the intent		
		 */
		@Override
		public void onReceive (Context ctxt, Intent i)
		{
			UserLogin ul;
			String action;
			
			action = i.getAction ();
			if (action.equals (SettingsActivity.ACT_CREDENTIALS)) {
				ul = new UserLogin (i.getStringExtra (SettingsActivity.E_USERKEY));
			
				updateCredentials (ul);				
				enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));
			} else if (action.equals (SettingsActivity.ACT_NOTIFY))
				enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));			
			else if (action.equals (ACTION_REFRESH))
				refresh ();
		}
		
	}
	
	/**
	 * A task that gets called whenever the stats need to be refreshed.
	 * In order to keep the GUI responsive, we do this through an AsyncTask
	 */
	private class RefreshTask extends AsyncTask<Connection, Void, DashboardData > {
		
		Bitmap defAvatar;
		
		/**
		 * Called before starting the task, inside the activity thread.
		 */
		@Override
		protected void onPreExecute ()
		{
			Drawable d;
			
			startRefresh ();

			d = getResources ().getDrawable (R.drawable.default_avatar);
			defAvatar = ((BitmapDrawable) d).getBitmap ();
		}
		
		/**
		 * Performs the real job, by using the provided
		 * connection to obtain the study queue.
		 * 	@param conn a connection to the WaniKani API site
		 */
		@Override
		protected DashboardData doInBackground (Connection... conn)
		{
			DashboardData dd;
			UserInformation ui;
			StudyQueue sq;
			int size;

			size = getResources ().getDimensionPixelSize (R.dimen.m_avatar_size);
			
			try {
				sq = conn [0].getStudyQueue ();
				/* getUserInformation should be called after at least one
				 * of the other calls, so we give Connection a chance
				 * to cache its contents */
				ui = conn [0].getUserInformation ();
				conn [0].resolve (ui, size, defAvatar);

				dd = new DashboardData (ui, sq);
				if (dd.gravatar != null)
					saveAvatar (dd);
				else
					restoreAvatar (dd);
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
				
				refreshComplete (dd);

				new RefreshTaskPartII ().execute (conn);
			} catch (AuthenticationException e) {
				error (R.string.status_msg_unauthorized);
			} catch (IOException e) {
				error (R.string.status_msg_error);
			}
		}
	}
	
	/**
	 * A task that gets called whenever the stats need to be refreshed, part II.
	 * This task retrieves all the data that is not needed to display the dashboard,
	 * so it can be run after the splash screen disappears (and the startup is faster). 
	 */
	private class RefreshTaskPartII extends AsyncTask<Connection, Void, DashboardData.OptionalData> {
					
		/**
		 * Called before starting the task, inside the activity thread.
		 */
		@Override
		protected void onPreExecute ()
		{
			/* empty */
		}
		
		/**
		 * Performs the real job, by using the provided
		 * connection to obtain the SRS distribution and the LevelProgression.
		 * If any operation goes wrong, the data is simply not retrieved (this
		 * is meant to be data of lesser importance), so it should be ok anyway.
		 * 	@param conn a connection to the WaniKani API site
		 */
		@Override
		protected DashboardData.OptionalData doInBackground (Connection... conn)
		{
			DashboardData.OptionalDataStatus srsStatus, lpStatus;
			SRSDistribution srs;
			LevelProgression lp;
			
			try {
				srs = conn [0].getSRSDistribution();
				srsStatus = DashboardData.OptionalDataStatus.RETRIEVED;
			} catch (IOException e) {
				srs = null;
				srsStatus = DashboardData.OptionalDataStatus.FAILED;
			}

			try {
				lp = conn [0].getLevelProgression ();
				lpStatus = DashboardData.OptionalDataStatus.RETRIEVED;
			} catch (IOException e) {
				lp = null;
				lpStatus = DashboardData.OptionalDataStatus.FAILED;
			}

			
			return new DashboardData.OptionalData (srs, srsStatus, lp, lpStatus);
		}	
						
		/**
		 * Called at completion of the job, inside the Activity thread.
		 * Updates the stats and the status page.
		 * 	@param dd the results encoded by 
		 *		{@link #doInBackground(Connection...)}
		 */
		@Override
		protected void onPostExecute (DashboardData.OptionalData od)
		{
			dd.setOptionalData (od);
			
			refreshComplete (dd);
		}
	}

	/**
	 * A listener that intercepts review button clicks.
	 * It starts the {@link WebReviewActivity}.
	 */
	private class ReviewClickListener implements View.OnClickListener {
		
		@Override
		public void onClick (View v)
		{
			SharedPreferences prefs;
			Intent intent;
			
			intent = new Intent (DashboardActivity.this, NotificationService.class);			
			intent.setAction (NotificationService.ACTION_HIDE_NOTIFICATION);			
			startService (intent);

			prefs = PreferenceManager.getDefaultSharedPreferences (DashboardActivity.this);
			if (SettingsActivity.getUseIntegratedBrowser (prefs)) {
				intent = new Intent (DashboardActivity.this, WebReviewActivity.class);
				intent.setAction (WebReviewActivity.OPEN_ACTION);
			} else
				intent = new Intent (Intent.ACTION_VIEW);

			intent.setData (Uri.parse (WebReviewActivity.WKConfig.REVIEW_START));
			startActivity (intent);
		}	
	}

	/**
	 * A listener that intercepts lessons button clicks.
	 * It starts the {@link WebReviewActivity}.
	 */
	private class LessonsClickListener implements View.OnClickListener {
		
		@Override
		public void onClick (View v)
		{
			SharedPreferences prefs;
			Intent intent;
			
			prefs = PreferenceManager.getDefaultSharedPreferences (DashboardActivity.this);
			if (SettingsActivity.getUseIntegratedBrowser (prefs) && INTEGRATED_LESSONS) {
				intent = new Intent (DashboardActivity.this, WebReviewActivity.class);
				intent.setAction (WebReviewActivity.OPEN_ACTION);
			} else
				intent = new Intent (Intent.ACTION_VIEW);
			
			intent.setData (Uri.parse (WebReviewActivity.WKConfig.LESSON_START));
			startActivity (intent);
		}	
	}

	/** The key checked by {@link #onCreate} to make 
	 *  sure that the statistics contained in the bundle are valid
	 * @see #onCreate
	 * @see #onSaveInstanceState */
	private static final String BUNDLE_VALID = "bundle_valid";

	/** The broadcast receiver that handles all the actions */
	private Receiver receiver;

	/** The object that implements the WaniKani API client */
	private Connection conn;
	
	/** The information displayed on the dashboard. It is built
	 * from the objects returned by the WaniKani API*/
	private DashboardData dd;
	
	/** The asynchronous task that performs the actual queries */
	RefreshTask rtask;
	
	/** The object that notifies us when the refresh timeout expires */
	Alarm alarm;
	
	/** Private prefix */
	private static final String PREFIX = "com.wanikani.androidnotifier.DashboardData";
	
	/** Compiler flag, enabling integrated lessons. Currently they are disabled
	 *  because javascript here is a lot different and it is quite hard to debug
	 *  (lessons are not quite as frequent as reviews)
	 */
	private static final boolean INTEGRATED_LESSONS = false;
	
	/** An action that should be invoked to force refresh. This is used typically
	 *  when reviews complete
	 */
	public static final String ACTION_REFRESH = PREFIX + "REFRESH";
	
	/**
	 * Constructor.
	 */
	public DashboardActivity ()
	{
		receiver = new Receiver ();
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
		DashboardData ldd;
		
	    super.onCreate (bundle);
	    
	    registerIntents ();
	    alarm = new Alarm ();
	    
	    prefs = PreferenceManager.getDefaultSharedPreferences (this);
	    if (!SettingsActivity.credentialsAreValid (prefs))
	    	settings ();
	    
	    conn = new Connection (SettingsActivity.getLogin (prefs));
	    if (bundle != null && bundle.containsKey (BUNDLE_VALID)) {
	    	ldd = new DashboardData (bundle);
	    	refreshComplete (ldd);
	    	if (ldd.isIncomplete ())
	    		refreshOptional ();
	    } else
	    	refresh ();
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
	 *  <li>{@link #ACTION_REFRESH}, when the dashboard should refreshed
	 * </ul>
	 * Both intents are triggered by {@link SettingsActivity}
	 */
	private void registerIntents ()
	{
		IntentFilter filter;
		LocalBroadcastManager lbm;
				
		lbm = LocalBroadcastManager.getInstance (this);
		
		filter = new IntentFilter (SettingsActivity.ACT_CREDENTIALS);
		filter.addAction (SettingsActivity.ACT_NOTIFY);
		filter.addAction (ACTION_REFRESH);
		lbm.registerReceiver (receiver, filter);
	}
	
	/**
	 * Registers all the click listeners.
	 * Currently they are:
	 * <ul>
	 * 	<li>The listener that handles "Available now" web link
	 *  <li>The listener of the "Review button"
	 * </ul>
	 */
	private void registerListeners ()
	{
		View view;
		
		view = findViewById (R.id.btn_review);
		view.setOnClickListener (new ReviewClickListener ());
		
		view = findViewById (R.id.btn_lessons_available);
		view.setOnClickListener (new LessonsClickListener ());
	}
	
	/**
	 * Unregister the intent listeners that were registered by {@link #registerIntent}. 
	 */
	private void unregisterIntents ()
	{
		LocalBroadcastManager lbm;
		
		lbm = LocalBroadcastManager.getInstance (this);
		
		lbm.unregisterReceiver (receiver);
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
	 * Called when optional data needs to be refreshed. This happen in
	 * some strange situations, e.g. when the application is stopped before
	 * optional data, and then is resumed from a bundle. I'm not even
	 * sure it can happen, however...
	 */
	private void refreshOptional ()
	{
			new RefreshTaskPartII ().execute (conn);			
	}

	/**
	 * Stores the avatar locally. Needed to avoid storing it into the
	 * bundle. Useful also to have a fallback when we can't reach the
	 * server.
	 * @param dd the data, containing a valid avatar bitmap. If null, this
	 * 	method does nothing
	 */
	protected void saveAvatar (DashboardData dd)
	{
		OutputStream os;
		
		if (dd == null || dd.gravatar == null)
			return;
		
		os = null;
		try {
			os = openFileOutput (AVATAR_FILENAME, Context.MODE_PRIVATE);
			dd.gravatar.compress(Bitmap.CompressFormat.PNG, 90, os);
		} catch (IOException e) {
			/* Life goes on... */
		} finally {
			try {
				if (os != null)
					os.close ();
			} catch (IOException e) {
				/* Probably next decode will go wrong */
			}
		}
	}
	
	/**
	 * Restores the avatar from local storage.
	 * @param dd the data, that will be filled with the bitmap, if everything
	 * 	goes fine
	 */
	protected void restoreAvatar (DashboardData dd)
	{
		InputStream is;
		
		if (dd == null)
			return;
		
		is= null;
		try {
			is = openFileInput (AVATAR_FILENAME);
			dd.gravatar = BitmapFactory.decodeStream (is);
		} catch (IOException e) {
			/* Life goes on... */
		} finally {
			try {
				if (is != null)
					is.close ();
			} catch (IOException e) {
				/* At least we tried */
			}
		}
	}
	/**
	 * Called by {@link RefreshTask} when asynchronous data 
	 * retrieval is completed.
	 * @param dd the retrieved data
	 */
	private void refreshComplete (DashboardData dd)
	{
		SharedPreferences prefs;
		ProgressBar pb;
		ImageView iw;
		TextView tw, ow;
		View view;
		long delay, refresh;
		String s;

		if (this.dd == null) {
			setContentView (R.layout.dashboard);
			registerListeners ();
		}
		
		spin (false);

		if (this.dd != null)
			dd.merge (this.dd);
		this.dd = dd;
		
		rtask = null;

		iw = (ImageView) findViewById (R.id.iv_gravatar);
		if (dd.gravatar == null)
			restoreAvatar (dd);
		
		if (dd.gravatar != null)
			iw.setImageBitmap (mask (dd.gravatar));

		tw = (TextView) findViewById (R.id.tv_username);
		tw.setText (dd.username);

		tw = (TextView) findViewById (R.id.tv_level);
		tw.setText (String.format (getString (R.string.fmt_level), dd.level));

		tw = (TextView) findViewById (R.id.tv_title);
		tw.setText (String.format (getString (R.string.fmt_title), dd.title));

		tw = (TextView) findViewById (R.id.reviews_val);
		tw.setText (Integer.toString (dd.reviewsAvailable));
		
		view = findViewById (R.id.tr_r_now);
		view.setVisibility (dd.reviewsAvailable > 0 ? View.VISIBLE : View.GONE);

		tw = (TextView) findViewById (R.id.tv_next_review);
		tw.setText (R.string.tag_next_review);
		
		tw = (TextView) findViewById (R.id.tv_next_review_val);
		ow = (TextView) findViewById (R.id.tv_next_review);
		view = findViewById (R.id.btn_review);
		if (dd.reviewsAvailable > 0) {
			ow.setVisibility (View.GONE);
			tw.setVisibility (View.GONE);
			view.setVisibility (View.VISIBLE);
		} else {
			ow.setVisibility (View.VISIBLE);
			tw.setText (niceInterval (dd.nextReviewDate));
			tw.setVisibility (View.VISIBLE);
			view.setVisibility (View.GONE);
		}
		
		tw = (TextView) findViewById (R.id.lessons_available);
		if (dd.lessonsAvailable > 1) {
			s = String.format (getString (R.string.fmt_lessons), dd.lessonsAvailable);
			tw.setText (s);
		} else if (dd.lessonsAvailable == 1)
			tw.setText (getString (R.string.fmt_one_lesson));
		
		view = findViewById (R.id.lay_lessons_available);
		view.setVisibility (dd.lessonsAvailable > 0 ? View.VISIBLE : View.GONE);
		
		tw = (TextView) findViewById (R.id.next_hour_val);
		tw.setText (Integer.toString (dd.reviewsAvailableNextHour));
		
		tw = (TextView) findViewById (R.id.next_day_val);
		tw.setText (Integer.toString (dd.reviewsAvailableNextDay));
		
		/* Now the optional stuff */
		switch (dd.od.lpStatus) {
		case RETRIEVING:
			if (this.dd.od.lpStatus != DashboardData.OptionalDataStatus.RETRIEVING) {
				view = findViewById (R.id.pb_w_section);
				view.setVisibility (View.VISIBLE);
			}
			view = findViewById (R.id.progress_w_section);
			view.setVisibility (View.VISIBLE);
			break;
			
		case RETRIEVED:
			pb = (ProgressBar) findViewById (R.id.pb_radicals);
			pb.setProgress (100 * dd.od.lp.radicalsProgress / dd.od.lp.radicalsTotal);

			pb = (ProgressBar) findViewById (R.id.pb_kanji);
			pb.setProgress (100 * dd.od.lp.kanjiProgress / dd.od.lp.kanjiTotal);

			view = findViewById (R.id.progress_w_section);
			view.setVisibility (View.GONE);
			
			view = findViewById (R.id.progress_section);
			view.setVisibility (View.VISIBLE);
			break;
			
		case FAILED:
			/* Just hide the spinner. 
			 * If we already have some data, it is displayed anyway */
			view = findViewById (R.id.progress_w_section);
			view.setVisibility (View.GONE);			
		}
				
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		refresh = SettingsActivity.getRefreshTimeout (prefs) * 60 * 1000; 
		if (dd.nextReviewDate != null)
			delay = dd.nextReviewDate.getTime () - System.currentTimeMillis ();
		else
			delay = refresh;
		
		if (delay > refresh || dd.reviewsAvailable > 0)
			delay = refresh;
		
		/* May happen if local clock is not perfectly synchronized with WK clock */
		if (delay < 1000)
			delay = 1000;
		
		alarm.schedule (this, delay);
	}

	/**
	 * Apply a circular mask on the given bitmap. This method is
	 * used to display the avatar.
	 * @param bmp an input bitmap
	 * @param result the output (masked) bitmap
	 */
	private Bitmap mask (Bitmap bmp)
	{
		Bitmap result, mask;
		Drawable dmask;
		Canvas canvas;
		Paint paint;

		result = Bitmap.createBitmap (bmp.getWidth (), bmp.getHeight (),
									  Bitmap.Config.ARGB_8888);
		canvas = new Canvas (result);
		
		dmask = getResources ().getDrawable (R.drawable.gravatar_mask);
		mask = ((BitmapDrawable) dmask).getBitmap ();
		
		paint = new Paint (Paint.ANTI_ALIAS_FLAG);
		paint.setXfermode (new PorterDuffXfermode (PorterDuff.Mode.DST_IN));
		canvas.drawBitmap (bmp, 0, 0, null);
		canvas.drawBitmap (mask, 0, 0, paint);
		
		return result;
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
	 * Called when retrieveing data from WaniKani. Updates the 
	 * status message or switches to the splash screen, depending
	 * on the current state  
	 */
	private void startRefresh ()
	{
		if (dd == null)
			setContentView (R.layout.splash);
		else
			spin (true);
	}

	/**
	 * Displays an error message, choosing the best way to do that.
	 * If we did not gather any stats, the only thing we can do is to display
	 * the error page and hope for the best
	 * @param id resource string ID
	 */
	private void error (int id)
	{
		SharedPreferences prefs;
		Resources res;
		String s;
		TextView tw;

		/* If the API code is invalid, switch to error screen even if we have
		 * already opened the dashboard */
		if (id == R.string.status_msg_unauthorized)
			dd = null;
		
		if (dd == null)
			setContentView (R.layout.error);
		else
			spin (false);
		
		res = getResources ();
		if (id == R.string.status_msg_unauthorized) {
			prefs = PreferenceManager.getDefaultSharedPreferences (this);
			s = SettingsActivity.diagnose (prefs, res);
		} else
			s = res.getString (id);
	
		tw = (TextView) findViewById (R.id.tv_alert);
		if (tw != null)
			tw.setText (s);
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
		if (date == null)
			return res.getString (R.string.fmt_no_reviews);
		
		delta = date.getTime () - new Date ().getTime ();
		forward = delta > 0;
		if (!forward)
			return res.getString (R.string.tag_available_now);
		
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
	
