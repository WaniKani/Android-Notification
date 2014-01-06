package com.wanikani.androidnotifier;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;

import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Connection.Meter;

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

public class MeterSpec implements Connection.Meter {

	public enum T {
		
		DASHBOARD_REFRESH,
			
		ITEMS,

		RECONSTRUCT_DIALOG,

		NOTIFY_TIMEOUT,

		NOTIFY_CHANGE_CONNECTIVITY,

		NOTIFY_DAILY_JOBS,

		OTHER_STATS,
		
		MORE_STATS,		
		
		OTHER_STATS_TOTAL {
			public T [] getComponents ()
			{
				return new T [] { 
					OTHER_STATS,
					MORE_STATS
				};
			}			
		},


		APPLICATION {
			public T [] getComponents ()
			{
				return new T [] { 
					DASHBOARD_REFRESH,
					ITEMS,
					RECONSTRUCT_DIALOG,
					OTHER_STATS,
					MORE_STATS
				};
			}
		},
		
		SERVICE {
			public T [] getComponents ()
			{
				return new T [] { 
					NOTIFY_TIMEOUT,
					NOTIFY_CHANGE_CONNECTIVITY,
					NOTIFY_DAILY_JOBS
				};
			}
		},
		
		OVERALL {			
			public T [] getComponents ()
			{
				return new T [] { 
					APPLICATION,
					SERVICE
				};
			}			
		};
		
		public Meter get (Context ctxt)
		{
			return instantiate (ctxt);
		}
		
		public Counter getCounter (Context ctxt, AmountType at)
		{
			return instantiate (ctxt).getCounter (at);
		}
		
		protected MeterSpec instantiate (Context ctxt)
		{
			return new MeterSpec (ctxt, this);
		}
		
		public T [] getComponents () 
		{
			return null;
		}
	}
	
	public enum AmountType {
		
		SINCE_LAST_RESET {			
			public float getRatio (long elapsed)
			{
				return 1;
			}
		},
		
		AVG_DAY {
			public float getRatio (long elapsed)
			{
				return 24f * 60f * 60f / (1 + elapsed / 1000);
			}
		},
		
		AVG_MONTH {
			public float getRatio (long elapsed)
			{
				return AVG_DAY.getRatio (elapsed) * 30;
			}
		};
		
		public abstract float getRatio (long elapsed);
		
		public String getUnit () 
		{
			return "Bytes";
		}
	}
	
	public static class Counter {
		
		public AmountType at;
		
		public long mobile;
		
		public long wifi;

		public long unknown;
		
		public Counter (AmountType at, long mobile, long wifi, long unknown)
		{
			this.at = at;
			this.mobile = mobile;
			this.wifi = wifi;
			this.unknown = unknown;
		}
		
		public long total ()
		{
			return mobile + wifi + unknown;
		}
	}
		
	private static final String PREFIX = MeterSpec.class.toString ();
	
	private static final String START_TIME = PREFIX + "START_TIME";
	
	private ConnectivityManager cmgr;
	
	private SharedPreferences prefs;
	
	private T type;
	
	private int count;
	
	private static final String CTAG_UNKNOWN = "u."; 

	private static final String CTAG_MOBILE = "m.";
	
	private static final String CTAG_WIFI = "w.";
	
	private static final String PREFERENCES_FILE = "meters.xml";
	
	private static Object mutex = new Object ();

	private MeterSpec (Context ctxt, T type)
	{
		this.type = type;
		
		prefs = prefs (ctxt);
		cmgr = (ConnectivityManager) ctxt.getSystemService (Context.CONNECTIVITY_SERVICE);
	}
	
	static SharedPreferences prefs (Context ctxt)
	{
		int flags;
		
		flags = Context.MODE_PRIVATE;
		if (Build.VERSION.SDK_INT >= 11)
			flags |= Context.MODE_MULTI_PROCESS;
		return ctxt.getSharedPreferences (PREFERENCES_FILE, flags);		
	}
	
	public void count (int bytes)
	{
		count += bytes;
	}
	
	public static Date getLastReset (SharedPreferences prefs)
	{
		synchronized (mutex) {
			if (!prefs.contains (START_TIME)) 
				prefs.edit ().putLong (START_TIME, System.currentTimeMillis ()).commit ();			
		}
		
		return new Date (prefs.getLong (START_TIME, System.currentTimeMillis ()));
	}
	
	public void sync ()
	{
		String key;
		Editor e;
		
		key = getKey (type, connectivity ());
		
		synchronized (mutex) {
			e = prefs.edit ();
			if (!prefs.contains (START_TIME))
				e.putLong (START_TIME, System.currentTimeMillis ());
			
			e.putLong (key, prefs.getLong (key, 0) + count);
			e.commit ();
		}
		count = 0;
	}
	
	private static String getKey (T type, String connectivity)
	{
		return PREFIX + type.name () + "." + connectivity;
	}
	
	private String connectivity ()
	{
		NetworkInfo info;
		
		if (cmgr == null)
			return CTAG_UNKNOWN;
		
		info = cmgr.getActiveNetworkInfo ();
		if (info == null)
			return CTAG_UNKNOWN; 		
		else if (info.getType () == ConnectivityManager.TYPE_MOBILE)
			return CTAG_MOBILE;
		else
			return CTAG_WIFI;
	}

	private static void addValue (SharedPreferences prefs, T t, Counter counter)
	{
		T components [];
		int i;
		
		components = t.getComponents ();
		if (components != null) {
			for (i = 0; i < components.length; i++)
				addValue (prefs, components [i], counter);			
		} else {
			counter.mobile += prefs.getLong (getKey (t, CTAG_MOBILE), 0);
			counter.wifi += prefs.getLong (getKey (t, CTAG_WIFI), 0);
			counter.unknown += prefs.getLong (getKey (t, CTAG_UNKNOWN), 0);
		}
	}
		
	private Counter getCounter (AmountType at)
	{
		Counter ans;
		Date date;
		float ratio;
		
		ans = new Counter (at, 0, 0, 0);
		synchronized (mutex) {
			addValue (prefs, type, ans);
			date = getLastReset (prefs); 
		}
		ratio = at.getRatio (System.currentTimeMillis () - date.getTime ());
		ans.mobile *= ratio;
		ans.wifi *= ratio;
		ans.unknown *= ratio;
		
		return ans;
	}
	
	public static void reset (Context ctxt)
	{
		Editor e;
		
		synchronized (mutex) {
			e = prefs (ctxt).edit ();
			e.putLong (START_TIME, System.currentTimeMillis ());
			for (T type : T.values ()) {
				e.putLong (getKey (type, CTAG_MOBILE), 0);
				e.putLong (getKey (type, CTAG_WIFI), 0);
				e.putLong (getKey (type, CTAG_UNKNOWN), 0);
			}
			e.commit ();
		}
	}	
}


