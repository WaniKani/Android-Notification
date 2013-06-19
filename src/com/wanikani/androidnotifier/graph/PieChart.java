package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;

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
 * A simple 3-d pie chart, comprising of the plot itself, a title and 
 * a legend. The look and feel tries to match the style of the dashboard.
 */
public class PieChart extends LinearLayout {

	/// The inflater
	LayoutInflater inflater;
	
	/// The chart title
	TextView title;
	
	/// The real pie plot image
	PiePlot plot;
	
	/// The legend
	LinearLayout legend;
	
	/// A spinner, which is displayed when no data has been published yet
	ProgressBar spinner;
	
	/**
	 * Constructor. It only shows the spinner and the title, until 
	 * {@link #setData(List)} gets called.
	 * @param ctxt context
	 * @param attrs attributes
	 */
	public PieChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
			
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.piechart, this);
		plot = (PiePlot) findViewById (R.id.pc_plot);
		legend = (LinearLayout) findViewById (R.id.pc_legend);
		title = (TextView) findViewById (R.id.pc_title);
		spinner = (ProgressBar) findViewById (R.id.pc_spinner);
		
		loadAttributes (ctxt, attrs);
		
		spin (true);
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
	 * Updates the plot with fresh data. Stops the spinner, if shown.
	 * @param dsets the data
	 */
	public void setData (List<DataSet> dsets)
	{
		LinearLayout item;		
		
		plot.setData (dsets);
		
		legend.removeAllViews ();
		for (DataSet ds : dsets) {
			if (ds.value > 0) {
				item = (LinearLayout) inflater.inflate (R.layout.legend, null); 
				customizeItem (item, ds);
				legend.addView (item);
			}
		}
		
		spin (false);
	}	
	
	/**
	 * Fills a new legend item.
	 * @param item the item to fill
	 * @param ds the dataset the item shall describe
	 */
	@SuppressWarnings ("deprecation")
	protected void customizeItem (LinearLayout item, DataSet ds)
	{
		Drawable tag;
		
		tag = new ColorDrawable (ds.color);
		item.findViewById (R.id.leg_color).setBackgroundDrawable (tag);
		((TextView) item.findViewById (R.id.leg_description)).
			setText (ds.description);
		((TextView) item.findViewById (R.id.leg_value)).
			setText (Integer.toString (Math.round (ds.value)));		
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
		legend.setVisibility (enabled ? View.GONE : View.VISIBLE);
	}
}
