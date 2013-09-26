package com.wanikani.androidnotifier;

import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.JavascriptInterface;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.wanikani.wklib.JapaneseIME;

public class LocalIMEKeyboard extends NativeKeyboard {
	
	private class IMEListener implements TextWatcher {
		
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
			"$.jStorage.listenKeyChange (\"currentItem\", window.wknNewQuestion);";
	private static final String JS_STOP_TRIGGERS =
			"$.jStorage.stopListening (\"currentItem\", window.wknNewQuestion);";
	
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
		
		jsl = new JSListener ();
		wv.addJavascriptInterface (jsl, "wknJSListener");
	}	
	
	public void show (boolean hasEnter)
	{
		super.show (hasEnter);
		
		wv.js (JS_INIT_TRIGGERS);
		divw.setVisibility (View.VISIBLE);
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
	}
}
