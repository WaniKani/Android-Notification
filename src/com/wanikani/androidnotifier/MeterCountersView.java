package com.wanikani.androidnotifier;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MeterCountersView extends LinearLayout {

	private class Binding {
		
		View div;
		
		TextView tw;
		
		public Binding (int div, int tw)
		{
			this.div = findViewById (div);
			this.tw = (TextView) findViewById (tw);
		}		
		
		public void update (MeterSpec.AmountType at, long value, boolean visible)
		{
			String multi;
			
			div.setVisibility (value != 0 || visible ? View.VISIBLE : View.GONE);
			if (value < 1024)
				multi = "";
			else {
				value = Math.round (value / 1024f);
				if (value < 1024)
					multi = "Ki";
				else {
					value = Math.round (value / 1024f);
					multi = "Mi";
				}
			}
			tw.setText (value + " " + multi + at.getUnit ());
		}
	}
	
	private LayoutInflater inflater;
	
	private Binding mobile, wifi, unknown;
	
	private View tdiv;
	
	public MeterCountersView (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);

		TextView title;
		TypedArray a;		
		
		inflater = (LayoutInflater) 
				ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		inflater.inflate (R.layout.meters_cnt, this);
		
		mobile = new Binding (R.id.mc_d_mobile, R.id.mc_mobile);
		wifi = new Binding (R.id.mc_d_wifi, R.id.mc_wifi);
		unknown = new Binding (R.id.mc_d_unknown, R.id.mc_unknown);
		
		a = ctxt.obtainStyledAttributes (attrs, R.styleable.PieChart);
		
		tdiv = findViewById (R.id.mc_title_div);
		if (a.hasValue (R.styleable.PieChart_title)) {
			title = (TextView) findViewById (R.id.mc_title);
			title.setText (a.getString (R.styleable.PieChart_title));
		} else {
			tdiv.setVisibility (View.GONE);
			tdiv = null;
		}
			
		a.recycle ();
		
		setData (new MeterSpec.Counter (MeterSpec.AmountType.SINCE_LAST_RESET, 0, 0, 0));
	}
	
	public void setData (MeterSpec.Counter data)
	{
		boolean empty;
		
		empty = data.total () == 0;
		if (tdiv != null) {
			tdiv.setVisibility (!empty ? View.VISIBLE : View.GONE);
			empty = false;
		}

		mobile.update (data.at, data.mobile, empty);
		wifi.update (data.at, data.wifi, false);
		unknown.update (data.at, data.unknown, false);
	}
}
