package com.wanikani.androidnotifier;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

/**
 * Implementation of the {@link Keyboard} interface that uses the default android keyboard.
 * This is more or less like using an external browser: the main difference being
 * that the keyboard shows up when entering the reviews page.
 */
public class NativeKeyboard implements Keyboard {

	/// Parent activity
	WebReviewActivity wav;
	
	/// Internal browser
	FocusWebView wv;
	
	/// The manager, used to popup the keyboard when needed
	InputMethodManager imm;
	
	/// The mute button
	private ImageButton muteH;	
	
	/**
	 * Constructor
	 * @param wav the parent activity
	 * @param wv the internal browser
	 */
	public NativeKeyboard (WebReviewActivity wav, FocusWebView wv)
	{
		this.wav = wav;
		this.wv = wv;
		
		imm = (InputMethodManager) wav.getSystemService (Context.INPUT_METHOD_SERVICE);
		
		muteH = (ImageButton) wav.findViewById (R.id.kb_mute_h);
	}
	
	/**
	 * Called by the parent activity when the keyboard needs to be shown.
	 * @param hasEnter if the keyboard contains the enter key. If unset, the hide button is shown instead
	 */
	@Override
	public void show (boolean hasEnter)
	{
		wv.enableFocus ();
		
		imm.showSoftInput (wv, InputMethodManager.SHOW_IMPLICIT);
		
		muteH.setVisibility (SettingsActivity.getShowMute (wav) ? View.VISIBLE : View.GONE);
	}
		
	/**
	 * Called when the keyboard should be iconized. This does never happen when using the
	 * native keyboard, so this method does nothing
	 * @param hasEnter if the keyboard contains the enter key. If unset, the hide button is shown instead
	 */
	@Override
	public void iconize (boolean hasEnter)
	{
		/* empty */
	}

	/**
	 * Hides the keyboard. Used also when the user chooses a different keyboard.
	 */		
	@Override
	public void hide ()
	{
		muteH.setVisibility (View.GONE);
	}
	
	/**
	 * Returns a reference to the mute button view.
	 * @return the mute button
	 */
	@Override
	public ImageButton getMuteButton ()
	{
		return muteH;
	}
	
	/**
	 * Ignore button pressed. Does nothing because it is not supported.
	 */
	@Override
	public void ignore ()
	{
		/* Not supported */
	}
	
	/**
	 * Tells if the ignore menu item may be shown.
	 * @return always false, since we do not implement it in this class
	 */
	@Override
	public boolean canIgnore ()
	{
		return false;
	}
}
