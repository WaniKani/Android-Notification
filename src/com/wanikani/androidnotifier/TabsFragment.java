package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.support.v4.app.Fragment;

public abstract class TabsFragment extends Fragment implements Tab {
	
	List<Tab> fragments;	

	public TabsFragment ()
	{
		fragments = new Vector<Tab> ();
	}
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		((MainActivity) main).register (this);			
	}
	
	@Override
	public void onStart ()
	{
		super.onStart ();
				
		fragments.clear ();
		
		addChildren (fragments);
	}
	
	@Override
	public void onStop ()
	{
		super.onStop ();
		
		fragments.clear ();
	}
	
	public abstract void addChildren (List<Tab> fragments);
	
	public void refreshComplete (DashboardData dd)
	{
		for (Tab t : fragments)
			t.refreshComplete (dd);
	}
	
	public void spin (boolean enable)
	{
		for (Tab t : fragments)
			t.spin (enable);		
	}
	
	public void flush (RefreshType rtype)
	{
		for (Tab t : fragments)
			t.flush (rtype);		
	}
	
	 public boolean scrollLock ()
	 {
		for (Tab t : fragments)
			if (t.scrollLock ())
				return true;
		
		return false;
	 }
	 
	 public boolean backButton ()
	 {
		for (Tab t : fragments)
			if (t.backButton ())
				return true;
		
		return false;
	 }

	@Override
    public boolean contains (Contents c)
	{
		for (Tab t : fragments)
			if (t.contains (c))
				return true;
			
		return false;
	}
}
