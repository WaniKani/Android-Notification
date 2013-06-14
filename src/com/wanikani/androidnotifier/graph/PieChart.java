package com.wanikani.androidnotifier.graph;

import java.util.List;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class PieChart extends LinearLayout {

	PiePlot plot;
	
	public PieChart (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		LayoutInflater inflater;
		
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.piechart, this);
		plot = (PiePlot) findViewById (R.id.pc_plot);
	}
	
	public void setData (List<DataSet> dsets)
	{
		plot.setData (dsets);
	}	
}
