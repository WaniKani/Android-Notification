package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.androidnotifier.R;

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

public abstract class IconizableChart extends LinearLayout {
	
	public static interface DataSource {
		
		public void loadData ();
		
	}
	
	class IconizeButtonListener implements View.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			toggle ();
		}
		
	}

	protected enum State {
		
		CLOSED  {
			public State evRefresh ()
			{
				return ICONIZED_RETRIEVING;
			}
			
			public State evToggle ()
			{
				return RETRIEVING;
			}
			
			public State evDataAvailable ()
			{
				return ICONIZED;
			}
			
			public boolean spinning ()
			{
				return false;
			}

			public boolean canClose ()
			{
				return false;
			}
			
			public boolean isOpen ()
			{
				return false;
			}			
		},
		
		ICONIZED_RETRIEVING  {

			public State evRefresh ()
			{
				return ICONIZED_RETRIEVING;
			}
			
			public State evToggle ()
			{
				return RETRIEVING;
			}
			
			public State evDataAvailable ()
			{
				return ICONIZED;
			}
			
			public boolean spinning ()
			{
				return true;
			}

			public boolean canClose ()
			{
				return false;
			}

			public boolean isOpen ()
			{
				return false;
			}			
		},
		
		RETRIEVING  {

			public State evRefresh ()
			{
				return RETRIEVING;
			}
			
			public State evToggle ()
			{
				return ICONIZED_RETRIEVING;
			}
			
			public State evDataAvailable ()
			{
				return OPEN;
			}
			
			public boolean spinning ()
			{
				return true;
			}

			public boolean canClose ()
			{
				return true;
			}

			public boolean isOpen ()
			{
				return false;
			}			
		},

		ICONIZED  {

			public State evRefresh ()
			{
				return ICONIZED_RETRIEVING;
			}
			
			public State evToggle ()
			{
				return OPEN;
			}
			
			public State evDataAvailable ()
			{
				return ICONIZED;
			}
			
			public boolean spinning ()
			{
				return false;
			}

			public boolean canClose ()
			{
				return false;
			}
			
			public boolean isOpen ()
			{
				return false;
			}						
		},
		
		OPEN  {

			public State evRefresh ()
			{
				return REFRESHING;
			}
			
			public State evToggle ()
			{
				return ICONIZED;
			}
			
			public State evDataAvailable ()
			{
				return OPEN;
			}
			
			public boolean spinning ()
			{
				return false;
			}

			public boolean canClose ()
			{
				return true;
			}
			
			public boolean isOpen ()
			{
				return true;
			}						
		},		
		
		REFRESHING  {

			public State evRefresh ()
			{
				return REFRESHING;
			}
			
			public State evToggle ()
			{
				return ICONIZED_RETRIEVING;
			}
			
			public State evDataAvailable ()
			{
				return OPEN;
			}
			
			public boolean spinning ()
			{
				return true;
			}

			public boolean canClose ()
			{
				return true;
			}
			
			public boolean isOpen ()
			{
				return true;
			}									
		};
		
		public abstract State evRefresh ();
		
		public abstract State evToggle ();
		
		public abstract State evDataAvailable ();
		
		public abstract boolean spinning ();
		
		public abstract boolean canClose ();
		
		public abstract boolean isOpen ();
		
	}
	
	/// The inflater
	LayoutInflater inflater;
	
	/// The chart title
	TextView title;
		
	/// A spinner, which is displayed when no data has been published yet
	ProgressBar spinner;
	
	/// The actual contents
	View contents;
	
	/// Iconize button
	ImageButton icb;
	
	State state;
	
	/// Expander open bitmap
	Bitmap openBmp;
	
	/// Expander close bitmap
	Bitmap closeBmp;
	
	/// Data has been requested yet
	boolean dataRequested;
	
	/// Data source
	DataSource dsource;
	
	/**
	 * Constructor. It only shows the spinner and the title, until 
	 * {@link #setData(List)} gets called.
	 * @param ctxt context
	 * @param attrs attributes
	 * @param id the layout id
	 */
	public IconizableChart (Context ctxt, AttributeSet attrs, int id)
	{
		super (ctxt, attrs);
			
		Resources res;
		
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (id, this);
					
		title = (TextView) findViewById (R.id.gt_title);
		spinner = (ProgressBar) findViewById (R.id.gt_spinner);
		contents = findViewById (R.id.gt_contents);
		icb = (ImageButton) findViewById (R.id.gt_button);
		icb.setOnClickListener (new IconizeButtonListener ());
		
		res = getResources ();
		openBmp = BitmapFactory.decodeResource (res, R.drawable.expander_open);
		closeBmp = BitmapFactory.decodeResource (res, R.drawable.expander_close);
		
		setState (State.CLOSED);
		
		loadAttributes (ctxt, attrs);
	}
	
	/**
	 * Performs the actual job of reading the attributes and updating 
	 * the look. Meant for cascading (which is not done at this stage).
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		TypedArray a;
		
		a = ctxt.obtainStyledAttributes (attrs, R.styleable.PieChart);
			 
		title.setText (a.getString (R.styleable.PieChart_title));
				
		a.recycle ();
	}
			
	private void setState (State state)
	{
		Bitmap bmp;

		this.state = state;
		
		bmp = (state.canClose ()) ? closeBmp : openBmp;
		icb.setImageBitmap (bmp);
		
		spinner.setVisibility (state.spinning () ? View.VISIBLE : View.GONE);
		contents.setVisibility (state.isOpen () ? View.VISIBLE : View.GONE);
	}
	
	public void setOpen (boolean open)
	{
		if (open != state.isOpen ())
			toggle ();
	}
	
	public boolean isOpen ()
	{
		return state.isOpen ();
	}
	
	private void toggle ()
	{
		setState (state.evToggle ());
		if (!dataRequested) {
			dataRequested = true;
			loadData ();
		}
	}
	
	protected void dataAvailable ()
	{
		setState (state.evDataAvailable ());
	}
	
	protected void startRefresh ()
	{
		setState (state.evRefresh ());
	}
	
	public void setDataSource (DataSource dsource)
	{
		this.dsource = dsource;
	}
	
	protected void loadData ()
	{
		if (dsource != null)
			dsource.loadData ();
	}	

}
