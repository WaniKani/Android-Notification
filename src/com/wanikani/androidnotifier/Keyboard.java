package com.wanikani.androidnotifier;

import android.widget.ImageButton;

public interface Keyboard {

	public void show (boolean hasEnter);
	
	public void iconize (boolean hasEnter);
	
	public void hide ();
	
	public ImageButton getMuteButton ();	
}
