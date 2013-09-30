package com.wanikani.androidnotifier;

import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.wanikani.wklib.JapaneseIME;

public class LocalIMEKeyboard extends NativeKeyboard {
	
	private class IMEListener implements TextWatcher, OnEditorActionListener {
		
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
	    	String s;
	    	
	        if (actionId == EditorInfo.IME_ACTION_DONE) {
	        	s = ew.getText ().toString ();
	        	if (translate)
	        		s = ime.fixup (s);
	        	wv.js (String.format (JS_INJECT_ANSWER, s));
	        	divw.setVisibility (View.GONE);
	        	imm.hideSoftInputFromWindow (ew.getWindowToken(), 0);
	        	ew.setText ("");
	        	return true;
	        }
	        
	        return false;
	    }
	}
	
	private class JSListenerShow implements Runnable {
		
		Rect rect;
		
		public JSListenerShow (Rect rect)
		{
			this.rect = rect;
			
			wav.runOnUiThread (this);
		}
		
		public void run ()
		{
			showIME (rect);
		}
	}
	
	private class JSListener {
		
		@JavascriptInterface
		public void newQuestion (String qtype, int left, int top, int right, int bottom)
		{
			imel.translate (qtype.equals ("reading"));
			new JSListenerShow (new Rect (left, top, right, bottom));
		}
		
	}

	private static final String JS_INIT_TRIGGERS =
			"window.wknNewQuestion = function (entry, type) {" +
			"   var qtype, form, rect;" +
			"   qtype = $.jStorage.get (\"questionType\");" +
			"   form = document.getElementById (\"answer-form\");" +
			"   rect = form.getBoundingClientRect ();" +
			"   wknJSListener.newQuestion (qtype, rect.left, rect.top, rect.right, rect.bottom);" +
			"};" +
			"$.jStorage.listenKeyChange (\"currentItem\", window.wknNewQuestion);" +
			"window.wknNewQuestion ();";
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
    
	public LocalIMEKeyboard (WebReviewActivity wav, FocusWebView wv)
	{
		super (wav, wv);
		
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
		
		jsl = new JSListener ();
		wv.addJavascriptInterface (jsl, "wknJSListener");
	}	
	
	public void show (boolean hasEnter)
	{
		super.show (hasEnter);
		
		wv.js (JS_INIT_TRIGGERS);
	}
	
	public void hide ()
	{
		super.hide ();

		wv.js (JS_STOP_TRIGGERS);
		divw.setVisibility (View.GONE);
	}

	protected void showIME (Rect rect)
	{
		RelativeLayout.LayoutParams rparams;

		rparams = (RelativeLayout.LayoutParams) divw.getLayoutParams ();
		rparams.topMargin = rect.top;
		rparams.leftMargin = rect.left;
		rparams.height = rect.height ();
		rparams.width = rect.width ();
		divw.setLayoutParams (rparams);
		
		divw.setVisibility (View.VISIBLE);
		imm.showSoftInput (wv, 0);
		
		ew.requestFocus ();
	}
}
