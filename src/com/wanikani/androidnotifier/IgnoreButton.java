package com.wanikani.androidnotifier;

/* -- Original description and copyright statements -- */

//==UserScript==
//@name        Wanikani Override
//@namespace   wkoverride
//@description Adds an "Ignore Answer" button during reviews that makes WaniKani ignore the current answer (useful if, for example, you made a stupid typo)
//@include     http://www.wanikani.com/review/session*
//@version     1.0.4
//@author      LordGravewish
//@grant       GM_addStyle
//@downloadURL https://userscripts.org/scripts/source/174048.user.js
//@updateURL   https://userscripts.org/scripts/source/174048.meta.js
//==/UserScript==

/*
*  ====  Wanikani  Override  ====
*    ==   by LordGravewish   ==
*
*  One of my biggest peeves with Wanikani is how once you get something wrong,
*  you can't change it. If you type too fast and make a typo, or if the spell checker
*  doesn't	like you, you're out of luck. It's really frustrating to know an answer but
*  have Wanikani deny it to you because it wants the '-ing' form of the verb, but you
*  typed the infinitive.
*  Sometimes you get lucky, and the spell checker is lenient enough to mark the answer
*  correctly. Other times, that's not the case.
*  Then, there's the other type of mistakes. The ones where you type a reading but were
*  supposed to type a meaning, or the opposite. Those are really annoying, since most
*  times you actually knew the correct reading or meaning respectively.
*
*
*  My problem is that the decision was completely out of my hands. My objective is to
*  learn Kanji, and I'm not going to cheat since that wouldn't bring me closer to where
*  I want to be, but Wanikani decides that I can't be trusted and does not let me correct it.
*
*  But now, with the client-side reviews update, we can finally do something about all this!
*
*
*
*  This userscript fixes this problem, by adding an "Ignore Answer" button to the footer
*  during reviews. This button can be clicked anytime an answer has been marked as wrong by
*  Wanikani.
*
*  Once clicked, the current answer will be changed from "incorrect" (red background)
*  to "ignored" (yellow background), and Wanikani will forget you even answered it.
*  This way, the item will appear again later on during the review cycle, effectively
*  giving you a second chance to get it right without stupid typos or the spell checker
*  getting in the way.
*
*
*  DISCLAIMER:
*  I am not responsible for any problems caused by this script.
*  This script was developed on Firefox 22.0 with Greasemonkey 1.10.
*  It was also tested on Chrome 28.0.1500.72 with Tampermonkey 3.3.3487.
*  Because I'm just one person, I can't guarantee the script works anywhere else.
*
*  Also, anyone using this script is responsible for using it correctly.
*  This should be used only if you make an honest mistake but actually knew the correct
*  answer. Using it in any other way will harm your Kanji learning process,
*  cheating will only make learning Japanese harder and you'll end up harming only yourselves!
*/

/*
*  This script is licensed under the Creative Commons License
*  "Attribution-NonCommercial 3.0 Unported"
*  
*  More information at:
*  http://creativecommons.org/licenses/by-nc/3.0/
*/

/*
*	=== Changelog ===~
*
*  1.0.4 (14 August 2013)
*  - Fixed the '~' hotkey in Chrome.
*
*  1.0.3 (5 August 2013)
*  - Wanikani update changed the review URL. While the script worked, it also affected the reviews summary page. This has been fixed
*
*  1.0.2 (29 July 2013)
*  - Made '~' a hotkey, as requested by some users of the Wanikani forums.
*  - Script now updates the question count and error count correctly.
*
*  1.0.1 (24 July 2013)
*  - Fixed a bug that would cause "Null" to appear instead of "Reading" or "Meaning" after ignoring a certain Vocabulary.
*  - Tested the script on Google Chrome with Tampermonkey (the script does not work natively).
*
*  1.0.0 (23 July 2013)
*  - First release.
*/

/*
 * I've basically copied here only the contents of unsafeWindow.WKO_ignoreAnswer, while the 
 * triggering is made by injecters of this class.
 * 		-- Alberto
 */
public class IgnoreButton {
	
	public static final String JS_CODE = 
"/* Check if the current item was answered incorrectly */\r\n" + 
"   var elmnts = document.getElementsByClassName(\"incorrect\");\r\n" + 
"	if(elmnts[0] === undefined)\r\n" + 
"	{\r\n" + 
"		alert(\"Wanikani Override error: Current item wasn't answered incorrectly!\");\r\n" + 
"		return false;\r\n" + 
"	}\r\n" + 
"	\r\n" + 
"	/* Grab information about current question */\r\n" + 
"	var curItem = $.jStorage.get(\"currentItem\");\r\n" + 
"	var questionType = $.jStorage.get(\"questionType\");\r\n" + 
"  \r\n" + 
"	/* Build item name */\r\n" + 
"	var itemName;\r\n" + 
"	\r\n" + 
"	if(curItem.rad)\r\n" + 
"		itemName = \"r\";\r\n" + 
"	else if(curItem.kan)\r\n" + 
"		itemName = \"k\";\r\n" + 
"	else\r\n" + 
"		itemName = \"v\";\r\n" + 
"	\r\n" + 
"	itemName += curItem.id;\r\n" + 
"	\r\n" + 
"	/* Grab item from jStorage.\r\n" + 
"	 * \r\n" + 
"	 * item.rc and item.mc => Reading/Meaning Completed (if answered the item correctly)\r\n" + 
"	 * item.ri and item.mi => Reading/Meaning Invalid (number of mistakes before answering correctly)\r\n" + 
"	 */\r\n" + 
"	var item = $.jStorage.get(itemName) || {};\r\n" + 
"	\r\n" + 
"	/* Update the item data to ignore the fact we got it wrong this time */\r\n" + 
"	if(questionType === \"meaning\")\r\n" + 
"  	{\r\n" + 
"		if(typeof item.mi == \"undefined\")\r\n" + 
"		{\r\n" + 
"	  		alert(\"Wanikani Override error: i.mi undefined.\");\r\n" + 
"	  		return false;\r\n" + 
"	  	}\r\n" + 
"	  	else if(item.mi <= 0)\r\n" + 
"	  	{\r\n" + 
"	  		alert(\"Wanikani Override error: i.mi <= 0\");\r\n" + 
"	  		return false;\r\n" + 
"	  	}\r\n" + 
"	  	\r\n" + 
"	  	item.mi -= 1;\r\n" + 
"	  	delete item.mc;\r\n" + 
"	}\r\n" + 
"	else\r\n" + 
"	{\r\n" + 
"		if(typeof item.ri == \"undefined\")\r\n" + 
"		{\r\n" + 
"	  		alert(\"Wanikani Override error: i.ri undefined.\");\r\n" + 
"	  		return false;\r\n" + 
"	  	}\r\n" + 
"	  	else if(item.ri <= 0)\r\n" + 
"	  	{\r\n" + 
"	  		alert(\"Wanikani Override error: i.ri <= 0\");\r\n" + 
"	  		return false;\r\n" + 
"	  	}\r\n" + 
"	  	\r\n" + 
"	  	item.ri -= 1;\r\n" + 
"	  	delete item.rc;\r\n" + 
"	}\r\n" + 
"	\r\n" + 
"	/* Save the new state back into jStorage */\r\n" + 
"	$.jStorage.set(itemName, item);\r\n" + 
"	\r\n" + 
"	/* Decrement the questions counter and wrong counter */\r\n" + 
"	var wrongCount = $.jStorage.get(\"wrongCount\");\r\n" + 
"	var questionCount = $.jStorage.get(\"questionCount\");\r\n" + 
"	$.jStorage.set(\"wrongCount\", wrongCount-1);\r\n" + 
"	$.jStorage.set(\"questionCount\", questionCount-1);\r\n" + 
"	\r\n" + 
"	/* Make the answer field yellow instead of red */\r\n" + 
"	$(\"#answer-form fieldset\").removeClass(\"incorrect\");\r\n" + 
"	$(\"#answer-form fieldset\").addClass(\"WKO_ignored\");\r\n" + 
"	\r\n" + 
"	return true;";
}
