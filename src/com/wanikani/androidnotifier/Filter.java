package com.wanikani.androidnotifier;

import java.util.List;

import com.wanikani.wklib.Item;

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
 * This (almost only semantical) interface models a provider of WK items.
 * Each implementation of this class is created only once, and then may
 * be called any number of times, to retrieve data.
 * <p>
 * Implementations should provide a <code>select</code> method (its signature
 * may vary, so it is not defined here) which starts the data retrieval
 * process and return immediately.
 * This class is then responsible of
 * <ul>
 * <li>Starting and stopping the spinners, by using
 *   {@link Filter.Callback#selectLevel(Filter, int, boolean)} and
 *   {@link ItemsFragment#selectOtherFilter(Filter, boolean, boolean)}
 *  <li>Incrementally publishing the items, as soon as they become available,
 *   by using @link {@link ItemsFragment#addData(Filter, java.util.List)}
 * </ul>
 * The select method may be called even if a previous retrieval operation
 * has not been completed. In that case, the filter should re-publish all the
 * data collected so far and complete the process.
 * Implementations should cache data.
 */
public interface Filter {
	
	/**
	 * The interface filters use to publish the results. It is implemented
	 * by @link ItemsFragment (but don't tell it to filters!)
	 */
	public interface Callback {
		
		/**
		 * Publishes the complete item list. The old items published so far
		 * (if any) must be replaced.
		 * @param sfilter the filter which is publishing these items
		 * @param list the new list
		 * @param ok tells if this is the complete list or something went wrong while
		 * loading data
		 */
		void setData (Filter sfilter, List<Item> list, boolean ok);
		
		/**
		 * Adds new items to the list. The old items published so far
		 * must be retained. No duplicate checking is performed.
		 * @param sfilter the filter which is publishing these items
		 * @param list the new list
		 */
		void addData (Filter sfilter, List<Item> list);

		/**
		 * Filters should call this method when no further updates will
		 * be published.
		 * @param sfilter the filter which is publishing these items
		 * @param ok tells if this is the complete list or something went wrong while
		 * loading data
		 */
		void noMoreData (Filter sfilter, boolean ok);

		/**
		 * Clears the contents of the list. This method must be typically used
		 * before starting incremental updates through @link {@link #addData(Filter, List)}.
		 * @param sfilter the filter which is publishing these items
		 */
		void clearData (Filter sfilter);

		/**
		 * Called by level-oriented filters to update the GUI.
		 * If data retrieval is in progress, they should set the 
		 * <code>spinning</code> flag. If it is unset, that means
		 * that data retrieval is ended. 
		 * @param sfilter the filter which is publishing these items
		 * @param level the level which is currently being loaded
		 * @param spinning set if data retrieval is in progress
		 */
		void selectLevel (Filter sfilter, int level, boolean spinning);
		
		/**
		 * Called by non level-oriented filters to update the GUI.
		 * If data retrieval is in progress, they should set the 
		 * <code>spinning</code> flag. If it is unset, that means
		 * that data retrieval is ended. 
		 * @param sfilter the filter which is publishing these items
		 * @param spinning set if data retrieval is in progress
		 */
		void selectOtherFilter (Filter filter, boolean spinning);
		
		/**
		 * Enables or disables sort orders, according to the info this
		 * filter is capable to offer. For instance recent unlocks
		 * do not provide statistical information, so sorting by
		 * errors is not possible.
		 * @param errors enables or disables sort by errors
		 * @param unlock enables or disables unlock date
		 * @param available anables or disables available date
		 */
		void enableSorting (boolean errors, boolean unlock, boolean available);
	}

	/** 
	 * Called to cancel any pending data retrieval task.
	 * Implementation should not call any notification method, until
	 * a new select request is issued 
	 */
	public void stopTask ();
	
	/**
	 * Clears the cache. Pending requests are not cancelled.
	 */
	public void flush ();
	
	/**
	 * Tells if the filtered items contain SRS information
	 */
	public boolean hasSRSLevelInfo ();
}
