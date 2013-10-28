package com.wanikani.androidnotifier;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class MetersActivity extends Activity {
	
	private class ResetListener implements View.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			MeterSpec.reset (PreferenceManager.getDefaultSharedPreferences (MetersActivity.this));
			refresh ();
		}
		
	}
	
	private class AmountChangeListener implements OnItemSelectedListener {
		
		@Override
		public void onItemSelected (AdapterView<?> parent, View selected, int position, long id)
		{
			refresh ();
		}
		
		@Override
		public void onNothingSelected (AdapterView<?> parent)
		{
			refresh ();
		}
	}
	
	/// The Date formatter
	private DateFormat df;

	private TextView lrw;
	
	private Spinner avw;

	private Button resetw;
	
	private Map<MeterSpec.T, MeterCountersView> cviews;
	
	public MetersActivity ()
	{
		df =  new SimpleDateFormat ("dd MMM yyyy, HH:mm", Locale.US);
		
		cviews = new EnumMap<MeterSpec.T, MeterCountersView> (MeterSpec.T.class);
	}
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);

		setContentView (R.layout.meters);
		
		lrw = (TextView) findViewById (R.id.me_last_reset);
		avw = (Spinner) findViewById (R.id.me_amount);
		avw.setOnItemSelectedListener (new AmountChangeListener ());
		
		resetw = (Button) findViewById (R.id.me_reset);
		resetw.setOnClickListener (new ResetListener ());
		
		addView (MeterSpec.T.SERVICE, R.id.me_service_total);
		addView (MeterSpec.T.NOTIFY_TIMEOUT, R.id.me_notify_timeout);
		addView (MeterSpec.T.NOTIFY_CHANGE_CONNECTIVITY, R.id.me_change_connectivity);
		addView (MeterSpec.T.NOTIFY_DAILY_JOBS, R.id.me_daily_jobs);
		
		addView (MeterSpec.T.APPLICATION, R.id.me_app_total);
		addView (MeterSpec.T.DASHBOARD_REFRESH, R.id.me_dahsboard_refresh);
		addView (MeterSpec.T.ITEMS, R.id.me_items);
		addView (MeterSpec.T.OTHER_STATS_TOTAL, R.id.me_other_stats);
		addView (MeterSpec.T.RECONSTRUCT_DIALOG, R.id.me_reconstruct);		
	}
	
	private void addView (MeterSpec.T type, int id)
	{		
		MeterCountersView view;
		
		view = (MeterCountersView) findViewById (id);
		cviews.put (type, view);
	}
	
	@Override
	public void onResume ()
	{
		super.onResume ();
		
		refresh ();
	}
	
	public MeterSpec.AmountType getAmountType ()
	{
		int id;
		
		id = (int) avw.getSelectedItemId ();
		switch (id) {
		case 0:
			return MeterSpec.AmountType.SINCE_LAST_RESET;
			
		case 1:
			return MeterSpec.AmountType.AVG_DAY;
			
		case 2:
			return MeterSpec.AmountType.AVG_MONTH;
		}
		
		return MeterSpec.AmountType.SINCE_LAST_RESET;
	}
	
	protected void refresh ()
	{
		MeterSpec.AmountType at;
		String msg;
		Date lr;
		
		lr = MeterSpec.getLastReset (PreferenceManager.getDefaultSharedPreferences (this));
		msg = getString (R.string.fmt_last_reset, df.format (lr));
		lrw.setText (msg);
		at = getAmountType ();
		
		for (Map.Entry<MeterSpec.T, MeterCountersView> e : cviews.entrySet ())
			e.getValue ().setData (e.getKey ().getCounter (this, at));
	}

}
