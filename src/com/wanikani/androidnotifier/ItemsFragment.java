package com.wanikani.androidnotifier;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Vector;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

	
	private static class Level {
		
		public int level;
		
		public Level (int level)
		{
			this.level = level;
		}
		
	}
	
	class LevelClickListener implements AdapterView.OnItemClickListener {
	
		public void onItemClick (AdapterView<?> adapter, View view, int position, long id)
		{
			Level l;
			
			l = (Level) adapter.getItemAtPosition (position);
			setLevelFilter (l.level);
		}
		
	}
	
	class LevelListAdapter extends BaseAdapter {

		List<Level> levels;
		
		public LevelListAdapter ()
		{
			levels = new Vector<Level> ();
		}
		
		@Override
		public int getCount ()
		{
			return levels.size ();
		}
		
		@Override
		public Level getItem (int position)
		{
			return levels.get (position);
		}
		
		public void clear ()
		{
			levels = new Vector<Level> ();
		}
		
		public void replace (List<Level> levels)
		{
			clear ();
			this.levels.addAll (levels);
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
			TextView view;

			inflater = main.getLayoutInflater ();
			row = inflater.inflate (R.layout.items_level, parent, false);
			view = (TextView) row.findViewById (R.id.tgr_level);
			view.setText (Integer.toString (getItem (position).level));
			
			levelAdded (getItem (position).level, row);
			
			return row;
		}		
	}
	
	class ItemListAdapter extends BaseAdapter {

		LayoutInflater inflater;

		List<Item> items;
		
		Comparator<Item> cmp;

		public ItemListAdapter (Comparator<Item> cmp)
		{
			this.cmp = cmp;
			
			items = new Vector<Item> ();
			inflater = main.getLayoutInflater ();			
		}		

		public void setComparator (Comparator<Item> cmp)
		{
			this.cmp = cmp;
			invalidate ();
		}
		
		@Override
		public int getCount ()
		{
			return items.size ();
		}
		
		@Override
		public Item getItem (int position)
		{
			return items.get (position);
		}
		
		@Override
		public long getItemId (int position)
		{
			return position;			
		}
	
		public void clear ()
		{
			items.clear ();
			items.clear ();
		}
		
		public void addAll (List<Item> newItems)
		{
			items.addAll (newItems);
			invalidate ();
		}
		
		public void invalidate ()
		{		
			Collections.sort (items, cmp);
			notifyDataSetChanged ();
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
			TextView ktw, otw;
			TextView tw;
			
			otw = (TextView) row.findViewById (R.id.it_onyomi);
			otw.setText (kanji.onyomi);

			ktw = (TextView) row.findViewById (R.id.it_kunyomi);
			ktw.setText (kanji.kunyomi);
			
			switch (kanji.importantReading) {
			case ONYOMI:
				otw.setTextColor (importantColor);
				ktw.setTextColor (normalColor);
				break;

			case KUNYOMI:
				otw.setTextColor (normalColor);
				ktw.setTextColor (importantColor);
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
	
	class ItemClickListener implements AdapterView.OnItemClickListener {
		
		public void onItemClick (AdapterView<?> adapter, View view, int position, long id)
		{
			Intent intent;
			String url;
			Item i;
			
			i = (Item) adapter.getItemAtPosition (position);
			url = null;
			switch (i.type) {
			case RADICAL:
				url = "http://www.wanikani.com/vocabulary/" + i.character;
				break;
				
			case KANJI:
				url = "http://www.wanikani.com/kanji/" + i.character;
				break;
				
			case VOCABULARY:
				url = "http://www.wanikani.com/vocabulary/" + i.character;
				break;
			}
			
			if (url == null)
				return;	/* That's strange for sure */
			
			intent = new Intent (Intent.ACTION_VIEW);
			intent.setData (Uri.parse (url));
			
			startActivity (intent);
		}
		
	}

	class MenuPopupListener implements View.OnClickListener {
		
		public void onClick (View view)
		{
			boolean filterV, sortV;
			View filterW, sortW;
			
			filterW = parent.findViewById (R.id.menu_filter);
			filterV = filterW.getVisibility () == View.VISIBLE;

			sortW = parent.findViewById (R.id.menu_order);			
			sortV = sortW.getVisibility () == View.VISIBLE;
			
			filterW.setVisibility (View.GONE);
			sortW.setVisibility (View.GONE);
			
			switch (view.getId ()) {
			case R.id.btn_item_filter:
				if (!filterV)
					filterW.setVisibility (View.VISIBLE);
				break;

			case R.id.btn_item_sort:
				if (!sortV)
					sortW.setVisibility (View.VISIBLE);
				break;
				
			}
		}
	}
	
	class RadioGroupListener implements Button.OnClickListener {
		
		public void onClick (View view)
		{
			View filterW, sortW;
			
			filterW = parent.findViewById (R.id.menu_filter);
			sortW = parent.findViewById (R.id.menu_order);			
			
			filterW.setVisibility (View.GONE);
			sortW.setVisibility (View.GONE);
			
			switch (view.getId ()) {
			case R.id.btn_filter_all:
				setLevelFilter (currentLevel);
				break;

			case R.id.btn_filter_missing:
				setLevelFilter (currentLevel, true);
				break;
				
			case R.id.btn_filter_critical:
				setCriticalFilter ();
				break;

			case R.id.btn_filter_unlocks:
				setUnlockFilter ();
				break;

			case R.id.btn_sort_errors:
				iad.setComparator (Item.SortByErrors.INSTANCE);
				break;

			case R.id.btn_sort_srs:
				iad.setComparator (Item.SortBySRS.INSTANCE);
				break;
				
			case R.id.btn_sort_time:
				iad.setComparator (Item.SortByTime.INSTANCE);
				break;

			case R.id.btn_sort_type:
				iad.setComparator (Item.SortByType.INSTANCE);
			}			
		}
	}

	MainActivity main;

	View parent;
	
	LevelListAdapter lad;
	
	int currentLevel;
	
	boolean spinning;
	
	ListView lview;
	
	ItemListAdapter iad;
	
	ListView iview;
	
	LevelClickListener lcl;
	
	ItemClickListener icl;
	
	MenuPopupListener mpl;
	
	RadioGroupListener rgl;
	
	ItemLibrary<Item> critical;

	ItemLibrary<Item> unlocks;
	
	EnumMap<SRSLevel, Drawable> srsht;

	LevelFilter levelf;	

	CriticalFilter criticalf;	
	
	UnlockFilter unlockf;
	
	private Filter currentFilter;

	int levels;
	
	boolean apprentice;

	int normalColor;
	
	int importantColor;
	
	int selectedColor;
	
	int unselectedColor;
	
	public void setMainActivity (MainActivity main)
	{
		this.main = main;
	}
	
	@Override
	public void onCreate (Bundle bundle)
	{
		Resources res;

		super.onCreate (bundle);

		setRetainInstance (true);

		currentLevel = -1;
		levelf = new LevelFilter (this);
		criticalf = new CriticalFilter (this);
		unlockf = new UnlockFilter (this);
		currentFilter = null;
		
		lcl = new LevelClickListener ();
		icl = new ItemClickListener ();
		
		mpl = new MenuPopupListener ();
		rgl = new RadioGroupListener ();

		res = getResources ();
		srsht = new EnumMap<SRSLevel, Drawable> (SRSLevel.class);
    	srsht.put (SRSLevel.APPRENTICE, res.getDrawable (R.drawable.apprentice));
    	srsht.put (SRSLevel.GURU, res.getDrawable (R.drawable.guru));
    	srsht.put (SRSLevel.MASTER, res.getDrawable (R.drawable.master));
    	srsht.put (SRSLevel.ENLIGHTEN, res.getDrawable (R.drawable.enlighten));
    	srsht.put (SRSLevel.BURNED, res.getDrawable (R.drawable.burned));
    	
    	normalColor = res.getColor (R.color.normal);
    	importantColor = res.getColor (R.color.important);
    	selectedColor = res.getColor (R.color.selected);
    	unselectedColor = res.getColor (R.color.unselected);
	}
	
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle bundle) 
    {
		RadioGroup rg;
		ImageButton btn;
		int i;
		
		super.onCreateView (inflater, container, bundle);
		
		parent = inflater.inflate(R.layout.items, container, false);
		
		lad = new LevelListAdapter ();
		lview = (ListView) parent.findViewById (R.id.lv_levels);
		lview.setAdapter (lad);
		lview.setOnItemClickListener (lcl);

		iad = new ItemListAdapter (Item.SortByType.INSTANCE);
		iview = (ListView) parent.findViewById (R.id.lv_items);
		iview.setAdapter (iad);
		iview.setOnItemClickListener (icl);
		
		btn = (ImageButton) parent.findViewById (R.id.btn_item_filter);
		btn.setOnClickListener (mpl);
		btn = (ImageButton) parent.findViewById (R.id.btn_item_sort);
		btn.setOnClickListener (mpl);
		
		rg = (RadioGroup) parent.findViewById (R.id.rg_filter);
		for (i = 0; i < rg.getChildCount (); i++)
			rg.getChildAt (i).setOnClickListener (rgl);
		rg.check (R.id.btn_filter_all);
		
		rg = (RadioGroup) parent.findViewById (R.id.rg_order);
		for (i = 0; i < rg.getChildCount (); i++)
			rg.getChildAt (i).setOnClickListener (rgl);
		rg.check (R.id.btn_sort_type);
		
    	return parent;
    }
	
	@Override
	public void onResume ()
	{
		super.onResume ();

		refreshComplete (main.getDashboardData ());
	}
	
	public void refreshComplete (DashboardData dd)
	{
		List<Level> l;
		Level level;
		int i, clevel;
		
		/* This must be done as soon as possible */
		levels = dd.level;
		
		if (!isResumed ())
			return;
		
		clevel = currentLevel > 0 ? currentLevel : dd.level;
		
		l = new Vector<Level> (dd.level);		
		for (i = dd.level; i > 0; i--) {
			level = new Level (i);
			l.add (level);
		}
		lad.replace (l);
		lad.notifyDataSetChanged ();
		
		setLevelFilter (clevel);
	}
	
	@Override
	public void onDetach ()
	{
		super.onDetach ();
		
		currentFilter = null;
		
		levelf.stopTask ();
		criticalf.stopTask ();
		unlockf.stopTask ();
}
	
	void setData (List<Item> list, boolean ok)
	{
		iad.clear ();
		iad.addAll (list);
		iad.notifyDataSetChanged ();
	}

	private void setLevelFilter (int level)
	{
		if (currentFilter != levelf)
			apprentice = false;
		setLevelFilter (level, apprentice);
	}

	private void setLevelFilter (int level, boolean apprentice)
	{
		RadioGroup fg;
		
		fg = (RadioGroup) parent.findViewById (R.id.rg_filter);
		fg.check (apprentice ? R.id.btn_filter_missing : R.id.btn_filter_all); 
		
		this.apprentice = apprentice;
		currentFilter = levelf;
		levelf.select (level, apprentice);
	}
	
	private void setCriticalFilter ()
	{
		RadioButton btn;
		
		btn = (RadioButton) parent.findViewById (R.id.btn_filter_critical); 
		btn.setSelected (true);

		currentFilter = criticalf;
		criticalf.select ();
	}

	private void setUnlockFilter ()
	{
		RadioButton btn;
		
		btn = (RadioButton) parent.findViewById (R.id.btn_filter_unlocks); 
		btn.setSelected (true);

		currentFilter = unlockf;
		unlockf.select (unlockf);
	}

	void addData (Filter sfilter, List<Item> list)
	{
		if (sfilter != currentFilter)
			return;
		iad.addAll (list);
		iad.notifyDataSetChanged ();
	}

	void clearData ()
	{
		iad.clear ();
		iad.notifyDataSetChanged ();
	}
	
	/**
	 * Show or hide the spinner.
	 * @param enable true if should be shown
	 */
	public void spin (boolean enable)
	{
		/* empty */
	}
	
	void selectOtherFilter (Filter filter, boolean selected, boolean spinning)
	{
		View filterPB, filterBtn, levels;
		
		if (filter != currentFilter)
			return;
		
		filterBtn = parent.findViewById (R.id.btn_item_filter);
		filterPB = parent.findViewById (R.id.pb_item_filter);
		levels = parent.findViewById (R.id.lv_levels);
		
		if (spinning) {
			filterBtn.setVisibility (View.GONE);
			filterPB.setVisibility (View.VISIBLE);
		} else {
			filterBtn.setVisibility (View.VISIBLE);
			filterPB.setVisibility (View.GONE);			
		}
		
		levels.setVisibility (selected ? View.GONE : View.VISIBLE);
	}
	
	void selectLevel (Filter filter, int level, boolean spinning)
	{
		if (filter != currentFilter)
			return;
		
		if (currentLevel != level && currentLevel > 0)
			spin (filter, currentLevel, false, false);
		
		currentLevel = level;
		this.spinning = spinning;
		
		spin (filter, level, true, spinning);
	}

	public void levelAdded (int level, View row)
	{
		if (currentLevel == level)
			spin (currentFilter, row, true, spinning);
		else
			spin (currentFilter, row, false, false);
	}
	
	protected void spin (Filter filter, int level, boolean select, boolean spin)
	{
		View row;
		
		row = lview.getChildAt (levels - level);
		if (row != null) 	/* May happen if the level is not visible */
			spin (filter, row, select, spin);
	}

	private void spin (Filter filter, View row, boolean select, boolean spin)
	{
		TextView tw;
		View sw;
		
		selectOtherFilter (filter, false, false);
		
		tw = (TextView) row.findViewById (R.id.tgr_level);
		sw = row.findViewById (R.id.pb_level);
		if (spin) {
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

	Connection getConnection ()
	{
		return main.getConnection ();				
	}
	
	public int getName ()
	{
		return R.string.tag_items;
	}
}
