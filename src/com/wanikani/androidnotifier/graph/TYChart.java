package com.wanikani.androidnotifier.graph;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
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

public class TYChart extends LinearLayout {

	/// The inflater
	LayoutInflater inflater;
	
	/// The chart title
	TextView title;
	
	/// The real TY plot image
	TYPlot plot;
	
	/// A spinner, which is displayed when no data has been published yet
	ProgressBar spinner;
	
	/// The alert layout
	View alertPanel;
	
	/// The alert message
	TextView alertMessage;
	
	/**
	 * Constructor. It only shows the spinner and the title, until 
	 * {@link #setData(List)} gets called.
	 * @param ctxt context
	 * @param attrs attributes
	 */
	public TYChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
			
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.tychart, this);
		plot = (TYPlot) findViewById (R.id.ty_plot);
		title = (TextView) findViewById (R.id.ty_title);
		spinner = (ProgressBar) findViewById (R.id.ty_spinner);
		alertPanel = findViewById (R.id.ty_lay_alert);
		alertMessage = (TextView) findViewById (R.id.ty_alert);
		
		loadAttributes (ctxt, attrs);
		
		spin (false);
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
		
		plot.loadAttributes (ctxt, attrs);
	}
	
	/**
	 * Shows or hides the spinner. Correspondingly, plot and legend are 
	 * hidden or shown. 
	 * @param enabled if the spinner should be shown
	 */
	public void spin (boolean enabled)
	{
		spinner.setVisibility (enabled ? View.VISIBLE : View.GONE);
		plot.setVisibility (enabled ? View.GONE : View.VISIBLE);
		alertPanel.setVisibility (View.GONE);
	}
	
	/**
	 * Shows an alert message
	 */
	public void alert (String msg)
	{
		spinner.setVisibility (View.GONE);
		plot.setVisibility (View.GONE);
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
	}
	
	/**
	 * Tells if a scroll gesture is currently going on.
	 * @return <tt>true</tt> if it is so
	 */
	public boolean scrolling ()
	{
		return plot.scrolling ();
	}
	
	/**
	 * Updates time origin
	 * 	@param date the origin
	 */
	public void setOrigin (Date date)
	{
		plot.setOrigin (date);
	}
	
	public void setDataSource (Pager.DataSource dsource)
	{
		plot.setDataSource (dsource);
	}

}
