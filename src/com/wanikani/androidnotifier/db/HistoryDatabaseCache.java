package com.wanikani.androidnotifier.db;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;

import com.wanikani.androidnotifier.graph.Pager;
import com.wanikani.androidnotifier.graph.Pager.Interval;
import com.wanikani.wklib.SRSDistribution;

public class HistoryDatabaseCache {

	public class PageSegment {
		
		public Interval interval;
		
		public HistoryDatabase.FactType type;
		
		public List<SRSDistribution> srsl;
		
		public PageSegment (int from, int to, HistoryDatabase.FactType type)
		{
			interval = new Interval (from, to);
			this.type = type;
			if (type == HistoryDatabase.FactType.PARTIAL ||
				type == HistoryDatabase.FactType.COMPLETE)
				srsl = new Vector<SRSDistribution> ();
		}
	}
	
	public class Page {
		
		Date lad;
		
		Interval interval;
		
		List<PageSegment> segments;
		
		public Page (Interval interval)
		{
			this.interval = interval;
			
			lad = new Date ();
			
			segments = new Vector<PageSegment> (1);
		}
		
	}
	
	public static abstract class DataSource implements Pager.DataSource {

		HistoryDatabaseCache dbc;
		
		Pager pager;
		
		Interval interval;
		
		public DataSource (HistoryDatabaseCache dbc)
		{
			this.dbc = dbc;
		}
		
		public void requestPage (Interval interval, Pager pager)
		{
			this.pager = pager;
			this.interval = interval;
			
			dbc.getPage (this, interval);
		}				
		
		public void pageAvailable (Page page)
		{
			Pager.DataSet ds;
			Pager.Segment segment;
			
			if (!page.interval.equals (interval))
				return;
			
			ds = new Pager.DataSet (interval);
			for (PageSegment pseg : page.segments) {
				switch (pseg.type){
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
		
		protected abstract void fillPartialSegment (Pager.Segment segment, PageSegment pseg);

		protected abstract void fillSegment (Pager.Segment segment, PageSegment pseg);
	}
	
	private class LoadPageTask extends AsyncTask<Interval, Void, Page> {

		Context ctxt;
		
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
			
			return page;
		}	
		
		@Override
		protected void onPostExecute (Page page)
		{
			pageAvailable (page);
		}
	}
	
	Hashtable<Integer, Page> pages;

	Context ctxt;
	
	List<DataSource> dsources;
	
	public HistoryDatabaseCache ()
	{
		pages = new Hashtable<Integer, Page> ();
		dsources = new Vector<DataSource> ();
	}	
	
	public void open (Context ctxt)
	{
		this.ctxt = ctxt;
	}
	
	public void close ()
	{
		ctxt = null;
	}
	
	public void addDataSource (DataSource dsource)
	{
		dsources.add (dsource);
	}
	
	public void getPage (DataSource dsource, Interval interval)
	{
		Page page;
		
		page = pages.get (interval.start);
		if (page != null)
			dsource.pageAvailable (page);
		else if (ctxt != null)
			new LoadPageTask (ctxt).execute (interval);
		else
			pageAvailable (dummyPage (interval));
	}
	
	private Page dummyPage (Interval i)
	{
		Page ans;
		
		ans = new Page (i);
		ans.segments.add (new PageSegment (i.start, i.stop, HistoryDatabase.FactType.MISSING));
		
		return ans;
	}
	
	private void pageAvailable (Page page)
	{
		pages.put (page.interval.start, page);
		for (DataSource dsource : dsources)
			dsource.pageAvailable (page);
	}
	
	public void flush ()
	{
		pages.clear ();
	}
}
