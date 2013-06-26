package com.wanikani.androidnotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
	/** Use integrated browser key. Must match preferences.xml */
	private static final String KEY_PREF_USE_INTEGRATED_BROWSER = "pref_use_integrated_browser";
	/** User key. Must match preferences.xml */
	private static final String KEY_PREF_USERKEY = "pref_userkey";
	/** Refresh timeout. Must match preferences.xml */
	private static final String KEY_PREF_REFRESH_TIMEOUT = "pref_refresh_timeout";
	/** Enable enter keyboard key. Must match preferences.xml */
	private static final String KEY_PREF_SHOW_KEYBOARD = "pref_show_keyboard";
	/** Enable enter keyboard key. Must match preferences.xml */
	private static final String KEY_PREF_ENTER = "pref_enter";
	/** Enable 42+ mode. Must match preferences.xml */
	private static final String KEY_PREF_42PLUS = "pref_42plus";
	
	/** The correct length of the API key (as far as we know) */
	private static final int EXPECTED_KEYLEN = 32;
	
	/** Default timeout */
	private static final int DEFAULT_REFRESH_TIMEOUT = 5;
	
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
		onSharedPreferenceChanged (prefs, KEY_PREF_ENABLED);
		onSharedPreferenceChanged (prefs, KEY_PREF_REFRESH_TIMEOUT);
		onSharedPreferenceChanged (prefs, KEY_PREF_USE_INTEGRATED_BROWSER);
		onSharedPreferenceChanged (prefs, KEY_PREF_SHOW_KEYBOARD);
		onSharedPreferenceChanged (prefs, KEY_PREF_ENTER);
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
		Resources res;
		Preference pref;
		int timeout;
		String s;
		
		pref = findPreference (key);
		res = getResources ();
		
		if (key.equals (KEY_PREF_USE_INTEGRATED_BROWSER)) {
			pref.setSummary (getUseIntegratedBrowser (prefs) ? 
							 R.string.pref_use_integrated_browser_desc :
							 R.string.pref_use_external_browser_desc);
			runIntegratedBrowserHook (prefs);
		} if (key.equals (KEY_PREF_SHOW_KEYBOARD)) {
			runShowKeyboardHooks (prefs);
		} else if (key.equals (KEY_PREF_USERKEY)) {
			s = prefs.getString (KEY_PREF_USERKEY, "").trim ();
			if (s.length () > 0)
				pref.setSummary (s);
			else
				pref.setSummary (R.string.pref_userkey_descr);
			enableNotificationCheck (prefs);
		} else if (key.equals (KEY_PREF_REFRESH_TIMEOUT)) {
			timeout = getInt (prefs, KEY_PREF_REFRESH_TIMEOUT, DEFAULT_REFRESH_TIMEOUT);
			if (timeout <= 0)
				timeout = 1;
			if (timeout > 1)
				s = res.getString (R.string.pref_refresh_descr, timeout);
			else
				s = res.getString (R.string.pref_refresh_one_min_descr);
			pref.setSummary (s);
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
	
	@SuppressWarnings ("deprecation")
	private void runIntegratedBrowserHook (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_SHOW_KEYBOARD);
		pref.setEnabled (getUseIntegratedBrowser (prefs));
		
		runShowKeyboardHooks (prefs);
	}

	@SuppressWarnings ("deprecation")
	private void runShowKeyboardHooks (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_ENTER);
		pref.setEnabled (getShowKeyboard (prefs) && getUseIntegratedBrowser (prefs));	
	}
	
	public static String diagnose (SharedPreferences prefs, Resources res)
	{
			StringBuffer sb;
			String key, fmt;
			int delta;
			
			sb = new StringBuffer (res.getString (R.string.status_msg_unauthorized));
			key = prefs.getString (KEY_PREF_USERKEY, "");
			delta = key.length () - EXPECTED_KEYLEN;
			if (key.length () == 0 || delta == 0)
				/* Say nothing more */;
			else if (delta > 0) {
				fmt = res.getString (R.string.status_msg_unauthorized_too_long);
				sb.append ('\n').append (String.format (fmt, delta));
			} else {
				fmt = res.getString (R.string.status_msg_unauthorized_too_short);
				sb.append ('\n').append (String.format (fmt, -delta));
			}
			
			return sb.toString ();
	}
	
	public static UserLogin getLogin (SharedPreferences prefs)
	{
		return new UserLogin (prefs.getString (KEY_PREF_USERKEY, ""));		
	}
	
	public static boolean getEnabled (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_ENABLED, true);	
	}
	
	public static boolean getUseIntegratedBrowser (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_USE_INTEGRATED_BROWSER, true);	
	}

	public static boolean credentialsAreValid (SharedPreferences prefs)
	{
		return prefs.getString (KEY_PREF_USERKEY, "").length () > 0;
	}
	
	public static int getRefreshTimeout (SharedPreferences prefs)
	{
		return getInt (prefs, KEY_PREF_REFRESH_TIMEOUT, DEFAULT_REFRESH_TIMEOUT);
	}
	
	public static boolean getShowKeyboard (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_SHOW_KEYBOARD, true);
	}
	
	public static boolean getEnter (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_ENTER, true);
	}
	
	public static boolean get42plus (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_42PLUS, false);
	}
	
	private static int getInt (SharedPreferences prefs, String key, int defval)
	{
		String s;
		
		s = prefs.getString (key, Integer.toString (defval));
		try {
			return Integer.parseInt (s);
		} catch (NumberFormatException e) {
			return defval;
		}
	}
	
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
