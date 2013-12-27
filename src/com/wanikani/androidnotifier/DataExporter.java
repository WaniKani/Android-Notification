package com.wanikani.androidnotifier;

import java.io.File;
import java.io.FileOutputStream;
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

	/**
	 * Entries of this enum match the possibile values for the "Export dest" settings menu item.
	 * They represent a possible destination of an export operation. They may evolve into
	 * an import location as well.
	 * This enum exposes some entry points used by {@link DataExporter#export(Context, Format)},
	 * which take care of preparing the destination file and exporting it.
	 */
	public enum Destination {
		
		/// Use bluetooth device, and send to paired host
		BLUETOOTH {
		
			/**
			 * Check whether a bluetooth device does exist
			 * @param ctxt the context
			 */
			public void testPrerequisites (Context ctxt)
				throws IOException
			{
				if (BluetoothAdapter.getDefaultAdapter () == null)
					throw new IOException (ctxt.getString (R.string.exp_no_bluetooth));
			}
			
			/**
			 * The temporary file has been created: we use {@link DataExporter#send(Context, Destination, File, String)}
			 * to export it to the device. The bluetooth selection is done at a later time through
			 * {@link #fixupIntent(Context, Intent)}.
			 * @param ctxt the context
			 * @param fmt the format (used to set the MIME type)
			 */
			public void finalize (Context ctxt, Format fmt)
			{
				send (ctxt, this, getFile (ctxt), fmt.getType ());
			}
			
			/**
			 * Called by {@link DataExporter#send(Context, Destination, File, String)} right before
			 * calling the intent. We use it to select the bluetooth device.
			 * @param ctxt the context
			 * @param intent the intent being prepared
			 */
			public void fixupIntent (Context ctxt, Intent intent)
			{
				setBluetoothService (ctxt, intent);
			}
		},
		
		/// Let the user choose any activity supporting the <code>SEND_ACTION</code> intent.
		CHOOSE {
			/**
			 * The temporary file has been created: we use {@link DataExporter#send(Context, Destination, File, String)}
			 * to export it to the device. 
			 * @param ctxt the context
			 * @param fmt the format (used to set the MIME type)
			 */
			public void finalize (Context ctxt, Format fmt)
			{
				send (ctxt, this, getFile (ctxt), fmt.getType ());
			}
		},
		
		/// Output to a file on the local filesystem
		FILESYSTEM {
			
			/**
			 * The output stream is actually the file being generated.
			 * @param ctxt the context
			 */
			public OutputStream getOutputStream (Context ctxt)
				throws IOException
			{
				return new FileOutputStream (SettingsActivity.getExportFile (ctxt));
			}
			
			/**
			 * Nothing to do, however showing a message is the least we can do.
			 * @param ctxt the context
			 * @param fmt the format
			 */
			public void finalize (Context ctxt, Format fmt)
			{
				msg (ctxt, ctxt.getString (R.string.exp_to_file, SettingsActivity.getExportFile (ctxt)));
			}
		};
		
		/**
		 * Called by {@link DataExporter#export(Context, Format)} right before exporting the file.
		 * May be used to check if hardware support, or other prerequisites.
		 * This implementation does nothing. 
		 * @param ctxt the context
		 * @throws IOException an exception, whose message can be displayed to the user
		 */
		public void testPrerequisites (Context ctxt)
			throws IOException
		{
			/* empty */
		}
		
		/**
		 * Returns an output stream used by {@link Format#export(OutputStream)} to export the database.
		 * It may be a temporary file used later on by {@link #finalize()}, or the final destination.
		 * This implementation creates a world readable temporary file (this is the easiest way
		 * to let external actions read its contents and -- well -- nothing sensible is in here!).
		 * @param ctxt the context
		 * @return the output stream
		 */
		public OutputStream getOutputStream (Context ctxt)
			throws IOException
		{
			return ctxt.openFileOutput (FILENAME, Context.MODE_WORLD_READABLE);
		}

		/**
		 * This method is called after the file has been exported. This implementation does nothing
		 * @param ctxt the context
		 * @param fmt the format
		 */
		public void finalize (Context ctxt, Format fmt)
		{
			/* empty */
		}
		
		/**
		 * Returns a file object representing the output stream created by {@link #getOutputStream(Context)}.
		 * The implementation of this method is needed only if enum implementations call 
		 * {@link DataExporter#send(Context, Destination, File, String)}.
		 * The current implementation is in-sync with this class' {@link #getOutputStream(Context)}
		 * @param ctxt the context
		 * @return a file object
		 */
		public File getFile (Context ctxt)
		{
			return ctxt.getFileStreamPath (FILENAME);
		}
		
		/**
		 * Called by {@link DataExporter#send(Context, Destination, File, String)} to finalize
		 * the setup of the intent being sent (if the enum decides to use this method).
		 * @param ctxt the context
		 * @param intent the intent being finalized
		 */
		public void fixupIntent (Context ctxt, Intent intent)
		{
			/* empty */
		}
	};
	
	/// The default export file. To avoid problems we use the archaic 8+3 convention
	private static final String FILENAME = "wkexport.csv";

	/** 
	 * A static instance. We use the singleton pattern so we can create the temporary file
	 * always at the same place, because we synchronize the {@link #export(Context, Format)}
	 * method, and there are no chance being overwritten at the wrong time. 
	 */
	public static final DataExporter INSTANCE = new DataExporter ();
	
	/**
	 * (Private) constructor.
	 */
	private DataExporter ()
	{
		/* empty */
	}
	
	/**
	 * Returns the default export file. We try to use the external SD, if present.
	 * Otherwise we revert to the internal storage (which defeats the point, I'm afraid).
	 * @param ctxt the context
	 * @return an absolute path to the export file
	 */
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
	
	/**
	 * Exports the db, according to current settings.
	 * @param ctxt the context
	 * @param fmt the export format
	 */
	public synchronized void export (Context ctxt, Format fmt)
	{	
		Destination dest;
		OutputStream os;
		
		os = null;
		dest = SettingsActivity.getExportDestination (ctxt);
		try {
			dest.testPrerequisites (ctxt);
			
			os = dest.getOutputStream (ctxt);
			fmt.export (os);
			os.close ();
			
			os = null;
			
			dest.finalize (ctxt, fmt);
						
		} catch (Exception e) {
			msg (ctxt, e.getMessage ());
		} finally {
			try {
				if (os != null)
					os.close ();
			} catch (IOException e) {
				/* ignore */
			}
		}
	}
	
	/**
	 * Called by destination enum implementations, if they need to use an <code>ACTION_SEND</code>
	 * intent to perform their duty. Enums have a chance to fix the intent right before it is fired.
	 * @param ctxt the context
	 * @param dest the destination enum
	 * @param file the file to send
	 * @param type the mime type
	 */
	protected static void send (Context ctxt, Destination dest, File file, String type)
	{
		Intent intent;
		
		intent = new Intent ();
		intent.setAction (Intent.ACTION_SEND);
		intent.setType (type);
		intent.putExtra (Intent.EXTRA_STREAM, Uri.fromFile (file));
		
		dest.fixupIntent (ctxt, intent);			
		
		ctxt.startActivity (intent);
	}
	
	/**
	 * Configures an intent object to use the bluetooth service, if available.
	 * Meant to be called inside of {@link Destination#fixupIntent(Context, Intent)}.
	 * @param ctxt a context
	 * @param intent the intent being configured
	 */
	protected static void setBluetoothService (Context ctxt, Intent intent)
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
	
	/**
	 * Displays a toast.
	 * @param ctxt the context
	 * @param msg the message to display
	 */
	protected static void msg (Context ctxt, String msg)
	{
		Toast toast;
		
		toast = Toast.makeText (ctxt, msg, Toast.LENGTH_SHORT);
		toast.show ();		
	}	
}
