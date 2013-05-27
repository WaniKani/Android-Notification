package com.wanikani.androidnotifier;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;

public class WebReviewActivity extends Activity {

	private class WebViewClientImpl extends WebViewClient {
				
	    @Override
	    public boolean shouldOverrideUrlLoading (WebView view, String url) 
	    {
	        bar.setVisibility (View.VISIBLE);

	        view.loadUrl(url);

	        return true;
	    }
		
		@Override  
	    public void onPageFinished(WebView view, String url)  
	    {  
			bar.setVisibility (View.GONE);
			if (url.startsWith ("http"))
				js (JS_INIT);
	    }
	}
	
	private class WebChromeClientImpl extends WebChromeClient {
	
		@Override
		public void onProgressChanged (WebView view, int progress)
		{
			bar.setProgress (progress);
		}
	
	};

	private class ShowHideKeyboard implements Runnable {
		
		boolean show;
		
		ShowHideKeyboard (boolean show)
		{
			this.show = show;
			
			runOnUiThread (this);
		}
		
		public void run ()
		{
			View view;
			
			view = findViewById (R.id.keyboard);
			view.setVisibility (show ? View.VISIBLE : View.GONE);			
		}
		
	}
	
	private class WKNKeyboard {
		
		@JavascriptInterface
		public void show ()
		{
			new ShowHideKeyboard (true);
		}

		@JavascriptInterface
		public void hide ()
		{
			new ShowHideKeyboard (false);
		}
};
	
	private class MetaListener implements View.OnClickListener {
	
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

	private class KeyListener implements View.OnClickListener {
		
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

	
	WebView wv;
	
	ProgressBar bar;
	
	private static final String PREFIX = "com.wanikani.androidnotifier.WebReviewActivity.";
	
	public static final String OPEN_ACTION = PREFIX + "OPEN";

	private static final String URL_START = "http://www.wanikani.com/review/session/start";
	
	private static final String JS_INIT = 
			"var textbox = document.getElementById (\"user_response\"); " +
			"if (textbox != null) {" +
			"	textbox.focus ();" +
			"	wknKeyboard.show ();" +
			"} else {" +
			"	wknKeyboard.hide ();" +
			"}";

	private static final String JS_ENTER = 
			"var form = document.getElementById (\"question-form\"); " +
			"form.submit ();";

	private static final String KB_LATIN = "qwertyuiopasdfghjklzxcvbnm";

	private static final String KB_ALT = "1234567890";
	
	private static final int key_table [] = new int [] {
		R.id.kb_0, R.id.kb_1,  R.id.kb_2, R.id.kb_3, R.id.kb_4,
		R.id.kb_5, R.id.kb_6,  R.id.kb_7, R.id.kb_8, R.id.kb_9,
		R.id.kb_10, R.id.kb_11,  R.id.kb_12, R.id.kb_13, R.id.kb_14,
		R.id.kb_15, R.id.kb_16,  R.id.kb_17, R.id.kb_18, R.id.kb_19,
		R.id.kb_20, R.id.kb_21,  R.id.kb_22, R.id.kb_23, R.id.kb_24,
		R.id.kb_25
	};
	
	private static final int meta_table [] = new int [] {
		R.id.kb_quote, R.id.kb_backspace, R.id.kb_meta, 
		R.id.kb_space, R.id.kb_enter		
	};
	
	private static final int meta_codes [] = new int [] {
		KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_NUM,
		KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER		
	};
	
	private String keyboard;
	
	private KeyListener klist;
	private MetaListener mlist;

	@Override
	public void onCreate (Bundle bundle) 
	{
		super.onCreate (bundle);
		
		setContentView (R.layout.web_review);
		
		initKeyboard ();
		
		bar = (ProgressBar) findViewById (R.id.pb_reviews);
		
		wv = (WebView) findViewById (R.id.wv_reviews);
		
		wv.getSettings ().setJavaScriptEnabled (true);
		wv.getSettings().setJavaScriptCanOpenWindowsAutomatically (true);
		wv.getSettings ().setSupportMultipleWindows (true);
		wv.getSettings ().setUseWideViewPort (true);
		wv.addJavascriptInterface (new WKNKeyboard (), "wknKeyboard");
		wv.setScrollBarStyle (ScrollView.SCROLLBARS_OUTSIDE_OVERLAY);
		wv.setWebViewClient (new WebViewClientImpl ());
		wv.setWebChromeClient (new WebChromeClientImpl ());
		
		wv.loadUrl (URL_START);
	}
	
	protected void initKeyboard ()
	{
		Button key;
		int i;
		
		loadKeyboard (KB_LATIN);		

		klist = new KeyListener ();
		mlist = new MetaListener ();

		for (i = 0; i < key_table.length; i++) {
			key = (Button) findViewById (key_table [i]);
			key.setOnClickListener (klist);
		}					

		for (i = 0; i < meta_table.length; i++) {
			key = (Button) findViewById (meta_table [i]);
			key.setOnClickListener (mlist);
		}					
	}
	
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
		}
	}
	
	public void insert (int keycode)
	{
		KeyEvent kdown, kup;
	
		if (keycode == KeyEvent.KEYCODE_NUM)
			loadKeyboard (keyboard == KB_ALT ? KB_LATIN : KB_ALT);
		else if (keycode == KeyEvent.KEYCODE_ENTER)
			js (JS_ENTER);
		else {
			kdown = new KeyEvent (KeyEvent.ACTION_DOWN, keycode);
			wv.dispatchKeyEvent (kdown);				
			kup = new KeyEvent (KeyEvent.ACTION_UP, keycode);
			wv.dispatchKeyEvent (kup);
		}
	}
	
	protected int charToKeyCode (char c)
	{
		if (Character.isLetter (c))
			return KeyEvent.KEYCODE_A + (Character.toLowerCase (c) - 'a');
		else if (Character.isDigit (c))
			return KeyEvent.KEYCODE_0 + (c - '0');
		
		switch (c) {
		case ' ':
			return KeyEvent.KEYCODE_SPACE;
		}
		
		return KeyEvent.KEYCODE_SPACE;
	}
	
	private void js (String s)
	{
       wv.loadUrl ("javascript:(function() { " + s + "})()");
	}
	
}
