package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
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

public class ItemsFragment extends Fragment implements Tab {

	private class LoadLevelTask extends AsyncTask<Void, ItemLibrary<Item>, Boolean > {
		
		Connection conn;
		
		Level level;
		
		public LoadLevelTask (Connection conn, Level level)
		{
			this.conn = conn;
			this.level = level;

		}
		
		@Override
		protected void onPreExecute ()
		{
			spin (level, true, false);
		}
		
		@Override
		protected Boolean doInBackground (Void... v)
		{
			ItemLibrary<Item> lib;
			List<Radical> imgrad;
			Radical rad;
			Iterator<Item> i;
			
			boolean ok;
			
			ok = true;
			lib = new ItemLibrary<Item> ();
			imgrad = new Vector<Radical> ();
			try {
				lib.addAll (conn.getRadicals (level.level));
				i = lib.list.iterator ();
				while (i.hasNext ()) {
					rad = (Radical) i.next ();
					if (rad.character == null) {
						imgrad.add (rad);
						i.remove ();
					}
				}
				publishProgress (new ItemLibrary<Item> (lib));
			} catch (IOException e) {
				ok = false;
			}
			
			for (Radical r : imgrad) {
				try {
					conn.loadImage (r);
				} catch (IOException e) {
					r.character = "?";
					ok = false;
				}				
				publishProgress (new ItemLibrary<Item> (r));
			}
			
			lib = new ItemLibrary<Item> ();
			try {
				lib.addAll (conn.getKanji (level.level));
				publishProgress (lib);
			} catch (IOException e) {
				ok = false;
			}
			
			lib = new ItemLibrary<Item> ();
			try {
				lib.addAll (conn.getVocabulary (level.level));
				publishProgress (lib);
			} catch (IOException e) {
				ok = false;
			}			

			return ok;
		}	
		
		@Override
		protected void onProgressUpdate (ItemLibrary<Item>... lib)
		{
			if (llt == this) {
				iad.addAll (lib [0].list);
				iad.notifyDataSetChanged ();
			}
		}
						
		@Override
		protected void onPostExecute (Boolean ok)
		{
			if (llt == this)
				spin (level, false, true);
		}
	}

	static class Level {
		
		public int level;
		
		public int position;
		
		public Level (int position, int level)
		{
			this.level = level;
			this.position = position;
		}
		
	}
	
	class LevelClickListener implements AdapterView.OnItemClickListener {
	
		public void onItemClick (AdapterView<?> adapter, View view, int position, long id)
		{
			select ((Level) adapter.getItemAtPosition (position));
		}
		
	}
	
	class LevelListAdapter extends ArrayAdapter<Level> {

		public LevelListAdapter (List<Level> data)
		{
			super (main, R.layout.items_level, data);
		}		
		
		@Override
		public View getView (int position, View row, ViewGroup parent) 
		{
			LayoutInflater inflater;
			TextView view;

			inflater = main.getLayoutInflater ();
			row = inflater.inflate (R.layout.items_level, parent, false);
			view = (TextView) row.findViewById (R.id.tgr_level);
			view.setText (Integer.toString (getItem (position).level));
			
			return row;
		}		
	}
	
	class ItemListAdapter extends BaseAdapter {

		LayoutInflater inflater;

		Vector<Item> items;		

		public ItemListAdapter ()
		{
			items = new Vector<Item> ();
			inflater = main.getLayoutInflater ();
		}		

		@Override
		public int getCount ()
		{
			return items.size ();
		}
		
		@Override
		public Item getItem (int position)
		{
			return items.elementAt (position);
		}
		
		@Override
		public long getItemId (int position)
		{
			return position;			
		}
	
		public void clear ()
		{
			items.clear ();
		}
		
		public void addAll (List<Item> newItems)
		{
			items.addAll (newItems);
		}
		
		protected void fillRadical (View row, Radical radical)
		{
			ImageView iw;
			TextView tw;
			
			tw = (TextView) row.findViewById (R.id.it_glyph);
			iw = (ImageView) row.findViewById (R.id.img_glyph);
			if (radical.character != null) {
				tw = (TextView) row.findViewById (R.id.it_glyph);
				tw.setText (radical.character);
				tw.setVisibility (View.VISIBLE);
				iw.setVisibility (View.GONE);
			} else if (radical.bitmap != null) {		
				iw.setImageBitmap (radical.bitmap);
				tw.setVisibility (View.GONE);
				iw.setVisibility (View.VISIBLE);				
			} /* else { should never happen! } */

		}

		protected void fillKanji (View row, Kanji kanji)
		{
			TextView ktw, ytw;
			TextView tw;
			
			ktw = (TextView) row.findViewById (R.id.it_onyomi);
			ktw.setText (kanji.onyomi);

			ytw = (TextView) row.findViewById (R.id.it_kunyomi);
			ytw.setText (kanji.kunyomi);
			
			switch (kanji.importantReading) {
			case ONYOMI:
				ytw.setTextColor (importantColor);
				ktw.setTextColor (normalColor);
				break;

			case KUNYOMI:
				ktw.setTextColor (importantColor);
				ytw.setTextColor (normalColor);
				break;
			}

			tw = (TextView) row.findViewById (R.id.it_glyph);
			tw.setText (kanji.character);
		}
		
		protected void fillVocab (View row, Vocabulary vocab)
		{
			TextView tw;
			
			tw = (TextView) row.findViewById (R.id.it_reading);
			tw.setText (vocab.kana);

			tw = (TextView) row.findViewById (R.id.it_glyph);
			tw.setText (vocab.character);
		}

		@Override
		public View getView (int position, View row, ViewGroup parent) 
		{
			ImageView iw;
			TextView tw;
			Item item;

			item = getItem (position);
			switch (item.type) {
			case RADICAL:
				row = inflater.inflate (R.layout.items_radical, parent, false);
				fillRadical (row, (Radical) item);
				break;
									
			case KANJI:
				row = inflater.inflate (R.layout.items_kanji, parent, false);
				fillKanji (row, (Kanji) item);
				break;

			case VOCABULARY:
				row = inflater.inflate (R.layout.items_vocab, parent, false);
				fillVocab (row, (Vocabulary) item);
				break;
			}

			if (item.stats != null) {
				iw = (ImageView) row.findViewById (R.id.img_srs);
				iw.setImageDrawable (srsht.get (item.stats.srs));
			}
			
			tw = (TextView) row.findViewById (R.id.it_meaning);
			tw.setText (item.meaning);
			
			return row;
		}		
	}

	DashboardData dd;
	
	MainActivity main;

	View parent;
	
	LevelListAdapter lad;
	
	ListView lview;
	
	ItemListAdapter iad;
	
	ListView iview;
	
	LevelClickListener lcl;
	
	Hashtable<Integer, ItemLibrary<Item>> ht;
	
	EnumMap<SRSLevel, Drawable> srsht;
	
	LoadLevelTask llt;
	
	int normalColor;
	
	int importantColor;
	
	int selectedColor;
	
	int unselectedColor;

	Level oldLevel;
	
	public void setMainActivity (MainActivity main)
	{
		this.main = main;
	}
	
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
		Resources res;
		
		ht = new Hashtable<Integer, ItemLibrary<Item>> ();
		srsht = new EnumMap<SRSLevel, Drawable> (SRSLevel.class); 

		parent = inflater.inflate(R.layout.items, container, false);
    	lcl = new LevelClickListener ();
    	
    	if (dd != null)
    		refreshComplete (dd);
    	
    	res = getResources ();
    	srsht.put (SRSLevel.APPRENTICE, res.getDrawable (R.drawable.apprentice));
    	srsht.put (SRSLevel.GURU, res.getDrawable (R.drawable.guru));
    	srsht.put (SRSLevel.MASTER, res.getDrawable (R.drawable.master));
    	srsht.put (SRSLevel.ENLIGHTEN, res.getDrawable (R.drawable.enlighten));
    	srsht.put (SRSLevel.BURNED, res.getDrawable (R.drawable.burned));
    	
    	normalColor = res.getColor (R.color.normal);
    	importantColor = res.getColor (R.color.important);
    	selectedColor = res.getColor (R.color.selected);
    	unselectedColor = res.getColor (R.color.unselected);
    	    	    	
    	return parent;
    }
		
	public void refreshComplete (DashboardData dd)
	{
		DashboardData ldd;
		
		ldd = this.dd;
		this.dd = dd;
		if (parent == null)
			return;	
		
		if (lad == null || ldd == null || ldd.level != dd.level)
			updateLevelsList ();
	}
	
	protected void updateLevelsList ()
	{
		List<Level> l;
		int i, j;
		
		l = new Vector<Level> (dd.level);		
		for (i = dd.level, j = 0; i > 0; i--)
			l.add (new Level (j++, i));
		
		lad = new LevelListAdapter (l);
		lview = (ListView) parent.findViewById (R.id.lv_levels);
		lview.setAdapter (lad);
		lview.setOnItemClickListener (lcl);

		iad = new ItemListAdapter ();
		iview = (ListView) parent.findViewById (R.id.lv_items);
		iview.setAdapter (iad);
	}
	
	
	@Override
	public void onDetach ()
	{
		super.onDetach ();
		
		llt = null;
	}

	protected void select (Level level)
	{
		if (oldLevel != null)
			spin (oldLevel, false, false);
		
		oldLevel = level;

		iad.clear ();
		iad.notifyDataSetChanged ();
		
		llt = new LoadLevelTask (main.getConnection (), level);
		llt.execute ();
	}
	
	protected ItemLibrary<Item> getLibrary (Level level)
	{
		ItemLibrary<Item> ans;
		
		ans = ht.get (level.level);
		if (ans == null) {
			try {
				ans = main.getConnection ().getItems (level.level);				
			} catch (IOException e) {
				return null;
			}
			ht.put (level.level, ans);
		}
		
		return ans;
	}
	
	/**
	 * Show or hide the spinner.
	 * @param enable true if should be shown
	 */
	public void spin (boolean enable)
	{
		/* empty */
	}
	
	public int getName ()
	{
		return R.string.tag_items;
	}
	
	protected void spin (Level level, boolean enable, boolean select)
	{
		TextView tw;
		View row, sw;
		
		row = lview.getChildAt (level.position);
		
		tw = (TextView) row.findViewById (R.id.tgr_level);
		sw = row.findViewById (R.id.pb_level);
		if (enable) {
			tw.setVisibility (View.GONE);
			sw.setVisibility (View.VISIBLE);
		} else {
			tw.setVisibility (View.VISIBLE);
			sw.setVisibility (View.GONE);
		}
		if (select) {
			tw.setTextColor (selectedColor);
			tw.setTypeface (null, Typeface.BOLD);
		} else {
			tw.setTextColor (unselectedColor);
			tw.setTypeface (null, Typeface.NORMAL);			
		}
	}
	
}
