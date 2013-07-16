package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.SQLException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.androidnotifier.db.HistoryDatabaseCache;
import com.wanikani.androidnotifier.db.HistoryDatabaseCache.Page;
import com.wanikani.androidnotifier.db.HistoryDatabaseCache.PageSegment;
import com.wanikani.androidnotifier.graph.Pager;
import com.wanikani.androidnotifier.graph.Pager.Series;
import com.wanikani.androidnotifier.graph.PieChart;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;
import com.wanikani.androidnotifier.graph.TYChart;
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
 * A fragment that displays some charts that show the user's progress.
 * Currently we show the overall SRS distribution and kanji/vocab
 * progress. In a future release, we may also include historical data
 * (we need to create and maintain a database, though).
 */
public class StatsFragment extends Fragment implements Tab {
	
	private class GetCoreStatsTask extends AsyncTask<Void, Void, HistoryDatabase.CoreStats> {
		
		private Context ctxt;
		
		public GetCoreStatsTask (Context ctxt)
		{
			this.ctxt = ctxt;
		}
		
		@Override
		protected HistoryDatabase.CoreStats doInBackground (Void... v)
		{
			try {
				return HistoryDatabase.getCoreStats (ctxt);
			} catch (SQLException e) {
				return new HistoryDatabase.CoreStats (0, 0, 0, 0, 0, 0);
			}
		}	
		
		@Override
		protected void onPostExecute (HistoryDatabase.CoreStats cs)
		{
			setCoreStats (cs);
		}

	}
	
	private abstract class DataSource extends HistoryDatabaseCache.DataSource {

		protected List<Series> series;
		
		protected int maxY;
		
		public DataSource (HistoryDatabaseCache hdbc)
		{
			super (hdbc);
			
			maxY = 100;
		}
		
		public List<Series> getSeries ()
		{
			return series;
		}
		
		public float getMaxY ()
		{
			return maxY;
		}
		
		public abstract void setCoreStats (HistoryDatabase.CoreStats cs);
		
		public void fillPartial ()
		{
			if (!isDetached ()) {
				if (rd != null)
					rd.cancel ();
				rd = new ReconstructDialog (main, main.getConnection ());
			}
		}
	}
	
	private class SRSDataSource extends DataSource {
	
		private List<Series> completeSeries;
		
		private List<Series> partialSeries;
		
		public SRSDataSource (HistoryDatabaseCache hdbc)
		{
			super (hdbc);
			
			Resources res;
			
			res = getResources ();
						
			completeSeries = new Vector<Series> ();
			completeSeries.add (new Series (res.getColor (R.color.apprentice), 
										    res.getString (R.string.tag_apprentice)));
			completeSeries.add (new Series (res.getColor (R.color.guru), 
							                res.getString (R.string.tag_guru)));
			completeSeries.add (new Series (res.getColor (R.color.master), 
										    res.getString (R.string.tag_master)));
			completeSeries.add (new Series (res.getColor (R.color.enlightened), 
											res.getString (R.string.tag_enlightened)));
			
			partialSeries = new Vector<Series> ();
			partialSeries.add (new Series (res.getColor (R.color.unlocked),
										   res.getString (R.string.tag_unlocked)));
			
			series = new Vector<Series> ();
			series.addAll (completeSeries);
			series.addAll (partialSeries);
		}
		
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			maxY = cs.maxUnlockedRadicals + cs.maxUnlockedKanji + cs.maxUnlockedVocab;
			if (maxY == 0)
				maxY = 100;			
		}
		
		protected void fillPartialSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = partialSeries;
			segment.data = new float [1][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl)
				segment.data [0][i++] = srs.apprentice.total;											
		}

		protected void fillSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = completeSeries;
			segment.data = new float [4][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.total;
				segment.data [1][i] = srs.guru.total;
				segment.data [2][i] = srs.master.total;
				segment.data [3][i] = srs.enlighten.total;
				i++;
			}			
		}
		
	}

	private class KanjiDataSource extends DataSource {
		
		private List<Series> completeSeries;
		
		private List<Series> partialSeries;

		public KanjiDataSource (HistoryDatabaseCache hdbc)
		{
			super (hdbc);
			
			Resources res;
			Series burned;
			
			res = getResources ();
						
			burned = new Series (res.getColor (R.color.burned),
								 res.getString (R.string.tag_burned));
			completeSeries = new Vector<Series> ();
			completeSeries.add (new Series (res.getColor (R.color.apprentice), 
										    res.getString (R.string.tag_apprentice)));
			completeSeries.add (new Series (res.getColor (R.color.guru), 
							                res.getString (R.string.tag_guru)));
			completeSeries.add (new Series (res.getColor (R.color.master), 
										    res.getString (R.string.tag_master)));
			completeSeries.add (new Series (res.getColor (R.color.enlightened), 
											res.getString (R.string.tag_enlightened)));
			
			partialSeries = new Vector<Series> ();
			partialSeries.add (new Series (res.getColor (R.color.unlocked),
										   res.getString (R.string.tag_unlocked)));
			
			series = new Vector<Series> ();
			series.addAll (completeSeries);
			series.addAll (partialSeries);
			
			series.add (burned);
			partialSeries.add (burned);
			completeSeries.add (burned);
		}
		
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			maxY = cs.maxKanji;
			if (maxY == 0)
				maxY = 100;			
		}
		
		protected void fillPartialSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = partialSeries;
			segment.data = new float [2][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.kanji;
				segment.data [1][i++] = srs.burned.kanji;
			}
		}

		protected void fillSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = completeSeries;
			segment.data = new float [5][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.kanji;
				segment.data [1][i] = srs.guru.kanji;
				segment.data [2][i] = srs.master.kanji;
				segment.data [3][i] = srs.enlighten.kanji;
				segment.data [4][i] = srs.burned.kanji;
				i++;
			}			
		}
		
	}

	private class VocabDataSource extends DataSource {
		
		private List<Series> completeSeries;
		
		private List<Series> partialSeries;

		public VocabDataSource (HistoryDatabaseCache hdbc)
		{
			super (hdbc);
			
			Resources res;
			Series burned;
			
			res = getResources ();
						
			burned = new Series (res.getColor (R.color.burned),
								 res.getString (R.string.tag_burned));
			completeSeries = new Vector<Series> ();
			completeSeries.add (new Series (res.getColor (R.color.apprentice), 
										    res.getString (R.string.tag_apprentice)));
			completeSeries.add (new Series (res.getColor (R.color.guru), 
							                res.getString (R.string.tag_guru)));
			completeSeries.add (new Series (res.getColor (R.color.master), 
										    res.getString (R.string.tag_master)));
			completeSeries.add (new Series (res.getColor (R.color.enlightened), 
											res.getString (R.string.tag_enlightened)));
			
			partialSeries = new Vector<Series> ();
			partialSeries.add (new Series (res.getColor (R.color.unlocked),
										   res.getString (R.string.tag_unlocked)));
			
			series = new Vector<Series> ();
			series.addAll (completeSeries);
			series.addAll (partialSeries);
			
			series.add (burned);
			partialSeries.add (burned);
			completeSeries.add (burned);
		}
		
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			maxY = cs.maxVocab;
			if (maxY == 0)
				maxY = 100;			
		}
		
		protected void fillPartialSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = partialSeries;
			segment.data = new float [2][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.vocabulary;
				segment.data [1][i++] = srs.burned.vocabulary;
			}
		}

		protected void fillSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = completeSeries;
			segment.data = new float [5][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.vocabulary;
				segment.data [1][i] = srs.guru.vocabulary;
				segment.data [2][i] = srs.master.vocabulary;
				segment.data [3][i] = srs.enlighten.vocabulary;
				segment.data [4][i] = srs.burned.vocabulary;
				i++;
			}			
		}
		
	}

	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;
	
	/// All the charts. This is needed to lock and unlock scrolling
	List<TYChart> charts;
	
	/// The core stats, used to trim graph scales
	HistoryDatabase.CoreStats cs;
	
	/// The task that retrives core stats
	GetCoreStatsTask task;
	
	/// The SRS plot datasource
	SRSDataSource srsds;
	
	/// The Kanji progress plot datasource
	KanjiDataSource kanjids;

	/// The Vocab progress plot datasource
	VocabDataSource vocabds;

	/// Overall number of kanji
	private static final int ALL_THE_KANJI = 1700;
	
	/// Overall number of vocab items
	private static final int ALL_THE_VOCAB = 5000;
	
	/// The database
	HistoryDatabaseCache hdbc;
	
	ReconstructDialog rd;
	
	/**
	 * Constructor
	 */
	public StatsFragment ()
	{
		this.charts = new Vector<TYChart> ();
		hdbc = new HistoryDatabaseCache ();
	}
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;
		
		this.main.register (this);
		
		if (cs == null && task == null) {
			task = new GetCoreStatsTask (main);
			task.execute ();
		}
		
		hdbc.open (main);
		
		if (rd != null)
			rd.attach (main);
	}
	
	@Override
	public void onDetach ()
	{
		super.onDetach ();		

		hdbc.close ();
		
		if (rd != null)
			rd.detach ();
	}
	
	private void setCoreStats (HistoryDatabase.CoreStats cs)
	{
		this.cs = cs;
		
		srsds.setCoreStats (cs);
		kanjids.setCoreStats (cs);
		vocabds.setCoreStats (cs);
		for (TYChart tyc : charts)
			tyc.invalidate ();
	}

	/**
	 * Called at fragment creation. Since it keeps valuable information
	 * we enable retain instance flag.
	 */
	@Override
	public void onCreate (Bundle bundle)
	{
		super.onCreate (bundle);

    	setRetainInstance (true);
	}

	/**
	 * Builds the GUI.
	 * @param inflater the inflater
	 * @param container
	 *  the parent view
	 * @param savedInstance an (unused) bundle
	 */
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
		super.onCreateView (inflater, container, savedInstanceState);
		
		parent = inflater.inflate(R.layout.stats, container, false);
		charts = new Vector<TYChart> ();

		srsds = new SRSDataSource (hdbc);
		kanjids = new KanjiDataSource (hdbc);
		vocabds = new VocabDataSource (hdbc);
		
		hdbc.addDataSource (srsds);
		hdbc.addDataSource (kanjids);
		hdbc.addDataSource (vocabds);
		
		attach (R.id.ty_srs, srsds);
		attach (R.id.ty_kanji, kanjids);
		attach (R.id.ty_vocab, vocabds);
		
		return parent;
    }
	
	private void attach (int chart, DataSource ds)
	{
		TYChart tyc;

		tyc = (TYChart) parent.findViewById (chart);
		if (cs != null)
			ds.setCoreStats (cs);
		
		tyc.setDataSource (ds);
		charts.add (tyc);		
	}

	@Override
	public void onResume ()
	{
		super.onResume ();
		
		refreshComplete (main.getDashboardData ());
	}
	
	@Override
	public int getName ()
	{
		return R.string.tag_stats;
	}
	
	@Override
	public void refreshComplete (DashboardData dd)
	{
		PieChart pc;

		if (dd == null || !isResumed ())
			return;
		
		switch (dd.od.srsStatus) {
		case RETRIEVING:
			break;
			
		case RETRIEVED:
			pc = (PieChart) parent.findViewById (R.id.pc_srs);
			if (hasSRSData (dd.od.srs)) {
				pc.setVisibility (View.VISIBLE);			
				pc.setData (getSRSDataSets (dd.od.srs));
			} else
				pc.setVisibility (View.GONE);

			pc = (PieChart) parent.findViewById (R.id.pc_kanji);			
			pc.setData (getKanjiProgDataSets (dd.od.srs));
				
			pc = (PieChart) parent.findViewById (R.id.pc_vocab);			
			pc.setData (getVocabProgDataSets (dd.od.srs));
			
			for (TYChart chart : charts)
				chart.setOrigin (dd.creation);
				
			break;
		
		case FAILED:
			if (dd.od.srs == null)
				showAlerts ();
		}
	}
	
	/**
	 * Tells whether the SRS pie chart will contain at least one slice or not.
	 * @param srs the SRS data
	 * @return <tt>true</tt> if at least one item exists
	 */
	protected boolean hasSRSData (SRSDistribution srs)
	{
		return 	(srs.apprentice.total + srs.guru.total +
				 srs.master.total + srs.enlighten.total) > 0;
	}
	
	/**
	 * Creates the datasets needed by the SRS distribution pie chart
	 * @param srs the SRS data 
	 * @return a list of dataset (one for each SRS level)
	 */
	protected List<DataSet> getSRSDataSets (SRSDistribution srs)
	{
		List<DataSet> ans;
		Resources res;
		DataSet ds;
		
		res = getResources ();
		ans = new Vector<DataSet> ();

		ds = new DataSet (res.getString (R.string.tag_apprentice),
						  res.getColor (R.color.apprentice),
						  srs.apprentice.total);
		ans.add (ds);
		
		ds = new DataSet (res.getString (R.string.tag_guru),
				  		  res.getColor (R.color.guru),
				  		  srs.guru.total);

		ans.add (ds);
		
		ds = new DataSet (res.getString (R.string.tag_master),
				   	      res.getColor (R.color.master),
				   	      srs.master.total);
		
		ans.add (ds);
		
		ds = new DataSet (res.getString (R.string.tag_enlightened),
						  res.getColor (R.color.enlightened),
						  srs.enlighten.total);
		
		ans.add (ds);
		
		return ans;
	}
	
	/**
	 * Creates the datasets needed by the kanji progression pie chart
	 * @param srs the SRS data 
	 * @return a list of dataset (one for each SRS level, plus burned and unlocked)
	 */
	protected List<DataSet> getKanjiProgDataSets (SRSDistribution srs)
	{
		List<DataSet> ans;
		Resources res;
		DataSet ds;
		int locked;
		
		res = getResources ();
		ans = new Vector<DataSet> ();

		locked = ALL_THE_KANJI;
		
		locked -= srs.apprentice.kanji;
		ds = new DataSet (res.getString (R.string.tag_apprentice),
						  res.getColor (R.color.apprentice),
						  srs.apprentice.kanji);
		ans.add (ds);
		
		locked -= srs.guru.kanji;
		ds = new DataSet (res.getString (R.string.tag_guru),
				  		  res.getColor (R.color.guru),
				  		  srs.guru.kanji);

		ans.add (ds);
		
		locked -= srs.master.kanji;
		ds = new DataSet (res.getString (R.string.tag_master),
				   	      res.getColor (R.color.master),
				   	      srs.master.kanji);
		
		ans.add (ds);
		
		locked -= srs.enlighten.kanji; 
		ds = new DataSet (res.getString (R.string.tag_enlightened),
						  res.getColor (R.color.enlightened),
						  srs.enlighten.kanji);
		ans.add (ds);
		
		locked -= srs.burned.kanji; 
		ds = new DataSet (res.getString (R.string.tag_burned),
						  res.getColor (R.color.burned),
						  srs.burned.kanji);
		ans.add (ds);
		
		if (locked < 0)
			locked = 0;
		ds = new DataSet (res.getString (R.string.tag_locked),
						  res.getColor (R.color.locked),
						  locked);
		ans.add (ds);

		return ans;
	}

	/**
	 * Creates the datasets needed by the kanji progression pie chart
	 * @param srs the SRS data 
	 * @return a list of dataset (one for each SRS level, plus burned and unlocked)
	 */
	protected List<DataSet> getVocabProgDataSets (SRSDistribution srs)
	{
		List<DataSet> ans;
		Resources res;
		DataSet ds;
		int locked;
		
		res = getResources ();
		ans = new Vector<DataSet> ();

		locked = ALL_THE_VOCAB;
		
		locked -= srs.apprentice.vocabulary;
		ds = new DataSet (res.getString (R.string.tag_apprentice),
						  res.getColor (R.color.apprentice),
						  srs.apprentice.vocabulary);
		ans.add (ds);
		
		locked -= srs.guru.vocabulary;
		ds = new DataSet (res.getString (R.string.tag_guru),
				  		  res.getColor (R.color.guru),
				  		  srs.guru.vocabulary);

		ans.add (ds);
		
		locked -= srs.master.vocabulary;
		ds = new DataSet (res.getString (R.string.tag_master),
				   	      res.getColor (R.color.master),
				   	      srs.master.vocabulary);
		
		ans.add (ds);
		
		locked -= srs.enlighten.vocabulary; 
		ds = new DataSet (res.getString (R.string.tag_enlightened),
						  res.getColor (R.color.enlightened),
						  srs.enlighten.vocabulary);
		ans.add (ds);
		
		locked -= srs.burned.vocabulary; 
		ds = new DataSet (res.getString (R.string.tag_burned),
						  res.getColor (R.color.burned),
						  srs.burned.vocabulary);
		ans.add (ds);
		
		if (locked < 0)
			locked = 0;
		ds = new DataSet (res.getString (R.string.tag_locked),
						  res.getColor (R.color.locked),
						  locked);
		ans.add (ds);

		return ans;
	}

	@Override
	public void spin (boolean enable)
	{
		View pb;
		
		if (parent != null) {
			pb = parent.findViewById (R.id.pb_status);
			pb.setVisibility (enable ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	public void flush ()
	{
		/* empty */
	}
	
	/**
	 * Shows an error message on each pie chart, when something goes wrong.
	 */
	public void showAlerts ()
	{
		PieChart pc;
		String msg;

		msg = getResources ().getString (R.string.status_msg_error);
		if (parent != null) {		
			pc = (PieChart) parent.findViewById (R.id.pc_srs);
			pc.alert (msg);

			pc = (PieChart) parent.findViewById (R.id.pc_kanji);			
			pc.alert (msg);
		
			pc = (PieChart) parent.findViewById (R.id.pc_vocab);			
			pc.alert (msg);
		}
	}

	@Override
	public boolean scrollLock ()
	{
		for (TYChart chart : charts)
			if (chart.scrolling ())
				return true;
		
		return false; 
	}

}
