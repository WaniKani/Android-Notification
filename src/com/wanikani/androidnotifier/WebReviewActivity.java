package com.wanikani.androidnotifier;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Xml.Encoding;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

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
 * This activity allows the user to perform its reviews through an integrated
 * browser. The only reason we need this (instead of just spawning an external
 * browser) is that we also display a minimal keyboard, that interacts with WK scripts
 * to compose kanas. Ordinarily, in fact, Android keyboards do not behave correctly.
 * <p>
 * The keyboard is displayed only when needed, so we need to check whether the
 * page contains a <code>user_response</code> text box, and it is enabled.
 * In addition, to submit the form, we simulate a click on the <code>option-submit</code>
 * button. Since the keyboard hides the standard controls (in particular the
 * info ("did you know...") balloons), we hide the keyboard when the user enters 
 * his/her response. 
 * <p>
 * To accomplish this, we register a JavascriptObject (<code>wknKeyboard</code>) and inject
 * a javascript to check how the page looks like. If the keyboard needs to be shown,
 * it calls its <code>show</code> (vs. <code>hide</code>) method.
 * The JavascriptObject is implemented by @link WebReviewActivity.WKNKeyboard.
 */
public class WebReviewActivity extends Activity {
	
	/**
	 * This class is barely a container of all the strings that should match with the
	 * WaniKani portal. Hopefully none of these will ever be changed, but in case
	 * it does, here is where to look for.
	 */
	public static class WKConfig {
		
		/** Review start page. Old version. Kept just to clear old configuration entries */
		static final String OBSOLETE_REVIEW_START = "http://www.wanikani.com/review/session/start";

		/** New review start page. This is the start page when client side reviews will be deployed */
		static final String CURRENT_REVIEW_START = "http://www.wanikani.com/review";

		/** Review start page. Of course must be inside of @link {@link #REVIEW_SPACE} */
		static final String LESSON_START = "http://www.wanikani.com/lesson";

		/** HTML id of the textbox the user types its answer in (reviews, client-side) */
		static final String ANSWER_BOX = "user-response";

		/** HTML id of the textbox the user types its answer in (lessons) */
		static final String LESSON_ANSWER_BOX_JP = "translit";
		
		/** HTML id of the textbox the user types its answer in (lessons) */
		static final String LESSON_ANSWER_BOX_EN = "lesson_user_response";

		/** HTML id of the submit button */
		static final String SUBMIT_BUTTON = "option-submit";

		/** HTML id of the lessons review form */
		static final String LESSONS_REVIEW_FORM = "new_lesson";
		
		/** Any object on the lesson pages */
		static final String LESSONS_OBJ = "nav-lesson";
		
		/** Reviews div */
		static final String REVIEWS_DIV = "reviews";		
	};
	
	/**
	 * The listener that receives events from the mute buttons.
	 */
	private class MuteListener implements View.OnClickListener {
		
		@Override
		public void onClick (View w)
		{
			SharedPreferences prefs;
			
			prefs = PreferenceManager.getDefaultSharedPreferences (WebReviewActivity.this);
			setMute (SettingsActivity.toggleMute (prefs)); 
		}
	}

	/**
	 * Web view controller. This class is used by @link WebView to tell whether
	 * a link should be opened inside of it, or an external browser needs to be invoked.
	 * Currently, I will let all the pages inside the <code>/review</code> namespace
	 * to be opened here. Theoretically, it could even be stricter, and use
	 * <code>/review/session</code>, but that would be prevent the final summary
	 * from being shown. That page is useful, albeit not as integrated with the app as
	 * the other pages.
	 */
	private class WebViewClientImpl extends WebViewClient {
				
		/**
		 * Called to check whether a link should be opened in the view or not.
		 * We also display the progress bar.
		 * 	@param view the web view
		 *  @url the URL to be opened
		 */
	    @Override
	    public boolean shouldOverrideUrlLoading (WebView view, String url) 
	    {
	        view.loadUrl (url);

	        return true;
	    }
		
	    /**
	     * Called when something bad happens while accessing the resource.
	     * Show the splash screen and give some explanation (based on the <code>description</code>
	     * string).
	     * 	@param view the web view
	     *  @param errorCode HTTP error code
	     *  @param description an error description
	     *  @param failingUrl error
	     */
	    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl)
	    {
	    	String s;
	    	
	    	s = getResources ().getString (R.string.fmt_web_review_error, description);
	    	splashScreen (s);
	    	bar.setVisibility (View.GONE);
	    }

		@Override  
	    public void onPageStarted (WebView view, String url, Bitmap favicon)  
	    {  
	        bar.setVisibility (View.VISIBLE);
		}
	
		/**
	     * Called when a page finishes to be loaded. We hide the progress bar
	     * and run the initialization javascript that shows the keyboard, if needed.
	     * In addition, if this is the initial page, we check whether Viet has
	     * deployed the client-side review system
	     */
		@Override  
	    public void onPageFinished (WebView view, String url)  
	    {  
			bar.setVisibility (View.GONE);

			if (url.startsWith ("http"))
				js (JS_INIT_KBD);			
	    }
	}
	
	/**
	 * An additional webclient, that receives a few callbacks that a simple 
	 * {@link WebChromeClient} does not intecept. 
	 */
	private class WebChromeClientImpl extends WebChromeClient {
	
		/**
		 * Called as the download progresses. We update the progress bar.
		 * @param view the web view
		 * @param progress progress percentage
		 */
		@Override	
		public void onProgressChanged (WebView view, int progress)
		{
			bar.setProgress (progress);
		}		
	};

	/**
	 * A small job that hides, shows or iconizes the keyboard. We need to implement this
	 * here because {@link WebReviewActibity.WKNKeyboard} gets called from a
	 * javascript thread, which is not necessarily an UI thread.
	 * The constructor simply calls <code>runOnUIThread</code> to make sure
	 * we hide/show the views from the correct context.
	 */
	private class ShowHideKeyboard implements Runnable {
		
		/** New state to enter */
		KeyboardStatus kbstatus;
		
		/**
		 * Constructor. It also takes care to schedule the invokation
		 * on the UI thread, so all you have to do is just to create an
		 * instance of this object
		 * @param kbstatus the new keyboard status to enter
		 */
		ShowHideKeyboard (KeyboardStatus kbstatus)
		{
			this.kbstatus = kbstatus;
			
			runOnUiThread (this);
		}
		
		/**
		 * Hides/shows the keyboard. Invoked by the UI thread.
		 * As a side effect, we update the {@link WebReviewActivity#flushCaches}
		 * bit.
		 */
		public void run ()
		{
			switch (kbstatus) {
			case VISIBLE:
				reviewsSession ();
				show ();
				break;

			case VISIBLE_LESSONS:
				reviewsSession ();
				showLessons (); 
				break;
				
			case HIDDEN:
				hide ();
				break;
				
			case ICONIZED:
			case ICONIZED_LESSONS:
				reviewsSession ();
				iconize (kbstatus);
				break;
			}
		}
		
		private void reviewsSession ()
		{
			flushCaches = true;
			CookieSyncManager.getInstance ().sync ();
		}
		
	}
	
	/**
	 * This class implements the <code>wknKeyboard</code> javascript object.
	 * It implements the @link {@link #show} and {@link #hide} methods. 
	 */
	private class WKNKeyboard {
		
		/**
		 * Called by javascript when the keyboard should be shown.
		 */
		@JavascriptInterface
		public void show ()
		{
			new ShowHideKeyboard (KeyboardStatus.VISIBLE);
		}

		/**
		 * Called by javascript when the keyboard should be shown, using
		 * lessons layout.
		 */
		@JavascriptInterface
		public void showLessons ()
		{
			new ShowHideKeyboard (KeyboardStatus.VISIBLE_LESSONS);
		}

		/**
		 * Called by javascript when the keyboard should be hidden.
		 */
		@JavascriptInterface
		public void hide ()
		{
			new ShowHideKeyboard (KeyboardStatus.HIDDEN);
		}

		/**
		 * Called by javascript when the keyboard should be iconized.
		 */
		@JavascriptInterface
		public void iconize ()
		{
			new ShowHideKeyboard (KeyboardStatus.ICONIZED);
		}

		/**
		 * Called by javascript when the keyboard should be iconized (lessons mode).
		 */
		@JavascriptInterface
		public void iconizeLessons ()
		{
			new ShowHideKeyboard (KeyboardStatus.ICONIZED_LESSONS);
		}	
	}
	
	/**
	 * A button listener that handles all the meta keys on the keyboard.
	 * Actually some buttons have nothing really special (like space, or apostrophe): 
	 * the real definition of meta key, here, is a key that does not change
	 * when the meta (123) button is pressed. 
	 */
	private class MetaListener implements View.OnClickListener {
	
		/**
		 * Called when one of the keys is pressed.
		 * Looks up in the @link {@link WebReviewActivity#meta_table} array
		 * and inserts the appropriate key code.
		 * 	@param v the keyboard view
		 */
		@Override
		public void onClick (View v)
		{
			int i, id;
		
			id = v.getId ();
		
			for (i = 0; i < meta_table.length; i++) {
				if (id == meta_table [i]) {
					insert (meta_codes [i]);
					break;
				}
			}
		}
	};

	/**
	 * A listener that handles all the ordinary keys.
	 */
	private class KeyListener implements View.OnClickListener {
		

		/**
		 * Called when one of the keys is pressed.
		 * Looks up in the @link {@link WebReviewActivity#key_table} array
		 * and inserts the appropriate key code. This is done by looking up
		 * the appropriate character in the {@link WebReviewActivity#keyboard}
		 * string, which is an ASCII string. Therefore, an additional translation
		 * (ASCII to Keycodes) is performed, through 
		 * {@link WebReviewActivity#charToKeyCode(char)}.
		 * 	@param v the keyboard view
		 */
		@Override
		public void onClick (View v)
		{
			int i, id;
			
			id = v.getId ();
			
			for (i = 0; i < key_table.length; i++) {
				if (id == key_table [i]) {
					if (i < keyboard.length ())
						insert (charToKeyCode (keyboard.charAt (i)));
					break;
				}
			}
		}
	};
		
	/**
	 * The listener attached to the embedded keyboard tip message.
	 * When the user taps the ok button, we write on the property
	 * that it has been acknowleged, so it won't show up any more. 
	 */
	private class OkListener implements DialogInterface.OnClickListener {
		
		@Override
		public void onClick (DialogInterface ifc, int which)
		{
			SharedPreferences prefs;
			
			prefs = PreferenceManager.getDefaultSharedPreferences (WebReviewActivity.this);
			SettingsActivity.setTipAck (prefs, true);
		}		
	}
	

	/**
	 * Our implementation of a menu listener. We listen for configuration changes. 
	 */
	private class MenuListener extends MenuHandler.Listener {
		
		/**
		 * When preferences change, we update the layout. 
		 */
		public void settingsChanged ()
		{
			updateLayout (PreferenceManager.getDefaultSharedPreferences (WebReviewActivity.this));
		}
		
		/**
		 * The dashboard listener exits the activity
		 */
		public void dashboard ()
		{
			finish ();
		}
	}
	
	/**
	 * Keyboard visiblity status.
	 */
	enum KeyboardStatus {
		/** Keyboard visible, all keys visible */
		VISIBLE, 
		
		/** Keyboard visible, all keys but ENTER visible */
		VISIBLE_LESSONS, 

		/** Keyboard invisible */
		HIDDEN, 
		
		/** Keyboard visible, just "Show" and "Enter" keys are visible */ 
		ICONIZED,

		/** Keyboard visible, just "Show" and "Enter" keys are visible, in lessons mode */ 
		ICONIZED_LESSONS
	};

	/** The web view, where the web contents are rendered */
	FocusWebView wv;
	
	/** The view containing a splash screen. Visible when we want to display 
	 * some message to the user */ 
	View splashView;
	
	/**
	 * The view contaning the ordinary content.
	 */
	View contentView;
	
	/** A textview in the splash screen, where we can display some message */
	TextView msgw;
	
	/** The web progress bar */
	ProgressBar bar;
			
	/** The local prefix of this class */
	private static final String PREFIX = "com.wanikani.androidnotifier.WebReviewActivity.";
	
	/** Open action, invoked to start this action */
	public static final String OPEN_ACTION = PREFIX + "OPEN";
	
	/** Flush caches bundle key */
	private static final String KEY_FLUSH_CACHES = PREFIX + "flushCaches";
	
	/** Javascript to be called each time an HTML page is loaded. It hides or shows the keyboard */
	private static final String JS_INIT_KBD = 
			"var textbox, lessobj, ltextbox, reviews;" +
			"textbox = document.getElementById (\"" + WKConfig.ANSWER_BOX + "\"); " +
			"lessobj = document.getElementById (\"" + WKConfig.LESSONS_OBJ + "\"); " +
			"ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_JP + "\"); " +
			"reviews = document.getElementById (\"" + WKConfig.REVIEWS_DIV + "\");" +
			"if (ltextbox == null) {" +
			"   ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_EN + "\"); " +
			"}" +
			"if (textbox != null && !textbox.disabled) {" +
			"   wknKeyboard.show (); " +
			"} else if (ltextbox != null) {" +
			"   wknKeyboard.showLessons ();" +
			"} else if (lessobj != null) {" +
			"   wknKeyboard.iconizeLessons ();" +
			"} else {" +
			"	wknKeyboard.hide ();" +			
			"}" +
			"if (reviews != null) {" +
			"   reviews.style.overflow = \"visible\";" +
			"} ";
	
	private static final String JS_FOCUS = 
			"var ltextbox;" +
			"ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_JP + "\"); " +
			"if (ltextbox == null) {" +
			"   ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_EN + "\"); " +
			"}" +
			"if (ltextbox != null) {" +
			"    ltextbox.focus (); " +
			"}";
	
	/** Javascript to be invoked to simulate a click on the submit (heart-shaped) button.
	 *  It also handles keyboard show/iconize logic. If the textbox is enabled, then this
	 *  is an answer, so we iconize the keyboard. Otherwise we are entering the new question,
	 *  so we need to show it  */
	private static final String JS_ENTER =
			"var textbox, ltextbox, form, submit;" +
			"textbox = document.getElementById (\"" + WKConfig.ANSWER_BOX + "\"); " +
		    "form = document.getElementById (\"new_lesson\"); " +
			"ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_JP + "\"); " +
			"if (ltextbox == null) {" +
			"   ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_EN + "\"); " +
			"}" +
			"if (textbox != null && textbox.value.length > 0) {" +
		    "   buttons = document.getElementsByTagName('button'); " +
		    "   buttons [0].click (); " +
			"}";
	
	/** The default keyboard. This is the sequence of keys from left to right, from top to bottom */
	private static final String KB_LATIN = "qwertyuiopasdfghjkl'zxcvbnm";

	/** The alt keyboard (loaded when the user presses the '123' button). 
	 *  This is the sequence of keys from left to right, from top to bottom */
	private static final String KB_ALT = "1234567890-.";
	
	/** A table that maps key position (left to right, top to bottom) to button IDs for the
	 *  ordinary keys */
	private static final int key_table [] = new int [] {
		R.id.kb_0, R.id.kb_1,  R.id.kb_2, R.id.kb_3, R.id.kb_4,
		R.id.kb_5, R.id.kb_6,  R.id.kb_7, R.id.kb_8, R.id.kb_9,
		R.id.kb_10, R.id.kb_11,  R.id.kb_12, R.id.kb_13, R.id.kb_14,
		R.id.kb_15, R.id.kb_16,  R.id.kb_17, R.id.kb_18, R.id.kb_19,
		R.id.kb_20, R.id.kb_21,  R.id.kb_22, R.id.kb_23, R.id.kb_24,
		R.id.kb_25, R.id.kb_26
	};
	
	/** A table that maps key positions (left to right, top to bottom) to button IDs for the
	 *  meta keys */
	private static final int meta_table [] = new int [] {
		R.id.kb_backspace, R.id.kb_meta, R.id.kb_space, R.id.kb_enter, R.id.kb_hide		
	};
	
	/** A table that maps key positions (left to right, top to bottom) to keycodes for the meta
	 *  keys */
	private static final int meta_codes [] = new int [] {
		KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_NUM,
		KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER,
		KeyEvent.KEYCODE_DPAD_DOWN
	};
	
	/** The current keyboard. It may be set to either {@link #KB_LATIN} or {@link #KB_ALT} */
	private String keyboard;
	
	/** The current keyboard status */
	protected KeyboardStatus kbstatus;
	
	/** The "show enter key" setting */
	protected boolean showEnterKey; 
	
	/** The mute button to be shown when the embedded keyboard is disabled */
	private ImageButton muteH;
	
	/** The mute button to be shown when the embedded keyboard is enabled */
	private ImageButton mute;
	
	/** Height of a tall keyboard key */
	private int tallKey;
	
	/** Height of a short keyboard key */
	private int shortKey;

	/** The mute drawable */
	private Drawable muteDrawable;
	
	/** The sound drawable */
	private Drawable notMutedDrawable;
	
	/** The menu handler */
	private MenuHandler mh;
	
	/** Set if visible */
	private boolean visible;
	
	/** Set if we have reviewed or had some lessons, so caches should be flushed */
	private boolean flushCaches;
	
	/**
	 * Called when the action is initially displayed. It initializes the objects
	 * and starts loading the review page.
	 * 	@param bundle the saved bundle
	 */
	@Override
	public void onCreate (Bundle bundle) 
	{
		super.onCreate (bundle);

		SharedPreferences prefs;
		Resources res;
		
		CookieSyncManager.createInstance (this);
		
		mh = new MenuHandler (this, new MenuListener ());
		
		setContentView (R.layout.web_review);
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		showEnterKey = SettingsActivity.getEnter (prefs);

		res = getResources ();
		muteDrawable = res.getDrawable(R.drawable.ic_mute);
		notMutedDrawable = res.getDrawable(R.drawable.ic_not_muted);

		initKeyboard (prefs);
		
		bar = (ProgressBar) findViewById (R.id.pb_reviews);
				
		/* First of all get references to views we'll need in the near future */
		splashView = findViewById (R.id.wv_splash);
		contentView = findViewById (R.id.wv_content);
		msgw = (TextView) findViewById (R.id.tv_message);
		wv = (FocusWebView) findViewById (R.id.wv_reviews);

		wv.getSettings ().setJavaScriptEnabled (true);
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically (true);
		wv.getSettings ().setSupportMultipleWindows (true);
		wv.getSettings ().setUseWideViewPort (true);
		wv.getSettings ().setDatabaseEnabled (true);
		wv.getSettings ().setDomStorageEnabled (true);
		wv.getSettings ().setDatabasePath (getFilesDir ().getPath () + "/wv");
		wv.addJavascriptInterface (new WKNKeyboard (), "wknKeyboard");
		wv.setScrollBarStyle (ScrollView.SCROLLBARS_OUTSIDE_OVERLAY);
		wv.setWebViewClient (new WebViewClientImpl ());
		wv.setWebChromeClient (new WebChromeClientImpl ());		
		
		wv.loadUrl (getIntent ().getData ().toString ());
	}
	
	@Override
	protected void onResume ()
	{
		SharedPreferences prefs;
		
		super.onResume ();

		visible = true;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		if (SettingsActivity.getMute (prefs) &&
			SettingsActivity.getShowMute (prefs))
			setMute (true);

		wv.acquire ();
	}
	
	@Override
	protected void onSaveInstanceState (Bundle bundle)
	{
		bundle.putBoolean (KEY_FLUSH_CACHES, flushCaches);
	}
	
	@Override 
	protected void onRestoreInstanceState (Bundle bundle)
	{
		if (bundle != null && bundle.containsKey (KEY_FLUSH_CACHES))
			flushCaches = bundle.getBoolean (KEY_FLUSH_CACHES);
	}
	
	@Override
	protected void onPause ()
	{
		LocalBroadcastManager lbm;
		Intent intent;
		SharedPreferences prefs;

		visible = false;

		super.onPause ();
		lbm = LocalBroadcastManager.getInstance (this);
		intent = new Intent (MainActivity.ACTION_REFRESH);
		intent.putExtra (MainActivity.E_FLUSH_CACHES, flushCaches);
		lbm.sendBroadcast (intent);

		/* Alert the notification service too (the main action may not be active) */
		intent = new Intent (this, NotificationService.class);
		intent.setAction (NotificationService.ACTION_NEW_DATA);
		startService (intent);
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);		
		if (SettingsActivity.getMute (prefs))
			setMute (false);
		
		wv.release ();
	}
	
	@Override
	public void onBackPressed ()
	{
		// We don't use it anymore because it's too buggy
		/*
		if (wv.canGoBack ())
			wv.goBack ();
		else*/
		
		/* We do this strange thing to stop a memory leakage when the last
		 * displayed page is the review summary */
		wv.loadData ("", "text/plain", Encoding.UTF_8.toString ());
		
		super.onBackPressed ();
	}
	
	/**
	 * Associates the menu description to the menu key (or action bar).
	 * The XML description is <code>review.xml</code>
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate (R.menu.review, menu);
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
	 * Sets up listener and bindings of the initial keyboard.
	 */
	protected void initKeyboard (SharedPreferences prefs)
	{
		View.OnClickListener klist, mlist, mutel;
		LayoutParams lp;
		View key;
		int i;

		muteH = (ImageButton) findViewById (R.id.kb_mute_h);
		mute = (ImageButton) findViewById (R.id.kb_mute);
		
		mutel = new MuteListener ();
		muteH.setOnClickListener (mutel);
		mute.setOnClickListener (mutel);
		setMute (false);	// resume will take care of that
		showMuteButtons (mute, true);
		
		kbstatus = KeyboardStatus.HIDDEN;
		loadKeyboard (KB_LATIN);		

		klist = new KeyListener ();
		mlist = new MetaListener ();

		key = findViewById (key_table [0]);
		lp = key.getLayoutParams ();
		shortKey = lp.height;
		tallKey = shortKey + shortKey / 2;

		for (i = 0; i < key_table.length; i++) {
			key = findViewById (key_table [i]);
			key.setOnClickListener (klist);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = findViewById (meta_table [i]);
			key.setOnClickListener (mlist);
		}
		
		updateLayout (prefs);
	}
	
	/**
	 * Updates the layout, according to the current preferences
	 * @param prefs the preferences
	 */
	private void updateLayout (SharedPreferences prefs)
	{
		LayoutParams lp;
		int height;
		View key;
		int i;

		height = SettingsActivity.getLargeKeyboard (prefs) ? tallKey : shortKey;
		for (i = 0; i < key_table.length; i++) {
			key = findViewById (key_table [i]);
			lp = key.getLayoutParams ();
			lp.height = height;
			key.setLayoutParams(lp);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = findViewById (meta_table [i]);
			lp = key.getLayoutParams ();
			lp.height = height;
			key.setLayoutParams(lp);
		}					
		
		lp = mute.getLayoutParams ();
		lp.height = height;
		mute.setLayoutParams(lp);
				
		showEnterKey = SettingsActivity.getEnter (prefs);
		if (kbstatus == KeyboardStatus.VISIBLE)
			show ();
	}
	
	private void showMuteButtons (View mute, boolean show)
	{
		SharedPreferences prefs;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		show &= SettingsActivity.getShowMute (prefs);
		
		mute.setVisibility (show ? View.VISIBLE : View.GONE);
		if (!show)
			setMute (false);
	}
	
	private void setMute (boolean m)
	{
		AudioManager am;
		Drawable d;
		
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	    
		am.setStreamMute (AudioManager.STREAM_MUSIC, m);
		d = m ? muteDrawable : notMutedDrawable;
	    muteH.setImageDrawable (d);
		mute.setImageDrawable (d);
	}
	
	/**
	 * Changes the bindings of the ordinary (non meta) keys. Meta keys never change.
	 * 	@param kbd the sequence of keys, from left to right, from top to bottom  
	 */
	protected void loadKeyboard (String kbd)
	{
		Button key;
		int i;
		
		keyboard = kbd;
		for (i = 0; i < kbd.length(); i++) {
			key = (Button) findViewById (key_table [i]);
			key.setText ("" + kbd.charAt (i));
		}
		while (i < key_table.length) {
			key = (Button) findViewById (key_table [i]);
			key.setText ("");
			i++;
		}
	}
	
	/**
	 * Delivers a keycode to the answer text box. There are two notable exceptions:
	 * <ul>
	 * 	<li>The {@link KeyEvent.KEYCODE_NUM} key just switches the keys, so it does not
	 * 	interact with the web page
	 *  <li>The {@link KeyEvent.KEYCODE_ENTER} key simulates a click on the submit button
	 *  rather than delivering the key evento to the action box. In fact, this has no effect
	 *  on text boxes.
	 *  </ul> 
	 */
	public void insert (int keycode)
	{
		KeyEvent kdown, kup;
	
		if (keycode == KeyEvent.KEYCODE_NUM) {
			/* Num == meta -> if iconized, it means "Show"*/
			if (kbstatus == KeyboardStatus.ICONIZED)
				show ();
			else if (kbstatus == KeyboardStatus.ICONIZED_LESSONS)
				showLessons ();
			else
				loadKeyboard (keyboard == KB_ALT ? KB_LATIN : KB_ALT);
		} else if (keycode == KeyEvent.KEYCODE_ENTER)
			js (JS_ENTER);
		else if (keycode == KeyEvent.KEYCODE_DPAD_DOWN)
			iconize (KeyboardStatus.ICONIZED_LESSONS);
		else {
			if (kbstatus == KeyboardStatus.VISIBLE_LESSONS)
				js (JS_FOCUS);
			kdown = new KeyEvent (KeyEvent.ACTION_DOWN, keycode);
			wv.dispatchKeyEvent (kdown);				
			kup = new KeyEvent (KeyEvent.ACTION_UP, keycode);
			wv.dispatchKeyEvent (kup);
		}
	}
	
	/**
	 * A simple ASCII to Android Keycode translation function. It is meant to work
	 * on ordinary (non meta) keys only. 
	 * 	@param c the ASCII character
	 *  @return a KeyCode 
	 */
	protected int charToKeyCode (char c)
	{
		if (Character.isLetter (c))
			return KeyEvent.KEYCODE_A + (Character.toLowerCase (c) - 'a');
		else if (Character.isDigit (c))
			return KeyEvent.KEYCODE_0 + (c - '0');
		
		switch (c) {
		case ' ':
			return KeyEvent.KEYCODE_SPACE;
			
		case '\'':
			return KeyEvent.KEYCODE_APOSTROPHE;
			
		case '-':
			return KeyEvent.KEYCODE_MINUS;
			
		case '.':
			return KeyEvent.KEYCODE_PERIOD;			
		}
		
		return KeyEvent.KEYCODE_SPACE;
	}
	
	/**
	 * Executes a javascript on the web page.
	 * @param js the javascript statements. This method wraps it into a function
	 */
	protected void js (String s)
	{
       wv.loadUrl ("javascript:(function() { " + s + "})()");
	}
	
	/**
	 * Displays the splash screen, also providing a text message
	 * @param msg the text message to display
	 */
	protected void splashScreen (String msg)
	{
		msgw.setText (msg);
		contentView.setVisibility (View.GONE);
		splashView.setVisibility (View.VISIBLE);
	}
	
	/**
	 * Hides the keyboard
	 */
	protected void hide ()
	{
		View view;
		
		showMuteButtons (muteH, false);
		kbstatus = KeyboardStatus.HIDDEN;
		view = findViewById (R.id.keyboard);
		view.setVisibility (View.GONE);
		wv.enableFocus ();
	}
	 
	/**
	 * Shows the keyboard. This method does what's expected either when
	 * called after the keyboard has been hidden, and when it is simply
	 * iconized. To do this, in addition to changing its visibility,
	 * we show the keys. 
	 */
	protected void show ()
	{
		showCommon (KeyboardStatus.VISIBLE, showEnterKey);
	}

	/**
	 * Shows the keyboard, hiding the enter key, which is problematic
	 * on lessons. 
	 */
	protected void showLessons ()
	{
		showCommon (KeyboardStatus.VISIBLE_LESSONS, false);
	}
	
	protected void showCommon (KeyboardStatus kbstatus, boolean showEnter)
	{
		SharedPreferences prefs;
		boolean embedded;
		
		this.kbstatus = kbstatus;
		prefs = PreferenceManager.getDefaultSharedPreferences (this);
		embedded = kbstatus == KeyboardStatus.VISIBLE ?
				SettingsActivity.getShowReviewsKeyboard (prefs) :
				SettingsActivity.getShowLessonsKeyboard (prefs);
		if (embedded && kbstatus == KeyboardStatus.VISIBLE)
			showEmbeddedKeyboardMessage (prefs);
				
		if (embedded)
			showEmbedded (showEnter);
		else
			showNative ();
	}
	
	protected void showEmbeddedKeyboardMessage (SharedPreferences prefs)
	{
		AlertDialog.Builder builder;
		Dialog dialog;
					
		if (!visible || SettingsActivity.getTipAck (prefs))
			return;
		
		builder = new AlertDialog.Builder (this);
		builder.setTitle (R.string.kbd_message_title);
		builder.setMessage (R.string.kbd_message_text);
		builder.setPositiveButton (R.string.kbd_message_ok, new OkListener ());
		
		dialog = builder.create ();
		
		dialog.show ();		
	}
	
	private void showNative ()
	{
		InputMethodManager imm;
		View view;
		
		wv.enableFocus ();
		
		view = findViewById (R.id.keyboard);
		view.setVisibility (View.GONE);

		showMuteButtons (muteH, true);
		imm = (InputMethodManager) getSystemService (Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput (wv, InputMethodManager.SHOW_IMPLICIT);		
	}
	
	/**
	 * This is the code shared between {@link #show} and {@link #showLessons}.
	 * @param showEnter if true, the bottom right key is <code>Enter</code>, 
	 * 		otherwise <code>Hide</code>.
	 */
	private void showEmbedded (boolean showEnter)
	{
		View view, key;
		int i;
		
		wv.disableFocus ();

		showMuteButtons (muteH, false);
		for (i = 0; i < key_table.length; i++) {
			key = findViewById (key_table [i]);
			key.setVisibility (View.VISIBLE);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = findViewById (meta_table [i]);
			key.setVisibility (View.VISIBLE);
		}
		
		key = findViewById (R.id.kb_meta);
		((Button) key).setText (R.string.key_meta);

		view = findViewById (R.id.keyboard);
		view.setVisibility (View.VISIBLE);
		
		key = findViewById (R.id.kb_enter);
		key.setVisibility (showEnter ? View.VISIBLE : View.GONE);

		key = findViewById (R.id.kb_hide);
		key.setVisibility (showEnter ? View.GONE : View.VISIBLE);
	}

	/**
	 * Iconize the keyboard. This method hides all the keys except
	 * Enter and Meta (which is renamed to "Show").
	 * @param kbs {@link KeboardStatus#ICONIZED} or {@link KeboardStatus#ICONIZED_LESSONS}
	 */
	protected void iconize (KeyboardStatus kbs)
	{
		View view, key;
		int i;
		
		kbstatus = kbs;
		for (i = 0; i < key_table.length; i++) {
			key = findViewById (key_table [i]);
			key.setVisibility (View.GONE);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = findViewById (meta_table [i]);
			key.setVisibility (View.GONE);
		}
		
		key = findViewById (R.id.kb_enter);
		/* If in ICONIZED_LESSON status, hide enter key (it does not work :) */
		key.setVisibility (kbs == KeyboardStatus.ICONIZED ? View.VISIBLE : View.GONE);
		
		key = findViewById (R.id.kb_meta);
		key.setVisibility (View.VISIBLE);
		((Button) key).setText (R.string.key_show);

		view = findViewById (R.id.keyboard);
		view.setVisibility (View.VISIBLE);					
	}
}
