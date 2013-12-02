package com.wanikani.androidnotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
	
	static enum Layout {
		
		AUTO, SMALL, LARGE
		
	};
	
	static enum Keyboard {
		LOCAL_IME, NATIVE
	};
	
	/** Preferences enabled key. Must match preferences.xml */
	private static final String KEY_PREF_ENABLED = "pref_enabled";
	/** Notify threshold */
	private static final String KEY_PREF_NOT_THRESHOLD = "pref_not_threshold";
	/** Enable lessons. Must match preferences.xml */
	private static final String KEY_PREF_LESSONS_ENABLED = "pref_lessons_enabled";
	/** Use integrated browser key. Must match preferences.xml */
	private static final String KEY_PREF_USE_INTEGRATED_BROWSER = "pref_use_integrated_browser";
	/** User key. Must match preferences.xml */
	private static final String KEY_PREF_USERKEY = "pref_userkey";
	/** Refresh timeout. Must match preferences.xml */
	private static final String KEY_PREF_REFRESH_TIMEOUT = "pref_refresh_timeout";
	/** Enable reviews improvements. Must match preferences.xml */
	private static final String KEY_PREF_REVIEW_IMPROVEMENTS = "pref_review_improvements";
	/** Show mute button. Must match preferences.xml */
	private static final String KEY_PREF_SHOW_MUTE = "pref_show_mute";
	/** Ignore button */
	private static final String KEY_PREF_IGNORE_BUTTON = "pref_ignore_button";
	/** WaniKani improve */
	private static final String KEY_PREF_WANIKANI_IMPROVE = "pref_wanikani_improve";
	/** Review Order */
	private static final String KEY_PREF_REVIEW_ORDER = "pref_review_order";
	/** Lesson Order */
	private static final String KEY_PREF_LESSON_ORDER = "pref_lesson_order";
	/** Frame placer */
	private static final String KEY_PREF_EXTERNAL_FRAME_PLACER = "pref_external_frame_placer";
	/** Frame placer dictionary */
	private static final String KEY_PREF_EXTERNAL_FRAME_PLACER_DICT = "pref_external_frame_placer_dict";
	/** Info popup */
	private static final String KEY_PREF_ERROR_POPUP = "pref_error_popup";
	/** Enable 42+ mode. Must match preferences.xml */
	private static final String KEY_PREF_42PLUS = "pref_42plus";
	/** Wanikani review URL */
	private static final String KEY_URL = "pref_review_url";
	/** Wanikani lesson URL */
	private static final String KEY_LESSON_URL = "pref_lesson_url2";
	/** Waninaki review URL counter */
	private static final String KEY_URL_VERSION = "review_url_version";
	/** Lock screen during reviews */
	private static final String KEY_LOCK_SCREEN = "pref_lock_screen";
	/** Full screen during reviews */
	private static final String KEY_FULLSCREEN = "pref_fullscreen";
	/** The infamous CPU/memory leak kludge */
	private static final String KEY_LEAK_KLUDGE = "pref_leak_kludge";
	/** The even more infamous timer reaper kludge */
	private static final String KEY_TIMER_REAPER = "pref_timer_reaper";
	/** The layout type */
	private static final String KEY_LAYOUT = "pref_layout";
	/** The export destination */
	private static final String KEY_PREF_EXPORT_DEST = "pref_export_dest";
	/** The export file */
	private static final String KEY_PREF_EXPORT_FILE = "pref_export_file";
	/** Disable keyboard suggestions */
	private static final String KEY_PREF_DISABLE_SUGGESTIONS = "pref_disable_suggestions";
	
	/** Embedded keyboard message has been read and acknowledged */
	private static final String KEY_TIP_ACK = "key_tip_ack";
	/** Ignore button message has been read and acknowledged */
	private static final String KEY_IGNORE_BUTTON_MESSAGE_ACK = "key_ignore_button_message_ack";
	/** Custom IME message has been read and acknowledged */
	private static final String KEY_CUSTOM_IME = "key_custom_ime";

	/** Mute review */
	private static final String KEY_MUTE = "mute";
	
	/** The correct length of the API key (as far as we know) */
	private static final int EXPECTED_KEYLEN = 32;
	
	/** Default timeout */
	private static final int DEFAULT_REFRESH_TIMEOUT = 5;
	
	/** The current login. Used to check whether something gets changed */
	private UserLogin login;
	
	/** The current notification settings. Used to check whether it is toggled */ 
	private boolean enabled;
	
	/** The current lessons notification settings. Used to check whether it is toggled */ 
	private boolean lessonsEnabled;
	
	/** The current threshold */
	private int threshold;
	
	/** This flag is set when initialization is complete. From this point on,
	 *  all the calls to {@link SettingsActivity#updateConfig} come from genuine
	 *  changes to the menu preferences 
	 */
	private boolean inited;

	/** The action sent when credentials are changed */
	public static final String ACT_CREDENTIALS = "com.wanikani.wanikaninotifier.action.CREDENTIALS";
	
	/** The action sent when notifications are enabled or disabled */
	public static final String ACT_NOTIFY = "com.wanikani.wanikaninotifier.action.NOTIFY";

	/** The action sent when preferences get changed */
	public static final String ACT_CHANGED = "com.wanikani.wanikaninotifier.action.CHANGED";

	/** Extra key in the intent {@link #ACT_CREDENTIALS} action, 
	 * containing the user key  */
	public static final String E_USERKEY = "userkey";

	/** Extra key in the intent {@link #ACT_CREDENTIALS} and {@link #ACT_NOTIFY} action, 
	 * containing the notifications enabled state  */
	public static final String E_ENABLED = "enabled";
	
	/// The japanese typeface path
	private static final String JAPANESE_TYPEFACE_FONT = "/system/fonts/MTLmr3m.ttf";
	
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
	    
		prefs = prefs (this);
		login = getLogin (prefs);
		enabled = getEnabled (prefs);
		lessonsEnabled = getLessonsEnabled (prefs);
		threshold = getReviewThreshold (prefs);
		
		onSharedPreferenceChanged (prefs, KEY_PREF_USERKEY);
		onSharedPreferenceChanged (prefs, KEY_PREF_ENABLED);
		onSharedPreferenceChanged (prefs, KEY_PREF_REFRESH_TIMEOUT);
		onSharedPreferenceChanged (prefs, KEY_PREF_NOT_THRESHOLD);
		onSharedPreferenceChanged (prefs, KEY_PREF_USE_INTEGRATED_BROWSER);
		onSharedPreferenceChanged (prefs, KEY_PREF_REVIEW_IMPROVEMENTS);
		onSharedPreferenceChanged (prefs, KEY_PREF_EXPORT_DEST);
		onSharedPreferenceChanged (prefs, KEY_PREF_EXPORT_FILE);
		onSharedPreferenceChanged (prefs, KEY_PREF_EXTERNAL_FRAME_PLACER);
		inited = true;
		
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
		int timeout, threshold;
		String s;
		
		pref = findPreference (key);
		res = getResources ();
				
		if (key.equals (KEY_PREF_ENABLED))
			runEnabledHooks (prefs);
		else if (key.equals (KEY_PREF_USE_INTEGRATED_BROWSER)) {
			pref.setSummary (getUseIntegratedBrowser (prefs) ? 
							 R.string.pref_use_integrated_browser_desc :
							 R.string.pref_use_external_browser_desc);
			runIntegratedBrowserHook (prefs);
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
		} else if (key.equals (KEY_PREF_NOT_THRESHOLD)) {
			threshold = getInt (prefs, KEY_PREF_NOT_THRESHOLD, 1);
			if (threshold <= 0)
				threshold = 1;
			if (threshold > 1)
				s = res.getString (R.string.pref_not_threshold_desc, threshold);
			else
				s = res.getString (R.string.pref_not_threshold_one_desc);
			pref.setSummary (s);
		} else if (key.equals (KEY_URL)) {
			s = getURL (prefs);
			if (s.length () == 0)
				pref.getEditor ().putString (KEY_URL, 
						WebReviewActivity.WKConfig.CURRENT_REVIEW_START).commit ();
		} else if (key.equals (KEY_LESSON_URL)) {
			s = getLessonURL (prefs);
			if (s.length () == 0)
				pref.getEditor ().putString (KEY_LESSON_URL, 
						WebReviewActivity.WKConfig.CURRENT_LESSON_START).commit ();
		} else if (key.equals (KEY_PREF_EXPORT_DEST))
			runExportDestHooks (prefs);
		else if (key.equals (KEY_PREF_EXPORT_FILE)) {
			s = getExportFile (prefs);			
			if (s.length () == 0)
				setString (pref, KEY_PREF_EXPORT_FILE, DataExporter.getDefaultExportFile (this));
		} else if (key.equals (KEY_PREF_REVIEW_IMPROVEMENTS))
			runReviewImprovementsHooks (prefs);
		else if (key.equals (KEY_PREF_EXTERNAL_FRAME_PLACER))
			runExternalFramePlacerHooks (prefs);
		 
		updateConfig (prefs);
	}
	
	private void setString (Preference pref, String key, String value)
	{
		EditTextPreference etp;
		
		pref.getEditor ().putString (key, value).commit ();
		
		etp = (EditTextPreference) pref;
		etp.setText (value);
	}
	
	@SuppressWarnings ("deprecation")
	private void enableNotificationCheck (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_ENABLED);
		pref.setEnabled (credentialsAreValid (prefs));
		
		runEnabledHooks (prefs);
	}
	
	@SuppressWarnings ("deprecation")
	private void runEnabledHooks (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_LESSONS_ENABLED);
		pref.setEnabled (getEnabled (prefs));		
	}
	
	@SuppressWarnings ("deprecation")
	private void runIntegratedBrowserHook (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_REVIEW_IMPROVEMENTS);
		pref.setEnabled (getUseIntegratedBrowser (prefs));

		pref = findPreference (KEY_PREF_SHOW_MUTE);
		pref.setEnabled (getUseIntegratedBrowser (prefs));
		
		pref = findPreference (KEY_PREF_REVIEW_ORDER);
		pref.setEnabled (getUseIntegratedBrowser (prefs));

		pref = findPreference (KEY_PREF_LESSON_ORDER);
		pref.setEnabled (getUseIntegratedBrowser (prefs));

		pref = findPreference (KEY_PREF_ERROR_POPUP);
		pref.setEnabled (getUseIntegratedBrowser (prefs));

		runReviewImprovementsHooks (prefs);
	}

	@SuppressWarnings ("deprecation")
	private void runReviewImprovementsHooks (SharedPreferences prefs)
	{
		Preference pref;
		boolean lime;
		
		lime =	getUseIntegratedBrowser (prefs) &&
				getReviewsKeyboard (prefs) == Keyboard.LOCAL_IME;
		
		pref = findPreference (KEY_PREF_DISABLE_SUGGESTIONS);
		pref.setEnabled (lime);
		
		pref = findPreference (KEY_PREF_ERROR_POPUP);
		pref.setEnabled (lime);
		
		pref = findPreference (KEY_PREF_IGNORE_BUTTON);
		pref.setEnabled (lime);
		
		pref = findPreference (KEY_PREF_REVIEW_ORDER);
		pref.setEnabled (lime);
		
		pref = findPreference (KEY_PREF_LESSON_ORDER);
		pref.setEnabled (lime);

		pref = findPreference (KEY_PREF_WANIKANI_IMPROVE);
		pref.setEnabled (lime);									
	}

	@SuppressWarnings ("deprecation")
	private void runExportDestHooks (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_EXPORT_FILE);
		pref.setEnabled (getExportDestination (prefs) == DataExporter.Destination.FILESYSTEM);
	}

	@SuppressWarnings ("deprecation")
	private void runExternalFramePlacerHooks (SharedPreferences prefs)
	{
		Preference pref;
		
		pref = findPreference (KEY_PREF_EXTERNAL_FRAME_PLACER_DICT);
		pref.setEnabled (getExternalFramePlacer (prefs));
	}

	public static SharedPreferences prefs (Context ctxt)
	{
		return PreferenceManager.getDefaultSharedPreferences (ctxt.getApplicationContext ());
	}
	
	public static String diagnose (Context ctxt, Resources res)
	{
			SharedPreferences prefs;
			StringBuffer sb;
			String key, fmt;
			int delta;
			
			prefs = prefs (ctxt);
			sb = new StringBuffer (res.getString (R.string.status_msg_unauthorized));
			key = prefs.getString (KEY_PREF_USERKEY, "");
			delta = key.length () - EXPECTED_KEYLEN;
			if (delta == 0)
				/* Say nothing more */;
			else if (key.length () == 0) {
				fmt = res.getString (R.string.status_msg_unauthorized_empty);
				sb.append ('\n').append (String.format (fmt, delta));
			} else if (delta > 0) {
				fmt = res.getString (R.string.status_msg_unauthorized_too_long);
				sb.append ('\n').append (String.format (fmt, delta));
			} else {
				fmt = res.getString (R.string.status_msg_unauthorized_too_short);
				sb.append ('\n').append (String.format (fmt, -delta));
			}
			
			return sb.toString ();
	}
	
	public static UserLogin getLogin (Context ctxt)
	{
		return getLogin (prefs (ctxt));
	}
	
	private static UserLogin getLogin (SharedPreferences prefs)
	{
		return new UserLogin (prefs.getString (KEY_PREF_USERKEY, ""));		
	}

	public static boolean getEnabled (Context ctxt)
	{
		return getEnabled (prefs (ctxt));
	}
	
	private static boolean getEnabled (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_ENABLED, true);	
	}
	
	public static boolean getLessonsEnabled (Context ctxt)
	{
		return getLessonsEnabled (prefs (ctxt));
	}
	
	private static boolean getLessonsEnabled (SharedPreferences prefs)
	{
		return getEnabled (prefs) && prefs.getBoolean (KEY_PREF_LESSONS_ENABLED, false);	
	}

	public static boolean getUseIntegratedBrowser (Context ctxt)
	{
		return getUseIntegratedBrowser (prefs (ctxt));
	}
	
	private static boolean getUseIntegratedBrowser (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_USE_INTEGRATED_BROWSER, true);	
	}

	public static final boolean credentialsAreValid (Context ctxt)
	{
		return credentialsAreValid (prefs (ctxt));
	}
	
	private static boolean credentialsAreValid (SharedPreferences prefs)
	{
		return prefs.getString (KEY_PREF_USERKEY, "").length () > 0;
	}
	
	public static int getRefreshTimeout (Context ctxt)
	{
		return getInt (prefs (ctxt), KEY_PREF_REFRESH_TIMEOUT, DEFAULT_REFRESH_TIMEOUT);
	}
	
	public static int getReviewThreshold (Context ctxt)
	{
		return getReviewThreshold (prefs (ctxt));
	}
	
	public static int getReviewThreshold (SharedPreferences prefs)
	{
		return getInt (prefs, KEY_PREF_NOT_THRESHOLD, 1);
	}

	public static Keyboard getReviewsKeyboard (Context ctxt)
	{
		return getReviewsKeyboard (prefs (ctxt));
	}

	private static Keyboard getReviewsKeyboard (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_REVIEW_IMPROVEMENTS, true) ?
				Keyboard.LOCAL_IME : Keyboard.NATIVE;
	}
	
	public static boolean getIgnoreButton (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_IGNORE_BUTTON, true);
	}
	
	public static boolean getWaniKaniImprove (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_WANIKANI_IMPROVE, false);
	}
	
	public static boolean getReviewOrder (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_REVIEW_ORDER, false);
	}
	
	public static boolean getLessonOrder (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_LESSON_ORDER, false);
	}

	public static boolean getDisableSuggestions (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_DISABLE_SUGGESTIONS, true);
	}

	public static boolean getExternalFramePlacer (Context ctxt)
	{
		return getExternalFramePlacer (prefs (ctxt));
	}
	
	public static boolean getExternalFramePlacer (SharedPreferences prefs)
	{
		return prefs.getBoolean (KEY_PREF_EXTERNAL_FRAME_PLACER, false);
	}

	public static ExternalFramePlacer.Dictionary getExternalFramePlacerDictionary (Context ctxt)
	{
		ExternalFramePlacer.Dictionary dict;
		String tag;
		
		tag = prefs (ctxt).getString (KEY_PREF_EXTERNAL_FRAME_PLACER_DICT, 
									  ExternalFramePlacer.Dictionary.JISHO.name ());

		dict = ExternalFramePlacer.Dictionary.valueOf (tag);
		if (dict == null)
			dict = ExternalFramePlacer.Dictionary.JISHO;
		
		return dict;
	}	

	public static boolean getErrorPopup (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_ERROR_POPUP, false);
	}

	public static boolean getShowMute (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_SHOW_MUTE, true);
	}
	
	public static boolean get42plus (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_PREF_42PLUS, false);
	}
	
	public static boolean getMute (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_MUTE, false);
	}
	
	public static boolean getTipAck (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_TIP_ACK, false);
	}
	
	public static boolean setTipAck (Context ctxt, boolean value)
	{
		return prefs (ctxt).edit ().putBoolean (KEY_TIP_ACK, value).commit ();
	}
	
	public static boolean getIgnoreButtonMessage (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_IGNORE_BUTTON_MESSAGE_ACK, false);
	}
	
	public static boolean setIgnoreButtonMessage (Context ctxt, boolean value)
	{
		return prefs (ctxt).edit ().putBoolean (KEY_IGNORE_BUTTON_MESSAGE_ACK, value).commit ();
	}

	public static boolean getCustomIMEMessage (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_CUSTOM_IME, false);
	}
	
	public static boolean setCustomIMEMessage (Context ctxt, boolean value)
	{
		return prefs (ctxt).edit ().putBoolean (KEY_CUSTOM_IME, value).commit ();
	}

	public static boolean getLockScreen (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_LOCK_SCREEN, true);
	}

	public static boolean getFullscreen (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_FULLSCREEN, false);
	}

	public static boolean getLeakKludge (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_LEAK_KLUDGE, true);
	}
	
	public static boolean getTimerReaper (Context ctxt)
	{
		return prefs (ctxt).getBoolean (KEY_TIMER_REAPER, true);
	}

	public static String getURL (Context ctxt)
	{
		return getURL (prefs (ctxt));
	}
	
	public static Layout getLayout (Context ctxt)
	{
		int i;
		
		try { 
			i = Integer.parseInt (prefs (ctxt).getString (KEY_LAYOUT, "0"));
		} catch (NumberFormatException e) {
			i = 0;
		}
			
		switch (i) {
		case 0:
			return Layout.AUTO;
			
		case 1:
			return Layout.SMALL;
			
		case 2:
			return Layout.LARGE;
		}
		
		return Layout.AUTO;
	}
	
	public static DataExporter.Destination getExportDestination (Context ctxt)
	{
		return getExportDestination (prefs (ctxt));
	}
	
	public static DataExporter.Destination getExportDestination (SharedPreferences prefs)
	{
		int i;
		
		try { 
			i = Integer.parseInt (prefs.getString (KEY_PREF_EXPORT_DEST, "0"));
		} catch (NumberFormatException e) {
			i = 0;
		}
			
		switch (i) {
		case 0:
			return DataExporter.Destination.BLUETOOTH;
			
		case 1:
			return DataExporter.Destination.CHOOSE;
			
		case 2:
			return DataExporter.Destination.FILESYSTEM;
		}
		
		return DataExporter.Destination.BLUETOOTH;
	}
	
	public static String getExportFile (Context ctxt)
	{
		return getExportFile (prefs (ctxt));
	}

	public static String getExportFile (SharedPreferences prefs)
	{
		return prefs.getString (KEY_PREF_EXPORT_FILE, "");
	}

	private static String getURL (SharedPreferences prefs)
	{
		String s;
		int version;
		
		s = prefs.getString (KEY_URL, WebReviewActivity.WKConfig.CURRENT_REVIEW_START);
		version = prefs.getInt (KEY_URL_VERSION, 0);
		if (version < 2)
			setURL (prefs, s = WebReviewActivity.WKConfig.CURRENT_REVIEW_START, 2);
		
		return s;
	}
	
	public static String getLessonURL (Context ctxt)
	{
		return getLessonURL (prefs (ctxt));
	}

	private static String getLessonURL (SharedPreferences prefs)
	{
		return prefs.getString (KEY_LESSON_URL, WebReviewActivity.WKConfig.CURRENT_LESSON_START);
	}

	private static void setURL (SharedPreferences prefs, String url, int version)
	{
		prefs.edit ().putString (KEY_URL, url).putInt (KEY_URL_VERSION, version).commit ();
	}

	public static boolean toggleMute (Context ctxt)
	{
		SharedPreferences prefs;
		boolean v;

		prefs = prefs (ctxt);
		v = !getMute (ctxt);
		prefs.edit ().putBoolean(KEY_MUTE, v).commit ();
		
		return v;
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
		boolean lenabled, lLessonsEnabled;
		int lthreshold;
		Intent i;
				
		llogin = getLogin (prefs);
		lenabled = getEnabled (prefs);
		lenabled &= findPreference (KEY_PREF_ENABLED).isEnabled ();
		
		lLessonsEnabled = getLessonsEnabled (prefs);
		lLessonsEnabled &= findPreference (KEY_PREF_LESSONS_ENABLED).isEnabled ();
		
		lthreshold = getReviewThreshold (prefs);
		
		lbm = LocalBroadcastManager.getInstance (this);
		if (!llogin.equals (login)) {
			i = new Intent (ACT_CREDENTIALS);
			i.putExtra (E_USERKEY, llogin.userkey);
			i.putExtra (E_ENABLED, lenabled);
			login = llogin;
			lbm.sendBroadcast (i);
		} else if (lenabled != enabled || 
				   lLessonsEnabled != lessonsEnabled ||
				   lthreshold != threshold) {
			i = new Intent (ACT_NOTIFY);
			i.putExtra (E_ENABLED, lenabled);
			enabled = lenabled;
			lessonsEnabled = lLessonsEnabled;
			threshold = lthreshold;
			lbm.sendBroadcast (i);
		} 

		if (inited) {
			i = new Intent (ACT_CHANGED);
			sendBroadcast (i);
		}
	}
	
	public static Typeface getJapaneseFont (Context ctxt)
	{
		try {
			return Typeface.createFromFile (JAPANESE_TYPEFACE_FONT);
		} catch (Exception e) {
			/* empty */
		}
		
		try {
			if (ctxt != null)
				return Typeface.createFromAsset (ctxt.getAssets (), "MTLmr3m.ttf");
		} catch (Exception e) {
			/* empty */
		}
		
		return null;
	}
}


