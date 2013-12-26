package com.wanikani.androidnotifier;

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
 * An interface implemented by all the fragments. It is used to give
 * a common interface to @link MainActivity.
 */
public interface Tab {

	/**
	 * The type of refresh that should be done
	 */
	public enum RefreshType {
		
		/// Light refresh: just dashboard data
		LIGHT,
		
		/// Medium refresh: all data that could be changed by reviews
		MEDIUM,
		
		/// Full refresh: clear all data. Not explicitly requested by user
		FULL_IMPLICIT,
		
		/// Full refresh: clear all data. Explicitly requested by user
		FULL_EXPLICIT
	}
	
	/**
	 * The tab information
	 */
	public enum Contents {
		
		/// Dashboard
		DASHBOARD,
		
		/// Stats 
		STATS,
		
		/// Items
		ITEMS
		
	}

	/**
	 * Returns the tab name
	 * @return the resource id of the string
	 */
	public int getName ();
	
	/**
	 * Called whenever dashboard data has been refreshed
	 * @return dd the new (possibly) partial data
	 */
	public void refreshComplete (DashboardData dd);
	
	/**
	 * Called when a new dashboard data refresh cycle starts or end.
	 * @param enable <code>true</code> if it is starting
	 */
	public void spin (boolean enable);
	
	/**
	 * Called when caches need to be flushed.
	 * 	@param rtype the type of refresh to be done
	 *  @param fg if the current tab is in foreground 
	 */
	public void flush (RefreshType rtype, boolean fg);
	
	/**
	 * Tells whether the tab is interested in scroll events.
	 * If so, we disable the tab scrolling gesture.
	 * @return true if it does
	 */
	 public boolean scrollLock ();
	 
	 /**
	  * Called when the back button is pressed.
	  * @return <tt>true</tt> if the event has been handled by the fragment
	  */
	 public boolean backButton ();
	 
	 /**
	  * Tells if this tab contains the given contents
	  * @param c the contents
	  * @return <tt>true</tt> if it does
	  */
	 public boolean contains (Contents c);
	 
	 /**
	  * Tells this tab that the facts table is changed
	  */
	 public void flushDatabase ();
}
