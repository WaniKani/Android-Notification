package com.wanikani.wklib;

import java.util.Date;

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
	
	public int currentLevelRadicalsAvailable;
	
	public int kanjiProgress;
	
	public int kanjiUnlocked;
	
	public int kanjiTotal;
	
	public int currentLevelKanjiAvailable;
	
	public Date currentLevelAvailable;

	ExtendedLevelProgression (ItemLibrary<Radical> rlib, ItemLibrary<Kanji> klib)
	{
		Date now;
		
		currentLevelAvailable = null;
		now = new Date ();
		for (Radical r : rlib.list) {
			radicalsTotal++;
			if (r.stats != null && r.stats.availableDate != null) {
				radicalsUnlocked++;
				if (r.stats.srs != SRSLevel.APPRENTICE)
					radicalsProgress++;
				if (r.stats.availableDate.before (now))
					currentLevelRadicalsAvailable++;
				if (currentLevelAvailable == null ||
					r.stats.availableDate.before (currentLevelAvailable))
					currentLevelAvailable = r.stats.availableDate;
			}
		}
			
		for (Kanji k : klib.list) {
			kanjiTotal++;
			if (k.stats != null && k.stats.availableDate != null) {
				kanjiUnlocked++;
				if (k.stats.srs != SRSLevel.APPRENTICE)
					kanjiProgress++;
				if (k.stats.availableDate.before (now))
					currentLevelKanjiAvailable++;
				if (currentLevelAvailable == null ||
					k.stats.availableDate.before (currentLevelAvailable))
					currentLevelAvailable = k.stats.availableDate;
			}
		}
	}
	
	public ExtendedLevelProgression ()
	{
		/* empty */
	}
}
