package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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

	static class Level {
		
		public int level;
		
		public Level (int level)
		{
			this.level = level;
		}
		
	}
	
	class LevelClickListener implements AdapterView.OnItemClickListener {
	
		View oldView;
		
		public void onItemClick (AdapterView<?> adapter, View view, int position, long id)
		{
			if (oldView != null)
				oldView.setSelected(false);
			
			view.setSelected (true);
			oldView = view;			
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
	
	class ItemListAdapter extends ArrayAdapter<Item> {

		public ItemListAdapter (List<Item> data)
		{
			super (main, R.layout.items_radical, data);
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
			} else {
				/* Need to set image URI */
				tw.setVisibility (View.GONE);
				iw.setVisibility (View.VISIBLE);				
			}

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
			LayoutInflater inflater;
			ImageView iw;
			TextView tw;
			Item item;

			inflater = main.getLayoutInflater ();
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
	
	ItemListAdapter iad;
	
	LevelClickListener lcl;
	
	Hashtable<Integer, ItemLibrary<Item>> ht;
	
	EnumMap<SRSLevel, Drawable> srsht; 
	
	int normalColor;
	
	int importantColor;
		
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
		ListView lw;
		int i;
		
		l = new Vector<Level> (dd.level);
		for (i = dd.level; i > 0; i--)
			l.add (new Level (i));

		lad = new LevelListAdapter (l);
		lw = (ListView) parent.findViewById (R.id.lv_levels);
		lw.setAdapter (lad);
		lw.setOnItemClickListener (lcl);
	}
	
	protected void select (Level level)
	{
		ItemLibrary<Item> lib;
		ListView lw;
		
		lib = getLibrary (level);
		if (lib != null) {
			iad = new ItemListAdapter (lib.list);
			lw = (ListView) parent.findViewById (R.id.lv_items);
			lw.setAdapter (iad);
		}
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
	
}
