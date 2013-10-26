package com.wanikani.androidnotifier;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.UserInformation;
import com.wanikani.wklib.Vocabulary;

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
 * Here we implement the reconstruction logic. This class handles both the GUI
 * and the feed algorithm. The actual reconstruction, however, is implemented in
 * the {@link HistoryDatabase} class.
 */
public class ReconstructDialog {

	/**
	 * A listener for reconstruction events.
	 */
	public interface Interface {

		/**
		 * Called when reconstruction completes successfully.
		 * @param cs the new core stats
		 */
		public void completed (HistoryDatabase.CoreStats cs);
		
	}
	
	/**
	 * Listener attached to the "Go" button of the initial dialog.
	 */
	private class GoListener implements DialogInterface.OnClickListener {

		/// Context
		Context ctxt;
		
		/**
		 * Constructor
		 * @param ctxt the context
		 */
		public GoListener (Context ctxt)
		{
			this.ctxt = ctxt;
		}
		
		@Override
		public void onClick (DialogInterface ifc, int which)
		{
			go (ctxt);
		}		
	}
	
	/**
	 * Listener attached to the "Cancel" button of the initial dialog. 
	 */
	private class CancelListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick (DialogInterface ifc, int which)
		{
			cancelled = true;
		}		
	}

	/**
	 * The current state of the update process. If we want to make the process
	 * resumable, this should hold all the needed info. At the moment, however,
	 * we keep here the info needed to resume/recreate the dialog if the activity
	 * is recreated.
	 */
	private class Update {
		
		/// Completion percentage
		int percentage;
		
		/// Last message displayed
		String msg;
		
		/// If set, the progress bar is shown
		boolean showBar;
		
		/**
		 * Constructor. Used when the update process is still going on
		 * @param done completed steps
		 * @param steps number of steps
		 * @param msg the message to display
		 */
		public Update (int done, int steps, String msg)
		{
			percentage = (100 * done) / steps;
			this.msg = msg;
			showBar = true;
		}
		
		/**
		 * Constructor. Used when the update process is complete.
		 * @param msg the completion message
		 */
		public Update (String msg)
		{
			this.msg = msg;
			showBar = false;
		}
	}
	
	/**
	 * The dialog. This class is destroyed and recreated as the main activity
	 * is paused and resumed, so we must make sure the Update class contains
	 * all needed info to recreate the original GUI.
	 */
	private class ProgressDialog {
		
		/// Progress bar
		ProgressBar pb;
		
		/// The message view
		TextView tv;
		
		/// The android dialog
		Dialog dialog;

		/**
		 * Constructor
		 * @param ctxt the context
		 */
		public ProgressDialog (Context ctxt)
		{
			LayoutInflater inflater;
			AlertDialog.Builder builder;
			View view;
						
			inflater = LayoutInflater.from (ctxt);
			view = inflater.inflate(R.layout.reconstruct, null);
			view.bringToFront ();
			
			pb = (ProgressBar) view.findViewById (R.id.pb_reconstruct);
			tv = (TextView) view.findViewById (R.id.txt_reconstructing);
		
			builder = new AlertDialog.Builder (ctxt);
			builder.setTitle (R.string.txt_reconstruct_title);
			builder.setView (view);
			builder.setNegativeButton (R.string.tag_reconstruct_cancel, new CancelListener ());
			
			dialog = builder.create ();
			
			dialog.show ();				
		}
		
		/**
		 * Called when the dialog should be closed
		 */
		public void dismiss ()
		{
			dialog.dismiss ();
		}
		
		/**
		 * Called when the dialog must be updated
		 * @param u new info
		 */
		public void publish (Update u)
		{			
			tv.setText (u.msg);
			if (u.showBar) {
				pb.setVisibility (View.VISIBLE);
				pb.setProgress (u.percentage);
			} else
				pb.setVisibility (View.GONE);
		}		
	}
	
	/**
	 * The asynch task that loads all the info from WK, feeds the database and
	 * publishes the progress.
	 */
	private class Task extends AsyncTask<Void, Update, HistoryDatabase.CoreStats> {

		/// WK connection
		private Connection conn;
		
		/// Context
		private Context ctxt;
		
		/// Number of radical levels to load at a time
		private static final int RADICALS_CHUNK = 10;
		
		/// Number of kanji levels to load at a time
		private static final int KANJI_CHUNK = 10;
		
		/// Number of vocab levels to load at a time
		private static final int VOCAB_CHUNK = 5;
		
		/**
		 * Constructor
		 * @param conn WK connection
		 * @param ctxt the context
		 */
		public Task (Connection conn, Context ctxt)
		{
			this.conn = conn;
			this.ctxt = ctxt;
		}
		
		/**
		 * Conveience method to create an array contaning all the integer between
		 * to numbers
		 * @param from the start number
		 * @param to the end number
		 * @return the array
		 */
		private int [] array (int from, int to)
		{
			int ans [], i;
			
			ans = new int [to - from + 1];
			for (i = from; i <= to; i++)
				ans [i - from] = i;
			
			return ans;
		}
		
		/**
		 * Returns the number of steps needed to load all the item types.
		 * @param level the user leve
		 * @param chunk chunk size
		 * @return the number of steps
		 */
		private int itemStepsFor (int level, int chunk)
		{
			return (level + chunk - 1) / chunk;
		}
		
		/**
		 * The reconstruction process itself. It opens a DB reconstruction object,
		 * loads all the items, and retrieves the new core stats 
		 * @return the core stats, or <tt>null</tt> if something goes wrong
		 */
		@Override
		protected HistoryDatabase.CoreStats doInBackground (Void... v)
		{
			HistoryDatabase.ReconstructTable rt;
			ItemLibrary<Vocabulary> vlib;
			ItemLibrary<Radical> rlib;
			ItemLibrary<Kanji> klib;
			UserInformation ui;
			HistoryDatabase hdb;
			int i, j, step, steps;
			Update u;

			hdb = null;
			rt = null;
			try {
				hdb = new HistoryDatabase (ctxt);
				hdb.openW ();

				ui = conn.getUserInformation ();
				steps = (2 * itemStepsFor (ui.level, RADICALS_CHUNK)) +   
						(2 * itemStepsFor (ui.level, KANJI_CHUNK)) +
						(2 * itemStepsFor (ui.level, VOCAB_CHUNK)) + 1;
				step = 0;
				
				u = new Update (step++, steps, ctxt.getString (R.string.rec_start));
				publishProgress (u);
				rt = hdb.startReconstructing (ui);
				for (i = 1; i <= ui.level; i += RADICALS_CHUNK) {
					j = i + RADICALS_CHUNK - 1;
					if (j > ui.level)
						j = ui.level;
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_radicals_r, i, j));
					publishProgress (u);
					rlib = conn.getRadicals (array (i, j));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_radicals_w));
					publishProgress (u);
					for (Radical r : rlib.list)
						rt.load (r);
				}
				
				for (i = 1; i <= ui.level; i += KANJI_CHUNK) {
					j = i + KANJI_CHUNK - 1;
					if (j > ui.level)
						j = ui.level;
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_kanji_r, i, j));
					publishProgress (u);
					klib = conn.getKanji (array (i, j));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_kanji_w));
					publishProgress (u);
					for (Kanji kanji : klib.list)
						rt.load (kanji);
				}
				
				for (i = 1; i <= ui.level; i += VOCAB_CHUNK) {
					j = i + VOCAB_CHUNK - 1;
					if (j > ui.level)
						j = ui.level;
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_vocab_r, i, j));
					publishProgress (u);
					vlib = conn.getVocabulary (array (i, j));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_vocab_w));
					publishProgress (u);
					for (Vocabulary vocab : vlib.list)
						rt.load (vocab);
				}

				hdb.endReconstructing (rt);
				
				u = new Update (step++, steps, ctxt.getString (R.string.rec_fixup_db));
				publishProgress (u);
				DatabaseFixup.run (ctxt, conn);
				
				return hdb.getCoreStats (ui);
				
			} catch (SQLException e) {
				return null;
			} catch (IOException e) {
				return null;
			} finally {
				if (rt != null)
					rt.close ();
				if (hdb != null)
					hdb.close ();
			}
		}	
						
		/**
		 * Ends the reconstruction process by telling everybody how it went.
		 * @param cs the core stats, or <tt>null</tt> if smoething wrong happened
		 */
		@Override
		protected void onPostExecute (HistoryDatabase.CoreStats cs)
		{
			if (cs == null)
				publishProgress (new Update (ctxt.getString (R.string.rec_error)));
			
			complete (cs);
		}
		
		/**
		 * Publishes the progress by updating the window
		 */
		@Override
		protected void onProgressUpdate (Update... u)
		{
			publish (u [0]);
		}
	}
	
	/// The connection
	Connection conn;
	
	/// Set if the user cancelled the operation
	boolean cancelled;
	
	/// Set if the process completed
	boolean completed;
	
	/// The dialog windows
	ProgressDialog pdialog;
	
	/// Last update
	Update lastUpdate;
	
	/// The listener interface
	Interface ifc;
	
	/**
	 * Constructor
	 * @param ifc the listener interface
	 * @param ctxt the context
	 * @param conn a WK connection
	 */
	public ReconstructDialog (Interface ifc, Context ctxt, Connection conn)
	{
		AlertDialog.Builder builder;
		Dialog dialog;
					
		this.conn = conn;
		this.ifc = ifc;
		
		builder = new AlertDialog.Builder (ctxt);
		builder.setTitle (R.string.txt_reconstruct_title);
		builder.setMessage (R.string.txt_reconstruct_intro);
		builder.setPositiveButton (R.string.tag_reconstruct_go, new GoListener (ctxt));
		builder.setNegativeButton (R.string.tag_reconstruct_cancel, null);
		
		dialog = builder.create ();
		
		dialog.show ();		
	}
	
	/**
	 * Starts the reconstruction process. This happens immediately after the
	 * user reads the description of what's going to happen
	 * @param ctxt the context
	 */
	public void go (Context ctxt)
	{
		attach (ctxt);
		new Task (conn, ctxt).execute ();
	}
	
	/**
	 * Cancels the process.
	 */
	public void cancel ()
	{
		cancelled = true;
	}
	
	/**
	 * Called when the fragment is attached to a context.
	 * We rebuild a dialog window and put all the info that was in the former one.
	 * @param ctxt the context
	 */
	public void attach (Context ctxt)
	{
		if (cancelled || completed)
			return;
		
		createDialog (ctxt);
	}
	
	/**
	 * Called when the fragment is detached from the context.
	 * We close the dialog and make sure updates are not published anymore. 
	 */
	public void detach ()
	{
		if (pdialog != null)
			pdialog.dismiss ();
		
		pdialog = null;
	}
	
	/**
	 * Convenience method that opens the progress dialog
	 * @param ctxt the context
	 */
	private void createDialog (Context ctxt)
	{
		pdialog = new ProgressDialog (ctxt);
		if (lastUpdate != null)
			publish (lastUpdate);
	}
	
	/**
	 * Called when the process completes successfully. We notify the listener.
	 * @param cs the core stats
	 */
	private void complete (HistoryDatabase.CoreStats cs)
	{		
		if (cs != null) {
			detach ();		
			completed = true;
			ifc.completed (cs);
		}
	}
	
	/**
	 * Publishes an updated on the progress dialog (if any).
	 * @param update the update info
	 */
	private void publish (Update update)
	{
		lastUpdate = update;
		
		if (pdialog != null)
			pdialog.publish (update);
	}
	
}
