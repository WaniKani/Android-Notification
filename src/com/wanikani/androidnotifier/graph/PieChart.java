package com.wanikani.androidnotifier.graph;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;

public class PieChart extends LinearLayout {

	LayoutInflater inflater;
	
	TextView title;
	
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
		title = (TextView) findViewById (R.id.pc_title);
		
		loadAttributes (ctxt, attrs);
	}
	
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		TypedArray a;
		
		a = ctxt.obtainStyledAttributes (attrs, R.styleable.PieChart);
			 
		title.setText (a.getString (R.styleable.PieChart_title));
				
		a.recycle ();		
		
		plot.loadAttributes (ctxt, attrs);
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
