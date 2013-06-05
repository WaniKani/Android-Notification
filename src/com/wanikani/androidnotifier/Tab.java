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
	 * Called at startup to give the tab a reference to the
	 * main activity
	 * @param main the main activity
	 */
	public void setMainActivity (MainActivity main);
	
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
	 */
	public void flush ();
}
