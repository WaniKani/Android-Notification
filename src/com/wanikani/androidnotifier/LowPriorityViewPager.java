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

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A pager that can disable horizontal scroll events.
 * This is needed because the items fragment has an horizontal scroll view, and
 * there is some kind of annoying conflict between them. 
 */
public class LowPriorityViewPager extends ViewPager {

	/// The main activity
	MainActivity main;
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public LowPriorityViewPager (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
	}
	
	/**
	 * Called at startup by @link MainActivity, to set the callback.
	 * @param main the main activity
	 */
	public void setMain (MainActivity main)
	{
		this.main = main;
	}
	
	/**
	 * Called when a touch event is detected. We discard it
	 * if the current fragment has a horizontal scroll view.
	 * @param ev the event
	 */
	public boolean onInterceptTouchEvent (MotionEvent ev)
	{		
		return (main == null || !main.hasScroll (getCurrentItem ())) && 
					super.onInterceptTouchEvent (ev);
	}
}
