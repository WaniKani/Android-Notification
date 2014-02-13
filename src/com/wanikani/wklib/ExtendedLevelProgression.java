package com.wanikani.wklib;

import org.json.JSONException;
import org.json.JSONObject;

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
public class ExtendedLevelProgression {

	public int radicalsProgress;
	
	public int radicalsUnlocked;
	
	public int radicalsTotal;
	
	public int kanjiProgress;
	
	public int kanjiUnlocked;
	
	public int kanjiTotal;
	
	ExtendedLevelProgression (ItemLibrary<Radical> rlib, ItemLibrary<Kanji> klib)
	{
		for (Radical r : rlib.list) {
			radicalsTotal++;
			if (r.stats != null) {
				radicalsUnlocked++;
				if (r.stats.srs != SRSLevel.APPRENTICE)
					radicalsProgress++;
			}
		}
			
		for (Kanji k : klib.list) {
			kanjiTotal++;
			if (k.stats != null) {
				kanjiUnlocked++;
				if (k.stats.srs != SRSLevel.APPRENTICE)
					kanjiProgress++;
			}
		}
	}
	
	public ExtendedLevelProgression ()
	{
		/* empty */
	}
}
