package com.wanikani.androidnotifier.db;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;

import com.wanikani.androidnotifier.db.HistoryDatabase.FactType;
import com.wanikani.androidnotifier.graph.Pager;
import com.wanikani.androidnotifier.graph.Pager.Interval;
import com.wanikani.wklib.SRSDistribution;

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
 * A wrapper of the {@link HistoryDatabase} class that implements the
 * {@link Pager} interfaces to retrieve data from the facts table.
 * While the pager takes care of paging requests, this class actually
 * retrieves them from the database (and caches requests, of course).
 * Since many plots may render different aspects of the same data,
 * it is possible to register multiple datasources, each one translating
 * raw db pages into pager datasets ready to be plotted.
 * To do this, applications must extend the abstract {@link DataSource}
 * class and implements the methods that perform this translation.   
 */
public class HistoryDatabaseCache {

	/**
	 * A subset of a page with homogeneous row types.
	 */
	public class PageSegment {
		
		/** This segment's interval */
		public Interval interval;
		
		/** The type of info contained */
		public HistoryDatabase.FactType type;
		
		/** The actual data. If data is missing (i.e. {@link #type} is
		 *  {@link FactType#MISSING}), this field is <code>null</code> */
		public List<SRSDistribution> srsl;
		
		/**
		 * Constructor.
		 * @param from start of segment
		 * @param to end of segment
		 * @param type facts type
		 */
		public PageSegment (int from, int to, HistoryDatabase.FactType type)
		{
			interval = new Interval (from, to);
			this.type = type;
			if (type == HistoryDatabase.FactType.PARTIAL ||
				type == HistoryDatabase.FactType.COMPLETE)
				srsl = new Vector<SRSDistribution> ();
		}
	}
	
	/**
	 * A DB page, which is a set of consecutive non overlapping
	 * segments. Typically instances of this class map 1:1 
	 * to {@link Pager.DataSet}, the difference being that
	 * this is class is an image of the facts table, while a dataset
	 * is further elaborated to contain the samples of a chart.
	 */
	public class Page {
		
		/** The last time this pages has been accessed */
		Date lad;
		
		/** The interval of time covered by this page */
		Interval interval;
		
		/** The segments comprising this page */
		List<PageSegment> segments;
		
		/**
		 * Constructor
		 * @param interval the interval of time covered by this page
		 */
		public Page (Interval interval)
		{
			this.interval = interval;
			
			lad = new Date ();
			
			segments = new Vector<PageSegment> (1);
		}
		
		/**
		 * Should be called each time this page is accessed, to make
		 * sure the access timestamp is freshened.
		 */
		public void access ()
		{
			lad = new Date ();
		}		
	}
	
	/**
	 * A comparator that sorts pages by their last access time
	 */
	private static class PageAgeComparator implements Comparator<Page> {
		
		/** Static instance. No need to create new instances */
		public static PageAgeComparator INSTANCE = new PageAgeComparator ();
		
		@Override
		public int compare (Page p1, Page p2)
		{
			return - p1.lad.compareTo (p2.lad);
		}
		
	}
	
	/**
	 * A partial implementation of the {@link Pager.DataSource} interface,
	 * providing an implementation of the 
	 * {@link Pager.DataSource#requestPage(Interval, Pager)}
	 * method which leverages the cache functionalities.
	 * Applications should extend this class, implement the translations
	 * methods and register instances on the cache.
	 */
	public static abstract class DataSource implements Pager.DataSource {

		/** The cache */
		private HistoryDatabaseCache dbc;
		
		/** The pager */
		private Pager pager;
		
		/** The interval of the last requested page */
		private Interval interval;
		
		/**
		 * Constructor.
		 * @param dbc the cache
		 */
		public DataSource (HistoryDatabaseCache dbc)
		{
			this.dbc = dbc;
		}
		
		@Override
		public void requestPage (Interval interval, Pager pager)
		{
			this.pager = pager;
			this.interval = interval;
			
			dbc.getPage (this, interval);
		}				
		
		/**
		 * Called when a page becomes available. Actually the cache
		 * broadcasts this event to all the registered datasources,
		 * so this method takes care of checking whether the page
		 * is of interest to this instance. After that, this
		 * method calls the translation methods to transform
		 * the raw pages into datasets. At the end of this process,
		 * it hands the dataset to the pager. 
		 * @param page the page just retrieved
		 */
		private void pageAvailable (Page page)
		{
			Pager.DataSet ds;
			Pager.Segment segment;
			
			if (!page.interval.equals (interval))
				return;
			
			ds = new Pager.DataSet (interval);
			for (PageSegment pseg : page.segments) {
				switch (pseg.type) {
				case MISSING:
					segment = new Pager.Segment (Pager.SegmentType.MISSING, pseg.interval);
					break;
					
				case PARTIAL:
					segment = new Pager.Segment (Pager.SegmentType.VALID, pseg.interval);
					fillPartialSegment (segment, pseg);
					break;
					
				case COMPLETE:
					segment = new Pager.Segment (Pager.SegmentType.VALID, pseg.interval);
					fillSegment (segment, pseg);
					break;		
					
				default:
					continue;
				}
				
				ds.segments.add (segment);
			}
				
			interval = pager.pageAvailable (ds);
			if (interval != null)
				dbc.getPage (this, interval);			
		}
		
		/**
		 * Translates a partial (reconstructed) db segment into a dataset segment. 
		 * @param segment the input segment
		 * @param pseg the output segment
		 */
		protected abstract void fillPartialSegment (Pager.Segment segment, PageSegment pseg);

		/**
		 * Translates a complete db segment into a dataset segment. 
		 * @param segment the input segment
		 * @param pseg the output segment
		 */
		protected abstract void fillSegment (Pager.Segment segment, PageSegment pseg);
	}
	
	/**
	 * The task that performs actual data retrieval.
	 */
	private class LoadPageTask extends AsyncTask<Interval, Void, Page> {

		/** The context */
		Context ctxt;
		
		/**
		 * Constructor
		 * @param ctxt the context
		 */
		public LoadPageTask (Context ctxt)
		{
			this.ctxt = ctxt;
		}
		
		@Override
		protected Page doInBackground (Interval... interval)
		{
			HistoryDatabase.FactType ltype, type;
			HistoryDatabase hdb;
			PageSegment segment;
			Page page;
			Cursor c;
			int i, day;
			
			synchronized (HistoryDatabase.MUTEX) { 
				page = new Page (interval [0]);
				hdb = new HistoryDatabase (ctxt);
				c = null;
				try {
					i = interval [0].start;
					hdb.openR ();
					c = hdb.selectFacts (interval [0].start, interval [0].stop);
					ltype = null;
					segment = null;
					day = interval [0].start - 1; /* A safe default in case we got an empty set */
					while (c.moveToNext ()) {
						day = HistoryDatabase.Facts.getDay (c);
						if (day != i) {
							page.segments.add (new PageSegment (i, day - 1, 
								                            	ltype = HistoryDatabase.FactType.MISSING));
							i = day;
						}
						
						type = HistoryDatabase.Facts.getType (c);
						if (type != ltype) {
							ltype = type;
							segment = new PageSegment (i, i, type);
							page.segments.add (segment);
						}
						if (type != HistoryDatabase.FactType.MISSING)
							segment.srsl.add (HistoryDatabase.Facts.getSRSDistribution (c));
					
						segment.interval.stop = i;
						i++;
					}
				
					if (day < interval [0].stop)
						page.segments.add (new PageSegment (day + 1, interval [0].stop, 
										   ltype = HistoryDatabase.FactType.MISSING));
				} catch (SQLException e) {
					return dummyPage (interval [0]);
				} finally {
					if (c != null)					
						c.close ();
					hdb.close ();
				}
			}
			
			return page;
		}	
		
		@Override
		protected void onPostExecute (Page page)
		{
			pageAvailable (page);
		}
	}
	
	/** The cached pages */
	Hashtable<Integer, Page> pages;

	/** The context */
	Context ctxt;
	
	/** Registered datasouces */
	List<DataSource> dsources;
	
	/** Number of pages per datasource after an LRU cleanup */
	private static final int PAGES_PER_SOURCE_LO = 4;
	
	/** Number of pages per datasource that trigger a LRU cleanup */
	private static final int PAGES_PER_SOURCE_HI = 6;
	
	/** 
	 * Constructor.
	 */
	public HistoryDatabaseCache ()
	{
		pages = new Hashtable<Integer, Page> ();
		dsources = new Vector<DataSource> ();
	}	
	
	/**
	 * Called when an activity becomes available
	 * @param ctxt the context
	 */
	public void open (Context ctxt)
	{
		this.ctxt = ctxt;
	}
	
	/**
	 * Called when an activity becomes unavailable
	 */
	public void close ()
	{
		ctxt = null;
	}
	
	/**
	 * Registers a datasource
	 * @param dsource the ds to be registered
	 */
	public void addDataSource (DataSource dsource)
	{
		dsources.add (dsource);
	}
	
	/**
	 * Retrieves a page, looking on the page cache first.
	 * @param dsource the requesting data source
	 * @param interval the requeted interval
	 */
	private void getPage (DataSource dsource, Interval interval)
	{
		Page page;
		
		page = pages.get (interval.start);
		if (page != null) {
			page.access ();
			dsource.pageAvailable (page);
		} else if (ctxt != null)
			new LoadPageTask (ctxt).execute (interval);
		else
			pageAvailable (dummyPage (interval));
	}
	
	/**
	 * Creates a dummy page. Called when the context is not available
	 * @param i the interval
	 * @return a dummy page
	 */
	private Page dummyPage (Interval i)
	{
		Page ans;
		
		ans = new Page (i);
		ans.segments.add (new PageSegment (i.start, i.stop, HistoryDatabase.FactType.MISSING));
		
		return ans;
	}
	
	/**
	 * Called by the task implementation after the page has been retrieved
	 * from the DB. Broadcasts this new page to all registered datasources
	 * and stores the page into the cache. 
	 * @param page the page
	 */
	private void pageAvailable (Page page)
	{
		makeRoom ();
		pages.put (page.interval.start, page);
		for (DataSource dsource : dsources)
			dsource.pageAvailable (page);
	}
	
	/**
	 * Called before inserting a page into the cache.
	 * If the cache size has been exceeded, a simple LRU cleanup
	 * is performed.
	 */
	private void makeRoom ()
	{
		Vector<Page> v;
		int i, expected;
		
		expected = PAGES_PER_SOURCE_HI * dsources.size () - 1;
		if (pages.size () > expected) {
			expected = PAGES_PER_SOURCE_LO * dsources.size () - 1;
			v = new Vector<Page> (pages.values ());
			Collections.sort (v, PageAgeComparator.INSTANCE);
			pages.clear ();
			for (i = 0; i < expected; i++)
				pages.put (v.get (i).interval.start, v.get (i));
		}
	}
	
	/**
	 * Clears the cache.
	 */
	public void flush ()
	{
		pages.clear ();
	}
}
