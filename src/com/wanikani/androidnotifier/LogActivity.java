package com.wanikani.androidnotifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ScrollView;
import android.widget.TextView;

public class LogActivity extends Activity {

	class MoveDown implements Runnable {
		
		ScrollView sv;
		
		public MoveDown (ScrollView sv)		
		{
			this.sv = sv;
			
			sv.post (this);
		}
		
		@Override
		public void run () {
	        sv.fullScroll (ScrollView.FOCUS_DOWN);
	    }
		
	}
	
	private static final String FILENAME = "logcat.txt";
	
	@Override
	public void onCreate (Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
	    TextView tv;
	    String s;
	    
	    setContentView (R.layout.log);
	    
	    try {
	    	s = dump ();
		    tv = (TextView) findViewById (R.id.tv_log);
		    tv.setText (s);
		    new MoveDown ((ScrollView) findViewById (R.id.sv_log));
	    } catch (IOException e) {
	    	/* empty */
	    }
	    
	}
	
	
	@TargetApi(8)
	public final String getFile ()
	{
		File file;
		
		if (Build.VERSION.SDK_INT >= 8)
			file = getExternalFilesDir (null);
		else {
			file = Environment.getExternalStorageDirectory ();
			file = new File (file.getAbsolutePath () + "/Android/data/" +
							 getPackageName () + "/files/");
		}
		
		if (file == null)
			file = getFilesDir ();
		
		if (file == null)
			file = new File ("/");
		
		return file.getAbsolutePath () + "/" + FILENAME; 
	}	
	
	private String dump ()
		throws IOException
	{
		BufferedReader bufr;
		StringBuilder sb;
		String line;
	    Process proc;
	      
	    Runtime.getRuntime ().exec ("logcat -d -v long -f " + getFile ());

	    proc = Runtime.getRuntime ().exec ("logcat -d -v long");
	      
	    bufr = new BufferedReader (new InputStreamReader (proc.getInputStream ()));
	    sb = new StringBuilder ();

	    while (true) {
	    	line = bufr.readLine ();
	    	if (line == null)
	    		break;
	    	sb.append (line).append ("\n");
	    }

	    return sb.toString ();
	}		
}
