package com.wanikani.androidnotifier;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class WebReviewActivity extends Activity {

	WebView wv;
	
	private static final String PREFIX = "com.wanikani.androidnotifier.WebReviewActivity.";
	
	public static final String OPEN_ACTION = PREFIX + "OPEN";
	
	private static final String URL_START = "http://www.wanikani.com/review/session/start";
	
	@Override
	public void onCreate (Bundle bundle) 
	{
		super.onCreate (bundle);
		
		wv = new WebView (this);
		wv.getSettings ().setJavaScriptEnabled (true);

		wv.loadUrl (URL_START);
		
		setContentView (wv);
	}
}
