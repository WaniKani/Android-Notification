package com.wanikani.androidnotifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.wanikani.wklib.Item;
import com.wanikani.wklib.SRSLevel;

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
 * The search window that appears at the bottom of the items tab.
 * This could have been part of the the {@link ItemsFragment} class, 
 * however it's already big enough, so moving to its own class makes
 * things a little bit more understandable.
 */
public class ItemSearchDialog {

	/**
	 * Implementations of this class receive notifications when the filter change. 
	 */
	public interface Listener {

		/**
		 * Called when any detail of the filter changes.
		 */
		public void filterChanged ();
		
	}
	
	/**
	 * The current state of the filter.
	 */
	public static class State {

		/**
		 * Which item types match the filter criteria.
		 */
		private EnumMap<Item.Type, Boolean> types;
		
		/**
		 * Which SRS levels match the filter criteria.
		 */
		public EnumMap<SRSLevel, Boolean> srses;
		
		/**
		 * A search string. Its interpretation is up to the calling class,
		 * however the idea is to be matched against any textual part of an item.
		 */
		public String filter;
		
		/**
		 * If set, the dialog is visible (and the filter applied)
		 */
		public boolean visible;
		
		/**
		 * Constructor.
		 */
		public State ()
		{
			types = new EnumMap<Item.Type, Boolean> (Item.Type.class);
			srses = new EnumMap<SRSLevel, Boolean> (SRSLevel.class);
			
			for (Item.Type t : Item.Type.values ())
				types.put (t, true);

			for (SRSLevel srs : SRSLevel.values ())
				srses.put (srs, true);
		}				
		
		/**
		 * Toggles the filter on an item type. 
		 */
		public void toggle (Item.Type type)
		{
			types.put (type, !types.get (type));
		}
		
		/**
		 * Toggles the filter on an SRS level. 
		 */
		public void toggle (SRSLevel level)
		{
			srses.put (level, !srses.get (level));
		}				
	}
	
	/**
	 * The listener attached to item type filter buttons. 
	 */
	private class TypeButtonListener 
		implements View.OnClickListener, View.OnLongClickListener {
	
		/// The item type represented by this button
		Item.Type type;
	
		/**
		 * Constructor.
		 * @param type the item type
		 */
		public TypeButtonListener (Item.Type type)
		{
			this.type = type;
		}
	
		/**
		 * Click event handler. Toggle the filter and notify the event. 
		 * @param view the button
		 */
		@Override
		public void onClick (View view)
		{
			iss.toggle (type);
			setActive (type);
			
			updateFilter ();
		}
		
		/**
		 * Long click event handler. Disable all the other item types
		 * and notify the event
		 * @param view the button
		 */
		@Override
		public boolean onLongClick (View view)
		{
			for (Item.Type type : Item.Type.values ())
				iss.types.put (type, false);
		
			iss.types.put (type, true);
		
			syncFromISS ();

			updateFilter ();
		
			return true;
		}
	}

	/**
	 * The listener attached to SRS level filter buttons. 
	 */
	private class SRSButtonListener 
		implements View.OnClickListener, View.OnLongClickListener {
		
		/// The SRS level represented by this button
		SRSLevel level;
		
		/**
		 * Constructor.
		 * @param level the SRS level
		 */
		public SRSButtonListener (SRSLevel level)
		{
			this.level = level;
		}
		
		/**
		 * Click event handler. Toggle the filter and notify the event. 
		 * @param view the button
		 */
		@Override
		public void onClick (View view)
		{
			iss.toggle (level);
			setActive (level);

			updateFilter ();
		}
		
		/**
		 * Long click event handler. Disable all the other SRS levels
		 * and notify the event
		 * @param view the button
		 */
		@Override
		public boolean onLongClick (View view)
		{
			for (SRSLevel srs : SRSLevel.values ())
				iss.srses.put (srs, false);
			
			iss.srses.put (level, true);
			
			syncFromISS ();
			
			updateFilter ();

			return true;
		}
		
	}
	
	/**
	 * Listener to changes of the text box.
	 */
	private class FilterWatcher implements TextWatcher {
		
		@Override
		public void afterTextChanged (Editable s)
		{
			updateFilter ();
		}
		
		@Override
		public void beforeTextChanged (CharSequence c, int start, int before, int count)
		{
			/* empty */
		}
		
		@Override
		public void onTextChanged (CharSequence c, int start, int before, int count)
		{
			/* empty */
		}
		
	}

	/**
	 * A structur holding the selected and deselected images for each 
	 * filter button.
	 */
	private class Images {
		
		/// Drawable to be displayed when the filter is not applied
		public Drawable deselected;
		
		/// Drawable to be displayed when the filter is applied
		public Drawable selected;
		
		/**
		 * Constructor
		 * @param res the resources
		 * @param deselected deselected drawable id
		 * @param selected selected drawable id
		 */
		public Images (Resources res, int deselected, int selected)
		{
			this.deselected = res.getDrawable (deselected);
			this.selected = res.getDrawable (selected);
		}
		
	}
	
	/// The complete filter state
	private State iss;
	
	/// The dialog view
	private View view;
	
	/// The text box
	private EditText filter;
	
	/// A mapping between item types and their buttons
	private EnumMap<Item.Type, ImageButton> typeButtons;
	
	/// A mapping between SRS levels and their buttons
	private EnumMap<SRSLevel, ImageButton> srsButtons;

	/// A mapping between item types and their images
	private EnumMap<Item.Type, Images> typeImages;

	/// A mapping between SRS levels and their images
	private EnumMap<SRSLevel, Images> srsImages;
	
	/// A reference to the object to notify when the filter changes
	private Listener listener;

	/**
	 * Constructor
	 * @param view the view
	 * @param iss the current state
	 * @param listener the filter changes listener
	 */
	public ItemSearchDialog (View view, State iss, Listener listener)
	{
		Resources res;
		
		this.iss = iss;
		this.view = view;
		this.listener = listener;
		
		res = view.getResources ();
		
		filter = (EditText) view.findViewById (R.id.et_filter);
		filter.addTextChangedListener (new FilterWatcher ());
		
		typeButtons = new EnumMap<Item.Type, ImageButton> (Item.Type.class);
		typeButtons.put (Item.Type.RADICAL, 
						 (ImageButton) view.findViewById (R.id.btn_radicals_filter));
		typeButtons.put (Item.Type.KANJI, 
				 		 (ImageButton) view.findViewById (R.id.btn_kanji_filter));
		typeButtons.put (Item.Type.VOCABULARY, 
						 (ImageButton) view.findViewById (R.id.btn_vocab_filter));

		srsButtons = new EnumMap<SRSLevel, ImageButton> (SRSLevel.class);
		srsButtons.put (SRSLevel.APPRENTICE, 
						(ImageButton) view.findViewById (R.id.btn_apprentice_filter));
		srsButtons.put (SRSLevel.GURU, 
						(ImageButton) view.findViewById (R.id.btn_guru_filter));
		srsButtons.put (SRSLevel.MASTER, 
				 		(ImageButton) view.findViewById (R.id.btn_master_filter));
		srsButtons.put (SRSLevel.ENLIGHTEN, 
				 		(ImageButton) view.findViewById (R.id.btn_enlighten_filter));
		srsButtons.put (SRSLevel.BURNED, 
						(ImageButton) view.findViewById (R.id.btn_burned_filter));

		typeImages = new EnumMap<Item.Type, Images> (Item.Type.class);
		typeImages.put (Item.Type.RADICAL, 
						new Images (res, R.drawable.deselected_blue, R.drawable.blue));
		typeImages.put (Item.Type.KANJI, 
						new Images (res, R.drawable.deselected_magenta, R.drawable.magenta));
		typeImages.put (Item.Type.VOCABULARY, 
						new Images (res, R.drawable.deselected_violet, R.drawable.violet));

		srsImages = new EnumMap<SRSLevel, Images> (SRSLevel.class);
		srsImages.put (SRSLevel.APPRENTICE, 
					   new Images (res, R.drawable.deselected_apprentice, 
							   	   R.drawable.apprentice));
		srsImages.put (SRSLevel.GURU, 
						new Images (res, R.drawable.deselected_guru, R.drawable.guru));
		srsImages.put (SRSLevel.MASTER, 
						new Images (res, R.drawable.deselected_master, R.drawable.master));
		srsImages.put (SRSLevel.ENLIGHTEN, 
					   new Images (res, R.drawable.deselected_enlighten, 
							   	   R.drawable.enlighten));
		srsImages.put (SRSLevel.BURNED, 
				       new Images (res, R.drawable.deselected_burned, R.drawable.burned));

		bind ();
		syncFromISS ();
		
		updateFilter ();
	}
	
	/**
	 * Convenience method that binds each listener to its own button.
	 */
	private void bind ()
	{
		TypeButtonListener tl;
		SRSButtonListener srsl;
		
		for (Item.Type t : Item.Type.values ()) {
			tl = new TypeButtonListener (t);
			typeButtons.get (t).setOnClickListener (tl);
			typeButtons.get (t).setOnLongClickListener (tl);
		}

		for (SRSLevel srs : SRSLevel.values ()) {
			srsl = new SRSButtonListener (srs);
			srsButtons.get (srs).setOnClickListener (srsl);
			srsButtons.get (srs).setOnLongClickListener (srsl);
		}
	}
	
	/**
	 * Updates visual representation from current filter state
	 */
	protected void syncFromISS ()
	{
		for (Item.Type t : Item.Type.values ())
			setActive (t);

		for (SRSLevel srs : SRSLevel.values ())
			setActive (srs);
		
		view.setVisibility (iss.visible ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Convenience method that changes the style of an item type button, basing
	 * itself on the current filter state
	 * @param type the item type
	 */
	private void setActive (Item.Type type)
	{
		Images images;
		Drawable d;
		
		images = typeImages.get (type);
		d = iss.types.get (type) ? images.selected : images.deselected; 
		typeButtons.get (type).setImageDrawable (d);
	}
	
	/**
	 * Convenience method that changes the style of an SRS level button, basing
	 * itself on the current filter state
	 * @param type the SRS level
	 */
	private void setActive (SRSLevel srs)
	{
		Images images;
		Drawable d;
		
		images = srsImages.get (srs);
		d = iss.srses.get (srs) ? images.selected : images.deselected; 
		srsButtons.get (srs).setImageDrawable (d);
	}

	/**
	 * Toggles visibility of the search dialog
	 */
	public void toggleVisibility ()
	{
		iss.visible ^= true;
		syncFromISS ();
		
		updateFilter ();
	}
	
	/**
	 * Convenience method  to be called when any detail of the filter changes.
	 * Its implementation forward the notification to the listener
	 */
	private void updateFilter ()
	{
		listener.filterChanged ();
	}
	
	/**
	 * Given an item list, it returns another list containing all the elements
	 * that match the filter. The original list is not touched. The returned
	 * object may be the original list if all the elements match the filter.
	 * @param l the list
	 * @return a subset
	 */
	public List<Item> filter (List<Item> l)
	{
		List<Item> ans;
		
		if (!iss.visible)
			return l;
					
		ans = new Vector<Item> (l.size ());
		for (Item i : l)
			if (matches (i))
				ans.add (i);
		
		return ans;
	}
	
	/**
	 * Tells whether an item matches the filter criteria
	 * @param i an item
	 * @return <tt>true</tt> if it does
	 */
	protected boolean matches (Item i)
	{
		String s;
		
		if (!iss.types.get (i.type))
			return false;
		
		if (i.stats != null && i.stats.srs != null &&
		    !iss.srses.get (i.stats.srs))
			return false;

		s = filter.getText ().toString ().trim ();
		if (s.length () == 0)
			return true;
		
		return i.matches (s);
	}
	
	/**
	 * Called when a request to show to search dialog is issued
	 * @param focusIfShown if set, and the dialog is already shown, focus on
	 * the text box
	 */
	public void show (boolean focusIfShown)
	{
		if (!iss.visible)
			toggleVisibility ();
		else if (focusIfShown)
			filter.requestFocus ();
	}
}
