package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Map;
import java.util.Vector;

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

/**
 * A support class used by {@link TYPlot} to retrieve data from a datasource.
 * The plot requests arbitrary sized intervals of time. The main object of this
 * class is to convert these requests into page (i.e. even sized, covering, non
 * overlapping interval) requests, because they can be cached more efficiently.       
 */
public class Pager {

	/**
	 * An interval of time.
	 */
	public static class Interval {
		
		/** The lower bound */
		public int start;
		
		/** The upper bound */
		public int stop;
		
		/**
		 * Constructor.
		 * @param start the lower bound
		 * @param stop the upper bound
		 */
		public Interval (int start, int stop)
		{
			this.start = start;
			this.stop = stop;
		}
		
		/**
		 * Returns the number of samples contained in this interval. 
		 * @return the size
		 */
		public int getSize ()
		{
			return stop - start + 1;
		}

		@Override
		public boolean equals (Object o)
		{
			Interval oi;
			
			if (!(o instanceof Interval))
				return false;
			
			oi = (Interval) o;
			
			return oi.start == start && oi.stop == stop;
		}
	}
	
	/**
	 * A series of samples.
	 */
	public static class Series {
		
		/** The color */
		public int color;
		
		/** The name */
		public String name;
		
		/**
		 * Constructor
		 * @param color the color
		 * @param name the name
		 */
		public Series (int color, String name)
		{
			this.color = color;
			this.name = name;
		}
				
	}
	
	/**
	 * A point in time when something remarkable happened.
	 */
	public static class Marker {

		/** The color */
		public int color;

		/** The name of the milestone */
		public String name;
		
		/**
		 * Constructor.
		 * @param color the color
		 * @param name the name of the milestone 
		 */
		public Marker (int color, String name)
		{
			this.color = color;
			this.name = name;
		}
	}
		
	
	/**
	 * The type of segment
	 */
	public static enum SegmentType {
		
		/** No data in this segment*/
		MISSING,
		
		/** Data segment */
		VALID 
		
	}
	
	/**
	 * A subset of a page with homogeneous {@link SegmentType}.  
	 * Given the possible values of this enumeration, a segment either contains
	 * no data, or contains all the samples contained in its interval.
	 */
	public static class Segment {
		
		/** The type of segment */
		public SegmentType type;
		
		/** The interval */
		public Interval interval;
		
		/** The subplots */
		public List<Series> series;
		
		/** The data of each subplot. First index is the subplot, while the
		 *  other is the actual data */
		public float data [][];
	
		/**
		 * Constructor. This is typically used when the segment contains valid data
		 * @param type the segment type
		 * @param interval the interval covered by this segment
		 * @param series the subplots
		 * @param data the data
		 */
		public Segment (SegmentType type, Interval interval,
						List<Series> series, float data [][])
		{
			this.type = type;
			this.interval = interval;
			this.series = series;
			this.data = data;
		}

		/**
		 * Constructor. This is typically used when the segment contains no data
		 * @param type the segment type
		 * @param interval the interval covered by this segment
		 */
		public Segment (SegmentType type, Interval interval)
		{
			this.type = type;
			this.interval = interval;
		}
		
		/**
		 * Returns the point in time inside this segment which is nearest
		 * to a given point in time. 
		 * @param ceil input point in time
		 * @return the nearest point inside this segment
		 */
		public int trim (int ceil)
		{
			return Math.min (ceil, interval.stop);
		}

	}
	
	/**
	 * A sequence of contiguous samples. This object is both used to return
	 * data from the datasource to the pager, and from the pager to the plot.
	 * In the former case the interval is paged. In the latter, it is
	 * arbitrarily sized and aligned.  
	 */
	public static class DataSet {
		
		/** The interval */
		public Interval interval;
		
		/** The data */
		public List<Segment> segments;
		
		/**
		 * Constructor
		 * @param interval the interval
		 */
		public DataSet (Interval interval)
		{
			this.interval = interval;
			segments = new Vector<Segment> ();
		}		
	}
	
	/**
	 * Implementations of this class retrieve the actual samples and give access
	 * to other auxiliary data.
	 */
	public static interface DataSource {
	
		/**
		 * Returns an optional list of milestones
		 * @return the milestones
		 */
		public Map<Integer, Marker> getMarkers ();
		
		/**
		 * The complete list of subplots. Each dataset returned by this 
		 * dataset must contain a subset of this collection. These subsets
		 * can also be overlapping.
		 */
		public List<Series> getSeries ();
		
		/**
		 * Returns a page of data. All pages have the same size, to
		 * make caching more efficient.
		 * @param interval the requested interval
		 * @param pager the pager to notify when data is available
		 */
		public void requestPage (Interval interval, Pager pager);
		
		/**
		 * The biggest sample in the whole series. Used to fix
		 * the Y scale. 
		 * @return the biggest sample value
		 */
		public float getMaxY ();
		
		/**
		 * Called when the user has requested data reconstruction
		 */
		public void fillPartial ();		
	}
	
	/**
	 * The interface implemented by plots to receive notifications
	 * when as becomes available.
	 */
	public static interface DataSink {

		/**
		 * Called when requested data is available.
		 * This is a callback invoked as a result to 
		 * {@link Pager#requestData(Interval)}.
		 * @param ds the data
		 */
		public void dataAvailable (DataSet ds);
		
	}
	
	/**
	 * A pending request. This is the class that performs the actual
	 * fragmentation and reassembly.
	 */
	private class Request {

		/** The data gathered so far */
		DataSet dataSet;
		
		/** The beginning of the next interval we are waiting for */
		int nextDay;

		/**
		 * Constructor.
		 * @param interval the requested interval
		 */
		public Request (Interval interval)
		{
			dataSet = new DataSet (interval);
			nextDay = interval.start - (interval.start % PAGE_SIZE);
		}
		
		/**
		 * Returns the next page this class needs to reconstruct data.
		 * @return the next page interval, or <tt>null</tt> if no more data
		 * is needed
		 */
		public Interval nextInterval ()
		{
			return nextDay <= dataSet.interval.stop ?
					new Interval (nextDay, nextDay + PAGE_SIZE - 1) : null;
		}
		
		/**
		 * Called by the pager when a new page data is received from the datasource.
		 * @param nds the page data
		 */
		public void feed (DataSet nds)
		{
			/* Not meant for us. Discard */
			if (nds.interval.start != nextDay)
				return;
			
			for (Segment s : nds.segments) {
				if (s.interval.start > dataSet.interval.stop)
					break;
				dataSet.segments.add (s);
				nextDay = s.trim (dataSet.interval.stop) + 1;
			}
		}
		
	}
	
	/** The datasource */
	DataSource dsource;
	
	/** The data sink */
	DataSink dsink;
	
	/** The current pending requets */
	Request request;

	/** The page size */
	private static final int PAGE_SIZE = 30;
	
	public Pager (DataSource dsource, DataSink dsink)
	{
		this.dsource = dsource;
		this.dsink = dsink;
	}
	
	/**
	 * Requests for new data. This method is called by the plot.
	 * If a request is issued when the last one is still pending  
	 * (i.e {@link DataSink#dataAvailable(DataSet)} not called yet), the
	 * pending request is cancelled.
	 * @param interval
	 */
	public void requestData (Interval interval)
	{
		boolean pending;
		
		pending = request != null;
		request = new Request (interval);
		if (!pending)
			dsource.requestPage (request.nextInterval (), this);
	}
	
	/**
	 * Called by the datasource when a page is available.
	 * @param ds page data
	 * @return the next interval to retrieve, or <tt>null</tt> if
	 * no more data is needed at this time
	 */
	public Interval pageAvailable (DataSet ds)
	{
		Interval ans;
		
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
	
	/**
	 * Called when the user requests data reconstruction
	 */
	public void fillPartial ()
	{
		dsource.fillPartial ();
	}

}
