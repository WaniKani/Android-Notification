package com.wanikani.androidnotifier;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wanikani.androidnotifier.db.CSVFormat;
import com.wanikani.androidnotifier.db.Format;

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

public class ImportActivity extends Activity {

	private class ImportListener implements View.OnClickListener {
				
		
		@Override
		public void onClick (View view)
		{
			String fname;
			
			fname = lview.getText ().toString ().trim ();
			if (fname.length () == 0) {
				toast (R.string.tag_empty_file);
				return;
			}

			lview.setEnabled (false);
			view.setVisibility (View.GONE);
			
			go (new File (fname));
		}
		
	}

	private static final String PREFIX = ImportActivity.class.getName () + ".";
	
	/**
	 * The contents of the import dialog, if present
	 */
	private static final String IMPORT_LOCATION = PREFIX + "import_location";
	
	/**
	 * The location view
	 */
	private EditText lview;
	
	/**
	 * The import button
	 */
	private Button importb;
	
	@Override
	public void onCreate (Bundle bundle)
	{
		super.onCreate (bundle);
		
		setContentView (R.layout.importdialog);
		
		lview = (EditText) findViewById (R.id.id_location);
		if (bundle != null) {
			if (bundle.containsKey (IMPORT_LOCATION))
				lview.setText (bundle.getString (IMPORT_LOCATION));
		}

		importb = (Button) findViewById (R.id.id_import);
		importb.setOnClickListener (new ImportListener ());
		
		setTitle (R.string.id_title);
		
		lview.requestFocus ();
		
		checkIntent (getIntent ());
	}
	
	@Override
	public void onSaveInstanceState (Bundle bundle)
	{
		bundle.putString (IMPORT_LOCATION, lview.getText ().toString ());
	}

	@Override
	protected void onNewIntent (Intent intent)
	{
		super.onNewIntent (intent);

		checkIntent (intent);
	}
	
	protected void checkIntent (Intent intent)
	{
		String action;
		Uri uri;
		
		action = intent.getAction ();
		if (action.equals (Intent.ACTION_VIEW) ||
		    action.equals (Intent.ACTION_EDIT)) {
			
			uri = intent.getData ();
			if (uri != null && uri.getScheme ().equals ("file"))
				lview.setText (uri.getPath ());
		}		
	}
		
	protected void go (File file)
	{
		Format fmt;
		
		fmt = CSVFormat.newInstance (this);
		try {
			done (fmt.importFile (file));
		} catch (Exception e) {
			error (e.getMessage ());
		}
	}
	
	protected void error (String message)
	{
		TextView tv;
		
		tv = (TextView) findViewById (R.id.id_message);
		tv.setText (getString (R.string.id_import_failed, message));
		tv.setTextColor (Color.RED);
		tv.setVisibility (View.VISIBLE);
	}
	
	protected void done (Format.ImportResult ir)
	{
		LocalBroadcastManager lbm;
		TextView tv;
		Intent i;
		
		tv = (TextView) findViewById (R.id.id_message);
		tv.setText (getString (R.string.id_import_ok, ir.read, ir.updated));
		tv.setTextColor (Color.GREEN);
		tv.setVisibility (View.VISIBLE);
		
		lbm = LocalBroadcastManager.getInstance (this);
		i = new Intent (MainActivity.ACTION_CLEAR);
		lbm.sendBroadcast (i);
	}

	private void toast (int id)
	{
		Toast.makeText (this, getString (id), Toast.LENGTH_LONG).show ();		
	}	
}
