package com.wanikani.androidnotifier;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.wanikani.wklib.Item;
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

/**
 * This fragment shows item lists. The user can choose how to filter
 * and how to sort them. Sorting is done through a simple comparator;
 * stock comparators provided by WKLib are enough for our purposes.
 * Filters is slightly more complex, and we use an implementation 
 * of a specific interface (@link Filter) for each of them. 
 */
public class ItemsFragment extends Fragment implements Tab, Filter.Callback {

	/**
	 * This enum represents the kind of additional information to display on
	 * each item's description. Since this app should run on small handests
	 * too, the room is not much, so the kind of string returned depends
	 * a lot on the kind of ordering the user chose.
	 * Note also that the string may depend on the filter, since they
	 * use different WK APIs, returning different subsets of the data schema.
	 */
	enum ItemInfo {
		
		/**
		 * Display days elapsed since unlock date. If the item has not
		 * been unlocked yet (or if unlock date is not available), 
		 * it returns an empty string.
		 */
		AGE {
			public String getInfo (Resources res, Item i)
			{
				Date unlock;
				long now, age;

				unlock = i.getUnlockedDate ();
				if (unlock == null)
					return ""; /* Not unlocked, or not available */
				
				now = System.currentTimeMillis ();
				age = now - unlock.getTime ();

				/* Express age in minutes */
				age /= 60 * 1000;
				if (age < 60)
					return res.getString (R.string.fmt_ii_less_than_one_hour);
				
				/* Express age in hours */
				age = Math.round (((float) age) / 60);
				if (age == 1) 
					return res.getString (R.string.fmt_ii_one_hour);
				if (age < 24) 
					return res.getString (R.string.fmt_ii_hours, age);
				
				/* Express age in days */
				age = Math.round (((float) age) / 24);
				if (age == 1) 
					return res.getString (R.string.fmt_ii_one_day);

				return res.getString (R.string.fmt_ii_days, age);
			}
		},
		
		/**
		 * Display time before next review. If the item has not
		 * been unlocked yet (or if available date is not available), 
		 * it returns an empty string.
		 */
		AVAILABLE {
			public String getInfo (Resources res, Item i)
			{
				Date date;
				long now, fw;
				
				date = i.getAvailableDate ();
				if (date == null)
					return "";
				
				now = System.currentTimeMillis ();
				fw = date.getTime () - now;

				if (fw <= 0)
					return res.getString (R.string.fmt_ni_now);
				
				/* Express fw in seconds */				
				fw /= 1000;
				if (fw < 60)
					return res.getString (R.string.fmt_ni_less_than_one_minute);

				/* Express fw in minutes */
				fw /= 60;
				if (fw == 1)
					return res.getString (R.string.fmt_ni_one_minute);
				if (fw < 60)
					return res.getString (R.string.fmt_ni_minutes, fw);
				
				/* Express fw in hours */
				fw = Math.round (((float) fw) / 60);
				if (fw == 1) 
					return res.getString (R.string.fmt_ni_one_hour);
				if (fw < 24) 
					return res.getString (R.string.fmt_ni_hours, fw);
				
				/* Express age in days */
				fw = Math.round (((float) fw) / 24);
				if (fw == 1) 
					return res.getString (R.string.fmt_ni_one_day);
				if (fw < 30)
					return res.getString (R.string.fmt_ni_days, fw);

				fw /= 30;
				if (fw == 1) 
					return res.getString (R.string.fmt_ni_one_month);
				
				return res.getString (R.string.fmt_ni_months, fw);
			}
		},

		/**
		 * Displays the percentage of correct answers.
		 * If intermediate (meaning & reading) stats are available, they
		 * are displayed too.
		 */
		ERRORS {
			public String getInfo (Resources res, Item i)
			{
				int pmean, pread;
				String simple;
				
				if (i.percentage < 0)
					return "";
				
				simple = res.getString (R.string.fmt_ii_percent, i.percentage);
				if (i.stats == null || 
					i.stats.reading == null || i.stats.meaning == null)
					return simple;
				
				if ((i.stats.meaning.correct + i.stats.meaning.incorrect) == 0)
					return simple;
				if ((i.stats.reading.correct + i.stats.reading.incorrect) == 0)
					return simple;
				
				pmean = i.stats.meaning.correct * 100 / 
						(i.stats.meaning.correct + i.stats.meaning.incorrect);
				pread = i.stats.reading.correct * 100 / 
						(i.stats.reading.correct + i.stats.reading.incorrect);
				
				return res.getString (R.string.fmt_ii_percent_full,
									  i.percentage, pmean, pread);
			}
		};
		
		/**
		 * Returns a string which describes the item.
		 * @param res the resource container
		 * @param i the item to be described
		 * @return a description
		 */
		public abstract String getInfo (Resources res, Item i);		
	}

	/**
	 * A simple wrapper of a level item, to be associated to the ListView. 
	 * Currently only the level number is used, so this structure is 
	 * almost pointless... 
	 */
	private static class Level {

		/// The level number
		public int level;
		
		/**
		 * Constructor
		 * @param level the level number
		 */
		public Level (int level)
		{
			this.level = level;
		}
		
	}
	
	/**
	 * This listener is registered to the level's ListView.
	 * When an item is clicked, all the items of that level are displayed.
	 */
	class LevelClickListener implements AdapterView.OnItemClickListener {
	
		public void onItemClick (AdapterView<?> adapter, View view, int position, long id)
		{
			Level l;
			
			l = (Level) adapter.getItemAtPosition (position);
			setLevelFilter (l.level);
		}
		
	}
	
	/**
	 * The implementation of the levels' ViewList. Pretty straightforward.
	 * No sorting or filtering.
	 * Levels are instances of the {@link Level} class (though I guess 
	 * Integers could have been used as well.
	 */
	class LevelListAdapter extends BaseAdapter {

		/// The current level set
		List<Level> levels;
		
		/// A position to view dictionary
		Map<Integer, View> l2v;
		
		/**
		 * Constructor.
		 */
		public LevelListAdapter ()
		{
			levels = new Vector<Level> ();
			l2v = new Hashtable<Integer, View> ();
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
		
		/**
		 * Deletes all the items in the list
		 */
		public void clear ()
		{
			levels = new Vector<Level> ();
			notifyDataSetChanged ();
		}

		/**
		 * Replaces the items in the list with a new set.
		 * @param levels the new list
		 * @return if the new levels are different from the old ones
		 */
		public boolean replace (List<Level> levels)
		{
			if (this.levels.size () == levels.size ())
				return false;
			
			clear ();
			this.levels.addAll (levels);
			notifyDataSetChanged ();
			
			return true;
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
			Level l;

			l = getItem (position);
			inflater = main.getLayoutInflater ();
			row = inflater.inflate (R.layout.items_level, parent, false);			
			view = (TextView) row.findViewById (R.id.tgr_level);
			view.setText (Integer.toString (l.level));
			
			row.setTag (l);
			l2v.put (l.level, row);
			
			levelAdded (getItem (position).level, row);
			
			return row;
		}
		
		/**
		 * Return the view associated to a given level
		 * @param level the level
		 * @return a view, or <tt>null</tt> if no view is (currently) associated
		 */
		public View getViewByLevel (int level)
		{
			View view;
			Level l;
			
			view = l2v.get (level);
			if (view != null) {
				l = (Level) view.getTag ();
				if (l.level != level)
					view = null;
			}
			
			return view;
		}
	}
	
	/**
	 * The item list holder. Keeps all the references to relevant views and
	 * takes care of updating the fields of each row. It is meant to be set
	 * as a tag (@see {@link View#setTag(Object)}, according to the holder pattern,
	 * though this is not done directly by this class.
	 */
	private abstract class ItemListHolder {
		
		/// The item type
		public Item.Type type;
		
		/// The row view, parent of each field
		public View row;
		
		/// Glyph, as a text
		public TextView glyphText;
				
		/// Reading
		public TextView reading;
		
		/// SRS image
		public ImageView srs;
		
		/// Additional (filter/order dependent) information
		public TextView info;
		
		/// Meanining
		public TextView meaning;
		
		/// Level
		public TextView level;
		
		/// The scroll view, needed fro scroll interaction
		public HiPriorityScrollView hpsw;
		
		/// The click listener associated to the row
		public ItemClickListener icl;
		
		/**
		 * Constructor
		 * @param ila the list adapter that will receive high priority scroll view
		 * events
		 * @param inflater the inflater
		 * @param parent scroll view
		 * @param type the item type represented by this holder
		 */
		public ItemListHolder (ItemListAdapter ila, LayoutInflater inflater, 
							   ViewGroup parent, Item.Type type)		
		{
			this.type = type;

			row = inflater.inflate (getLayout (), parent, false);			
			glyphText = (TextView) row.findViewById (R.id.it_glyph);
			reading = (TextView) row.findViewById (R.id.it_reading);
			srs = (ImageView) row.findViewById (R.id.img_srs);
			info = (TextView) row.findViewById (R.id.it_info);
			meaning = (TextView) row.findViewById (R.id.it_meaning);
			hpsw = (HiPriorityScrollView) row.findViewById (R.id.hsv_item);
			level = (TextView) row.findViewById (R.id.it_level);
			
			icl = new ItemClickListener (ila);
			hpsw.setCallback (icl);
			row.setOnClickListener (icl);
			
			if (jtf != null)
				glyphText.setTypeface (jtf);			
		}
		
		/**
		 * Returns the resource ID of the layout representing this object
		 * @return the layout id
		 */
		protected abstract int getLayout ();
		
		/**
		 * Updates the row fields.
		 * @param item the item
		 * @param iinfo additional (filter/sort order dependent) information
		 */
		public void fill (Item item, String iinfo)
		{
			if (currentFilter != levelf)
				level.setText (Integer.toString (item.level));				
			
			if (item.stats != null) {
				srs.setImageDrawable (srsht.get (item.stats.srs));
				srs.setVisibility (View.VISIBLE);
			} else
				srs.setVisibility (View.INVISIBLE);
			
			info.setText (iinfo);			
			meaning.setText (showAnswers ? item.meaning : "");
			
			icl.setURL (item.getURL ());
		}
	}
	
	/**
	 * Implementation of the holder for radical item rows.
	 */
	private class RadicalHolder extends ItemListHolder {

		/// The glyph, as an image
		ImageView glyphImage;

		/// Glyph view
		View glyphView;
		
		/**
		 * Constructor.
		 * @param ila the list adapter that will receive high priority scroll view
		 * events
		 * @param inflater the inflater
		 * @param parent scroll view
		 */
		public RadicalHolder (ItemListAdapter ila, LayoutInflater inflater, ViewGroup parent)
		{
			super (ila, inflater, parent, Item.Type.RADICAL);
			
			glyphImage = (ImageView) row.findViewById (R.id.img_glyph);			
			glyphView = row.findViewById (R.id.f_glyph);						
		}
		
		@Override
		protected int getLayout ()
		{
			return R.layout.items_radical;
		}
		
		@Override
		public void fill (Item item, String iinfo)
		{
			Radical radical;
			
			radical = (Radical) item;
			
			super.fill (radical, iinfo);
			
			if (radical.character != null) {
				glyphText.setText (radical.character);
				glyphText.setVisibility (View.VISIBLE);
				glyphView.setVisibility (View.GONE);
			} else {
				try {
					glyphImage.setImageBitmap (rimg.getImage (getActivity (), radical));
					glyphText.setVisibility (View.GONE);
					glyphView.setVisibility (View.VISIBLE);
				} catch (IOException e) {
					/* Should not happen */
				}
			} 

		}
		
	}

	/**
	 * Implementation of the holder for kanji item rows.
	 */
	private class KanjiHolder extends ItemListHolder {

		public TextView onyomi;
		
		public TextView kunyomi;
		
		/**
		 * Constructor.
		 * @param ila the list adapter that will receive high priority scroll view
		 * events
		 * @param inflater the inflater
		 * @param parent scroll view
		 */
		public KanjiHolder (ItemListAdapter ila, LayoutInflater inflater, ViewGroup parent)
		{
			super (ila, inflater, parent, Item.Type.KANJI);
			
			onyomi = (TextView) row.findViewById (R.id.it_onyomi);
			kunyomi = (TextView) row.findViewById (R.id.it_kunyomi);
		}
		
		@Override
		protected int getLayout ()
		{
			return R.layout.items_kanji;
		}
		
		@Override
		public void fill (Item item, String iinfo)
		{
			Kanji kanji;
			
			kanji = (Kanji) item;
			
			super.fill (kanji, iinfo);
			
			onyomi.setText (showAnswers ? kanji.onyomi : "");

			kunyomi.setText (showAnswers ? kanji.kunyomi : "");
			
			switch (kanji.importantReading) {
			case ONYOMI:
				onyomi.setTextColor (importantColor);
				kunyomi.setTextColor (normalColor);
				break;

			case KUNYOMI:
				onyomi.setTextColor (normalColor);
				kunyomi.setTextColor (importantColor);
				break;
			}

			glyphText.setText (kanji.character);
		}
		
	}

	/**
	 * Implementation of the holder for vocab item rows.
	 */
	private class VocabHolder extends ItemListHolder {
		
		/**
		 * Constructor.
		 * @param ila the list adapter that will receive high priority scroll view
		 * events
		 * @param inflater the inflater
		 * @param parent scroll view
		 */
		public VocabHolder (ItemListAdapter ila, LayoutInflater inflater, ViewGroup parent)
		{
			super (ila, inflater, parent, Item.Type.VOCABULARY);			
		}
		
		@Override
		protected int getLayout ()
		{
			return R.layout.items_vocab;
		}
		
		@Override
		public void fill (Item item, String iinfo)
		{
			Vocabulary vocab;
			
			vocab = (Vocabulary) item;
			
			super.fill (vocab, iinfo);
			
			reading.setText (showAnswers ? vocab.kana : "");

			glyphText.setText (vocab.character);
		}
		
	}

	/**
	 * The implementation of the items' ViewList. Items are instances
	 * of the WKLib {@link Item} class. This class implements sorting
	 * and filtering through {@link ItemSearchDialog}.
	 */
	class ItemListAdapter extends BaseAdapter implements ItemSearchDialog.Listener {

		/// The full (unfiltered) list of items.
		List<Item> allItems;
		
		/// The current list of items. It is always sorted.
		List<Item> filteredItems;

		/// The current comparator
		Comparator<Item> cmp;
		
		/// What to put into the "extra info" textview
		ItemInfo iinfo;
		
		/// If set, tabs should be locked, because the user is swiping
		/// a row larger than screen size
		boolean lock;
		
		/**
		 * Constructor
		 * @param cmp the comparator
		 * @param iinfo what to put into the "extra info" textview
		 */
		public ItemListAdapter (Comparator<Item> cmp, ItemInfo iinfo)
		{
			this.cmp = cmp;
			this.iinfo = iinfo;
			
			allItems = new Vector<Item> ();
			filteredItems = new Vector<Item> ();
		}		

		/**
		 * Changes the comparator. Ordinarily the extra info is strictly
		 * bound to the comparator, so we give a chance to update it as well.
		 * @param cmp the comparator
		 * @param iinfo what to put into the "extra info" textview
		 */
		public void setComparator (Comparator<Item> cmp, ItemInfo iinfo)
		{
			this.cmp = cmp;
			this.iinfo = iinfo;

			invalidate ();
		}
		
		@Override
		public void filterChanged ()
		{
			invalidate ();
		}
		
		@Override
		public int getCount ()
		{
			return filteredItems.size ();
		}
		
		@Override
		public Item getItem (int position)
		{
			return filteredItems.get (position);
		}
		
		@Override
		public long getItemId (int position)
		{
			return position;			
		}
	
		/**
		 * Empties the list
		 */
		public void clear ()
		{
			allItems.clear ();
			filteredItems.clear ();
			notifyDataSetChanged ();
		}

		/**
		 * Appends the items to the list. Afterward the list is sorted again.
		 * @param newItems the additional items to show
		 */
		public void addAll (List<Item> newItems)
		{
			allItems.addAll (newItems);
			invalidate ();
		}
		
		/**
		 * Sorts the collection again, also refreshing the list.
		 */
		private void invalidate ()
		{		
			filteredItems = isd != null ? isd.filter (allItems) : allItems;  
			Collections.sort (filteredItems, cmp);
			notifyDataSetChanged ();
		}
		
		@Override
		public View getView (int position, View row, ViewGroup parent) 
		{
			LayoutInflater inflater;
			ItemListHolder holder;
			Item item;

			item = getItem (position);
			holder = row != null ? (ItemListHolder) row.getTag () : null;
			if (holder == null || holder.type != item.type) {
				inflater = main.getLayoutInflater ();			

				switch (item.type) {
				case RADICAL:
					holder = new RadicalHolder (this, inflater, parent);
					break;
					
				case KANJI:
					holder = new KanjiHolder (this, inflater, parent);
					break;
					
				case VOCABULARY:
					holder = new VocabHolder (this, inflater, parent);
					break;					
				}
				
				holder.row.setTag (holder);
			}

			holder.fill (item, iinfo.getInfo (getResources (), item));
			
			return holder.row;
		}		
	}
	
	/**
	 * The listener registered to the filter and sort buttons.
	 * It shows or hide the menu, according to the well-known
	 * menu pattern. 
	 */
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
				
			case R.id.btn_item_view:
				toggleShowAnswers ();
				break;
				
			case R.id.btn_item_search:
				if (isd != null)
					isd.toggleVisibility ();
			}
		}
	}
	
	/**
	 * The listener registered to the filter/sort radio group buttons.
	 * When an item is clicked, it updates the list accordingly.
	 */
	class RadioGroupListener implements View.OnClickListener {
		
		public void onClick (View view)
		{
			View filterW, sortW;

			filterW = parent.findViewById (R.id.menu_filter);
			sortW = parent.findViewById (R.id.menu_order);			
			
			filterW.setVisibility (View.GONE);
			sortW.setVisibility (View.GONE);
			
			switch (view.getId ()) {

			case R.id.btn_filter_none:
				sortByType ();
				setNoFilter ();
				break;			
			
			case R.id.btn_filter_by_level:
				sortByType ();
				setLevelFilter (currentLevel);
				break;

			case R.id.btn_filter_critical:
				sortByErrors ();
				setCriticalFilter ();
				break;

			case R.id.btn_filter_unlocks:
				sortByTime ();
				setUnlockFilter ();
				break;

			case R.id.btn_sort_errors:
				sortByErrors ();
				break;

			case R.id.btn_sort_srs:
				sortBySRS ();
				break;
				
			case R.id.btn_sort_time:
				sortByTime ();
				break;
				
			case R.id.btn_sort_available:
				sortByAvailable ();
				break;
				
			case R.id.btn_sort_type:
				sortByType ();
			}
		}
		
		/**
		 * Switches to SRS sort order, fixing both ListView and radio buttons.
		 */
		private void sortBySRS ()
		{
			RadioGroup rg;
			
			rg = (RadioGroup) parent.findViewById (R.id.rg_order);
			rg.check (R.id.btn_sort_srs);
			iad.setComparator (Item.SortBySRS.INSTANCE, ItemInfo.AVAILABLE);				
		}
		
		/**
		 * Switches to age sort order, fixing both ListView and radio buttons.
		 */
		private void sortByTime ()
		{
			RadioGroup rg;
			
			rg = (RadioGroup) parent.findViewById (R.id.rg_order);
			rg.check (R.id.btn_sort_time);
			iad.setComparator (Item.SortByTime.INSTANCE, ItemInfo.AGE);				
		}

		/**
		 * Switches to next review sort order, fixing both ListView and radio buttons.
		 */
		private void sortByAvailable ()
		{
			RadioGroup rg;
			
			rg = (RadioGroup) parent.findViewById (R.id.rg_order);
			rg.check (R.id.btn_sort_available);
			iad.setComparator (Item.SortByAvailable.INSTANCE, ItemInfo.AVAILABLE);				
		}

		/**
		 * Switches to errors sort order, fixing both ListView and radio buttons.
		 */
		private void sortByErrors ()
		{
			RadioGroup rg;
			
			rg = (RadioGroup) parent.findViewById (R.id.rg_order);
			rg.check (R.id.btn_sort_errors);
			iad.setComparator (Item.SortByErrors.INSTANCE, ItemInfo.ERRORS);
		}

		/**
		 * Switches to type sort order, fixing both ListView and radio buttons.
		 */
		private void sortByType ()
		{
			RadioGroup rg;
			
			rg = (RadioGroup) parent.findViewById (R.id.rg_order);
			rg.check (R.id.btn_sort_type);
			iad.setComparator (Item.SortByType.INSTANCE, ItemInfo.AVAILABLE);
		}

	}

	/**
	 * The listener registered to each item's hyperlink. It opens
	 * the page, through @link {@link MainActivity#item()}, so
	 * it uses the internal browser if the user chose so.
	 * It is both a {@link HiPriorityScrollView#Callback}, to intercept
	 * clicks on the scroll view, and a {@link View#OnClickListener},
	 * to intercept them when they fall out of the scroll view.
	 * It also handles lock and unlock events. 
	 */
	class ItemClickListener implements HiPriorityScrollView.Callback, View.OnClickListener {
		
		/// The list adapter, that will receive lock and unlock events
		ItemListAdapter ila;
				
		/// The URL to open
		private String url;
		
		/**
		 * Constructor.
		 * @param ila the item list adapter
		 */
		public ItemClickListener (ItemListAdapter ila)
		{
			this.ila = ila;
		}
		
		/**
		 * Updates the URL to open
		 * @param url the new URL to open
		 */
		public void setURL (String url)
		{
			this.url = url;
		}
		
		
		@Override
		public void onClick (View view)
		{
			if (url != null)
				main.item (url);
		}
		
		/**
		 * Called when a motion on an item starts. If the item is actually
		 * larger than the screen, we lock the tabs
		 * @param hpsw the item's scroll view
		 * @param childIsLarger set if the item is actually too large 
		 */
		@Override
		public void down (HiPriorityScrollView hpsw, boolean childIsLarger) 
		{
			ila.lock = childIsLarger;
		}
		
		/**
		 * Called when a motion on an item stops. We unlock.
		 * @param hpsw the item's scroll view
		 * @param childIsLarger set if the item is actually too large 
		 */
		@Override
		public void up (HiPriorityScrollView hpsw, boolean childIsLarger)
		{
			ila.lock = false;
			onClick (hpsw);
		}

		/**
		 * Called when a motion on an item stops. We unlock.
		 * @param hpsw the item's scroll view
		 * @param childIsLarger set if the item is actually too large 
		 */
		@Override
		public void cancel (HiPriorityScrollView hpsw, boolean childIsLarger)
		{
			ila.lock = false;
		}
	}

	/// The show all key
	private static final String KEY_SHOW_ANSWERS = "SHOW_ANSWERS";
	
	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;

	/* ---------- Levels stuff ---------- */
	
	/// The list adapter of the levels' list
	LevelListAdapter lad;
	
	/// The levels' list view
	ListView lview;

	/// The levels' list click listener
	LevelClickListener lcl;
	
	/// The last level clicked so far
	int currentLevel;
	
	/// The number of levels
	int levels;

	/// Normal reading color
	int normalColor;
	
	/// Important reading color
	int importantColor;
	
	/// Selected level color
	int selectedColor;
	
	/// Unselected level color
	int unselectedColor;
	
	/* ---------- Items stuff ---------- */

	/// The list adapter of the items' list
	ItemListAdapter iad;
	
	/// The items' list view
	ListView iview;
	
	/// The radical images cache
	RadicalImages rimg;
	
	/* ---------- Sort/filter stuff ---------- */

	/// The popup menu listener
	MenuPopupListener mpl;
	
	/// The menu buttons' listener
	RadioGroupListener rgl;
	
	/// True if the "other filters" button is spinning
	boolean spinning;

	/// A SRS level to turtle icon map
	EnumMap<SRSLevel, Drawable> srsht;

	/// Show sensitive information that may break SRS
	boolean showAnswers;
	
	/// Item Search State
	ItemSearchDialog.State iss;
	
	/// Item Search dialog (null when detached)
	ItemSearchDialog isd;

	/* ---------- Filters ---------- */
	
	/// The "no filter" instance
	NoFilter nof;
	
	/// The level filter instance
	LevelFilter levelf;	

	/// The critical items filter instance
	CriticalFilter criticalf;	
	
	/// The recent unlocks items filter instance
	UnlockFilter unlockf;

	/// The current filter
	private Filter currentFilter;
	
	/// The japanese typeface
	private Typeface jtf;
	
	/// Need to restart refresh
	private boolean resumeRefresh;
	
	/// The japanese typeface path
	private final String JAPANESE_TYPEFACE_FONT = "/system/fonts/MTLmr3m.ttf";
	
	public ItemsFragment ()
	{
		rimg = new RadicalImages ();
		
		try {
			jtf = Typeface.createFromFile (JAPANESE_TYPEFACE_FONT);
		} catch (Exception e) {
			jtf = null;
		}
		
		currentLevel = -1;		
	}
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;

		this.main.register (this);			
	}
	
	/**
	 * Creation of the fragment. We build up all the singletons, leaving the
	 * bundle alone, because we set the retain instance flag to <code>true</code>.
	 * 	@param bundle the saved instance state
	 */
	@Override
	public void onCreate (Bundle bundle)
	{
		Resources res;

		super.onCreate (bundle);

		setRetainInstance (true);

		nof = new NoFilter (this);
		levelf = new LevelFilter (this);
		criticalf = new CriticalFilter (this);
		unlockf = new UnlockFilter (this);
		currentFilter = levelf;
		
		lcl = new LevelClickListener ();
		
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

    	lad = new LevelListAdapter ();
		iad = new ItemListAdapter (Item.SortByType.INSTANCE, ItemInfo.AVAILABLE);
		
		iss = new ItemSearchDialog.State ();
	}
	
	/**
	 * Builds the GUI and create the default item listing. Which is
	 * by item type, sorted by time.
	 * @param inflater the inflater
	 * @param container the parent view
	 * @param savedInstance an (unused) bundle
	 */
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle bundle) 
    {
		RadioGroup rg;
		ImageButton btn;
		View sview;
		int i;
		
		super.onCreateView (inflater, container, bundle);
		
		parent = inflater.inflate(R.layout.items, container, false);
		
		lview = (ListView) parent.findViewById (R.id.lv_levels);
		lview.setAdapter (lad);
		lview.setOnItemClickListener (lcl);

		iview = (ListView) parent.findViewById (R.id.lv_items);
		iview.setAdapter (iad);
		
		sview = parent.findViewById (R.id.it_search_win);
		isd = new ItemSearchDialog (sview, iss, currentFilter, iad);
				
		btn = (ImageButton) parent.findViewById (R.id.btn_item_filter);
		btn.setOnClickListener (mpl);
		btn = (ImageButton) parent.findViewById (R.id.btn_item_sort);
		btn.setOnClickListener (mpl);
		btn = (ImageButton) parent.findViewById (R.id.btn_item_view);
		btn.setOnClickListener (mpl);
		btn = (ImageButton) parent.findViewById (R.id.btn_item_search);
		btn.setOnClickListener (mpl);
		
		rg = (RadioGroup) parent.findViewById (R.id.rg_filter);
		for (i = 0; i < rg.getChildCount (); i++)
			rg.getChildAt (i).setOnClickListener (rgl);
		rg.check (R.id.btn_filter_none);
		
		rg = (RadioGroup) parent.findViewById (R.id.rg_order);
		for (i = 0; i < rg.getChildCount (); i++)
			rg.getChildAt (i).setOnClickListener (rgl);
		rg.check (R.id.btn_sort_type);
		
		enableSorting (true, true, true);
				
    	return parent;
    }
	
	/**
	 * Called when the app is resumed. We need to (re?)build the list views.
	 */
	public void onResume ()
	{
		super.onResume ();

		SharedPreferences prefs;
		
		prefs = SettingsActivity.prefs (getActivity ());
		showAnswers = prefs.getBoolean (KEY_SHOW_ANSWERS, true);
		
		/* Make sure that refreshCompleted has been called at least once */
		if (levels > 0)
			redrawAll ();
	}
	
	/**
	 * Redraws the entire GUI. Called when resuming, to build the new view.
	 */
	private void redrawAll ()
	{
		boolean refresh;
		List<Level> l;
		Level level;
		int i;
		
		/* Very first resume. Need to set current level == to the user's level */
		if (currentLevel < 0)
			currentLevel = levels;
		
		l = new Vector<Level> (levels);		
		for (i = levels; i > 0; i--) {
			level = new Level (i);
			l.add (level);
		}
		
		refresh = lad.replace (l); 
		if (refresh)
			lad.notifyDataSetChanged ();
		
		refresh |= iad.isEmpty ();
		
		refresh |= resumeRefresh;
		
		if (refresh) {
			if (currentFilter == nof)
				setNoFilter ();
			else if (currentFilter == levelf)
				setLevelFilter (currentLevel);
			else if (currentFilter == criticalf)
				setCriticalFilter ();
			else if (currentFilter == unlockf)
				setUnlockFilter ();
		}
	}
	
	/**
	 * Called when data has been refreshed. Actually the only field we
	 * are intested in is the user's level, so we store it even if it
	 * the view has not been created yet.  
	 */
	public void refreshComplete (DashboardData dd)
	{				
		levels = dd.level;
		if (currentLevel < 0 && isResumed ())
			redrawAll ();
	}
	
	/**
	 * Called when the view is destroyed. We take this chance to stop all
	 * the threads that may attempt to update a dead view. 
	 */
	@Override
	public void onDestroyView ()
	{
		super.onDetach ();
		
		resumeRefresh = currentFilter.stopTask ();
		
		nof.stopTask ();
		levelf.stopTask ();
		criticalf.stopTask ();
		unlockf.stopTask ();
		isd = null;
	}

	/**
	 * Switched to "no filter" view
	 */
	public void setNoFilter ()
	{
		RadioButton btn;
		
		currentFilter = nof;

		if (parent != null) {
			btn = (RadioButton) parent.findViewById (R.id.btn_filter_none); 
			btn.setChecked (true);

			nof.select (main.getConnection ());
			iview.setSelection (0);
		}
		
		filterChanged ();
	}
			
	/**
	 * Switches to level list filter. 
	 * @param level the level to display 
	 */
	public void setLevelFilter (int level)
	{
		RadioGroup fg;
		
		currentFilter = levelf;

		if (parent != null) {
			fg = (RadioGroup) parent.findViewById (R.id.rg_filter);
			fg.check (R.id.btn_filter_by_level); 
		
			levelf.select (main.getConnection (), level);
			iview.setSelection (0);
		}
		
		filterChanged ();
	}
	
	/**
	 * Switches to critical items filter. 
	 */
	public void setCriticalFilter ()
	{
		RadioButton btn;
		
		currentFilter = criticalf;

		if (parent != null) {
			btn = (RadioButton) parent.findViewById (R.id.btn_filter_critical); 
			btn.setChecked (true);

			criticalf.select (main.getConnection ());
			iview.setSelection (0);
		}
		
		filterChanged ();
	}

	/**
	 * Switches to recent unlocks filter. 
	 */
	public void setUnlockFilter ()
	{
		RadioButton btn;
		
		currentFilter = unlockf;

		if (parent != null) {
			btn = (RadioButton) parent.findViewById (R.id.btn_filter_unlocks); 
			btn.setChecked (true);

			unlockf.select (main.getConnection ());
			iview.setSelection (0);
		}
		
		filterChanged ();
	}
	
	/**
	 * Called when the filter changes
	 */
	protected void filterChanged ()
	{
		if (isd != null)
			isd.itemFilterChanged (currentFilter);
	}
	
	/**
	 * Toggle show answer flag
	 */
	protected void toggleShowAnswers ()
	{
		SharedPreferences prefs;
		Context ctxt;
		
		ctxt = getActivity ();
		if (ctxt == null)
			return;
		
		showAnswers ^= true;
		
		prefs = SettingsActivity.prefs (ctxt);
		prefs.edit ().putBoolean (KEY_SHOW_ANSWERS, showAnswers).commit ();
		
		if (iad != null)
			iad.filterChanged ();
	}

	/**
	 * Replaces the contents of the items' list view contents with a new list.
	 * @param sfilter the source filter 
	 * @param list the new list
	 * @param ok if this is the complete list or something went wrong while
	 * loading data
	 */
	@Override
	public void setData (Filter sfilter, List<Item> list, boolean ok)
	{
		if (sfilter != currentFilter)
			return;

		iad.clear ();
		iad.addAll (list);
		iad.notifyDataSetChanged ();
		
		alert (ok);
	}

	/**
	 * Updates the contents of the items' list view contents, by adding
	 * new items.
	 * @param sfilter the source filter 
	 * @param list the new list
	 * @param ok if this is the complete list or something went wrong while
	 * loading data
	 */
	@Override
	public void addData (Filter sfilter, List<Item> list)
	{
		if (sfilter != currentFilter)
			return;
		iad.addAll (list);
		iad.notifyDataSetChanged ();
	}

	@Override
	public void clearData (Filter sfilter)
	{
		if (sfilter != currentFilter)
			return;

		iad.clear ();
		iad.notifyDataSetChanged ();
	}
	
	/**
	 * Called at the end of data retrieval. It shows or hides the alert info.  
	 * @param sfilter the filter which is publishing these items
	 * @param ok tells if this is the complete list or something went wrong while
	 * loading data
	 */
	@Override
	public void noMoreData (Filter sfilter, boolean ok)
	{
		if (sfilter != currentFilter)
			return;
		
		alert (ok);
	}
	
	@Override
	public void loadRadicalImage (Radical r)
		throws IOException
	{
		rimg.getImage (getActivity (), r);
	}
	
	/**
	 * Displays or hides an alert message.
	 * @param ok if true, data retrieval was ok, otherwise is partial
	 */
	protected void alert (boolean ok)
	{
		TextView message;
		View panel;
		
		panel = parent.findViewById (R.id.it_lay_alert);
		if (!ok) {
			panel.setVisibility (View.VISIBLE);
			message = (TextView) parent.findViewById (R.id.it_alert);
			message.setText (getResources ().getString (R.string.status_msg_partial));
		} else
			panel.setVisibility (View.GONE);
	}
	
	/**
	 * Show or hide the dashboard data spinner. 
	 * Needed to implement the @param Tab interface, but we actually ignore this.
	 * @param enable true if should be shown
	 */
	public void spin (boolean enable)
	{
		/* empty */
	}

	@Override
	public void selectLevel (Filter filter, int level, boolean spinning)
	{
		if (filter != currentFilter)
			return;
		
		if (currentLevel != level && currentLevel > 0)
			selectLevel (currentLevel, false, false);
		
		currentLevel = level;
		this.spinning = spinning;
		
		selectLevel (level, true, spinning);
	}

	/**
	 * Internal implementation of the {@link #selectLevel(Filter, int, boolean)}
	 * logic. This gets called when it is certain that we are being called
	 * by the correct filter, or no filter is involved, so checks are skipped.
	 * In addition, this method may be called also to <i>unselect</i> a level.
	 * @param level the level on which to act
	 * @param select <code>true</code> if the level should be selected.
	 * 	Otherwise it is unselected
	 * @param spin <code>true</code> if the spinner should be displayed.
	 * 	Otherwise it is hidden. Makes sense only if <code>select</code>
	 *  is <code>true</code> too
	 */
	protected void selectLevel (int level, boolean select, boolean spin)
	{
		View row;

		row = lad.getViewByLevel (level);
		selectLevel (row, select, spin);
	}

	/**
	 * Internal implementation of the {@link #selectLevel(Filter, int, boolean)}
	 * logic. This gets called when it is certain that we are being called
	 * by the correct filter, or no filter is involved, so checks are skipped.
	 * In addition, this method may be called also to <i>unselect</i> a level.
	 * @param row the level row on which to act. This parameter may be <code>null</code>,
	 *  if the label is not visible. Calling the method is still important, to
	 *  show the levels list, if hidden
	 * @param select <code>true</code> if the level should be selected.
	 * 	Otherwise it is unselected
	 * @param spin <code>true</code> if the spinner should be displayed.
	 * 	Otherwise it is hidden. Makes sense only if <code>select</code>
	 *  is <code>true</code> too
	 */
	private void selectLevel (View row, boolean select, boolean spin)
	{
		TextView tw;
		View sw;
		
		selectOtherFilter (false, false);
		
		if (row == null)
			return;
		
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

	@Override
	public void selectOtherFilter (Filter filter, boolean spinning)
	{
		if (filter != currentFilter)
			return;
		
		selectOtherFilter (true, spinning);
	}
	
	/**
	 * Internal implementation of the {@link #selectOtherFilter(Filter, boolean)}
	 * logic. This gets called when it is certain that we are being called
	 * by the correct filter, or no filter is involved, so checks are skipped.
	 * In addition, this method may be called also to <i>exit</i> filter mode.
	 * When entering filter mode, the level list is hidden.
	 * @param select <code>true</code> if the level should be selected.
	 * 	Otherwise it is unselected
	 * @param spin <code>true</code> if the spinner should be displayed.
	 * 	Otherwise it is hidden. Makes sense only if <code>select</code>
	 *  is <code>true</code> too
	 */
	public void selectOtherFilter (boolean selected, boolean spinning)
	{
		View filterPB, levels, filler;
		
		filterPB = parent.findViewById (R.id.pb_item_filter);
		levels = parent.findViewById (R.id.lv_levels);
		filler = parent.findViewById (R.id.lv_filler);
		
		if (spinning)
			filterPB.setVisibility (View.VISIBLE);
		else
			filterPB.setVisibility (View.GONE);			
		
		if (selected) {
			levels.setVisibility (View.GONE);
			filler.setVisibility (View.VISIBLE);
		} else {
			levels.setVisibility (View.VISIBLE);
			filler.setVisibility (View.GONE);
		} 
	}
	
	@Override
	public void enableSorting (boolean errors, boolean unlock, boolean available)
	{
		View view;
		
		view = parent.findViewById (R.id.btn_sort_errors);
		view.setEnabled (errors);

		view = parent.findViewById (R.id.btn_sort_time);
		view.setEnabled (unlock);
		
		view = parent.findViewById (R.id.btn_sort_available);
		view.setEnabled (available);
	}
	
	/**
	 * This methods is called by the levels' list adapter, right before
	 * displaying a new row. It is needed to make sure that the spinner
	 * is displayed on the newly created row if it should.
	 * @param level the level being created
	 * @param row the row being created
	 */
	public void levelAdded (int level, View row)
	{
		if (currentLevel == level)
			selectLevel (row, true, spinning);
		else
			selectLevel (row, false, false);
	}
	
	/**
	 * Returns the tab name ID.
	 * @param the <code>tag_items</code> ID
	 */
	public int getName ()
	{
		return R.string.tag_items;
	}

	/**
	 * Clears the cache and redisplays data. This may be called quite early,
	 * so we check the null pointers. 
	 */
	@Override
	public void flush (Tab.RefreshType rtype)
	{
		/* Might be called really early! */
		if (criticalf == null)
			return;
		
		switch (rtype) {
		case LIGHT:
			break;
			
		case FULL:
			nof.flush ();
			levelf.flush ();

			if (currentFilter == nof)
				nof.select (main.getConnection ());
			else if (currentFilter == levelf && currentLevel > 0)
				levelf.select (main.getConnection (), currentLevel);

			/* Fall through */

		case MEDIUM:
			criticalf.flush ();
			unlockf.flush ();
			
			if (currentFilter == criticalf)
				criticalf.select (main.getConnection ());
			else if (currentFilter == unlockf)
				unlockf.select (main.getConnection ());
			
		}
	}

	/**
	 * Tells if we are interested in scroll events. We do, if the user is swiping
	 * an item list
	 * @return true if the user is swiping
	 */
	public boolean scrollLock ()
	{
		 return iad != null && iad.lock;
	}
	
	/**
	 * Shows the search dialog.
	 * @param switching <tt>true</tt> if the activity was in background
	 * @param level the level to be selected (or <tt>null</tt> if no SRS filter should be set)
	 * @param type the item type to be selected (or <tt>null</tt> if all types should
	 * be shown).
	 */
	public void showSearchDialog (boolean switching, SRSLevel level, Item.Type type)
	{
		if (level != null)
			iss.set (level);
		if (type != null)
			iss.set (type);
		
		isd.show (!switching);
	}

	/**
	 * Hides the search dialog.
	 */
	public void hideSearchDialog ()
	{
		isd.setVisibility (false);
	}

	/**
	 * The back button is handled only if the search dialog is shown.
	 * In that case, it is hidden
	 * 	@return false
	 */
	@Override
	public boolean backButton ()
	{
		return isd.setVisibility (false);
	}

	@Override
    public boolean contains (Contents c)
	{
		return c == Contents.ITEMS;
	}
}
