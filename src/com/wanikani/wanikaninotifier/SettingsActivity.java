package com.wanikani.wanikaninotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

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
 * The activity that handles the preferences menu.
 * In addition to the standard behaviour, whenever credentials or notification settings
 * are changed, we send an intent to the {@link DashboardActivity} to trigger refresh and
 * start/stop of the notification service. 
 */
public class SettingsActivity 
	extends PreferenceActivity 
	implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	/** Preferences enabled key. Must match preferences.xml */
	private static final String KEY_PREF_ENABLED = "pref_enabled";
	/** User key. Must match preferences.xml */
	private static final String KEY_PREF_USERKEY = "pref_userkey";
	/** Wanikani URL key. Must match preferences.xml */
	private static final String KEY_PREF_URL = "pref_url";
	
	/** The current login. Used to check whether something gets changed */
	private UserLogin login;
	
	/** The current notification settings. Used to check whether it is toggled */ 
	private boolean enabled;
	
	/** The action sent when credentials are changed */
	public static final String ACT_CREDENTIALS = "com.wanikani.wanikaninotifier.action.CREDENTIALS";
	
	/** The action sent when notifications are enabled or disabled */
	public static final String ACT_NOTIFY = "com.wanikani.wanikaninotifier.action.NOTIFY";

	/** Extra key in the intent {@link #ACT_CREDENTIALS} action, 
	 * containing the user key  */
	public static final String E_USERKEY = "userkey";

	/** Extra key in the intent {@link #ACT_CREDENTIALS} and {@link #ACT_NOTIFY} action, 
	 * containing the notifications enabled state  */
	public static final String E_ENABLED = "enabled";
	
	/** 
	 * Called when the activity is first created. We setup the menu, basing on the  
	 * current preferences.
	 * 	@pram savedInstanceState bundle (ignored)
	 */
	@SuppressWarnings ("deprecation")
	@Override	
	public void onCreate(Bundle savedInstanceState) 
	{	
		SharedPreferences prefs;
		
	    super.onCreate(savedInstanceState);
	    
	    addPreferencesFromResource (R.xml.preferences);
	    
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		login = getLogin (prefs);
		enabled = getEnabled (prefs);
				
		onSharedPreferenceChanged (prefs, KEY_PREF_USERKEY);
		onSharedPreferenceChanged (prefs, KEY_PREF_URL);
		onSharedPreferenceChanged (prefs, KEY_PREF_ENABLED);
		prefs.registerOnSharedPreferenceChangeListener (this);
	}
	
	
	/**
	 * Called when preferences get changed. We check the new values,
	 * handle changes (for instance, if credentials are not supplied, we
	 * must disabled the "notification" checkbox.
	 */
	@SuppressWarnings ("deprecation")
	public void onSharedPreferenceChanged (SharedPreferences prefs, String key)
	{
		Preference pref;
		String s;
		
		pref = findPreference (key);
		
		if (key.equals (KEY_PREF_ENABLED)) {
				/* empty */
		} else if (key.equals (KEY_PREF_USERKEY)) {
			s = prefs.getString (KEY_PREF_USERKEY, "").trim ();
			if (s.length () > 0)
				pref.setSummary (s);
			else
				pref.setSummary (R.string.pref_userkey_descr);
			enableNotificationCheck (prefs);
		} else if (key.equals (KEY_PREF_URL)) {
			s = prefs.getString (KEY_PREF_URL, "").trim ();
			if (s.length () > 0)
				pref.setSummary (s);
			else
				pref.setSummary (R.string.pref_default_url);
		}
		
		updateConfig (prefs);
	}
	
	@SuppressWarnings ("deprecation")
	private void enableNotificationCheck (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_ENABLED);
		pref.setEnabled (credentialsAreValid (prefs));	
	}
	
	public static UserLogin getLogin (SharedPreferences prefs)
	{
		return new UserLogin (prefs.getString (KEY_PREF_USERKEY, ""));		
	}
	
	public static boolean getEnabled (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_ENABLED, true);	
	}
	
	public static boolean credentialsAreValid (SharedPreferences prefs)
	{
		return prefs.getString (KEY_PREF_USERKEY, "").length () > 0;
	}
	
	public static String getURL (SharedPreferences prefs)
	{
		return prefs.getString (KEY_PREF_URL, "");
	}
	int val;
	@SuppressWarnings ("deprecation")
	private void updateConfig (SharedPreferences prefs)
	{
		LocalBroadcastManager lbm;
		UserLogin llogin;
		boolean lenabled;
		Intent i;
		
		llogin = getLogin (prefs);
		lenabled = getEnabled (prefs);
		lenabled &= findPreference (KEY_PREF_ENABLED).isEnabled ();
		
		lbm = LocalBroadcastManager.getInstance (this);
		if (!llogin.equals (login)) {
			i = new Intent (ACT_CREDENTIALS);
			i.putExtra (E_USERKEY, llogin.userkey);
			i.putExtra (E_ENABLED, lenabled);
			login = llogin;
			lbm.sendBroadcast (i);
		} else if (lenabled != enabled) {
			i = new Intent (ACT_NOTIFY);
			i.putExtra (E_ENABLED, lenabled);
			enabled = lenabled;
			lbm.sendBroadcast (i);
		} 
	}
}
