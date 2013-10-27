package com.wanikani.androidnotifier;

public class ReviewOrder {
	
	public static final String JS_CODE =
"function init(){\r\n" + 
"	console.log('start');\r\n" + 
"    var cur = $.jStorage.get(\"currentItem\");\r\n" + 
"	var qt = $.jStorage.get(\"questionType\");\r\n" + 
"	var actList = $.jStorage.get(\"activeQueue\");\r\n" + 
"	var revList = $.jStorage.get(\"reviewQueue\");\r\n" + 
"    \r\n" + 
"    console.log(cur);\r\n" + 
"    var curt = cur.kan?'kan':cur.voc?'voc':'rad';\r\n" + 
"    \r\n" + 
"    var removedCount = 0;\r\n" + 
"    for(var i=0;i<actList.length;i++){\r\n" + 
"        var it = actList[i];\r\n" + 
"        var itt = cur.kan?'kan':cur.voc?'voc':'rad';\r\n" + 
"        console.log(it);\r\n" + 
"        if(!(curt==itt&&cur.id==it.id)){\r\n" + 
"           actList.splice(i--,1);\r\n" + 
"           revList.push(it);\r\n" + 
"           removedCount++;\r\n" + 
"        }\r\n" + 
"    }\r\n" + 
"    console.log('removed: '+removedCount);\r\n" + 
"    \r\n" + 
"    for(var i=revList.length-1;i>=0;i--){\r\n" + 
"        var it=revList[i];\r\n" + 
"        if(it.kan){\r\n" + 
"           revList.splice(i,1);\r\n" + 
"           revList.push(it);\r\n" + 
"           console.log(it.kan);\r\n" + 
"        }\r\n" + 
"    }\r\n" + 
"    for(var i=revList.length-1;i>=0;i--){\r\n" + 
"        var it=revList[i];\r\n" + 
"        if(it.rad){\r\n" + 
"           revList.splice(i,1);\r\n" + 
"           revList.push(it);\r\n" + 
"           console.log(it.rad);\r\n" + 
"        }\r\n" + 
"    }\r\n" + 
"    \r\n" + 
"    for(var i=0;i<removedCount;i++){\r\n" + 
"        actList.push(revList.pop());\r\n" + 
"    }\r\n" + 
"    $.jStorage.set(\"reviewQueue\",revList);\r\n" + 
"    $.jStorage.set(\"activeQueue\",actList);\r\n" + 
"    \r\n" + 
"    var stats = $(\"#stats\")[0];\r\n" + 
"    var t = document.createElement('div');\r\n" + 
"    stats.appendChild(t);\r\n" + 
"    \r\n" + 
"    t.innerHTML = '<div id=\"wkroStatus\"><table align=\"right\"><tbody>'+\r\n" + 
"        '<tr><td>Rad</td><td align=\"right\"><span id=\"wkroRadCount\"></span></td></tr>'+\r\n" + 
"        '<tr><td>Kan</td><td align=\"right\"><span id=\"wkroKanCount\"></span></td></tr>'+\r\n" + 
"        '<tr><td>Voc</td><td align=\"right\"><span id=\"wkroVocCount\"></span></td></tr>'+\r\n" + 
"        '</tbody></table></div>';\r\n" + 
"    \r\n" + 
"    $.jStorage.listenKeyChange(\"activeQueue\",displayUpdate);\r\n" + 
"    displayUpdate();\r\n" + 
"	console.log('end');\r\n" + 
"}\r\n" + 
"\r\n" + 
"function displayUpdate(){\r\n" + 
"    var radC = 0, kanC = 0, vocC = 0;\r\n" + 
"    var list = $.jStorage.get(\"reviewQueue\").concat($.jStorage.get(\"activeQueue\"));\r\n" + 
"    console.log(list.length);\r\n" + 
"    for(var i=0;i<list.length;i++){\r\n" + 
"        var it=list[i];\r\n" + 
"        if(it.rad)\r\n" + 
"        	radC++;\r\n" + 
"        else if(it.kan)\r\n" + 
"           	kanC++;\r\n" + 
"        else if(it.voc)\r\n" + 
"           	vocC++;\r\n" + 
"    }\r\n" + 
"    console.log(radC+\" \"+kanC+\" \"+vocC);\r\n" + 
"    var radSpan = $(\"#wkroRadCount\")[0];\r\n" + 
"    var kanSpan = $(\"#wkroKanCount\")[0];\r\n" + 
"    var vocSpan = $(\"#wkroVocCount\")[0];\r\n" + 
"    radSpan.innerHTML = radC;\r\n" + 
"    kanSpan.innerHTML = kanC;\r\n" + 
"    vocSpan.innerHTML = vocC;\r\n" + 
"}\r\n" + 

// Glue code //
"if ($(\"#wkroStatus\").length > 0) {" +
"    $(\"#wkroStatus\").show (); " +
"} else {" +
"    setTimeout(init,8000);\r\n" + 
"    console.log('load');" +
"}";			

	public static final String JS_UNINIT_CODE =
"$(\"#wkroStatus\").hide ();";
}