package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.ProgressPlot.DataSet;

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
public class ProgressChart extends TableLayout {
	
	public class SubPlot implements View.OnClickListener {

		TableRow row;
		
		TableRow legendRow;
		
		ProgressPlot plot;
		
		LinearLayout legend;
		
		public SubPlot (String title)
		{
			SpannableString spans;
			TextView tview;
			ViewGroup template;
			Context ctxt;

			ctxt = getContext ();

			template = (ViewGroup) inflater.inflate (R.layout.progresschart, null);
			row = (TableRow) template.getChildAt (0);
			legendRow = (TableRow) template.getChildAt (1);
			template.removeAllViews ();
			removeView (template);
			addView (row);
			addView (legendRow);
						
			tview = (TextView) row.findViewById (R.id.pp_title);
			spans = new SpannableString (title);
			spans.setSpan (new UnderlineSpan (), 0, title.length (), 0);
			spans.setSpan (new ForegroundColorSpan (Color.BLUE), 0, title.length (), 0);
			tview.setText (spans);
			tview.setClickable (true);
			tview.setOnClickListener (this);
			
			plot = (ProgressPlot) row.findViewById (R.id.pp_plot);
			plot.setLayoutParams (new TableRow.LayoutParams (0, LayoutParams.WRAP_CONTENT, 1));

			legend = (LinearLayout) legendRow.findViewById (R.id.pp_legend);
			
			legendRow.setVisibility (View.GONE);
		}
		
		public void onClick (View view)
		{
			int visibility;
			
			visibility = legendRow.getVisibility () == View.VISIBLE ? View.GONE : View.VISIBLE;
			legendRow.setVisibility (visibility);
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
		}	

	}

	/// The inflater
	LayoutInflater inflater;

	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attributes
	 */
	public ProgressChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
			
		inflater = (LayoutInflater) 
		ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
		
	public SubPlot addData (String title)
	{
		return new SubPlot (title);
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
	
}
