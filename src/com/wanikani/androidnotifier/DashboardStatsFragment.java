package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DashboardStatsFragment extends TabsFragment {
	
	View parent;
	
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

		parent = inflater.inflate(R.layout.dashboard_stats, container, false);
				
		return parent;
    }
	
	public void addChildren (List<Tab> fragments)
	{
		FragmentManager mgr;
		
		mgr = getFragmentManager ();		
		
		fragments.add ((Tab) mgr.findFragmentById (R.id.ds_dashboard));
		fragments.add ((Tab) mgr.findFragmentById (R.id.ds_stats));
	}
	
	@Override
	public int getName ()
	{
		return R.string.tag_dashboard; 
	}
	
}
