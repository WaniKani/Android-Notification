package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
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
public class PieChart extends IconizableChart {

	/**
	 * A dataset that is not displayed. 
	 */
	public static class InfoSet extends DataSet {
		
		/**
		 * Constructor.
		 * @param description the legend description
		 * @param value the value
		 */
		public InfoSet (String description, float value)
		{
			super (description, -1, value);
		}
		
	}

	/// The real pie plot image
	PiePlot plot;
	
	/// The legend
	LinearLayout legend;
	
	/// The alert layout
	View alertPanel;
	
	/// The alert message
	TextView alertMessage;
	
	/// Is data available
	boolean available;
	
	/**
	 * Constructor. 
	 * @param ctxt context
	 * @param attrs attributes
	 */
	public PieChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs, R.layout.piechart);
		
		plot = (PiePlot) findViewById (R.id.pc_plot);
		legend = (LinearLayout) findViewById (R.id.pc_legend);
		alertPanel = findViewById (R.id.pc_lay_alert);
		alertMessage = (TextView) findViewById (R.id.pc_alert);
	}
	
	/**
	 * Updates the plot with fresh data. Stops the spinner, if shown.
	 * @param dsets the data
	 * @param infosets additional info not to be drawn
	 * @param anchor where to put infosets
	 */
	public void setData (List<DataSet> dsets, List<InfoSet> infosets, int anchor)
	{
		LinearLayout item;		
		int i;
		
		plot.setData (dsets);
		
		legend.removeAllViews ();
		i = 0;
		for (DataSet ds : dsets) {
			if (i++ == anchor)
				addInfosets (infosets);
			
			if (ds.value > 0) {
				item = (LinearLayout) inflater.inflate (R.layout.legend, null); 
				customizeItem (item, ds, true);
				legend.addView (item);
			}
		}
		
		if (i <= anchor)
			addInfosets (infosets);
		
		available = true;
		dataAvailable ();		
	}
	
	private void addInfosets (List<InfoSet> infosets)
	{
		LinearLayout item;		

		if (infosets != null && !infosets.isEmpty ()) {
			for (DataSet ds : infosets) {
				if (ds.value > 0) {
					item = (LinearLayout) inflater.inflate (R.layout.legend, null); 
					customizeItem (item, ds, false);
					legend.addView (item);
				}
			}			
		}		
	}	
	
	/**
	 * Fills a new legend item.
	 * @param item the item to fill
	 * @param ds the dataset the item shall describe
	 * @param color if set, add a palette
	 */
	@SuppressWarnings ("deprecation")
	protected void customizeItem (LinearLayout item, DataSet ds, boolean color)
	{
		Drawable tag;
		
		if (color) {
			tag = new ColorDrawable (ds.color);
			item.findViewById (R.id.leg_color).setBackgroundDrawable (tag);
		} else
			item.findViewById (R.id.leg_color).setVisibility (View.GONE);
		((TextView) item.findViewById (R.id.leg_description)).
			setText (ds.description);
		((TextView) item.findViewById (R.id.leg_value)).
			setText (Integer.toString (Math.round (ds.value)));		
	}
	
	/**
	 * Shows an alert message
	 */
	public void alert (String msg)
	{
		dataAvailable ();
		
		plot.setVisibility (View.GONE);
		legend.setVisibility (View.GONE);
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
	}
	
	protected void loadData ()
	{
		if (available)
			dataAvailable ();
	}
}
