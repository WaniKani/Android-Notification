package com.wanikani.androidnotifier;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/* 
 *  Copyright (c) 2013 Alberto Cuda
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class AboutActivity extends Activity {
	
	static int LINKS [] = new int [] {
		R.id.info_link_alucardeck,
		R.id.info_link_edict,
		R.id.info_link_jeshuamorissey,
		R.id.info_link_rui,
		R.id.info_link_seiji
	};
	
	@Override
	public void onCreate (Bundle bundle) 
	{		
		super.onCreate (bundle);
		
		TextView tv;
		int i;

		setContentView (R.layout.about);
		
		for (i = 0; i < LINKS.length; i++) {
			tv = (TextView) findViewById (LINKS [i]);
			if (tv != null)
				tv.setMovementMethod (LinkMovementMethod.getInstance ());
		}
	}
}
