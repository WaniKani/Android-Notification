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

public class ReconstructDialog {

	public interface Interface {
		
		public void completed ();
		
	}
	
	private class GoListener implements DialogInterface.OnClickListener {
		
		Context ctxt;
		
		public GoListener (Context ctxt)
		{
			this.ctxt = ctxt;
		}
		
		public void onClick (DialogInterface ifc, int which)
		{
			go (ctxt);
		}		
	}
	
	private class CancelListener implements DialogInterface.OnClickListener {
		
		public void onClick (DialogInterface ifc, int which)
		{
			cancelled = true;
		}
		
	}

	private class Update {
		
		int percentage;
		
		String msg;
		
		boolean showBar;
		
		public Update (int done, int steps, String msg)
		{
			percentage = (100 * done) / steps;
			this.msg = msg;
			showBar = true;
		}
		
		public Update (String msg)
		{
			this.msg = msg;
			showBar = false;
		}
	}
	
	private class ProgressDialog {
		
		ProgressBar pb;
		
		TextView tv;
		
		Dialog dialog;

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
		
		public void dismiss ()
		{
			dialog.dismiss ();
		}
		
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
	
	private class Task extends AsyncTask<Void, Update, Boolean> {
		
		private Connection conn;
		
		private Context ctxt;
		
		private Update lupd;
		
		private static final int RADICALS_CHUNK = 10;
		
		private static final int KANJI_CHUNK = 10;
		
		private static final int VOCAB_CHUNK = 5;
		
		public Task (Connection conn, Context ctxt)
		{
			this.conn = conn;
			this.ctxt = ctxt;
		}
		
		private int [] array (int from, int to)
		{
			int ans [], i;
			
			ans = new int [to - from + 1];
			for (i = from; i <= to; i++)
				ans [i - from] = i;
			
			return ans;
		}
		
		private int itemStepsFor (int level, int chunk)
		{
			return (level + chunk - 1) / chunk;
		}
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			HistoryDatabase.ReconstructTable rt;
			ItemLibrary<Vocabulary> vlib;
			ItemLibrary<Radical> rlib;
			ItemLibrary<Kanji> klib;
			UserInformation ui;
			HistoryDatabase hdb;
			int i, step, steps;
			Update u;

			hdb = null;
			rt = null;
			try {
				hdb = new HistoryDatabase (ctxt);
				hdb.openW ();

				ui = conn.getUserInformation ();
				steps = (2 * itemStepsFor (ui.level, RADICALS_CHUNK)) +   
						(2 * itemStepsFor (ui.level, KANJI_CHUNK)) +
						(2 * itemStepsFor (ui.level, VOCAB_CHUNK));
				step = 0;
				
				u = new Update (step++, steps, ctxt.getString (R.string.rec_start));
				publishProgress (u);
				rt = hdb.startReconstructing (ui);
				for (i = 1; i <= ui.level; i += RADICALS_CHUNK) {
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_radicals_r,
									i, i + RADICALS_CHUNK - 1));
					publishProgress (u);
					rlib = conn.getRadicals (array (i, i + RADICALS_CHUNK - 1));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_radicals_w));
					publishProgress (u);
					for (Radical r : rlib.list)
						rt.load (r);
				}
				
				for (i = 1; i <= ui.level; i += KANJI_CHUNK) {
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_kanji_r,
									i, i + KANJI_CHUNK - 1));
					publishProgress (u);
					klib = conn.getKanji (array (i, i + KANJI_CHUNK - 1));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_kanji_w));
					publishProgress (u);
					for (Kanji kanji : klib.list)
						rt.load (kanji);
				}
				
				for (i = 1; i <= ui.level; i += VOCAB_CHUNK) {
					u = new Update (step++, steps, 
									ctxt.getString (R.string.rec_vocab_r,
									i, i + VOCAB_CHUNK - 1));
					publishProgress (u);
					vlib = conn.getVocabulary (array (i, i + VOCAB_CHUNK - 1));
					u = new Update (step++, steps, ctxt.getString (R.string.rec_vocab_w));
					publishProgress (u);
					for (Vocabulary vocab : vlib.list)
						rt.load (vocab);
				}

				hdb.endReconstructing (rt);
				
			} catch (SQLException e) {
				return false;
			} catch (IOException e) {
				return false;
			} finally {
				if (rt != null)
					rt.close ();
				if (hdb != null)
					hdb.close ();
			}
			
			return true;	
		}	
						
		@Override
		protected void onPostExecute (Boolean b)
		{
			if (!b)
				publishProgress (new Update (ctxt.getString (R.string.rec_error)));
			
			complete (b);
		}
		
		@Override
		protected void onProgressUpdate (Update... u)
		{
			publish (u [0]);
		}
	}
	
	Connection conn;
	
	boolean cancelled;
	
	boolean completed;
	
	ProgressDialog pdialog;
	
	Update lastUpdate;
	
	Interface ifc;
	
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
	
	public void go (Context ctxt)
	{
		attach (ctxt);
		new Task (conn, ctxt).execute ();
	}
	
	public void cancel ()
	{
		cancelled = true;
	}
	
	public void attach (Context ctxt)
	{
		if (cancelled || completed)
			return;
		
		createDialog (ctxt);
	}
	
	public void detach ()
	{
		if (pdialog != null)
			pdialog.dismiss ();
		
		pdialog = null;
	}
	
	private void createDialog (Context ctxt)
	{
		pdialog = new ProgressDialog (ctxt);
		if (lastUpdate != null)
			publish (lastUpdate);
	}
	
	private void complete (boolean ok)
	{		
		if (ok) {
			detach ();		
			completed = true;
			ifc.completed ();
		}
	}
	
	private void publish (Update update)
	{
		lastUpdate = update;
		
		if (pdialog != null)
			pdialog.publish (update);
	}
	
}
