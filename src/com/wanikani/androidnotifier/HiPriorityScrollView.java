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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

/**
 * A customization of the stock Horizontal Scroll View, on which it is possible
 * to register a callback interface which receives touch events.
 * This is used by {@link ItemFragment}, in order to lock the tabs and allow
 * long items to scroll freely.
 */
public class HiPriorityScrollView extends HorizontalScrollView {

	/**
	 * The callback interface that receives motion events of a @link HiPriorityScrollView.
	 * @see HiPriorityScrollView#setCallback(Callback)
	 */
	public interface Callback {
		
		/**
		 * Called when a motion starts. If the child is larger than this view,
		 * it means that the scroll is <i>really</i> interested to this event 
		 * @param hpsw the scroll view receiving an event
		 * @param childIsLarger if set, this child is larger than this view 
		 */
		public void down (HiPriorityScrollView hpsw, boolean childIsLarger);
		
		/**
		 * Called when a motion stops.
		 * @param hpsw the scroll view receiving an event
		 * @param childIsLarger if set, this child is larger than this view 
		 */
		public void up (HiPriorityScrollView hpsw, boolean childIsLarger);

		/**
		 * Called when a motion is cancelled.
		 * @param hpsw the scroll view receiving an event
		 * @param childIsLarger if set, this child is larger than this view 
		 */
		public void cancel (HiPriorityScrollView hpsw, boolean childIsLarger);
	};
	
	/// The callback
	private Callback callback;
	
	private float lastX, lastY;
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public HiPriorityScrollView (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
	}
	
	/**
	 * Set a callback that receives motion events.
	 * @param callback the callback
	 */
	public void setCallback (Callback callback)
	{
		this.callback = callback;
	}	

	/**
	 * Called when a touch event is detected. It invokes the callback,
	 * if it is something it should be notified of
	 * @param event the event
	 */
	@Override
	public boolean onTouchEvent (MotionEvent event)
	{		
		boolean childIsLarger;
		View child;
				
		child = getChildAt (0);
		childIsLarger = child.getWidth () > getWidth ();
		
		if (callback != null) {
			switch (event.getAction ()) {
			case MotionEvent.ACTION_DOWN:
				lastX = event.getRawX ();
				lastY = event.getRawY ();
				callback.down (this, childIsLarger);
				break;

			case MotionEvent.ACTION_UP:
				if (lastX == event.getRawX () &&
					lastY == event.getRawY ())
					callback.up (this, childIsLarger);
				else
					callback.cancel (this, childIsLarger);
						
				break;
				
			case MotionEvent.ACTION_CANCEL:
				callback.cancel (this, childIsLarger);
				break;
			}
		}
		
		return super.onTouchEvent (event);
	}
}
