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

public class ItemSearchDialog {

	public interface Listener {
		
		public void filterChanged ();
		
	}
	
	public static class State {
		
		private EnumMap<Item.Type, Boolean> types;
		
		public EnumMap<SRSLevel, Boolean> srses;
		
		public String filter;
		
		public boolean visible;
		
		public State ()
		{
			types = new EnumMap<Item.Type, Boolean> (Item.Type.class);
			srses = new EnumMap<SRSLevel, Boolean> (SRSLevel.class);
			
			for (Item.Type t : Item.Type.values ())
				types.put (t, true);

			for (SRSLevel srs : SRSLevel.values ())
				srses.put (srs, true);
		}				
		
		public void toggle (Item.Type type)
		{
			types.put (type, !types.get (type));
		}
		
		public void toggle (SRSLevel level)
		{
			srses.put (level, !srses.get (level));
		}				
	}
	
	private class TypeButtonListener 
		implements View.OnClickListener, View.OnLongClickListener {
	
		Item.Type type;
	
		public TypeButtonListener (Item.Type type)
		{
			this.type = type;
		}
	
		@Override
		public void onClick (View view)
		{
			iss.toggle (type);
			setActive (type);
			
			updateFilter ();
		}
	
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

	private class SRSButtonListener 
		implements View.OnClickListener, View.OnLongClickListener {
		
		SRSLevel level;
		
		public SRSButtonListener (SRSLevel level)
		{
			this.level = level;
		}
		
		@Override
		public void onClick (View view)
		{
			iss.toggle (level);
			setActive (level);

			updateFilter ();
		}
		
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
	
	private class Images {
		
		public Drawable selected;
		
		public Drawable unselected;
		
		public Images (Resources res, int selected, int unselected)
		{
			this.selected = res.getDrawable (selected);
			this.unselected = res.getDrawable (unselected);
		}
		
	}
	
	private State iss;
	
	private View view;
	
	private EditText filter;
	
	private EnumMap<Item.Type, ImageButton> typeButtons;
	
	private EnumMap<SRSLevel, ImageButton> srsButtons;

	private EnumMap<Item.Type, Images> typeImages;

	private EnumMap<SRSLevel, Images> srsImages;
	
	private Listener listener;

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
						new Images (res, R.drawable.selected_blue, R.drawable.blue));
		typeImages.put (Item.Type.KANJI, 
						new Images (res, R.drawable.selected_magenta, R.drawable.magenta));
		typeImages.put (Item.Type.VOCABULARY, 
						new Images (res, R.drawable.selected_violet, R.drawable.violet));

		srsImages = new EnumMap<SRSLevel, Images> (SRSLevel.class);
		srsImages.put (SRSLevel.APPRENTICE, 
					   new Images (res, R.drawable.selected_apprentice, 
							   	   R.drawable.apprentice));
		srsImages.put (SRSLevel.GURU, 
						new Images (res, R.drawable.selected_guru, R.drawable.guru));
		srsImages.put (SRSLevel.MASTER, 
						new Images (res, R.drawable.selected_master, R.drawable.master));
		srsImages.put (SRSLevel.ENLIGHTEN, 
					   new Images (res, R.drawable.selected_enlighten, 
							   	   R.drawable.enlighten));
		srsImages.put (SRSLevel.BURNED, 
				       new Images (res, R.drawable.selected_burned, R.drawable.burned));

		bind ();
		syncFromISS ();
		
		updateFilter ();
	}
	
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
	
	protected void syncFromISS ()
	{
		for (Item.Type t : Item.Type.values ())
			setActive (t);

		for (SRSLevel srs : SRSLevel.values ())
			setActive (srs);
		
		view.setVisibility (iss.visible ? View.VISIBLE : View.GONE);
	}
	
	private void setActive (Item.Type type)
	{
		Images images;
		Drawable d;
		
		images = typeImages.get (type);
		d = iss.types.get (type) ? images.selected : images.unselected; 
		typeButtons.get (type).setImageDrawable (d);
	}
	
	private void setActive (SRSLevel srs)
	{
		Images images;
		Drawable d;
		
		images = srsImages.get (srs);
		d = iss.srses.get (srs) ? images.selected : images.unselected; 
		srsButtons.get (srs).setImageDrawable (d);
	}

	public void toggleVisibility ()
	{
		iss.visible ^= true;
		syncFromISS ();
		
		updateFilter ();
	}
	
	public void updateFilter ()
	{
		listener.filterChanged ();
	}
	
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
}
