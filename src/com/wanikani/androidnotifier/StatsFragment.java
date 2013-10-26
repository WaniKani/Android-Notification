package com.wanikani.androidnotifier;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wanikani.androidnotifier.db.HistoryDatabase;
import com.wanikani.androidnotifier.db.HistoryDatabaseCache;
import com.wanikani.androidnotifier.db.HistoryDatabaseCache.PageSegment;
import com.wanikani.androidnotifier.graph.HistogramChart;
import com.wanikani.androidnotifier.graph.HistogramPlot;
import com.wanikani.androidnotifier.graph.Pager;
import com.wanikani.androidnotifier.graph.Pager.Series;
import com.wanikani.androidnotifier.graph.PieChart;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;
import com.wanikani.androidnotifier.graph.TYChart;
import com.wanikani.wklib.Connection;
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

	/**
	 * Listener of the "other stats" button. We start the other stats activity.
	 */
	private class OtherStatsListener implements View.OnClickListener {
		
		@Override
		public void onClick (View view)
		{
			Intent intent;
			
			intent = new Intent (main, OtherStatsActivity.class);			
			startActivity (intent);
		}
		
	}
	
	/**
	 * This task gets called at application start time to acquire some overall
	 * info from the database. The main object, here, is to fix the Y scale. 
	 */
	private class GetCoreStatsTask extends AsyncTask<Void, Void, HistoryDatabase.CoreStats> {
		
		/// The context
		private Context ctxt;
		
		/// The connection
		private Connection conn;
		
		/**
		 * Constructor
		 * @param ctxt the context
		 * @param conn the connection
		 */
		public GetCoreStatsTask (Context ctxt, Connection conn)
		{
			this.ctxt = ctxt;
			this.conn = conn;
		}
		
		/**
		 * The worker function. We simply call the DB that will perform the real job.
		 * @return the stats
		 */
		@Override
		protected HistoryDatabase.CoreStats doInBackground (Void... v)
		{
			try {
				return HistoryDatabase.getCoreStats (ctxt, conn.getUserInformation ());
			} catch (IOException e) {
				return HistoryDatabase.getCoreStats (ctxt, null);				
			}
		}	
		
		/**
		 * Called when DB has been accesses. Publish the info to the main class
		 * 	@param stats
		 */
		@Override
		protected void onPostExecute (HistoryDatabase.CoreStats cs)
		{
			setCoreStats (cs);
		}

	}
	
	/**
	 * Handler of reconstruction dialog events.
	 */
	private class ReconstructListener implements ReconstructDialog.Interface {

		/// The reconstruct dialog
		ReconstructDialog rd;
		
		/**
		 * Constructor. Builds the reconstruct dialog, and start reconstruction process.
		 */
		public ReconstructListener ()
		{
			rd = new ReconstructDialog (this, main, main.getConnection ());
		}
		
		/**
		 * Called by reconstruction dialog when it's over and everything went
		 * smoothly. Notify the main class.
		 * @param cs the new core stats
		 */
		public void completed (HistoryDatabase.CoreStats cs)
		{
			reconstructCompleted (this, cs);
		}		
	}
	
	/**
	 * The base class for all the datasources in this main class. All the common tasks 
	 * are implemented at this level
	 */
	private abstract class DataSource extends HistoryDatabaseCache.DataSource {

		/// The series.
		protected List<Series> series;
		
		/// Y axis scale
		protected int maxY;
		
		/// A mapping between levels and their levelup days
		protected Map<Integer, Pager.Marker> markers;
		
		/// The levelup color
		protected int markerColor;
		
		/**
		 * Constructor
		 * @param hdbc the database cache
		 */
		public DataSource (HistoryDatabaseCache hdbc)
		{
			super (hdbc);
			
			Resources res;
			
			res = getResources ();
			
			markerColor = res.getColor (R.color.levelup);		
			maxY = 100;
			markers = new Hashtable<Integer, Pager.Marker> ();
		}
		
		/**
		 * Returns the series, which nonetheless must have been created by the
		 * subclass.
		 * 	@param return the series
		 */
		@Override
		public List<Series> getSeries ()
		{
			return series;
		}
		
		/**
		 * Returns the Y scale, which nonetheless must have been set by
		 * the subclass
		 * 	@param the Y scale
		 */
		public float getMaxY ()
		{
			return maxY;
		}
		
		/**
		 * Called by the main class when core stats become available.
		 * Subclasses can (should) override this method, by must still call
		 * this implementation, that takes care of populating the levelup hashtable.
		 * @param cs the new corestats
		 */
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			if (cs.levelups != null)
				loadLevelups (cs.levelups);
		}
		
		/**
		 * Creates the levelups hashtable. Called by 
		 * {@link #setCoreStats(com.wanikani.androidnotifier.db.HistoryDatabase.CoreStats)},
		 * so there is no need to access the DB.
		 * @param levelups
		 */
		private void loadLevelups (Map<Integer, Integer> levelups)
		{
			markers.clear ();

			for (Map.Entry<Integer, Integer> e : levelups.entrySet ()) {
				markers.put (e.getValue (), newMarker (e.getValue (), e.getKey ()));
			}
		}		
		
		/**
		 * Adds a levelup marker. This method may be overridden by subclasses in order
		 * to change marker color or tag
		 * @param day the day
		 * @param level the level
		 * @return a marker
		 */
		protected Pager.Marker newMarker (int day, int level)
		{
			return new Pager.Marker (markerColor, Integer.toString (level));
		}
		
		/**
		 * Returns the marker hashtable. This is populated at this level, so
		 * subclasses need not override this method (though they can still to it
		 * if they want to display something fancier).
		 * @return the leveup markers 
		 */
		@Override
		public Map<Integer, Pager.Marker> getMarkers ()
		{
			return markers;
		}
		
		/**
		 * Called when the user requested partial info to be reconstructed.
		 * We open the reconstruct dialog and let it handle the business
		 */
		public void fillPartial ()
		{
			openReconstructDialog ();
		}
	}
	
	/**
	 * The SRS distribution datasource implementation.
	 */
	private class SRSDataSource extends DataSource {
	
		/// The series of complete segments 
		private List<Series> completeSeries;
		
		/// The series of partial segments
		private List<Series> partialSeries;
		
		/**
		 * Constructor.
		 * @param hdbc the database cache
		 */
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
		
		/**
		 * Called when core stats become available. Setup the {@link DataSource#maxY}
		 * field
		 * @param cs the core stats
		 */
		@Override
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			super.setCoreStats (cs);
			
			maxY = cs.maxUnlockedRadicals + cs.maxUnlockedKanji + cs.maxUnlockedVocab;
			if (maxY == 0)
				maxY = 100;			
		}
		
		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is partial. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
		@Override
		protected void fillPartialSegment (Pager.Segment segment, PageSegment pseg)
		{
			int i;
			
			segment.series = partialSeries;
			segment.data = new float [1][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl)
				segment.data [0][i++] = srs.apprentice.total;											
		}

		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is complete. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
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

	/**
	 * The kanji distribution datasource implementation.
	 */
	private class KanjiDataSource extends DataSource {
		
		/// The series of complete segments 
		private List<Series> completeSeries;
		
		/// The series of partial segments 
		private List<Series> partialSeries;

		/**
		 * Constructor.
		 * @param hdbc the database cache
		 */
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
		
		/**
		 * Called when core stats become available. Setup the {@link DataSource#maxY}
		 * field
		 * @param cs the core stats
		 */
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			super.setCoreStats (cs);

			maxY = cs.maxKanji;
			if (maxY == 0)
				maxY = 100;			
		}
		
		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is partial. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
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

		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is complete. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
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

	/**
	 * The vocab distribution datasource implementation.
	 */
	private class VocabDataSource extends DataSource {
		
		/// The series of complete segments 
		private List<Series> completeSeries;
		
		/// The series of partial segments 
		private List<Series> partialSeries;

		/**
		 * Constructor.
		 * @param hdbc the database cache
		 */
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
		
		/**
		 * Called when core stats become available. Setup the {@link DataSource#maxY}
		 * field
		 * @param cs the core stats
		 */
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			super.setCoreStats (cs);

			maxY = cs.maxVocab;
			if (maxY == 0)
				maxY = 100;			
		}
		
		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is partial. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
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

		/**
		 * Called by some ancestor when we need to translate a raw page segment
		 * into a plot segment <i>and</i> data is complete. 
		 * @param segment raw input segment
		 * @param pseg output segment
		 */
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
	
	private abstract class GenericChart {
		
		HistoryDatabase.CoreStats cs;
		
		DashboardData dd;		
		
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			this.cs = cs;
			
			updateIfComplete ();
		}
		
		public void setDashboardData (DashboardData dd)
		{
			this.dd = dd;
			
			updateIfComplete ();
		}
		
		public boolean scrolling ()
		{
			return false;
		}
		
		protected void updateIfComplete ()
		{
			if (dd != null && cs != null)
				update ();
		}
		
		protected abstract void update ();
		
		protected Map<Integer, Integer> getDays ()
		{
			Map<Integer, Integer> ans;
			Integer lday, cday;
			int i;

			lday = 0;
			ans = new Hashtable<Integer, Integer> ();
			for (i = 1; i <= dd.level; i++) {
				cday = cs.levelups.get (i);
				if (cday != null && lday != null && lday < cday)
					ans.put (i - 1, cday - lday);
				lday = cday;
			}

			return ans;
		}
	}
	
	/**
	 * This class, given the core stats, calculates the expected completion time
	 * of the fifty WK levels and of next level. This is done through an exponentially
	 * weighted average. The exponent is different in the two cases.
	 */
	private class LevelEstimates extends GenericChart {
		
		/// The exponent for l50 completion
		private static final float WEXP_L50 = 0.707f;
				
		/// The exponent for next level completion
		private static final float WEXP_NEXT = 0.42f;
		
		/// The Date formatter
		private DateFormat df;
		
		public LevelEstimates ()
		{
			df =  new SimpleDateFormat ("dd MMM yyyy", Locale.US);
		}
		
		protected void update ()
		{
			Map<Integer, Integer> days;
			boolean show;
			int vity;
			
			days = getDays ();
			show = false;
			
			show |= updateL50 (days);
			show |= updateNextLevel (days);
			vity = show ? View.VISIBLE : View.GONE;
		
			parent.findViewById (R.id.ct_eta).setVisibility (vity);
			parent.findViewById (R.id.ctab_eta). setVisibility (vity);
		}
		
		private boolean updateL50 (Map<Integer, Integer> days)
		{
			TextView tw;
			Float delay;
			Calendar cal;

			delay = weight (days, WEXP_L50);
			tw = (TextView) parent.findViewById (R.id.tv_eta_l50);
			if (delay != null && dd.level < ALL_THE_LEVELS) {
				cal = Calendar.getInstance ();
				cal.setTime (dd.creation);				
				cal.add (Calendar.DATE, (int) (delay * ALL_THE_LEVELS));
				tw.setText (df.format (cal.getTime ()));
				tw.setVisibility (View.VISIBLE);
				
				return true;
			} else {
				tw.setVisibility (View.GONE);
				
				return false;
			}
		}
		
		private boolean updateNextLevel (Map<Integer, Integer> days)
		{
			View nlw, avgw;
			TextView tw, nltag;
			Integer lastlup;
			Float delay, avgl;
			Calendar cal;
			String s;
			
			avgl = delay = weight (days, WEXP_NEXT);
			nlw = parent.findViewById (R.id.div_eta_next);
			avgw = parent.findViewById (R.id.div_eta_avg);
			if (delay != null) {
				avgw.setVisibility (View.VISIBLE);

				tw = (TextView) parent.findViewById (R.id.tv_eta_avg);
				tw.setText (beautify (delay));
				
				tw = (TextView) parent.findViewById (R.id.tv_eta_next);
				lastlup = cs.levelups.get (dd.level);
				if (lastlup != null) {
					cal = Calendar.getInstance ();
					cal.setTime (dd.creation);
					cal.set (Calendar.HOUR_OF_DAY, 0);
					cal.set (Calendar.MINUTE, 0);
					cal.set (Calendar.SECOND, 0);
					cal.add (Calendar.DATE, lastlup);
					delay -= ((float) System.currentTimeMillis () - cal.getTimeInMillis ()) / (24 * 3600 * 1000);
					delay -= 0.5F;	/* This compensates the fact that granularity is one day */										

					if (delay <= avgl) {
						nltag = (TextView) main.findViewById (R.id.tag_eta_next);
						nlw.setVisibility (View.VISIBLE);
						if (delay > 0) {
							nltag.setText (R.string.tag_eta_next_future);
							s = main.getString (R.string.fmt_eta_next_future, beautify (delay));
						} else {
							nltag.setText (R.string.tag_eta_next_past);
							s = main.getString (R.string.fmt_eta_next_past, beautify (-delay));
						}
						tw.setText (s);
					} else
						nlw.setVisibility (View.GONE);
				} else
					nlw.setVisibility (View.GONE);
				
				return true;
				
			} else {
				avgw.setVisibility (View.GONE);
				nlw.setVisibility (View.GONE);

				return false;
			}
		}
		
		public String beautify (float days)
		{
			long dfield, hfield;
			Resources res;
			String ds, hs;
			
			res = getResources ();
			dfield = (long) days;
			hfield = (long) ((days - dfield) * 24);
			if (dfield > 1)
				ds = res.getString (R.string.fmts_bd, dfield);
			else if (dfield == 1)
				ds = res.getString (R.string.fmts_bd_one);
			else
				ds = null;
			
			if (hfield > 1)
				hs = res.getString (R.string.fmts_bh, hfield);
			else if (hfield == 1)
				hs = res.getString (R.string.fmts_bh_one);
			else
				hs = null;

			if (ds != null)
				if (hs != null)
					return ds + ", " + hs;
				else
					return ds;
			else
				if (hs != null)
					return hs;
				else
					return res.getString (R.string.fmts_bnow);
				
		}
		
		private Float weight (Map<Integer, Integer> days, float wexp)
		{
			float cw, num, den;
			Integer delta;
			int i;
			
			num = den = 0;
			cw = wexp;
			for (i = dd.level - 1; i > 0; i--) {
				delta = days.get (i);
				if (delta != null) {
					num += cw * delta;
					den += cw;
				}
				cw *= wexp;
			}
			
			return den != 0 ? num / den : null;
		}
		
		@Override
		protected Map<Integer, Integer> getDays ()
		{
			Map<Integer, Integer> ans;
			int i, minl, maxl, minv, maxv;
			Integer days;

			ans = super.getDays ();
			
			minl = maxl = minv = maxv = -1;			
			for (i = 1; i < dd.level; i++) {
				days = ans.get (i);
				if (days != null) {
					if (minv == -1 || days < minv) {
						minl = i;
						minv = days;						
					}
					if (maxv == -1 || days > maxv) {
						maxl = i;
						maxv = days;
					}						
				}
			}

			if (minl > 0)
				ans.remove (minl);
			if (maxl > 0)
				ans.remove (maxl);
			
			return ans;
		}

				
	}
	
	private class LevelupSource extends GenericChart {

		List<HistogramPlot.Series> series;
		
		private static final long LEVELUP_CAP = 30;
		
		public LevelupSource ()
		{
			Resources res;
			
			res = getResources ();
			
			series = new Vector<HistogramPlot.Series> ();
			series.add (new HistogramPlot.Series (res.getColor (R.color.apprentice)));
			series.add (new HistogramPlot.Series (res.getColor (R.color.guru)));
			series.add (new HistogramPlot.Series (res.getColor (R.color.master)));
			series.add (new HistogramPlot.Series (res.getColor (R.color.enlightened)));
		}
		
		protected void update ()
		{
			List<HistogramPlot.Samples> bars;
			Map<Integer, Integer> days;
			HistogramPlot.Samples bar;
			HistogramPlot.Sample sample;
			HistogramChart chart;
			Integer day;
			int i;
			
			bars = new Vector<HistogramPlot.Samples> ();
			days = getDays ();
			for (i = 1; i < dd.level; i++) {
				bar = new HistogramPlot.Samples (Integer.toString (i));				
				sample = new HistogramPlot.Sample ();				
				bar.samples.add (sample);
				
				sample.series = series.get (i % series.size ());
				day = days.get (i);
				sample.value = day != null ? day : 0;
				
				bars.add (bar);
			}
			
			chart = (HistogramChart) parent.findViewById (R.id.hi_levels);
			chart.setData (series, bars, LEVELUP_CAP);
			
			/// chart.setVisibility (bars.isEmpty () ? View.GONE : View.VISIBLE);
			// Need a more robust db fixup algorithm to display this (available on next branch)
			chart.setVisibility (View.GONE);
		}
		
		public boolean scrolling ()
		{
			return ((HistogramChart) parent.findViewById (R.id.hi_levels)).scrolling ();
		}		
	}

	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;
	
	/// All the TY charts. This is needed to lock and unlock scrolling
	List<TYChart> charts;
	
	/// All the generic (non TY) charts.
	List<GenericChart> gcharts;	
	
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
	
	/// Overall number of levels
	private static final int ALL_THE_LEVELS = 50;
	
	/// The database
	HistoryDatabaseCache hdbc;
	
	/// The object that listens for reconstruction events
	ReconstructListener rlist;
	
	/**
	 * Constructor
	 */
	public StatsFragment ()
	{
		charts = new Vector<TYChart> ();
		gcharts = new Vector<GenericChart> ();
		hdbc = new HistoryDatabaseCache ();
	}
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;
		
		this.main.register (this);
		
		hdbc.open (main);
		
		if (rlist != null)
			rlist.rd.attach (main);
	}
	
	@Override
	public void onDetach ()
	{
		super.onDetach ();		

		hdbc.close ();
		
		if (rlist != null)
			rlist.rd.detach ();
	}
	
	/**
	 * Called when core stats become available. Update each datasource and
	 * request plots to be refreshed.
	 * @param cs the core stats
	 */
	private void setCoreStats (HistoryDatabase.CoreStats cs)
	{
		this.cs = cs;
		
		srsds.setCoreStats (cs);
		kanjids.setCoreStats (cs);
		vocabds.setCoreStats (cs);
		for (TYChart tyc : charts)
			tyc.refresh ();
		
		for (GenericChart gc : gcharts)
			gc.setCoreStats (cs);
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
		
		View view;
		
		parent = inflater.inflate(R.layout.stats, container, false);
		charts = new Vector<TYChart> ();
		gcharts = new Vector<GenericChart> ();

		srsds = new SRSDataSource (hdbc);
		kanjids = new KanjiDataSource (hdbc);
		vocabds = new VocabDataSource (hdbc);
		
		hdbc.addDataSource (srsds);
		hdbc.addDataSource (kanjids);
		hdbc.addDataSource (vocabds);
		
		attach (R.id.ty_srs, srsds);
		attach (R.id.ty_kanji, kanjids);
		attach (R.id.ty_vocab, vocabds);
	
		gcharts.add (new LevelEstimates ());
		gcharts.add (new LevelupSource ());
		
		if (cs != null)
			setCoreStats (cs);
		else if (task == null) {
			task = new GetCoreStatsTask (main, main.getConnection ());
			task.execute ();
		}
		
		view = parent.findViewById (R.id.btn_other_stats);
		view.setOnClickListener (new OtherStatsListener ());
		
		return parent;
    }
	
	/**
	 * Binds each datasource to its chart 
	 * @param chart the chart ID
	 * @param ds the datasource
	 */
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
		
		for (GenericChart gc : gcharts)
			gc.setDashboardData (dd);
		
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
	public void flush (Tab.RefreshType rtype)
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
		
		for (GenericChart chart : gcharts)
			if (chart.scrolling ())
				return true;

		return false; 
	}
	
	/**
	 * Called when the user wants to start the reconstruct process.
	 * We open the dialog and let it handle the process.
	 */
	private void openReconstructDialog ()
	{
		if (!isDetached ()) {
			if (rlist != null)
				rlist.rd.cancel ();
			rlist = new ReconstructListener (); 
		}		
	}
	
	/**
	 * Called when reconstruction is successfully completed. Refresh datasources 
	 * and plots.
	 * @param rlist the listener
	 * @param cs the core stats
	 */
	private void reconstructCompleted (ReconstructListener rlist,
									   HistoryDatabase.CoreStats cs)
	{
		if (this.rlist == rlist) {
			
			hdbc.flush ();
			
			setCoreStats (cs);
			
			rlist = null;
		}
	}
	
	/**
	 * The back button is not handled.
	 * 	@return false
	 */
	@Override
	public boolean backButton ()
	{
		return false;
	}
	
	@Override
    public boolean contains (Contents c)
	{
		return c == Contents.STATS;
	}
	
	private static Map<Integer, Integer> getDays (HistoryDatabase.CoreStats cs,
												  DashboardData dd, boolean remove)
	{
		Map<Integer, Integer> ans;
		Integer lday, cday, days;
		int i, minl, maxl, minv, maxv;
		
		lday = 0;
		ans = new Hashtable<Integer, Integer> ();
		for (i = 1; i <= dd.level; i++) {
			cday = cs.levelups.get (i);
			if (cday != null && lday != null && lday < cday)
				ans.put (i - 1, cday - lday);
			lday = cday;
		}
		
		minl = maxl = minv = maxv = -1;			
		for (i = 1; i < dd.level; i++) {
			days = ans.get (i);
			if (days != null) {
				if (minv == -1 || days < minv) {
					minl = i;
					minv = days;						
				}
				if (maxv == -1 || days > maxv) {
					maxl = i;
					maxv = days;
				}						
			}
		}
		
		if (remove) {
			if (minl > 0)
				ans.remove (minl);
			if (maxl > 0)
				ans.remove (maxl);
		}
			
		return ans;
	}
}
