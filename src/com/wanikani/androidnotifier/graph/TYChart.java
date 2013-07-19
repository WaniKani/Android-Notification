package com.wanikani.androidnotifier.graph;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
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

/**
 * A time-value chart, comprising of the plot itself, and some more 
 * auxiliary stuff. 
 * The look and feel tries to match the style of the dashboard
 */
public class TYChart extends LinearLayout {

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
	
	/// The listener for the "partial data" message
	View.OnClickListener partialListener;
	
	View.OnClickListener chainListener;
	
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
		alertMessage.setOnClickListener (new AlertListener ());
		alertMessage.setClickable (true);
		
		partialListener = new PartialListener ();
		
		plot.setTYChart (this);
		
		loadAttributes (ctxt, attrs);
		
		spin (false);
		hideAlert ();
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
	}
	
	public void retrieving (boolean enabled)
	{
		spinner.setVisibility (enabled ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Shows an alert message
	 */
	public void alert (CharSequence msg, View.OnClickListener ocl)
	{
		spinner.setVisibility (View.GONE);
		alertPanel.setVisibility (View.VISIBLE);
		alertMessage.setText (msg);
		chainListener = ocl;
	}
	
	public void hideAlert ()
	{
		alertPanel.setVisibility (View.GONE);
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

	public void partialShown (boolean shown)
	{
		String s;
		
		if (shown) { 
			s = getResources ().getString (R.string.alert_can_fill);
			s = "<font color=\"blue\"><u>" + s + "</u></font>";
			alert (Html.fromHtml (s), partialListener);
		} else
			hideAlert ();
	}
	
	public void refresh ()
	{
		plot.refresh ();
	}	
}
