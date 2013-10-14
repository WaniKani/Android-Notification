package com.wanikani.androidnotifier;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WaniKaniImprove {

	private class State implements Runnable {
		
		private String item;
		
		private String type;
		
		private String jstoreEn [];

		public State (String item, String type, String jstoreEn [])
		{
			this.item = item;
			this.type = type;
			this.jstoreEn = jstoreEn;
		}
		
		public void publish ()
		{
			activity.runOnUiThread (this);
		}
		
		public String getURL ()
		{
			StringBuffer sb;
			
			sb = new StringBuffer ("http://www.wanikani.com/quickview/");
			
			if (type.equals ("kanji"))
				sb.append ("kanji/").append (item).append ('/');
			else if (type.equals ("vocabulary"))
				sb.append ("vocabulary/").append (item).append ('/');
			else
				sb.append ("radicals/").
					append (jstoreEn [0].toLowerCase ().replace (' ', '-')).
					append ('/');
			
			return sb.toString ();
		}
		
		public void run ()
		{
			showDialog (this);
		}
	}
	
	private class WebViewClientImpl extends WebViewClient {
		
		Dialog dialog;
		
		ProgressBar pb;
		
		TextView tv;
		
		String title;
		
		WebViewClientImpl (Dialog dialog, String title)
		{
			this.dialog = dialog;
			this.title = title;

			pb = (ProgressBar) dialog.findViewById (R.id.pb_lastitem);
			tv = (TextView) dialog.findViewById (R.id.tv_lastitem_message);
		}
		
		@Override  
	    public void onPageStarted (WebView view, String url, Bitmap favicon)  
	    {  
	        pb.setVisibility (View.VISIBLE);
		}
	
		@Override
	    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl)
	    {
			dialog.setTitle (title);
	    	pb.setVisibility (View.GONE);
	    	tv.setText (activity.getResources ().getString (R.string.status_msg_error));
	    	tv.setVisibility (View.VISIBLE);
	    	view.setVisibility (View.GONE);
	    }

		@Override  
	    public void onPageFinished (WebView view, String url)  
	    {  
			dialog.setTitle (title);
			pb.setVisibility (View.GONE);
	    }
	}

	
	private static final String JS_INIT_PAGE =
"\r\n" + 
"$('<li id=\"option-show-previous\"><span title=\"Check previous item\" lang=\"ja\"><i class=\"icon-question-sign\"></i></span></li>').insertAfter('#option-home').addClass('disabled');\r\n" + 
"$('<style type=\"text/css\"> .qtip{ max-width: 380px !important; } #additional-content ul li { width: 16% !important; } #additional-content {text-align: center;} #option-show-previous img { max-width: 12px; background-color: #00A2F3; padding: 2px 3px; }</style>').appendTo('head');\r\n" + 
"\r\n" +
// Bind the show previous button
"$('#option-show-previous').on('click', function (event)" + 
"{" +
"	wknWanikaniImprove.show ();" + 
"});";	

	
	public static final String JS_CODE =
"function checkAnswer()\r\n" + 
"{\r\n" + 
"    answerException = $.trim($('#answer-exception').text());\r\n" + 
"    if(answerException)\r\n" + 
"    {\r\n" + 
"        if(answerException.indexOf('answer was a bit off') !== -1)\r\n" + 
"        {\r\n" + 
"            console.log('answerException: your answer was a bit off');\r\n" + 
"            $('#option-show-previous span').css('background-color', '#F5F7AB').attr('title', 'Your answer was a bit off');\r\n" + 
"        }\r\n" + 
"        else if(answerException.indexOf('possible readings') !== -1)\r\n" + 
"        {\r\n" + 
"            console.log('answerException: other possible readings');\r\n" + 
"            $('#option-show-previous span').css('background-color', '#CDE0F7').attr('title', 'There are other possible readings');\r\n" + 
"        }\r\n" + 
"        else if(answerException.indexOf('possible meanings') !== -1)\r\n" + 
"        {\r\n" + 
"            console.log('answerException: other possible meanings');\r\n" + 
"            $('#option-show-previous span').css('background-color', '#CDE0F7').attr('title', 'There are other possible meanings');\r\n" + 
"        }\r\n" + 
"        else if(answerException.indexOf('View the correct') !== -1)\r\n" + 
"        {\r\n" + 
"            console.log('answerException: wrong answer');\r\n" + 
"        }\r\n" + 
"        else\r\n" + 
"        {\r\n" + 
"            console.log('answerException: ' + answerException);\r\n" + 
"            $('#option-show-previous span').css('background-color', '#FBFBFB');\r\n" + 
"        }\r\n" + 
"    }\r\n" + 
"    else\r\n" + 
"    {\r\n" + 
"        $('#option-show-previous span').css('background-color', '#FBFBFB');\r\n" + 
"    }\r\n" + 
"    \r\n" + 
"    if ($('#answer-form form fieldset').hasClass('correct'))\r\n" + 
"    {\r\n" + 
"        console.log('Correct answer');\r\n" + 
"        moveNext();\r\n" + 
"\r\n" + 
"    }\r\n" + 
"    else if ($('#answer-form form fieldset').hasClass('incorrect'))\r\n" + 
"    {\r\n" + 
"        console.log('Wrong answer');\r\n" + 
"    }\r\n" + 
"}\r\n" + 
"\r\n" + 
"function moveNext()\r\n" + 
"{\r\n" + 
"    console.log('Moving to next question');\r\n" + 
"    $('#answer-form button').click();\r\n" + 
"}" + 
	// The trigger //
"	 jstored_currentItem = $.jStorage.get('currentItem');" + 
"    $('#option-show-previous').removeClass('disabled');" +
"    currentItem = $.trim($('#character span').html());" + 
"    currentType = $('#character').attr('class');" + 
"    currentQuestionType = $.trim($('#question-type').text());" + 
"    wknWanikaniImprove.save (currentItem, currentType, currentQuestionType, jstored_currentItem.en);" +
"    checkAnswer();";
	
	private State currentState;
	
	private WebReviewActivity activity;
	
	private FocusWebView wv;
	
	public void init (WebReviewActivity activity, FocusWebView wv)
	{
		this.activity = activity;
		this.wv = wv;

		wv.addJavascriptInterface (this, "wknWanikaniImprove");
	}
	
	public void initPage ()
	{
		currentState = null;
		wv.js (JS_INIT_PAGE);
	}
	
	@JavascriptInterface
	public void save (String currentItem, String currentType, String currentQuestionType, String jstoreEn [])
	{
		currentState = new State (currentItem, currentType, jstoreEn);
	}
	
	@JavascriptInterface
	public void show ()
	{
		if (currentState != null)
			currentState.publish ();
	}
	
	private void showDialog (State state)
	{
		Resources res;
		WebView webview;
		Dialog dialog;
		String title;
		
		if (!activity.visible)
			return;
		
		res = activity.getResources ();
		
		dialog = new Dialog (activity);
		dialog.setTitle (res.getString (R.string.fmt_last_item_wait));
		dialog.setContentView (R.layout.lastitem);
		webview = (WebView) dialog.findViewById (R.id.wv_lastitem);
		webview.getSettings ().setJavaScriptEnabled (true);
		title = res.getString (R.string.fmt_last_item, state.type);
		webview.setWebViewClient (new WebViewClientImpl (dialog, title));
		webview.loadUrl (state.getURL ());
		
		dialog.show ();
	}
	
}
			