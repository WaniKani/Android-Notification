package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
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

public class HistogramChart extends IconizableChart {

	/// The real histogram
	HistogramPlot plot;
	
	/// The legend
	LinearLayout legend;
	
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
		super (ctxt, attrs, R.layout.histogramchart);
			
		plot = (HistogramPlot) findViewById (R.id.hc_plot);
		legend = (LinearLayout) findViewById (R.id.hc_legend);
		alertPanel = findViewById (R.id.hc_lay_alert);
		alertMessage = (TextView) findViewById (R.id.hc_alert);
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
		
		dataAvailable ();
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
	 * Shows an alert message
	 * @param msg the message
	 * @param ocl the listener which receives an event when clicked
	 */
	public void alert (String msg, View.OnClickListener ocl)
	{
		dataAvailable ();
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (Html.fromHtml ("<font color=\"blue\"><u>" + msg + "</u></font>"));
		alertMessage.setClickable (true);
		alertMessage.setOnClickListener (ocl);
	}
	
	/**
	 * Shows an alert message
	 * @param msg the message
	 */
	public void alert (String msg)
	{
		dataAvailable ();
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
		alertMessage.setClickable (false);
	}

	public void hideAlert ()
	{
		alertPanel.setVisibility (View.GONE);
	}
}
