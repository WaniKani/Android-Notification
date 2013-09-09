package com.wanikani.androidnotifier;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

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
public class DataExporter {
	
	public enum Destination {
		
		BLUETOOTH,
		
		CHOOSE,
		
		FILESYSTEM
		
	};
	
	private static final String FILENAME = "dbexport.csv";
	
	public static final DataExporter INSTANCE = new DataExporter ();
	
	private DataExporter ()
	{
		/* empty */
	}
	
	@TargetApi(8)
	public static final String getDefaultExportFile (Context ctxt)
	{
		File file;
		
		if (Build.VERSION.SDK_INT >= 8)
			file = ctxt.getExternalFilesDir (null);
		else {
			file = Environment.getExternalStorageDirectory ();
			file = new File (file.getAbsolutePath () + "/Android/data/" +
							 ctxt.getPackageName () + "/files/");
		}
		
		if (file == null)
			file = ctxt.getFilesDir ();
		
		if (file == null)
			file = new File ("/");
		
		return file.getAbsolutePath () + "/" + DataExporter.FILENAME; 
	}	
	
	public synchronized void export (Context ctxt, Format fmt)
	{	
		OutputStream os;
		File file;
		
		if (BluetoothAdapter.getDefaultAdapter () == null) {
			msg (ctxt, R.string.exp_no_bluetooth);
			return;
		}
		
		
		file = null;
		os = null;
		try {
			os = ctxt.openFileOutput (FILENAME, Context.MODE_WORLD_READABLE);
			file = ctxt.getFileStreamPath (FILENAME);
			fmt.export (os);
			os.close ();
			os = null;
			
			send (ctxt, file, fmt.getType ());
			
		} catch (Exception e) {
			msg (ctxt, R.string.exp_internal_failure, e.getMessage ());
		} finally {
			try {
				if (os != null)
					os.close ();
			} catch (IOException e) {
				/* ignore */
			}
		}
	}
	
	protected void send (Context ctxt, File file, String type)
	{
		Intent intent;
		
		intent = new Intent ();
		intent.setAction (Intent.ACTION_SEND);
		intent.setType (type);
		intent.putExtra (Intent.EXTRA_STREAM, Uri.fromFile (file));
		
		setBluetoothService (ctxt, intent);			
		
		ctxt.startActivity (intent);
	}
	
	protected void setBluetoothService (Context ctxt, Intent intent)
	{
		PackageManager pkgm;
		List<ResolveInfo> apps;
		
		pkgm = ctxt.getPackageManager ();
		apps = pkgm.queryIntentActivities (intent, 0);
		for (ResolveInfo rinfo : apps) {
			if (rinfo.activityInfo.packageName.equals ("com.android.bluetooth")) {
				intent.setClassName (rinfo.activityInfo.packageName, rinfo.activityInfo.name);
				return;
			}
		}
	}
	
	protected void msg (Context ctxt, int id, String... fmt)
	{
		Toast toast;
		
		toast = Toast.makeText (ctxt, ctxt.getString (id, (Object []) fmt), Toast.LENGTH_SHORT);
		toast.show ();		
	}	
}
