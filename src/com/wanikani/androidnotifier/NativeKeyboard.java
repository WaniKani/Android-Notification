package com.wanikani.androidnotifier;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

public class NativeKeyboard implements Keyboard {

	WebReviewActivity wav;
	
	FocusWebView wv;
	
	InputMethodManager imm;
	
	private ImageButton muteH;	
	
	public NativeKeyboard (WebReviewActivity wav, FocusWebView wv)
	{
		this.wav = wav;
		this.wv = wv;
		
		imm = (InputMethodManager) wav.getSystemService (Context.INPUT_METHOD_SERVICE);
		
		muteH = (ImageButton) wav.findViewById (R.id.kb_mute_h);
	}
	
	public void show (boolean hasEnter)
	{
		wv.enableFocus ();
		
		imm.showSoftInput (wv, InputMethodManager.SHOW_IMPLICIT);
		
		muteH.setVisibility (SettingsActivity.getShowMute (wav) ? View.VISIBLE : View.GONE);
	}
	
	public void iconize (boolean hasEnter)
	{
		/* empty */
	}
	
	public void hide ()
	{
		muteH.setVisibility (View.GONE);
	}
	
	public ImageButton getMuteButton ()
	{
		return muteH;
	}

}
