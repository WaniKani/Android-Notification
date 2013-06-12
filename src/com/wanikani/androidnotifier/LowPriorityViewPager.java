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

import java.lang.reflect.Method;

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
	
	/// A package private setCurrentItem method we use to perform smooth scrolling
	Method setCurrentItemInternalMethod;
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public LowPriorityViewPager (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		try {
			setCurrentItemInternalMethod = getClass ().
					getDeclaredMethod ("setCurrentItemInternal", 
							           Integer.class, Boolean.class, 
							           Boolean.class, Integer.class);
			setCurrentItemInternalMethod.setAccessible (true);
		} catch (Exception e) {
			/* Any exception is ok, after all. We simply disable the mechanism */
		}
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
		return (main == null || !main.scrollLock (getCurrentItem ())) && 
					super.onInterceptTouchEvent (ev);
	}
	
	/**
	 * Called to switch to another tab. The superclass implementation
	 * is not good enough because velocity is set to zero (don't know why).
	 * So if we managed to fetch the package-private worker method,
	 * we use it directly. Otherwise, we simply move to the next page
	 * in the ordinary way, it's not the end of the world... 
	 * @param pos the new position
	 */
	@Override
	public void setCurrentItem (int pos, boolean smooth)
	{
		if (smooth && setCurrentItemInternalMethod != null) {
			try {
				setCurrentItemInternalMethod.invoke (this, pos, smooth, false, 1);
				return;
			} catch (Exception e) {
				/* Fall back */
			}
		}
			
		super.setCurrentItem (pos, smooth);		
	}
}
