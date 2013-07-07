package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Queue;
import java.util.Vector;

import android.graphics.Color;

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

public class Pager {

	public static class Interval {
		
		public int start;
		
		public int stop;
		
		public Interval (int start, int stop)
		{
			this.start = start;
			this.stop = stop;
		}
		
		public int getSize ()
		{
			return stop - start + 1;
		}

		public boolean equals (Object o)
		{
			Interval oi;
			
			if (!(o instanceof Interval))
				return false;
			
			oi = (Interval) o;
			
			return oi.start == start && oi.stop == stop;
		}
	}
	
	public static class Series {
		
		public int color;
		
		public String name;
		
		public Series (int color, String name)
		{
			this.color = color;
			this.name = name;
		}
				
	}
	
	public static enum SegmentType {
		
		MISSING,
		
		VALID 
		
	}
	
	public static class Segment {
		
		public SegmentType type;
		
		public Interval interval;
		
		public List<Series> series;
		
		public float data [][];
	
		public Segment (SegmentType type, Interval interval,
						List<Series> series, float data [][])
		{
			this.type = type;
			this.interval = interval;
			this.series = series;
			this.data = data;
		}

		public Segment (SegmentType type, Interval interval)
		{
			this.type = type;
			this.interval = interval;
		}
		
		public int trim (int ceil)
		{
			interval.stop = Math.min (ceil, interval.stop);
			
			return interval.stop;
		}

	}
	
	public static class DataSet {
		
		public Interval interval;
		
		public List<Segment> segments;
		
		public DataSet (Interval interval)
		{
			this.interval = interval;
			segments = new Vector<Segment> ();
		}		
	}
	
	public static interface DataSource {
	
		public List<Series> getSeries ();
		
		public void requestPage (Interval interval, Pager pager);
		
		public float getMaxY ();
		
	}
	
	public static interface DataSink {
		
		public void dataAvailable (DataSet ds);
		
	}
	
	private class Request {
		
		DataSet dataSet;
		
		int nextDay;

		public Request (Interval interval)
		{
			dataSet = new DataSet (interval);
			nextDay = interval.start - (interval.start % PAGE_SIZE);
		}
		
		public Interval nextInterval ()
		{
			return nextDay <= dataSet.interval.stop ?
					new Interval (nextDay, nextDay + PAGE_SIZE - 1) : null;
		}
		
		public void feed (DataSet nds)
		{
			/* Not meant for us. Discard */
			if (nds.interval.start != nextDay)
				return;
			
			for (Segment s : nds.segments) {
				if (s.interval.start > dataSet.interval.stop)
					break;
				dataSet.segments.add (s);
				nextDay = s.trim (dataSet.interval.stop);
			}
		}
		
	}
	
	DataSource dsource;
	
	DataSink dsink;
	
	Request request;
	
	private static final int PAGE_SIZE = 30;
	
	public Pager (DataSource dsource, DataSink dsink)
	{
		this.dsource = dsource;
		this.dsink = dsink;
	}
	
	public void requestData (Interval interval)
	{
		boolean pending;
		
		pending = request != null;
		request = new Request (interval);
		if (!pending)
			dsource.requestPage (request.nextInterval (), this);
	}
	
	public Interval pageAvailable (DataSet ds)
	{
		Interval ans;
		
		dsink.dataAvailable (ds);
		if (request == null)
			return null;
		
		request.feed (ds);
		
		ans = request.nextInterval ();
		if (ans == null) {
			dsink.dataAvailable (request.dataSet);
			request = null;
		}
		
		return ans;
	}	
}
