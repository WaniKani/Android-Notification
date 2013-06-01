package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
		
		public String level;
		
		public Level (int level)
		{
			this.level = Integer.toString (level);
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

			if (row == null) {
				inflater = main.getLayoutInflater ();
				row = inflater.inflate (R.layout.items_level, parent, false);
				view = (TextView) row.findViewById (R.id.tgr_level);
				view.setText (super.getItem (position).level);
			}
			
			return row;
		}		
	}
	
	DashboardData dd;
	
	MainActivity main;

	View parent;
	
	LevelListAdapter lad;
	
	LevelClickListener lcl;
		
	public void setMainActivity (MainActivity main)
	{
		this.main = main;
	}
	
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
    	parent = inflater.inflate(R.layout.items, container, false);
    	lcl = new LevelClickListener ();
    	
    	if (dd != null)
    		refreshComplete (dd);
    	
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
		for (i = dd.level; i > 0; i--) {
			l.add (new Level (i));
		}

		lad = new LevelListAdapter (l);
		lw = (ListView) parent.findViewById (R.id.lv_levels);
		lw.setAdapter (lad);
		lw.setOnItemClickListener (lcl);
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
	
	public void setLevel (int level)
	{
		
	}
}
