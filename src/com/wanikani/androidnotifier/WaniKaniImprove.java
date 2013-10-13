package com.wanikani.androidnotifier;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WaniKaniImprove {

	private class State implements Runnable {
		
		private String item;
		
		private String type;
		
		private String questionType;

		public State (String item, String type, String questionType)
		{
			this.item = item;
			this.type = type;
			this.questionType = questionType;
		}
		
		public void publish ()
		{
			activity.runOnUiThread (this);
		}
		
		public void run ()
		{
			Toast.makeText (activity, "item: " + item, Toast.LENGTH_LONG).show();
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
"    wknWanikaniImprove.save (currentItem, currentType, currentQuestionType);" +
"    checkAnswer();";
	
	private State currentState;
	
	private Activity activity;
	
	private FocusWebView wv;
	
	public void init (Activity activity, FocusWebView wv)
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
	public void save (String currentItem, String currentType, String currentQuestionType)
	{
		currentState = new State (currentItem, currentType, currentQuestionType);
	}
	
	@JavascriptInterface
	public void show ()
	{
		if (currentState != null)
			currentState.publish ();
	}
	
	
}
			