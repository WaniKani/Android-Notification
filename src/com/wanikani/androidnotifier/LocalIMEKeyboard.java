package com.wanikani.androidnotifier;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.wanikani.wklib.JapaneseIME;

public class LocalIMEKeyboard extends NativeKeyboard {

	private class WebViewListener implements FocusWebView.Listener {
		
		@Override
		public void onScroll (int dx, int dy)
		{
			scroll (dx, dy);
		}
	}
	
	private class IMEListener implements TextWatcher, OnEditorActionListener, View.OnClickListener {
		
	    boolean translate;
	    
	    @Override
	    public void afterTextChanged (Editable s)
	    {
	    	JapaneseIME.Replacement repl;
	    	Editable et;
	    	int pos;
	    	
	    	if (!translate)
	    		return;
	    	
	    	pos = ew.getSelectionStart ();
	    	et = ew.getText ();
	    	repl = ime.replace (et.toString (), pos);
	    	if (repl != null)
	    		et.replace (repl.start, repl.end, repl.text);
	    }
	    
	    @Override
	    public void beforeTextChanged (CharSequence cs, int start, int count, int after)
	    {
	    	/* empty */
	    }
	    
	    @Override
	    public void onTextChanged (CharSequence cs, int start, int before, int count)
	    {
	    	/* empty */
	    }
	    
	    public void translate (boolean enable)
	    {
	    	translate = enable;
	    }
	    
	    @Override
	    public boolean onEditorAction (TextView tv, int actionId, KeyEvent event)
	    {
	    	
	        if (actionId == EditorInfo.IME_ACTION_DONE) {
	        	next ();
	        	return true;
	        }
	        
	        return false;
	    }
	    
	    @Override
	    public void onClick (View view)
	    {
	    	next ();
	    }
	    
	    private void next ()
	    {
	    	String s;
	    	
        	s = ew.getText ().toString ();
        	if (translate)
        		s = ime.fixup (s);
        	wv.js (String.format (JS_INJECT_ANSWER, s));
	    }
	}
	
	private class JSListenerSetClass implements Runnable {
		
		public boolean enable;
		
		public String clazz;
		
		public JSListenerSetClass (String clazz)
		{
			this.clazz = clazz;
			
			enable = true;
			
			wav.runOnUiThread (this);
		}
		
		public JSListenerSetClass ()
		{
			enable = false;
			
			wav.runOnUiThread (this);
		}

		public void run ()
		{
			if (enable)
				setClass (clazz);
			else
				unsetClass ();
		}
		
	}
	
	private class JSListenerShow implements Runnable {
		
		Rect frect, trect;
		
		public JSListenerShow (Rect frect, Rect trect)
		{
			this.frect = frect;
			this.trect = trect;
			
			wav.runOnUiThread (this);
		}
		
		public void run ()
		{
			replace (frect, trect);
		}
	}
	
	private class JSListener {
		
		@JavascriptInterface
		public void replace (int fleft, int ftop, int fright, int fbottom,
							 int tleft, int ttop, int tright, int tbottom)
		{
			fleft = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, fleft, dm);
			fright = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, fright, dm);
			ftop = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, ftop, dm);
			fbottom = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, fbottom, dm);
			
			tleft = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, tleft, dm);
			tright = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, tright, dm);
			ttop = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, ttop, dm);
			tbottom = (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP, tbottom, dm);

			new JSListenerShow (new Rect (fleft, ftop, fright, fbottom),
								new Rect (tleft, ttop, tright, tbottom));
		}

		@JavascriptInterface
		public void newQuestion (String qtype)
		{
			imel.translate (qtype.equals ("reading"));			
			new JSListenerSetClass ();
		}
		
		@JavascriptInterface
		public void setClass (String clazz)
		{
			new JSListenerSetClass (clazz);
		}
	}

	private static final String JS_INIT_TRIGGERS =
			"window.wknReplace = function () {" +
			"   var form, frect, txt, trect, button, brect;" +
			"   form = document.getElementById (\"answer-form\");" +
			"   txt = document.getElementById (\"user-response\");" +
			"   button = form.getElementsByTagName (\"button\") [0];" +
			"   frect = form.getBoundingClientRect ();" +
			"   trect = txt.getBoundingClientRect ();" +
			"   brect = button.getBoundingClientRect ();" +
			"   wknJSListener.replace (frect.left, frect.top, frect.right, frect.bottom," +
			"						   trect.left, trect.top, brect.left, trect.bottom);" +
			"};" +
			"window.wknNewQuestion = function (entry, type) {" +
			"   var qtype, e;" +
			"   qtype = $.jStorage.get (\"questionType\");" +
			"   window.wknReplace ();" +
			"   e = $(\"#character span\");" +
			"   e.text (e.text ().replace (/〜/g, \"~\")); " +
			"   wknJSListener.newQuestion (qtype);" +			
			"};" +
			"$.jStorage.listenKeyChange (\"currentItem\", window.wknNewQuestion);" +
			"window.wknNewQuestion ();" +
			"var oldAddClass = jQuery.fn.addClass;" +
			"jQuery.fn.addClass = function () {" +
			"    var res;" +
			"    res = oldAddClass.apply (this, arguments);" +
			"    if (this.selector == \"#answer-form fieldset\")" +
			"         wknJSListener.setClass (arguments [0]); " +
			"    return res;" +
			"};";
	
	private final String JS_REPLACE = 
			"window.wknReplace ();";
	
	private static final String JS_STOP_TRIGGERS =
			"$.jStorage.stopListening (\"currentItem\", window.wknNewQuestion);";
	private static final String JS_INJECT_ANSWER = 
			"$(\"#user-response\").val (\"%s\");" +
			"$(\"#answer-form button\").click ();";
	
    JapaneseIME ime;
    
    View divw;
    
    EditText ew;
    
    IMEListener imel;
    
    JSListener jsl;
    
    DisplayMetrics dm;
    
    Button next;
    
    int correctFG, incorrectFG;
    
    int correctBG, incorrectBG;
    
	public LocalIMEKeyboard (WebReviewActivity wav, FocusWebView wv)
	{
		super (wav, wv);
		
		Resources res;
		
		dm = wav.getResources ().getDisplayMetrics ();
		
		ime = new JapaneseIME ();
		
		ew = (EditText) wav.findViewById (R.id.ime);
		divw = wav.findViewById (R.id.ime_div);
		imel = new IMEListener ();		
		ew.addTextChangedListener (imel);
		ew.setInputType (InputType.TYPE_CLASS_TEXT);
		ew.setOnEditorActionListener (imel);
		ew.setGravity (Gravity.CENTER);
		ew.setImeActionLabel (">>", EditorInfo.IME_ACTION_DONE);
		ew.setImeOptions (EditorInfo.IME_ACTION_DONE);
		
		next = (Button) wav.findViewById (R.id.ime_next);
		next.setOnClickListener (imel);
		
		jsl = new JSListener ();
		wv.addJavascriptInterface (jsl, "wknJSListener");
		
		wv.registerListener (new WebViewListener ());
		
		res = wav.getResources ();
		correctFG = res.getColor (R.color.correctfg);
		incorrectFG = res.getColor (R.color.incorrectfg);
		correctBG = res.getColor (R.color.correctbg);
		incorrectBG = res.getColor (R.color.incorrectbg);
	}	
	
	public void show (boolean hasEnter)
	{
		super.show (hasEnter);
		
		wv.js (JS_INIT_TRIGGERS);
	}

	public void hide ()
	{
		super.hide ();

		imm.hideSoftInputFromWindow (ew.getWindowToken (), 0);
		wv.js (JS_STOP_TRIGGERS);
		divw.setVisibility (View.GONE);
	}

	/* The commented-out sections are there until I find a way to resize the font as well */
	protected void replace (Rect frect, Rect trect)
	{
		RelativeLayout.LayoutParams rparams;
		LinearLayout.LayoutParams params;

		rparams = (RelativeLayout.LayoutParams) divw.getLayoutParams ();
		rparams.topMargin = frect.top;
		rparams.leftMargin = frect.left;
//		rparams.height = frect.height ();
		rparams.width = frect.width ();
		rparams.addRule (RelativeLayout.ALIGN_PARENT_RIGHT);
		rparams.addRule (RelativeLayout.ALIGN_PARENT_LEFT);
		divw.setLayoutParams (rparams);
	
		params = (LinearLayout.LayoutParams) ew.getLayoutParams ();
				
//		params.topMargin = trect.top - frect.top;
//		params.bottomMargin = frect.bottom - trect.bottom;
		params.leftMargin = trect.left - frect.left;
		params.rightMargin = params.leftMargin;
		
		ew.setLayoutParams (params);
				
		params = (LinearLayout.LayoutParams) next.getLayoutParams ();
//		params.topMargin = trect.top - frect.top;
//		params.bottomMargin = frect.bottom - trect.bottom;
		next.setLayoutParams (params);		

		divw.setVisibility (View.VISIBLE);
		
		ew.requestFocus ();
	}
	
	protected void scroll (int dx, int dy)
	{
		RelativeLayout.LayoutParams rparams;

		rparams = (RelativeLayout.LayoutParams) divw.getLayoutParams ();
		rparams.topMargin -= dy;
		divw.setLayoutParams (rparams);
	}
	
	public void setClass (String clazz)
	{
		if (clazz.equals ("correct"))
			disable (correctFG, correctBG);
		else if (clazz.equals ("incorrect"))
			disable (incorrectFG, incorrectBG);
	}
	
	public void unsetClass ()
	{
		enable ();
	}
	
	private void disable (int fg, int bg)
	{
		ew.setTextColor (fg);
		ew.setBackgroundColor (bg);
		ew.setEnabled (false);
	}

	private void enable ()
	{
    	ew.setText ("");
		ew.setTextColor (Color.BLACK);
		ew.setBackgroundColor (Color.WHITE);
		ew.setEnabled (true);

		imm.showSoftInput (ew, 0);
		ew.requestFocus ();
	}
}
