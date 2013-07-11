package com.wanikani.androidnotifier;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ReconstructDialog {

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
	
		public Update (int percentage, String msg)
		{
			this.percentage = percentage;
			this.msg = msg;
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
		
		public void cancel ()
		{
			dialog.cancel ();
		}
		
		public void publish (Update u)
		{
			tv.setText (u.msg);
			pb.setProgress (u.percentage);
		}		
	}
	
	private class Task extends AsyncTask<Void, Update, Boolean> {
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			int i;
			
			for (i = 0; i < 100; i += 5) {
				if (cancelled)
					break;
				super.publishProgress (new Update (i, "At " + i));
				try {
					Thread.sleep (1000);
				} catch (Exception e) {
					
				}
			}
			
			return true;	
		}	
						
		@Override
		protected void onPostExecute (Boolean b)
		{
			if (pdialog != null)
				pdialog.cancel ();
		}
		
		@Override
		protected void onProgressUpdate (Update... u)
		{
			if (pdialog != null)
				pdialog.publish (u [0]);
		}
	}
	
	boolean cancelled;
	
	ProgressDialog pdialog;
	
	public ReconstructDialog (Context ctxt)
	{
		AlertDialog.Builder builder;
		Dialog dialog;
					
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
		new Task ().execute ();
	}
	
	public void cancel ()
	{
		cancelled = true;
	}
	
	public void attach (Context ctxt)
	{
		createDialog (ctxt);
	}
	
	public void detach ()
	{
		pdialog = null;
	}
	
	private void createDialog (Context ctxt)
	{
		pdialog = new ProgressDialog (ctxt);
	}
}
