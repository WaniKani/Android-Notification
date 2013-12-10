package com.wanikani.androidnotifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.wanikani.wklib.AuthenticationException;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.ItemsCache;
import com.wanikani.wklib.LevelProgression;
import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.SRSLevel;
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
 * The main activity, started when the app is launched.
 * This class' responsibilities are mostly related to dashboard data
 * retrieval, since all the core tasks are implemented at fragment level.
 */
public class MainActivity extends FragmentActivity implements Runnable {
	
	/**
	 * The pager model. It also broadcasts requests to all the
	 * tabs throught the @link Tab interface.
	 */
	public class PagerAdapter extends FragmentPagerAdapter {
        
		/// The tabs
		List<Tab> tabs;
		
		/**
		 * Constructor.
		 * @param fm the fragment manager
		 * @param tabs the list of tabs
		 */
		public PagerAdapter (FragmentManager fm, List<Tab> tabs) 
		{
			super (fm);
			
			this.tabs = tabs;
	    }

		 @Override
	     public int getCount () 
		 {
			 return tabs.size ();
	     }

		 @Override
	     public Fragment getItem (int position) 
		 {
	         return (Fragment) tabs.get (position);
	     }
		 
		 @Override
		 public CharSequence getPageTitle (int position) 
		 {
			Resources res;
				
			res = getResources ();
				
			return res.getString (tabs.get (position).getName ());
		 }
		 
		 public void replace (Tab tab)
		 {
			 int i;
			 
			 for (i = 0; i < tabs.size (); i++)
				 if (tabs.get (i).getClass ().equals (tab))
					 tabs.set (i, tab);
		 }
		 
		 /**
		  * Broadcasts the spin event, which is sent when data refresh
		  * is started or completed
		  * @see Tab#spin(boolean)
		  * @param enable if <code>true</code>, refresh is started
		  */
		 public void spin (boolean enable)
		 {
			 for (Tab tab : tabs)
				 tab.spin (enable);
		 }
		 
		 /**
		  * Broadcasts the refresh-complete event, which is sent to the
		  * tabs, providing fresh dashboard data
		  * @see Tab#refreshComplete(DashboardData)
		  * @param dd dashboard data
		  */
		 public void refreshComplete (DashboardData dd)
		 {
			 for (Tab tab : tabs)
				 tab.refreshComplete (dd);
		 }
		 
		 public void flushDatabase ()
		 {
			 for (Tab tab : tabs)
				 tab.flushDatabase ();
		 }
		 
		 /**
		  * Broadcasts the flush request, to clear all the tabs' caches 
		  * @see Tab#flush()
		  * @param rtype the type of refresh
		  */
		 public void flush (Tab.RefreshType rtype)
		 {
			 if (conn != null) {
				 switch (rtype) {
				 case FULL:				 
					 conn.flush ();
					 /* fall through */
				 
				 case MEDIUM:				 				 
				 case LIGHT:
				 }
			 }
			 
			 for (Tab tab : tabs)
				 tab.flush (rtype);			 
		 }
		 
		 /**
		  * Returns the index of a tab.
		  * @param c its contents
		  */
		 public int getTabIndex (Tab.Contents c)
		 {
			 int i;
			 
			 for (i = 0; i < tabs.size (); i++)
				 if (tabs.get (i).contains (c))
					 return i;
			 
			 return -1;
		 }
		 
		 public boolean backButton ()
		 {
			 int idx;
			 
			 idx = pager.getCurrentItem ();
			 return idx >= 0 && idx < tabs.size () ?
					 tabs.get (idx).backButton () : false;
		 }
    }

	/**
	 * A receiver that gets notifications on several occasions:
	 * <ul>
	 * 	<li>@link {@link SettingsActivity#ACT_CREDENTIALS}: We need this
	 * 			both to update the credentials used to fill the stats
	 * 			screen, and to enable notifications if the user chose so.
	 * 	<li>@link {@link SettingsActivity#ACT_NOTIFY}: Enable the notification
	 * 			flag
	 * 	<li>@link {@link #ACTION_REFRESH}: force a refresh onto the dashboard.
	 *  <li>@link {@link #ACTION_CLEAR}: force a complete refresh (including stats)
	 * </ul>
	 * All the events that cause a refresh of dashboard data, will also
	 * trigger a cache flush.
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
			Tab.RefreshType rtype;
			UserLogin ul;
			String action;
			
			/* This check is meant to avoid a somehow strange race condition: 
			 * if a WebReviewActivity is started by tapping on the notification (i.e. the app is
			 * NOT active), and is closed right before starting the MainActivity, the ACTION_REFRESH
			 * is delivered by the wrong (dying) thread. This causes a ViewRoot$CalledFromWrongThreadException 
			 * It looks like an Android bug... but I'm not so sure. At any rate this check
			 * does no harm
			 */
			if (!Looper.getMainLooper ().equals (Looper.myLooper ()))
				return;
			
			action = i.getAction ();
			if (action.equals (SettingsActivity.ACT_CREDENTIALS)) {
				ul = new UserLogin (i.getStringExtra (SettingsActivity.E_USERKEY));
			
				updateCredentials (ul);				
				enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));
			} else if (action.equals (SettingsActivity.ACT_NOTIFY))
				enableNotifications (i.getBooleanExtra (SettingsActivity.E_ENABLED, true));			
			else if (action.equals (ACTION_REFRESH)) {
				rtype = i.getBooleanExtra (E_FLUSH_CACHES, true) ? 
						Tab.RefreshType.FULL : Tab.RefreshType.MEDIUM;
				refresh (rtype);
			} else if (action.equals (ACTION_CLEAR))
				flushDatabase ();
		}
		
	}
	
	/**
	 * A task that gets called whenever stats need to be refreshed.
	 * In order to keep the GUI responsive, we do this through an AsyncTask
	 */
	private class RefreshTask extends AsyncTask<Connection, Void, DashboardData > {
		
		/// The default "turtle" avatar
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
			Connection.Meter meter;
			DashboardData dd;
			UserInformation ui;
			StudyQueue sq;
			int size;

			size = getResources ().getDimensionPixelSize (R.dimen.m_avatar_size);
			meter = MeterSpec.T.DASHBOARD_REFRESH.get (MainActivity.this);
			try {
				sq = conn [0].getStudyQueue (meter);
				/* getUserInformation should be called after at least one
				 * of the other calls, so we give Connection a chance
				 * to cache its contents */
				ui = conn [0].getUserInformation (meter);
				conn [0].resolve (meter, ui, size, defAvatar);

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
				
				refreshComplete (dd, true);
				
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
			DashboardData.OptionalDataStatus srsStatus, lpStatus, ciStatus;
			SRSDistribution srs;
			LevelProgression lp;
			ItemLibrary<Item> critical;
			int cis;
			
			try {
				srs = conn [0].getSRSDistribution(MeterSpec.T.DASHBOARD_REFRESH.get (MainActivity.this));
				srsStatus = DashboardData.OptionalDataStatus.RETRIEVED;
			} catch (IOException e) {
				srs = null;
				srsStatus = DashboardData.OptionalDataStatus.FAILED;
			}

			try {
				lp = conn [0].getLevelProgression (MeterSpec.T.DASHBOARD_REFRESH.get (MainActivity.this));
				lpStatus = DashboardData.OptionalDataStatus.RETRIEVED;
			} catch (IOException e) {
				lp = null;
				lpStatus = DashboardData.OptionalDataStatus.FAILED;
			}

			try {
				critical = conn [0].getCriticalItems (MeterSpec.T.DASHBOARD_REFRESH.get (MainActivity.this));
				ciStatus = DashboardData.OptionalDataStatus.RETRIEVED;
				cis = critical.list.size ();
			} catch (IOException e) {
				ciStatus = DashboardData.OptionalDataStatus.FAILED;
				cis = 0;
			}
			
			return new DashboardData.OptionalData (srs, srsStatus, lp, lpStatus, 
												   cis, ciStatus);
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
			
			refreshComplete (dd, false);
		}
	}
	
	/**
	 * The listener of menu-related events. We intercept the refresh request
	 * and deliver the event to the main class
	 */
	private class MenuListener extends MenuHandler.Listener {
		
		public MenuListener ()
		{
			super (MainActivity.this);
		}
		
		@Override
		public void refresh ()
		{
			MainActivity.this.refresh (Tab.RefreshType.FULL);
		}
		
		@Override
		public void settingsChanged ()
		{			
			if (dd != null)
				refreshComplete (dd, false);
		}		

		@Override
		public void importFile ()
		{
			Intent intent;
			
			intent = new Intent (MainActivity.this, ImportActivity.class);
			intent.setAction (Intent.ACTION_DEFAULT);
			startActivity (intent);
		}
	}
	
	private class DBFixupListener implements DatabaseFixup.Listener {
		
		@Override
		public void done (boolean ok)
		{
			setDBFixup (ok ? FixupState.DONE : FixupState.FAILED);
		}
		
	}

	enum FixupState {
		
		NOT_RUNNING,
		
		RUNNING,
		
		DONE,
		
		FAILED
		
	}
	
	/** The prefix */
	private static final String PREFIX = MainActivity.class + ".";
	
	/*** The avatar bitmap filename */
	private static final String AVATAR_FILENAME = "avatar.png";
		
	/** The key checked by {@link #onCreate} to make 
	 *  sure that the statistics contained in the bundle are valid
	 * @see #onCreate
	 * @see #onSaveInstanceState */
	private static final String BUNDLE_VALID = PREFIX + "bundle_valid";
	
	/**
	 * The key stored into the bundle to keep the items cache
	 */
	private static final String ITEMS_CACHE = PREFIX + "ITEMS_CACHE";
	
	/**
	 * The key stored into the bundle to keep track of the current tab
	 */
	private static final String CURRENT_TAB = PREFIX + "current_tab";
	
	/**
	 * The key stored into the bundle to keep track of refresh operations
	 */
	private static final String REFRESHING = PREFIX + "refreshing";
	
	/**
	 * The key stored into the bundle to keep track of db fixup operations
	 */
	private static final String FIXUP_STATE = PREFIX + "fixup_state";
	

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
	
	/** The pager */
	LowPriorityViewPager pager;
	
	/** Pager adapter instance */
	PagerAdapter pad;
		
	/** The dashboard height, in dip */
	private int DASHBOARD_HEIGHT = 430;
	
	/** The dashboard-stats combo fragment */
	DashboardStatsFragment dsf; 
	
	/** The dashboard fragment */
	DashboardFragment dashboardf;
	
	/** The items fragment */
	ItemsFragment itemsf;
	
	/** The stats fragment */
	StatsFragment statsf;
	
	/** The menu handler */
	MenuHandler mh;

	/** Is this activity visible? */
	boolean visible;
	
	/** Current layout */
	SettingsActivity.Layout layout;
	
	/** Was the last instance of this activity refreshing before being destroyed? */
	boolean resumeRefresh;
	
	/** Are we fixing the db up? */
	FixupState dbfixup;
	
	/** An action that should be invoked to force refresh. This is used typically
	 *  when reviews complete
	 */
	public static final String ACTION_REFRESH = PREFIX + "REFRESH";
	
	/** An action that should be invoked when the history DB is changed
	 */
	public static final String ACTION_CLEAR = PREFIX + "CLEAR";

	/**
	 * Extra parameter to {@link #ACTION_REFRESH}, to tell if caches should be flushed 
	 * or not.
	 */
	public static final String E_FLUSH_CACHES = PREFIX + "flushCaches";
	
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
	    super.onCreate (bundle);

	    receiver = new Receiver ();
	    alarm = new Alarm ();
	    mh = new MenuHandler (this, new MenuListener ());
	    dbfixup = FixupState.NOT_RUNNING;

	    /* Must be placed first, because fragments need this early */
	    conn = new Connection (SettingsActivity.getLogin (this));

	    if (dsf == null)
	    	dsf = new DashboardStatsFragment ();
	    if (dashboardf == null)
	    	dashboardf = new DashboardFragment ();
	    if (itemsf == null)
	    	itemsf = new ItemsFragment ();
	    if (statsf == null)
	    	statsf = new StatsFragment ();
	    
		setContentView (R.layout.main);
		switchTo (R.id.f_splash);

		pager = (LowPriorityViewPager) findViewById (R.id.pager);
        pager.setMain (this);
	    setLayout ();
	    		
        registerIntents ();
	    
	    if (!SettingsActivity.credentialsAreValid (this))
	    	mh.settings ();

	    if (bundle != null && bundle.containsKey (BUNDLE_VALID)) {
	    	dd = new DashboardData (bundle);
			pager.setCurrentItem (bundle.getInt (CURRENT_TAB));
			try {
				conn.cache = (ItemsCache) bundle.getSerializable (ITEMS_CACHE);
			} catch (Throwable t) {
				/* In case serialization fails (e.g. version mismatch during upgrade) */
			}
			
			resumeRefresh = bundle.getBoolean (REFRESHING);
			
			dbfixup = (FixupState) bundle.getSerializable (FIXUP_STATE);
	    } else
	    	pager.setCurrentItem (pad.getTabIndex (Tab.Contents.DASHBOARD), false);
	}
	
	/**
	 * Wraps {@link SettingsActivity#getLayout(Context)} making sure
	 * that {@link SettingsActivity.Layout#AUTO} is never returned.
	 * It is translated into one of the concrete enums by looking at the size
	 * of the device.
	 * @return the layout
	 */
	protected SettingsActivity.Layout getLayout ()
	{
		SettingsActivity.Layout ans;
		DisplayMetrics dm;
		float hdip;

		ans = SettingsActivity.getLayout (this);
		if (ans != SettingsActivity.Layout.AUTO)
			return ans;
					
		dm = new DisplayMetrics ();
		getWindowManager ().getDefaultDisplay ().getMetrics (dm);
		hdip = (Math.max (dm.heightPixels, dm.widthPixels)) / dm.density;
			
		return hdip >= DASHBOARD_HEIGHT * 1.5 ? 
				SettingsActivity.Layout.LARGE : SettingsActivity.Layout.SMALL;
	}

	/**
	 * Populates the adapter and enforces the layout, using the preferences.
	 */
	void setLayout ()
	{
		List<Tab> tabs;
		
	    tabs = new Vector<Tab> ();

	    layout = getLayout ();
	    
	    switch (layout) {
	    case AUTO:
	    case SMALL:
	    	tabs.add (statsf);
	    	tabs.add (dashboardf);
	    	tabs.add (itemsf);
	    	break;
	    	
	    case LARGE:
	    	tabs.add (dsf);
	    	tabs.add (itemsf);
	    }		
        pad = new PagerAdapter (getSupportFragmentManager (), tabs);
        pager.setAdapter (pad);
	}
	
	/**
	 * Called by each tab to register itself to the activity and get called
	 * when something interesting happens
	 * @param tab the tab
	 */
	void register (Tab tab)
	{
		if (pad != null)			
			pad.replace (tab);
		
		if (tab instanceof DashboardStatsFragment)
			dsf = (DashboardStatsFragment) tab;
		else if (tab instanceof DashboardFragment)
			dashboardf = (DashboardFragment) tab;
		else if (tab instanceof ItemsFragment)
			itemsf = (ItemsFragment) tab;
		else if (tab instanceof StatsFragment)
			statsf = (StatsFragment) tab;
	}
	
	@Override
	public void onStart ()
	{
	    super.onStart ();
	    
	    DashboardData ldd;
	    
	    /* A refresh task may be going on, if the review activity
	     * has sent a refresh request before disappearing
	     */
	    if (rtask == null) {
	    	ldd = dd;
	    	dd = null;
	    	if (ldd != null) {
	    		refreshComplete (ldd, false);
	    		if (resumeRefresh)
	    			refresh (Tab.RefreshType.LIGHT);
	    		else if (ldd.isIncomplete ())
	    			refreshOptional ();
	    	} else
	    		refresh (Tab.RefreshType.LIGHT);
	    }
	    
	    resumeRefresh = false;
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
		super.onSaveInstanceState (bundle);
		
		if (dd != null) {
			dd.serialize (bundle);
			bundle.putBoolean (BUNDLE_VALID, true);
			if (conn != null)
				bundle.putSerializable (ITEMS_CACHE, conn.cache);
			bundle.putInt (CURRENT_TAB, pager.getCurrentItem ());
			bundle.putBoolean (REFRESHING, rtask != null);
			bundle.putSerializable (FIXUP_STATE, dbfixup);
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
		visible = true;

		if (layout != getLayout ())
			reboot ();
	}
		
	/**
	 * Restarts the activity 
	 */
	private void reboot ()
	{
		Intent i;
		
		i = new Intent (this, getClass ());
		i.setAction (Intent.ACTION_MAIN);
		
		startActivity (i);
		finish ();
	}
	
	/**
	 * Called when the application pauses.
	 * Update the @link #visible flag.
	 */
	@Override
	public void onPause ()
	{
		super.onPause ();
				
		visible = false;
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
		mh.unregister (this);
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
		filter.addAction (ACTION_CLEAR);
		lbm.registerReceiver (receiver, filter);
		
		filter = new IntentFilter (ACTION_REFRESH);
		registerReceiver (receiver, filter);
	}
	
	/**
	 * Unregister the intent listeners that were registered by {@link #registerIntent}. 
	 */
	private void unregisterIntents ()
	{
		LocalBroadcastManager lbm;
		
		lbm = LocalBroadcastManager.getInstance (this);
		
		lbm.unregisterReceiver (receiver);
		unregisterReceiver (receiver);
	}
	
	/**
	 * Associates the menu description to the menu key (or action bar).
	 * The XML description is <code>main.xml</code>
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate (R.menu.main, menu);
		
		return true;
	}
	
	/**
	 * Menu handler. Relays the call to the common {@link MenuHandler}.
	 * 	@param item the selected menu item
	 */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		return mh.onOptionsItemSelected (item) || super.onOptionsItemSelected (item);
	}

	/**
	 * Called to update the credentials. It also triggers a refresh of 
	 * the GUI.
	 * @param login the new credentials
	 */
	private void updateCredentials (UserLogin login)
	{
		conn = new Connection (login);
		
		refresh (Tab.RefreshType.FULL);
	}

	/**
	 * Called when the refresh timeout expires.
	 * We call {@link #refresh()}, but we keep caches.
	 */
	public void run ()
	{
		refresh (Tab.RefreshType.LIGHT);
	}

	/**
	 * Called when the GUI needs to be refreshed. 
	 * It starts an asynchrous task that actually performs the job and
	 * optionally clears the cache any fragment may hold.
	 * 	@param rtype the type of refresh 
	 */
	private void refresh (Tab.RefreshType rtype)
	{
			if (rtask != null)
				rtask.cancel (false);
			
			pad.flush (rtype);
			
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
	 * Schedules a new refresh. Should be called right after
	 * a refresh operation completes
	 */
	protected void reschedule ()
	{
		long delay, refresh;

		/* Should not happen, because rescheduling makes sense only
		 * when at least one successful retrieval completes. However...*/
		if (dd == null)
			return;
		
		refresh = SettingsActivity.getRefreshTimeout (this) * 60 * 1000; 
		if (dd.nextReviewDate != null)
			delay = dd.nextReviewDate.getTime () - System.currentTimeMillis ();
		else
			delay = refresh;
		
		if (delay > refresh || dd.reviewsAvailable > 0)
			delay = refresh;
		
		/* May happen if local clock is not perfectly synchronized with WK clock */
		if (delay < 1000)
			delay = 10000;
		
		alarm.schedule (this, delay);		
	}

	/**
	 * Called by {@link RefreshTask} when asynchronous data 
	 * retrieval is completed.
	 * @param dd the retrieved data
	 * @param intermediate if this is an updated, and more will come
	 */
	private void refreshComplete (DashboardData dd, boolean intermediate)
	{
		if (!intermediate)
			pad.spin (false);

		if (this.dd == null)
			switchTo (R.id.f_main);
		
		if (this.dd != null)
			dd.merge (this.dd);

		this.dd = dd;
		
		shareData (dd);
		
		rtask = null;

		if (dd.gravatar == null)
			restoreAvatar (dd);
		
		pad.refreshComplete (dd);

		reschedule ();
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
	 * Sends fresh dashboard data to the notification service.
	 * @param dd the data
	 */
	private void shareData (DashboardData dd)
	{
		Intent intent;
		Bundle b;
		
		/* If the current activity is not visible, leave the notification service
		 * decide whether to update data or not (there's some code in it to avoid
		 * displaying the icon while reviews are going on, and this call would
		 * break it) */
		if (visible && dd != null) {
			intent = new Intent (this, NotificationService.class);
			intent.setAction (NotificationService.ACTION_NEW_DATA);
			b = new Bundle ();
			dd.serialize (b);
			intent.putExtra (NotificationService.KEY_DD, b);
			
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
			switchTo (R.id.f_splash);
		else
			pad.spin (true);
	}

	/**
	 * Displays an error message, choosing the best way to do that.
	 * If we did not gather any stats, the only thing we can do is to display
	 * the error page and hope for the best
	 * @param id resource string ID
	 */
	private void error (int id)
	{
		Resources res;
		String s;
		TextView tw;

		/* If the API code is invalid, switch to error screen even if we have
		 * already opened the dashboard */
		if (id == R.string.status_msg_unauthorized)
			dd = null;
		
		if (dd == null)
			switchTo (R.id.f_error);
		else {
			pad.spin (false);
			reschedule ();
		}
		
		res = getResources ();
		if (id == R.string.status_msg_unauthorized)
			s = SettingsActivity.diagnose (this, res);
		else
			s = res.getString (id);
	
		tw = (TextView) findViewById (R.id.tv_alert);
		if (tw != null)
			tw.setText (s);
	}

	/**
	 * Called when the "Review" button is clicked. According
	 * to user preferences, it sill start either an integrated WebView
	 * or an external browser.
	 */
	public void review ()
	{
		Intent intent;
		
		intent = new Intent (MainActivity.this, NotificationService.class);			
		intent.setAction (NotificationService.ACTION_HIDE_NOTIFICATION);			
		startService (intent);
		
		open (SettingsActivity.getURL (this));
	}
	
	/**
	 * Called when the "Unlock" button is clicked. According
	 * to user preferences, it sill start either an integrated WebView
	 * or an external browser. 
	 */
	public void lessons ()
	{
		open (SettingsActivity.getLessonURL (this));
	}
	
	/**
	 * Called to open an item page.
	 * @param url the url page
	 */
	public void item (String url)
	{
		open (url);
	}
	
	/**
	 * Called to open the forum page.
	 */
	public void chat ()
	{
		open ("http://www.wanikani.com/chat");
	}

	/**
	 * Called to open the review summary page.
	 */
	public void reviewSummary ()
	{
		open ("http://www.wanikani.com/review");
	}

	/**
	 * Open an URL. Depending on the integrated browser key, it chooses
	 * whether to use the internal or the external browser.
	 * 	@param url the URL to open
	 */
	protected void open (String url)
	{
		Intent intent;
			
		if (SettingsActivity.getUseIntegratedBrowser (this)) {
			intent = new Intent (MainActivity.this, WebReviewActivity.class);
			intent.setAction (WebReviewActivity.OPEN_ACTION);
		} else
			intent = new Intent (Intent.ACTION_VIEW);
		
		intent.setData (Uri.parse (url));
		startActivity (intent);		
	}
	
	/**
	 * Shows the items tab, and applies a filter to displays only the apprentice items
	 * of a given kind. Needed by the dashboard to implement the "remaining items"
	 * feature
	 * @param type the type to display. Theorically it could be null, but
	 * 	do we really want to?
	 */
	public void showRemaining (Item.Type type)
	{
		pager.setCurrentItem (pad.getTabIndex (Tab.Contents.ITEMS), true);
		itemsf.setLevelFilter (dd.level);
		itemsf.showSearchDialog (true, SRSLevel.APPRENTICE, type);
	}

	/**
	 * Shows the items tab, and applies a filter to displays only the critical items.
	 * Needed by the dashboard to implement the "critical items" feature
	 */
	public void showCritical ()
	{
		pager.setCurrentItem (pad.getTabIndex (Tab.Contents.ITEMS), true);
		itemsf.setCriticalFilter ();
		itemsf.hideSearchDialog ();
	}

	/**
	 * Shows the items tab, and shows the search dialog
	 */
	public void showSearch ()
	{
		int idx;
		
		idx = pad.getTabIndex (Tab.Contents.ITEMS);
		if (pager.getCurrentItem () != idx) {
			pager.setCurrentItem (pad.getTabIndex (Tab.Contents.ITEMS), true);
			itemsf.showSearchDialog (true, null, null);
		} else
			itemsf.showSearchDialog (false, null, null);
	}

	/**
	 * Returns the latest version of the dashboard data. Used by tabs.
	 * @return the dashboard data 
	 */
	public DashboardData getDashboardData ()
	{
		return dd;
	}
	
	/**
	 * Returns the WKLib connection
	 * @return the connection 
	 */
	public Connection getConnection ()
	{
		return conn;
	}

	/**
	 * Tells whether if a given tab is intercepting scroll events
	 * @return true if it does
	 */
	public boolean scrollLock (int item)
	{
		return pad.tabs.get (item).scrollLock (); 
	}
	
	/**
	 * Updates the splash screen with the version ID
	 * 	@param view the view
	 */
	private void setVersion (View view)
	{
		PackageManager pmgr;
		PackageInfo pinfo;
		Resources res;
		TextView tv;
		
		pmgr = getPackageManager ();
		res = getResources ();
		try {
			pinfo = pmgr.getPackageInfo (getPackageName (), 0);
			tv = (TextView) view.findViewById (R.id.splash_version);
			tv.setText (res.getString (R.string.tag_version, pinfo.versionName));
		} catch (NameNotFoundException e) {
			/* leave it as it is */
		}				
	}
	
	/**
	 * Switches to one of the three screeens (main, dashboard or error)
	 * @param id the id to switch to
	 */
	public void switchTo (int id)
	{
		View view;
		
		view = findViewById (R.id.f_main);
		view.setVisibility (id == R.id.f_main ? View.VISIBLE : View.INVISIBLE);

		view = findViewById (R.id.f_splash);
		setVersion (view);
		view.setVisibility (id == R.id.f_splash ? View.VISIBLE : View.INVISIBLE);

		view = findViewById (R.id.f_error);
		view.setVisibility (id == R.id.f_error ? View.VISIBLE : View.INVISIBLE);
	}
	
	@Override
	public boolean onSearchRequested ()
	{
		showSearch ();
		
		return true;
	}
	
	@Override
	public void onBackPressed ()
	{
		if (!pad.backButton())
			super.onBackPressed ();
	}
	
	public void dbFixup ()
	{
		setDBFixup (FixupState.RUNNING);
		
		new DatabaseFixup (this, conn).asyncRun(new DBFixupListener ());
	}
	
	private void setDBFixup (FixupState dbfixup)
	{
		this.dbfixup = dbfixup;
		
		if (statsf != null && statsf.isResumed ())
			statsf.setFixup (dbfixup);
	}
	
	private void flushDatabase ()
	{
		pad.flushDatabase ();
	}
}
