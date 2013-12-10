package com.wanikani.androidnotifier;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
 * A tab holding the dashboard and stats items. They are kind of similar,
 * so it is probably a good enhancement to group them, when using a tablet.
 */
public class DashboardStatsFragment extends TabsFragment {

	/// The parent view
	View parent;
	
	/**
	 * Builds the GUI.
	 * @param inflater the inflater
	 * @param container the parent view
	 * @param savedInstance an (unused) bundle
	 */
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
		super.onCreateView (inflater, container, savedInstanceState);

		parent = inflater.inflate(R.layout.dashboard_stats, container, false);
				
		return parent;
    }

	@Override
	public void addChildren (List<Tab> fragments)
	{
		FragmentManager mgr;
		
		mgr = getFragmentManager ();		
		
		fragments.add ((Tab) mgr.findFragmentById (R.id.ds_dashboard));
		fragments.add ((Tab) mgr.findFragmentById (R.id.ds_stats));
	}
	
	@Override
	public int getName ()
	{
		return R.string.tag_dashboard; 
	}

	/**
	 * This class overrides the stock spin method, because we want only
	 * the dashboard to show the spinner. I think it's better this way.
	 * @param enable to enable spinner
	 */
	@Override
	public void spin (boolean enable)
	{
		if (enable) {
			if (fragments.size () > 0)
				fragments.get (0).spin (enable);
		} else
			super.spin (enable);
	}

	@Override
	public void flushDatabase ()
	{
		for (Tab t : fragments)
			t.flushDatabase ();
	}
	
}
