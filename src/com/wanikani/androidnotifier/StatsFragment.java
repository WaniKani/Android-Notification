package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wanikani.androidnotifier.graph.PiePlot;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;
import com.wanikani.wklib.SRSDistribution;

public class StatsFragment extends Fragment implements Tab {

	View parent;
		
	MainActivity main;
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;
	}

	/**
	 * Called at fragment creation. Since it keeps valuable information
	 * we enable retain instance flag.
	 */
	@Override
	public void onCreate (Bundle bundle)
	{
		super.onCreate (bundle);

    	setRetainInstance (true);
	}

	/**
	 * Builds the GUI.
	 * @param inflater the inflater
	 * @param container the parent view
	 * @param savedInstance an (unused) bundle
	 */
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
		super.onCreateView (inflater, container, savedInstanceState);

		parent = inflater.inflate(R.layout.stats, container, false);
        
    	return parent;
    }

	@Override
	public void onResume ()
	{
		super.onResume ();
		
		refreshComplete (main.getDashboardData ());
	}

	@Override
	public int getName ()
	{
		return R.string.tag_stats;
	}
	
	public void refreshComplete (DashboardData dd)
	{
		PiePlot srsPie;

		if (dd == null || !isResumed ())
			return;
		
		if (dd.od != null && dd.od.srs != null) {
			srsPie = (PiePlot) parent.findViewById (R.id.pp_srs);
			srsPie.setData (getDataSets (dd.od.srs));
		}
	}
	
	protected List<DataSet> getDataSets (SRSDistribution srs)
	{
		List<DataSet> ans;
		Resources res;
		
		res = getResources ();
		ans = new Vector<DataSet> ();
		
		ans.add (getDataSet (srs.apprentice, res.getColor (R.color.apprentice)));
		ans.add (getDataSet (srs.guru, res.getColor (R.color.guru)));
		ans.add (getDataSet (srs.master, res.getColor (R.color.master)));
		ans.add (getDataSet (srs.enlighten, res.getColor (R.color.enlightened)));
		
		return ans;
	}
	
	protected DataSet getDataSet (SRSDistribution.Level level, int color)
	{
		return new DataSet (color, level.total);
	}
	
	public void spin (boolean enable)
	{
		
	}
	
	public void flush ()
	{
		
	}
	
	 public boolean scrollLock ()
	 {
		return false; 
	 }

}
