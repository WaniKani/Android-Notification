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

public class HistogramChart extends LinearLayout {

	/// The inflater
	LayoutInflater inflater;
	
	/// The chart title
	TextView title;
	
	/// The real histogram
	HistogramPlot plot;
	
	/// The legend
	LinearLayout legend;
	
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
	public HistogramChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
			
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.histogramchart, this);
		plot = (HistogramPlot) findViewById (R.id.hc_plot);
		legend = (LinearLayout) findViewById (R.id.hc_legend);
		title = (TextView) findViewById (R.id.hc_title);
		spinner = (ProgressBar) findViewById (R.id.hc_spinner);
		alertPanel = findViewById (R.id.hc_lay_alert);
		alertMessage = (TextView) findViewById (R.id.hc_alert);
		
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
	 * @param series the series
	 * @param bars the values
	 * @param cap the cap
	 */
	public void setData (List<HistogramPlot.Series> series, List<HistogramPlot.Samples> bars, long cap)
	{
		LinearLayout item;		
		
		plot.setData (series, bars, cap);
		
		legend.removeAllViews ();
		for (HistogramPlot.Series s : series) {
			if (s.name != null) {
				item = (LinearLayout) inflater.inflate (R.layout.legend, null); 
				customizeItem (item, s);
				legend.addView (item);
			}
		}
		
		spin (false);
	}	
	
	/**
	 * Fills a new legend item.
	 * @param item the item to fill
	 * @param series the series the item shall describe
	 */
	@SuppressWarnings ("deprecation")
	protected void customizeItem (LinearLayout item, HistogramPlot.Series series)
	{
		Drawable tag;
		
		tag = new ColorDrawable (series.color);
		item.findViewById (R.id.leg_color).setBackgroundDrawable (tag);
		((TextView) item.findViewById (R.id.leg_description)).
			setText (series.name);
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
	 * Shows or hides the spinner. Correspondingly, plot and legend are 
	 * hidden or shown. 
	 * @param enabled if the spinner should be shown
	 */
	public void spin (boolean enabled)
	{
		spinner.setVisibility (enabled ? View.VISIBLE : View.GONE);
		plot.setVisibility (enabled ? View.GONE : View.VISIBLE);
		legend.setVisibility (enabled ? View.GONE : View.VISIBLE);
		alertPanel.setVisibility (View.GONE);
	}
	
	/**
	 * Shows an alert message
	 */
	public void alert (String msg)
	{
		spinner.setVisibility (View.GONE);
		plot.setVisibility (View.GONE);
		legend.setVisibility (View.GONE);
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
	}
}
