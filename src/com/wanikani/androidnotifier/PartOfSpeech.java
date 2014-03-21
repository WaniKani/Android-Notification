package com.wanikani.androidnotifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class PartOfSpeech {
	
	private static final String SCRIPT_FNAME = "partofspeech.js";
	
	private static final String TAG = PartOfSpeech.class.getName ();
	
	public static final Pattern URLS [] = new Pattern []  { 
		Pattern.compile (".*://www.wanikani.com/.*vocabulary/.*"),
		Pattern.compile (".*://www.wanikani.com/review/session.*"),
		Pattern.compile (".*://www.wanikani.com/lesson/session.*")
	};

	public static void enter (Context ctxt, FocusWebView wv, String url)
	{
		AssetManager mgr;
		InputStream is;
		Reader r;
		int rd;
		char buf [];
		StringBuffer sb;
		
		Log.d (TAG, "Entering PartOfSpeech");
		
		if (!matches (url))
			return;
		
		Log.d (TAG, "URI matches");
		
		mgr = ctxt.getAssets ();
		r = null;
		try {
			Log.d (TAG, "Opening script");
			is = mgr.open (SCRIPT_FNAME);
			r = new InputStreamReader (is);
			sb = wv.jsStart ();
			buf = new char [1024];
			while (true) {
				rd = r.read (buf);
				if (rd < 0)
					break;
				sb.append (buf, 0, rd);
			}
			Log.d (TAG, "Script loaded, feeding JS");
			wv.jsEnd (sb);
			Log.d (TAG, "All done, exiting");
		} catch (Throwable t) {
			Log.e (TAG, "Failed to load script", t);
		} finally {
			try {
				if (r != null)
					r.close ();
			} catch (IOException e) {
				/* empty */
			}
		}
	}
	
	protected static boolean matches (String url)
	{
		int i;
		
		for (i = 0; i < URLS.length; i++)
			if (URLS [i].matcher (url).matches ())
				return true;
				
		return false;
	}
	
}