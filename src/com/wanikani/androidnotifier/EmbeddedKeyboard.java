package com.wanikani.androidnotifier;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.ImageButton;

import com.wanikani.androidnotifier.WebReviewActivity.KeyboardStatus;
import com.wanikani.androidnotifier.WebReviewActivity.WKConfig;

public class EmbeddedKeyboard implements Keyboard { 
	
	private class FocusOut implements Runnable {
		
		FocusOut ()
		{
			wav.runOnUiThread (this);
		}
		
		public void run ()
		{
			focusOut ();
		}
	}
	
	private class Focus {

		@JavascriptInterface
		public void focusOut ()
		{
			new FocusOut ();
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
			SettingsActivity.setTipAck (wav, true);
		}		
	}
	

	private static final String JS_FOCUS = 
			"var ltextbox;" +
			"ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_JP + "\"); " +
			"if (ltextbox == null) {" +
			"   ltextbox = document.getElementById (\"" + WKConfig.LESSON_ANSWER_BOX_EN + "\"); " +
			"}" +
			"if (ltextbox != null) {" +
			"    if (ltextbox.value.length == 0) {" +
			"        wknFocus.focusOut ();" +
			"    }" +
			"    ltextbox.focus (); " +
			"}";
	
	/** Javascript to be invoked to simulate a click on the submit (heart-shaped) button.
	 *  It also handles keyboard show/iconize logic. If the textbox is enabled, then this
	 *  is an answer, so we iconize the keyboard. Otherwise we are entering the new question,
	 *  so we need to show it  */
	private static final String JS_ENTER =
			"var textbox, form, submit;" +
			"textbox = document.getElementById (\"" + WKConfig.ANSWER_BOX + "\"); " +
		    "form = document.getElementById (\"new_lesson\"); " +
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

	WebReviewActivity wav;
	
	FocusWebView wv;
	
	View kview;
	
	/** The current keyboard. It may be set to either {@link #KB_LATIN} or {@link #KB_ALT} */
	private String keyboard;
	
	/** Height of a tall keyboard key */
	private int tallKey;
	
	/** Height of a short keyboard key */
	private int shortKey;
	
	/** Vibrator service */
	Vibrator vibrator;

	/** The mute button to be shown when the embedded keyboard is enabled */
	private ImageButton mute;
	
	public EmbeddedKeyboard (WebReviewActivity wav, FocusWebView wv)
	{
		this.wav = wav;		
		this.wv = wv;
		
		kview = wav.findViewById (R.id.keyboard);
		vibrator = (Vibrator) wav.getSystemService (Context.VIBRATOR_SERVICE) ;
		
		mute = (ImageButton) wav.findViewById (R.id.kb_mute);
		
		wv.addJavascriptInterface (new Focus (), "wknFocus");
		
		init ();
	}
	
	protected void init ()
	{
		View.OnClickListener klist, mlist;
		LayoutParams lp;
		View key;
		int i;

		loadKeyboard (KB_LATIN);
		
		klist = new KeyListener ();
		mlist = new MetaListener ();

		key = wav.findViewById (key_table [0]);
		lp = key.getLayoutParams ();
		shortKey = lp.height;
		tallKey = shortKey + shortKey / 2;

		for (i = 0; i < key_table.length; i++) {
			key = wav.findViewById (key_table [i]);
			key.setOnClickListener (klist);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = wav.findViewById (meta_table [i]);
			key.setOnClickListener (mlist);
		}
	}
	
	/**
	 * Updates the layout, according to the current preferences
	 */
	private void updateLayout ()
	{
		LayoutParams lp;
		int height;
		View key;
		int i;

		height = SettingsActivity.getLargeKeyboard (wav) ? tallKey : shortKey;
		for (i = 0; i < key_table.length; i++) {
			key = wav.findViewById (key_table [i]);
			lp = key.getLayoutParams ();
			lp.height = height;
			key.setLayoutParams(lp);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = wav.findViewById (meta_table [i]);
			lp = key.getLayoutParams ();
			lp.height = height;
			key.setLayoutParams(lp);
		}					
		
		lp = mute.getLayoutParams ();
		lp.height = height;
		mute.setLayoutParams(lp);
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
			key = (Button) wav.findViewById (key_table [i]);
			key.setText ("" + kbd.charAt (i));
		}
		while (i < key_table.length) {
			key = (Button) wav.findViewById (key_table [i]);
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
	
		if (vibrator != null && SettingsActivity.getVibrate (wav))
			vibrator.vibrate (50);
		
		if (keycode == KeyEvent.KEYCODE_NUM) {
			/* Num == meta -> if iconized, it means "Show"*/
			if (wav.kbstatus.isIconized ())
				wav.kbstatus.maximize (wav);
			else
				loadKeyboard (keyboard == KB_ALT ? KB_LATIN : KB_ALT);
		} else if (keycode == KeyEvent.KEYCODE_ENTER) {
			wv.js (JS_ENTER);
		} else if (keycode == KeyEvent.KEYCODE_DPAD_DOWN)
			wav.kbstatus.iconize (wav);
		else {			
			if (wav.kbstatus == KeyboardStatus.LESSONS_MAXIMIZED)
				wv.js (JS_FOCUS);
			
			kdown = new KeyEvent (KeyEvent.ACTION_DOWN, keycode);
			wv.dispatchKeyEvent (kdown);				
			kup = new KeyEvent (KeyEvent.ACTION_UP, keycode);
			wv.dispatchKeyEvent (kup);
		}
	}	

	/**
	 * Makes sure the lessons text box is focused out. This is necessary
	 * to avoid a very strange behaviour: if the user taps on the lessons answer
	 * box, then kana are not formed in the correct way. 
	 */
	private void focusOut ()
	{
		MotionEvent mev;
		float x, y;
		long tstamp;

		tstamp = SystemClock.uptimeMillis ();
		x = wv.getWidth () - 1;
		y = 1;
		mev = MotionEvent.obtain (tstamp, tstamp + 100, MotionEvent.ACTION_DOWN,
								  x, y, 0);
		wv.dispatchTouchEvent (mev);
		mev.recycle ();
		mev = MotionEvent.obtain (tstamp, tstamp + 100, MotionEvent.ACTION_UP,
				  x, y, 0);
		wv.dispatchTouchEvent (mev);
		mev.recycle ();
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
	
	public void show (boolean hasEnter)
	{
		View view, key;
		int i;
		
		wv.disableFocus ();
		
		updateLayout ();
		showEmbeddedKeyboardMessage ();

		for (i = 0; i < key_table.length; i++) {
			key = wav.findViewById (key_table [i]);
			key.setVisibility (View.VISIBLE);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = wav.findViewById (meta_table [i]);
			key.setVisibility (View.VISIBLE);
		}
		
		key = wav.findViewById (R.id.kb_meta);
		((Button) key).setText (R.string.key_meta);

		view = wav.findViewById (R.id.keyboard);
		view.setVisibility (View.VISIBLE);
		
		key = wav.findViewById (R.id.kb_enter);
		key.setVisibility (hasEnter ? View.VISIBLE : View.GONE);

		key = wav.findViewById (R.id.kb_hide);
		key.setVisibility (hasEnter ? View.GONE : View.VISIBLE);		
	}
	
	public void iconize (boolean hasEnter)
	{
		View view, key;
		int i;
		
		for (i = 0; i < key_table.length; i++) {
			key = wav.findViewById (key_table [i]);
			key.setVisibility (View.GONE);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = wav.findViewById (meta_table [i]);
			key.setVisibility (View.GONE);
		}
		
		key = wav.findViewById (R.id.kb_enter);
		key.setVisibility (hasEnter ? View.VISIBLE : View.GONE);
		
		key = wav.findViewById (R.id.kb_meta);
		key.setVisibility (View.VISIBLE);
		((Button) key).setText (R.string.key_show);

		view = wav.findViewById (R.id.keyboard);
		view.setVisibility (View.VISIBLE);					
	}
	
	public void hide ()
	{
		kview.setVisibility (View.GONE);
		wv.enableFocus ();
	}
	
	public ImageButton getMuteButton ()
	{
		return mute;
	}
	
	protected void showEmbeddedKeyboardMessage ()
	{
		AlertDialog.Builder builder;
		Dialog dialog;
					
		if (!wav.visible || SettingsActivity.getTipAck (wav))
			return;
		
		builder = new AlertDialog.Builder (wav);
		builder.setTitle (R.string.kbd_message_title);
		builder.setMessage (R.string.kbd_message_text);
		builder.setPositiveButton (R.string.kbd_message_ok, new OkListener ());
		
		dialog = builder.create ();
		
		dialog.show ();		
	}
	

}
