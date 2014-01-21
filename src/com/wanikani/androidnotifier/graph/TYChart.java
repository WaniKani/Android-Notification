package com.wanikani.androidnotifier.graph;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
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

/**
 * A time-value chart, comprising of the plot itself, and some more 
 * auxiliary stuff. 
 * The look and feel tries to match the style of the dashboard
 */
public class TYChart extends IconizableChart {

	/**
	 * A class listening for click events on the alerts message.
	 * If a chain listener (typically an instance of the 
	 * @link PartialListener) is configured, it is called.
	 */
	private class AlertListener implements View.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			if (chainListener != null)
				chainListener.onClick (view);
		}
	}
	
	private class PartialListener implements View.OnClickListener {
		
		public void onClick (View view)
		{
			plot.fillPartial ();
		}
		
	}
	
	/// The real TY plot image
	TYPlot plot;
	
	/// The alert layout
	View alertPanel;
	
	/// The alert message
	TextView alertMessage;
	
	/// The listener for the "partial data" message
	View.OnClickListener partialListener;

	/** The listener that actually reacts to clicks on the alert message.
	 *  We use this "two-stage" listeners to avoid having to register and unregister
	 *  the listener (this object may change, depending on the alert message)
	 */
	View.OnClickListener chainListener;
	
	/**
	 * Constructor. It only shows the spinner and the title, until 
	 * {@link #setData(List)} gets called.
	 * @param ctxt context
	 * @param attrs attributes
	 */
	public TYChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs, R.layout.tychart);
			
		plot = (TYPlot) findViewById (R.id.ty_plot);
		alertPanel = findViewById (R.id.ty_lay_alert);
		alertMessage = (TextView) findViewById (R.id.ty_alert);
		alertMessage.setOnClickListener (new AlertListener ());
		alertMessage.setClickable (true);
		
		partialListener = new PartialListener ();
		
		plot.setTYChart (this);
		
		hideAlert ();
	}
	
	/**
	 * Shows an alert message
	 * @param msg the message
	 * @param ocl an optional click listener
	 */
	public void alert (CharSequence msg, View.OnClickListener ocl)
	{
		spinner.setVisibility (View.GONE);
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
		chainListener = ocl;
	}
	
	/**
	 * Hides the alert message
	 */
	public void hideAlert ()
	{
		alertPanel.setVisibility (View.GONE);
	}
	
	/**
	 * Tells if a scroll gesture is currently going on.
	 * @return <tt>true</tt> if it is so
	 */
	public boolean scrolling (boolean strict)
	{
		return plot.scrolling (strict);
	}
	
	/**
	 * Updates time origin
	 * 	@param date the origin
	 */
	public void setOrigin (Date date)
	{
		plot.setOrigin (date);
	}
	
	/**
	 * Changes the datasource and updates the chart
	 * @param dsource the new datasource
	 */
	public void setDataSource (Pager.DataSource dsource)
	{
		plot.setDataSource (dsource);
	}

	/**
	 * Called by {@link TYPlot} when a segment containing partial information
	 * is shown. In that case we must display a warning message to allow the
	 * user to start the reconstruction process  
	 * @param shown if to be shown
	 */
	void partialShown (boolean shown)
	{
		String s;
		
		if (shown) { 
			s = getResources ().getString (R.string.alert_can_fill);
			s = "<font color=\"blue\"><u>" + s + "</u></font>";
			alert (Html.fromHtml (s), partialListener);
		} else
			hideAlert ();
	}
	
	/**
	 * Forces a refresh of the plot
	 */
	public void refresh ()
	{
		plot.refresh ();
	}
	
	public void loadData ()
	{
		/* empty */
	}
}
