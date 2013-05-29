package com.wanikani.wklib;

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

public enum SRSLevel {

	APPRENTICE, GURU, MASTER, ENLIGHTEN, BURNED;
	
	public static SRSLevel fromString (String s)
	{
		if (s.equals ("apprentice"))
			return APPRENTICE;
		else if (s.equals ("guru"))
			return GURU;
		else if (s.equals ("master"))
			return MASTER;
		else if (s.equals ("enlighten"))
			return ENLIGHTEN;
		else if (s.equals ("burned"))
			return BURNED;
		
		return null;
	}
}
