package com.wanikani.androidnotifier;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wanikani.androidnotifier.db.FontDatabase;
import com.wanikani.androidnotifier.db.FontDatabase.FontEntry;
import com.wanikani.androidnotifier.db.FontDatabase.FontTable;

public class CustomFontActivity extends Activity {

	class FontHolder {
		
		FontEntry fe;
		
		CheckBox cbv;
		
		Button delb;
		
		Button downlb;

		TextView namev;
		
		TextView samplev;
		
		public FontHolder (View view)
		{
			cbv = (CheckBox) view.findViewById (R.id.cf_enabled);
			namev = (TextView) view.findViewById (R.id.cf_font);
			samplev = (TextView) view.findViewById (R.id.cf_font_sample);
			downlb = (Button) view.findViewById (R.id.cf_download);
			delb = (Button) view.findViewById (R.id.cf_delete);
		}
		
		public void fill (FontEntry fe)
		{
			this.fe = fe;
						
			cbv.setEnabled (fe.available);
			cbv.setChecked (fe.enabled && fe.available);			
			namev.setText (fe.name);			
			samplev.setTypeface (fe.load ());
			samplev.setVisibility (fe.available ? View.VISIBLE : View.GONE);
			delb.setVisibility (fe.canBeDeleted () ? View.VISIBLE : View.GONE);
			downlb.setVisibility (fe.canBeDownloaded () ? View.VISIBLE : View.GONE);
			
			cbv.setTag (this);
			downlb.setTag (this);			
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
		
		public void invalidate ()
		{
			rescan ();
			notifyDataSetChanged ();
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
				holder.downlb.setOnClickListener (this);
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
			if (holder != null) {
				if (view == holder.delb)
					delete (holder.fe);
				else if (view == holder.downlb)
					download (holder.fe);
			}
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
			fonts = FontDatabase.getFonts (CustomFontActivity.this);
		}
	}
	
	class FontImportListener implements View.OnClickListener {
			
		Button button;
		
		View view;
		
		public FontImportListener (Button button)
		{
			this.button = button;
		}
		
		@Override
		public void onClick (View view)
		{
			String name, file;
			FontEntry fe;
			
			if (view == button) {
				this.view = showDialog (R.string.tag_cf_import, R.string.tag_cf_path, this);
				return;				
			} 
			
			if (this.view == null)
				return;
			
			name = ((TextView) this.view.findViewById (R.id.fd_name)).getText ().toString ();
			file = ((TextView) this.view.findViewById (R.id.fd_location)).getText ().toString ();
			
			name = name.trim ();
			if (name.length () == 0) {
				message (R.string.tag_empty_name);
				return;
			}
			
			if (FontDatabase.exists (CustomFontActivity.this, name)) {
				message (R.string.tag_already_exists);
				return;
			}
				
			if (!new File (file).canRead ()) {
				message (R.string.tag_cant_read);
				return;
			}
						
			fe = new FontEntry (-1, name, file, null, false, false, false);
			
			download (fe);			
			
			dialog.dismiss ();
		}
	}
	
	class FontDownloadListener implements View.OnClickListener {
		
		Button button;
		
		View view;
		
		public FontDownloadListener (Button button)
		{
			this.button = button;
		}
		
		@Override
		public void onClick (View view)
		{
			String name, url;
			FontEntry fe;
			
			if (view == button) {				
				this.view = showDialog (R.string.tag_cf_download, R.string.tag_cf_url, this);
				return;
			}
			
			if (this.view == null)
				return;
			
			name = ((TextView) this.view.findViewById (R.id.fd_name)).getText ().toString ();
			url = ((TextView) this.view.findViewById (R.id.fd_location)).getText ().toString ();
			
			name = name.trim ();
			if (name.length () == 0) {
				message (R.string.tag_empty_name);
				return;
			}
			
			if (FontDatabase.exists (CustomFontActivity.this, name)) {
				message (R.string.tag_already_exists);
				return;
			}
				
			try {
				new URL (url);
			} catch (MalformedURLException e) {
				message (R.string.tag_bad_url);
				return;
			}
						
			fe = new FontEntry (-1, name, null, url, false, false, false);
			
			download (fe);
		}
	}

	public class CancelListener implements DialogInterface.OnClickListener {
		
		@Override
		public void onClick (DialogInterface difc, int which)
		{
			difc.dismiss ();
		}
		
	}
	
	public static final String PREFIX = "font-";
	
	ListView flist;
	
	FontListAdapter fla;
	
	Dialog dialog;
	
	FontEntry fe;
	
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
		btn.setOnClickListener (new FontImportListener (btn));
		
		btn = (Button) findViewById (R.id.cf_download);
		btn.setOnClickListener (new FontDownloadListener (btn));
	}
	
	@Override
	public void onPause ()
	{
		super.onPause ();
		
		if (dialog != null && dialog.isShowing ())
			dialog.dismiss ();
	}

	private View showDialog (int titleid, int locid, View.OnClickListener listener)
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
		builder.setPositiveButton (R.string.tag_cf_ok, new CancelListener ());
		builder.setNegativeButton (R.string.tag_cf_cancel, new CancelListener ());
		
		dialog = builder.create ();
		dialog.show ();
		
		((AlertDialog) dialog).getButton (Dialog.BUTTON_POSITIVE).setOnClickListener (listener);
		
		
		return view;
	}
	
	private void delete (FontEntry fe)
	{
		FontDatabase.delete (this, fe);
		fla.invalidate ();
	}
	
	private void download (FontEntry fe)
	{
		Intent intent;
		
		intent = new Intent (this, WebReviewActivity.class);
		intent.setAction (WebReviewActivity.DOWNLOAD_ACTION);
		intent.setData (Uri.parse (fe.url));
		intent.putExtra (WebReviewActivity.EXTRA_DOWNLOAD_PREFIX, PREFIX);
		
		this.fe = fe;
			
		startActivityForResult (intent, 1);
	}

	@Override
	protected void onActivityResult (int reqCode, int resCode, Intent data)
	{
		switch (resCode) {
		case RESULT_CANCELED:
			break;
			
		case RESULT_OK:
			fe.filename = data.getStringExtra (WebReviewActivity.EXTRA_FILENAME);
			if (Typeface.createFromFile (fe.filename) != null) {
				FontDatabase.setAvailable (this, fe, true);
				fla.invalidate ();
			} else {
				new File (fe.filename).delete ();
				message (R.string.tag_invalid_font);
			}
		}				
	}
	
	private void message (int id)
	{
		Toast.makeText (this, getString (id), Toast.LENGTH_LONG).show ();		
	}
}
