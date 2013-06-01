package com.wanikani.androidnotifier;

public interface Tab {

	public int getName ();
	
	public void refreshComplete (DashboardData dd);
	
	public void spin (boolean enable);
	
	public void setMainActivity (MainActivity main);
}
