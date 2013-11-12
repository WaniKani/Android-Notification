package com.wanikani.androidnotifier;

/*
 * A port of the alucardeck's WK Reorder script (http://www.wanikani.com/chat/api-and-third-party-apps/3878)
 * The script can be downloaded from here: http://userscripts.org/scripts/review/182793
 * I had to replace events with direct function calls because they don't seem to work on all android devices
 */
public class LessonOrder {
	
	public static final String JS_CODE =
"function get(id) {\r\n" + 
"    if (id && typeof id === 'string') {\r\n" + 
"        id = document.getElementById(id);\r\n" + 
"    }\r\n" + 
"    return id || null;\r\n" + 
"}\r\n" + 
"\r\n" + 
"function init(){\r\n" + 
"	console.log('init() start');\r\n" + 
"	var stats = $(\"#stats\")[0];\r\n" + 
"    var t = document.createElement('div');\r\n" + 
"    stats.appendChild(t);\r\n" + 
"    t.innerHTML = '<div id=\"divSt\">Not Ordered!</div><button id=\"reorderBtn\" type=\"button\" onclick=\"window.wknReorder();\">Reorder!</button></div>';\r\n" + 
//"    window.addEventListener('reorderWK',reorder); \r\n" + 
"    console.log('init() end');\r\n" + 
"}\r\n" + 
"\r\n" + 
//"function reorder(){\r\n" +
"window.wknReorder = function() {\r\n" +
"    console.log('reorder() start');\r\n" + 
"    var divSt = get(\"divSt\");\r\n" + 
"    var reorderBtn = get(\"reorderBtn\");\r\n" + 
"    reorderBtn.style.visibility=\"hidden\";\r\n" + 
"    divSt.innerHTML = '<img src=\"http://images.wikia.com/nonsensopedia/images/5/58/Rickroll.gif\"/>';\r\n" + 
"    var character = get(\"main-info\");\r\n" + 
"    character.innerHTML = '<img src=\"http://stream1.gifsoup.com/view3/1732953/trololo-lol-short-o.gif\"/>';\r\n" + 
"    var hb = get(\"header-buttons\");\r\n" + 
"    hb.innerHTML = '<img src=\"http://stream1.gifsoup.com/view3/1732953/trololo-lol-short-o.gif\"/>';\r\n" + 
"    console.log('reorder() end');\r\n" + 
"}\r\n" + 
"\r\n" + 
// Glue code //
"if ($(\"#divSt\").length > 0) {" +
"    $(\"#divSt\").show (); " +
"} else {" +
"    init();\r\n" + 
"    console.log('script load end');" +
"}";			

	public static final String JS_UNINIT_CODE =
"$(\"#divSt\").hide ();";
}