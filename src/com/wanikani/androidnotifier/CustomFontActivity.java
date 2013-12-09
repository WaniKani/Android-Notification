package com.wanikani.androidnotifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wanikani.androidnotifier.db.FontDatabase;
import com.wanikani.androidnotifier.db.FontDatabase.FontEntry;
import com.wanikani.androidnotifier.db.FontDatabase.WellKnownFont;

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
			
			updateChecks ();
		}
		
		private void updateChecks ()
		{
			FontEntry sys;
			boolean nonsys;
			
			rescan ();
			nonsys = false;
			sys = null;
			for (FontEntry fe : fonts) {
				if (WellKnownFont.SYSTEM.is (fe))
					sys = fe;
				else if (fe.enabled)
					nonsys = true;
			}
			if (!nonsys && sys != null) {
				FontDatabase.setEnabled (CustomFontActivity.this, sys, true);
				sys.enabled = true;
				notifyDataSetChanged ();
			}
		}
		
		public void rescan ()
		{
			fonts = FontDatabase.getFonts (CustomFontActivity.this);
		}
	}
	
	abstract class ImportDialog implements View.OnClickListener {

		Button button;
		
		View dview;
		
		int titleid, locid;
		
		TextView namevw;
		
		TextView locvw;
		
		private final String PREFIX = ImportDialog.class.getName () + ".";
		
		private final String KEY_ID = PREFIX + "ID";

		private final String KEY_NAME = PREFIX + "NAME";
		
		private final String KEY_LOC = PREFIX + "LOC";
		
		public ImportDialog (Button button, int titleid, int locid)
		{
			this.button = button;
			this.titleid = titleid;
			this.locid = locid;
		}
		
		public void saveInstanceState (Bundle bundle)
		{
			bundle.putInt (KEY_ID, button.getId ());
			bundle.putString (KEY_NAME, namevw.getText ().toString ());
			bundle.putString (KEY_LOC, locvw.getText ().toString ());
		}
		
		public void restoreInstanceState (Bundle bundle)
		{
			if (!bundle.containsKey (KEY_ID) || bundle.getInt (KEY_ID) != button.getId ())
				return;
			
			onClick (button);
			
			namevw.setText (bundle.getString (KEY_NAME));
			locvw.setText (bundle.getString (KEY_LOC));
		}		
				
		@Override
		public void onClick (View view)
		{
			String name, loc;
			
			if (view == button) {
				dview = showDialog (titleid, locid, this);
				dialogShown (dview);
				return;				
			} 
			
			if (dview == null)
				return;
						
			name = namevw.getText ().toString ().trim ();
			loc = locvw.getText ().toString ().trim ();
			if (validate (name, loc)) { 
				dialog.dismiss ();				
				doImport (name, loc);
			}
		}
		
		protected void dialogShown (View dview)
		{
			namevw = ((TextView) dview.findViewById (R.id.fd_name));
			locvw = ((TextView) dview.findViewById (R.id.fd_location));			
		}
		
		protected boolean validate (String name, String loc)
		{
			if (name.length () == 0) {
				message (R.string.tag_empty_name);
				return false;
			}
			
			if (FontDatabase.exists (CustomFontActivity.this, name)) {
				message (R.string.tag_already_exists);
				return false;
			}
				
			return true;
		}
		
		public boolean handle (Uri uri)
		{
			return false;
		}
		
		protected abstract void doImport (String name, String loc);
	}
	
	class FontImportListener extends ImportDialog {
			
		public FontImportListener (Button button)
		{
			super (button, R.string.tag_cf_import, R.string.tag_cf_path);						
		}

		@Override
		protected void dialogShown (View dview)
		{
			super.dialogShown (dview);
			
			dview.findViewById (R.id.fd_hint).setVisibility (View.VISIBLE);
		}

		@Override
		protected boolean validate (String name, String loc)
		{
			if (!super.validate (name, loc))
				return false;
			
			if (!new File (loc).canRead ()) {
				message (R.string.tag_cant_read);
				return false;
			}

			return true;
		}
		
		@Override
		protected void doImport (String name, String loc)
		{						
			fe = new FontEntry (-1, name, loc, null, false, false, false);
			
			importFile (fe, false);		
		}
		
		@Override
		public boolean handle (Uri uri)
		{
			if (super.handle (uri))
				return true;
			
			if (uri.getScheme ().equals ("file")) {
				onClick (button);
				locvw.setText (uri.getPath ());				
				return true;
			}				
			
			return false;
		}		
	}
	
	class FontDownloadListener extends ImportDialog {
		
		public FontDownloadListener (Button button)
		{
			super (button, R.string.tag_cf_download, R.string.tag_cf_url);
		}
		
		@Override
		public boolean validate (String name, String loc)
		{
			if (!super.validate (name, loc))
				return false;
			
			try {
				new URL (loc);
			} catch (MalformedURLException e) {
				message (R.string.tag_bad_url);
				return false;
			}
			
			return true;
		}
		
		public void doImport (String name, String loc)
		{
			FontEntry fe;
			
			fe = new FontEntry (-1, name, null, loc, false, false, false);
			
			download (fe);
		}
		
		@Override
		public boolean handle (Uri uri)
		{
			if (super.handle (uri))
				return true;
			
			if (uri.getScheme ().equals ("http") || uri.getScheme ().equals ("https")) {
				onClick (button);
				locvw.setText (uri.toString ());				
				return true;
			}				
			
			return false;
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
	
	private static final String UNZIP_PREFIX = PREFIX + "uz-";

	ListView flist;
	
	FontListAdapter fla;
	
	Dialog dialog;
	
	ImportDialog idial;
	
	FontEntry fe;
	
	List<ImportDialog> idials;
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);
		
		ImportDialog idial;
		Button btn;

		idials = new Vector<ImportDialog> ();
		
		setContentView (R.layout.customfont);
		
		fla = new FontListAdapter ();
		flist = (ListView) findViewById (R.id.cf_samples);
		flist.setAdapter (fla);
		
		btn = (Button) findViewById (R.id.cf_import);
		idial = new FontImportListener (btn);
		idials.add (idial);
		btn.setOnClickListener (idial);
		
		btn = (Button) findViewById (R.id.cf_download);
		idial = new FontDownloadListener (btn);
		idials.add (idial);
		btn.setOnClickListener (idial);
		
		cleanup ();
		
		checkIntent ();
	}
	
	protected void checkIntent ()
	{
		String action;
		Intent intent;
		Uri uri;
		
		intent = getIntent ();
		
		action = intent.getAction ();
		if (action.equals (Intent.ACTION_VIEW) ||
		    action.equals (Intent.ACTION_EDIT)) {
			
			uri = intent.getData ();
			for (ImportDialog idial : idials)
				if (idial.handle (uri))
					break;
		}		
	}

	@Override
	protected void onSaveInstanceState (Bundle bundle)
	{
		super.onSaveInstanceState (bundle);
		if (idial != null && dialog.isShowing ())
			idial.saveInstanceState (bundle);
	}
	
	@Override
	protected void onRestoreInstanceState (Bundle bundle)
	{
		super.onRestoreInstanceState (bundle);
		
		for (ImportDialog idial : idials)
			idial.restoreInstanceState (bundle);												
	}
	
	@Override
	public void onPause ()
	{
		super.onPause ();

		if (dialog != null && dialog.isShowing ())
			dialog.dismiss ();
	}
	
	private void cleanup ()
	{
		List<FontEntry> fes;
		Set<String> used;
		File dir;
		
		fes = FontDatabase.getFonts (this);
		used = new HashSet<String> ();
		for (FontEntry fe : fes)
			if (fe.filename != null)
				used.add (new File (fe.filename).getName ());
		
		dir = getFilesDir ();
		for (File f : dir.listFiles ()) {
			if (f.getName ().startsWith (PREFIX) && !used.contains (f.getName ()))
				f.delete ();
		}
	}

	private View showDialog (int titleid, int locid, ImportDialog idial)
	{
		LayoutInflater inflater;
		AlertDialog.Builder builder;
		TextView lview;
		View view;
					
		this.idial = idial;

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
		
		((AlertDialog) dialog).getButton (Dialog.BUTTON_POSITIVE).setOnClickListener (idial);
		
		
		return view;
	}
	
	private void delete (FontEntry fe)
	{
		if (fe.url != null)
			new File (fe.filename).delete ();
		
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
	
	protected boolean extract (FontEntry pfe, ZipInputStream zis, ZipEntry ze, String filename)
	{
		FontEntry fe;
		OutputStream os;
		String ename;
		boolean ok;
		File output;
		byte buf [];
		int rd;
		
		ename = ze.getName ();
		ename = new File (ename).getName ();			
		
		output = null;
		os = null;
		ok = false;
		try {
			output = new File (getFilesDir (), filename);
			os = new FileOutputStream (output);
			buf = new byte [1024];
			while (true) {
				rd = zis.read (buf);
				if (rd < 0)
					break;
				os.write (buf, 0, rd);			
			}
			os.close ();

			fe = new FontEntry (-1, pfe.name + "; " + ename, output.getAbsolutePath (), 
								 pfe.url, false, true, false);
			ok = fe.load () != null && FontDatabase.insertFixDuplicates (this, fe, true);			
		} catch (Exception e) {
			/* empty */
		} finally {
			if (!ok)
				output.delete ();
		}
		
		return ok;
	}
	
	protected boolean readAsArchive (FontEntry fe)
	{
		ZipInputStream zis;
		File entry;		
		long suffix;
		ZipEntry ze;
		boolean once;
		
		suffix = System.currentTimeMillis ();		
		zis = null;
		once = false;
		try {
			zis = new ZipInputStream (new FileInputStream (fe.filename));
			while (true) {
				ze = zis.getNextEntry ();
				if (ze == null)
					break;
				
				once |= extract (fe, zis, ze, UNZIP_PREFIX + (suffix++));
			}
		} catch (Exception e) {
			return once;
		} finally {
			try {
				if (zis != null)
					zis.close ();
			} catch (Exception e) {
				/* empty */
			}
		}
		
		return once;
	}

	protected void importFile (FontEntry fe, boolean downloaded)
	{
		boolean archive;

		if (fe.load () != null) {
			fe.load ();
			FontDatabase.setAvailable (this, fe, true);
			fla.invalidate ();
		} else {
			archive = readAsArchive (fe);				
			
			if (downloaded)
				new File (fe.filename).delete ();
			
			if (archive)
				fla.invalidate ();
			else
				message (R.string.tag_invalid_font);
		}		
	}
	
	@Override
	protected void onActivityResult (int reqCode, int resCode, Intent data)
	{
		switch (resCode) {
		case RESULT_CANCELED:
			break;
			
		case RESULT_OK:
			fe.filename = data.getStringExtra (WebReviewActivity.EXTRA_FILENAME);
			importFile (fe, true);
		}				
	}
	
	private void message (int id)
	{
		Toast.makeText (this, getString (id), Toast.LENGTH_LONG).show ();		
	}
}
