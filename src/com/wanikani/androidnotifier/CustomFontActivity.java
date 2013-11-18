package com.wanikani.androidnotifier;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.androidnotifier.db.FontDatabase;
import com.wanikani.androidnotifier.db.FontDatabase.FontEntry;

public class CustomFontActivity extends Activity {

	class FontHolder {
		
		FontEntry fe;
		
		CheckBox cbv;
		
		Button delb;
		
		TextView namev;
		
		TextView samplev;
		
		public FontHolder (View view)
		{
			cbv = (CheckBox) view.findViewById (R.id.cf_enabled);
			namev = (TextView) view.findViewById (R.id.cf_font);
			samplev = (TextView) view.findViewById (R.id.cf_font_sample);
			delb = (Button) view.findViewById (R.id.cf_delete);
		}
		
		public void fill (FontEntry fe)
		{
			this.fe = fe;
			
			cbv.setChecked (fe.enabled);
			namev.setText (fe.name);
			samplev.setTypeface (fe.face);
			delb.setVisibility (fe.system ? View.GONE : View.VISIBLE);
			
			cbv.setTag (this);
			delb.setTag (this);			
		}
	}
	
	class FontListAdapter extends BaseAdapter 
		implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

		/// The list of fonts.
		List<FontEntry> fonts;
		
		public FontListAdapter ()
		{
			rescan ();
		}		

		@Override
		public int getCount ()
		{
			return fonts.size ();
		}
		
		@Override
		public FontEntry getItem (int position)
		{
			return fonts.get (position);
		}
		
		@Override
		public long getItemId (int position)
		{
			return position;			
		}
	
		@Override
		public View getView (int position, View row, ViewGroup parent) 
		{
			LayoutInflater inflater;
			FontHolder holder;
			FontEntry entry;

			entry = getItem (position);
			holder = row != null ? (FontHolder) row.getTag () : null;
			if (holder == null) {
				inflater = getLayoutInflater ();
				row = inflater.inflate (R.layout.fontsample, parent, false);
				holder = new FontHolder (row);
				row.setTag (holder);
				
				holder.delb.setOnClickListener (this);
				holder.cbv.setOnCheckedChangeListener (this);
			}

			holder.fill (entry);
			
			return row;
		}
		
		@Override
		public void onClick (View view)
		{
			FontHolder holder;
			
			holder = (FontHolder) view.getTag ();
			if (holder != null)
				FontDatabase.delete (CustomFontActivity.this, holder.fe);
		}
		
		@Override
		public void onCheckedChanged  (CompoundButton button, boolean checked)
		{
			FontHolder holder;
			
			holder = (FontHolder) button.getTag ();
			if (holder != null)
				FontDatabase.setEnabled (CustomFontActivity.this, holder.fe, checked);			
		}
		
		public void rescan ()
		{
			fonts = FontDatabase.getAllFonts (CustomFontActivity.this);
		}
	}
	
	class FontImportListener implements View.OnClickListener, DialogInterface.OnClickListener {
						
		@Override
		public void onClick (View view)
		{
			showDialog (R.string.tag_cf_import, R.string.tag_cf_path, this);
		}
		
		@Override
		public void onClick (DialogInterface difc, int which)
		{
			
		}
	}
	
	class FontDownloadListener implements View.OnClickListener, DialogInterface.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			showDialog (R.string.tag_cf_download, R.string.tag_cf_url, this);
		}
		
		@Override
		public void onClick (DialogInterface difc, int which)
		{
			
		}
	}

	public class CancelListener implements DialogInterface.OnClickListener {
		
		@Override
		public void onClick (DialogInterface difc, int which)
		{
			difc.dismiss ();
		}
		
	}
	
	ListView flist;
	
	FontListAdapter fla;
	
	Dialog dialog;
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);
		
		Button btn;

		setContentView (R.layout.customfont);
		
		fla = new FontListAdapter ();
		flist = (ListView) findViewById (R.id.cf_samples);
		flist.setAdapter (fla);
		
		btn = (Button) findViewById (R.id.cf_import);
		btn.setOnClickListener (new FontImportListener ());
		
		btn = (Button) findViewById (R.id.cf_download);
		btn.setOnClickListener (new FontDownloadListener ());		
	}
	
	@Override
	public void onPause ()
	{
		super.onPause ();
		
		if (dialog != null && dialog.isShowing ())
			dialog.dismiss ();
	}

	private void showDialog (int titleid, int locid, Dialog.OnClickListener listener)
	{
		LayoutInflater inflater;
		AlertDialog.Builder builder;
		TextView lview;
		View view;
					
		inflater = getLayoutInflater (); 
		view = inflater.inflate (R.layout.fontdialog, null);
		lview = (TextView) view.findViewById (R.id.fd_location_tag);
		lview.setText (locid);
		view.bringToFront ();
				
		builder = new AlertDialog.Builder (this);
		builder.setTitle (getString (titleid));
		builder.setView (view);
		builder.setPositiveButton (R.string.tag_cf_ok, listener);
		builder.setNegativeButton (R.string.tag_cf_cancel, new CancelListener ());
		
		dialog = builder.create ();
		
		dialog.show ();					
	}
}
