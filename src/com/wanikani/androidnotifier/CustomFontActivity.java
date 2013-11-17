package com.wanikani.androidnotifier;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.wanikani.androidnotifier.db.FontDatabase;
import com.wanikani.androidnotifier.db.FontDatabase.FontEntry;

public class CustomFontActivity extends Activity {

	class FontHolder {
		
		FontEntry fe;
		
		CheckBox cbv;
		
		TextView namev;
		
		TextView samplev;
		
		public FontHolder (View view)
		{
			cbv = (CheckBox) view.findViewById (R.id.cf_enabled);
			namev = (TextView) view.findViewById (R.id.cf_font);
			samplev = (TextView) view.findViewById (R.id.cf_font_sample);
		}
		
		public void fill (FontEntry fe)
		{
			this.fe = fe;
			
			cbv.setChecked (fe.enabled);
			namev.setText (fe.name);
			samplev.setTypeface (fe.face);
		}
	}
	
	class FontListAdapter extends BaseAdapter {

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
			}

			holder.fill (entry);
			
			return row;
		}	
		
		public void rescan ()
		{
			fonts = FontDatabase.getAllFonts (CustomFontActivity.this);
		}
	}
	
	ListView flist;
	
	FontListAdapter fla;
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);

		setContentView (R.layout.customfont);
		
		fla = new FontListAdapter ();
		flist = (ListView) findViewById (R.id.cf_samples);
		flist.setAdapter (fla);
	}
	
}
