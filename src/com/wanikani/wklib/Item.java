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

public class Item {
	
	public enum Type {
		
		RADICAL,	
		KANJI,
		VOCAB
	}
	
	public class Performance {
		
		public int correct;
		
		public int incorrect;
		
		public int maxStreak;
		
		public int currentStreak;
		
	};
	
	public class Stats {
		
		public SRSDistribution.Level srs;
		
		public Date unlockedDate;
		
		public Date availableDate;
		
		public Date burnedDate;
		
		public boolean burned;
				
		public Item.Performance reading;
		
		public Item.Performance meaning;
		
	};
	
	public Type type;
	
	public String character;
	
	public String meaning;
	
	public int level;
	
	public Stats stats;
	
	protected Item (Type type)
	{
		this.type = type;
	}
	
}
