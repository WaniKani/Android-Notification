package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;

public class PieChart extends LinearLayout {

	LayoutInflater inflater;
	
	PiePlot plot;
	
	LinearLayout legend;
	
	public PieChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.piechart, this);
		plot = (PiePlot) findViewById (R.id.pc_plot);
		legend = (LinearLayout) findViewById (R.id.pc_legend);
	}
	
	public void setData (List<DataSet> dsets)
	{
		LinearLayout item;		
		
		plot.setData (dsets);
		
		legend.removeAllViews ();
		for (DataSet ds : dsets) {
			item = (LinearLayout) inflater.inflate (R.layout.legend, null); 
			customizeItem (item, ds);
			legend.addView (item);
		}
	}	
	
	protected void customizeItem (LinearLayout item, DataSet ds)
	{
		GradientDrawable tag;
		
		tag = new GradientDrawable (Orientation.LEFT_RIGHT, new int [] {ds.color, ds.color});
		tag.setStroke (0, Color.BLACK);
		item.findViewById (R.id.leg_color).setBackgroundDrawable (tag);
		((TextView) item.findViewById (R.id.leg_description)).
			setText (ds.description);
		((TextView) item.findViewById (R.id.leg_value)).
			setText (Integer.toString (Math.round (ds.value)));		
	}
}
