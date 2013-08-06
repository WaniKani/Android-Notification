package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.support.v4.app.Fragment;

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
 * A fragment that contains other fragments. This is useful to group
 * more than one fragment into a single tab when the device is large enough.
 */
public abstract class TabsFragment extends Fragment implements Tab {
	
	/// The fragments
	List<Tab> fragments;	

	/**
	 * Constructor
	 */
	public TabsFragment ()
	{
		fragments = new Vector<Tab> ();
	}
	
	/**
	 * Called when attached. As per tab contract, we register on the main activity.
	 * 	@param main the activity
	 */
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		((MainActivity) main).register (this);			
	}
	
	/**
	 * Called when the activity is starting. Obtains thew list of 
	 * fragments, by calling {@link #addChildren(List)} on the derived class.
	 */
	@Override
	public void onStart ()
	{
		super.onStart ();
				
		fragments.clear ();
		
		addChildren (fragments);
	}
	
	/**
	 * Called when stopping. Clears the fragment list
	 */
	@Override
	public void onStop ()
	{
		super.onStop ();
		
		fragments.clear ();
	}
	
	/**
	 * This method must be implemented by derived class to provide a list of 
	 * the fragments that we are supposed to handle
	 * @param fragments a list that shall be populated by the subclass
	 */
	public abstract void addChildren (List<Tab> fragments);
	
	@Override
	public void refreshComplete (DashboardData dd)
	{
		for (Tab t : fragments)
			t.refreshComplete (dd);
	}
	
	@Override
	public void spin (boolean enable)
	{
		for (Tab t : fragments)
			t.spin (enable);		
	}
	
	@Override
	public void flush (RefreshType rtype)
	{
		for (Tab t : fragments)
			t.flush (rtype);		
	}
	
	@Override
	 public boolean scrollLock ()
	 {
		for (Tab t : fragments)
			if (t.scrollLock ())
				return true;
		
		return false;
	 }
	 
	@Override
	 public boolean backButton ()
	 {
		for (Tab t : fragments)
			if (t.backButton ())
				return true;
		
		return false;
	 }

	@Override
    public boolean contains (Contents c)
	{
		for (Tab t : fragments)
			if (t.contains (c))
				return true;
			
		return false;
	}
}
