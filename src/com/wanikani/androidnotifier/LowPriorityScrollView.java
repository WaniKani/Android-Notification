package com.wanikani.androidnotifier;

/* 
 *  Copyright (c) 2014 Alberto Cuda
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class LowPriorityScrollView extends ScrollView {

	public interface Callback {
		
		public boolean canScroll (LowPriorityScrollView lpsw);
		
	}
	
	Callback callback;
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public LowPriorityScrollView (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
	}
	
	public void setCallback (Callback callback)
	{
		this.callback = callback;
	}
	
	public boolean onInterceptTouchEvent (MotionEvent ev)
	{		
		return (callback == null || callback.canScroll (this)) && 
					super.onInterceptTouchEvent (ev);
	}
}
