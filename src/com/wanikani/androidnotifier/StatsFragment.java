package com.wanikani.androidnotifier;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.androidnotifier.graph.Pager;
import com.wanikani.androidnotifier.graph.Pager.Series;
import com.wanikani.androidnotifier.graph.PieChart;
import com.wanikani.androidnotifier.graph.PieChart.InfoSet;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;
import com.wanikani.androidnotifier.graph.TYChart;
import com.wanikani.androidnotifier.stats.ItemAgeChart;
import com.wanikani.androidnotifier.stats.ItemDistributionChart;
import com.wanikani.androidnotifier.stats.KanjiProgressChart;
import com.wanikani.androidnotifier.stats.NetworkEngine;
import com.wanikani.androidnotifier.stats.ReviewsTimelineChart;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.SRSDistribution;
import com.wanikani.wklib.SRSLevel;

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
			Connection.Meter meter;
			
			meter = MeterSpec.T.DASHBOARD_REFRESH.get (ctxt);
			
			try {
				return HistoryDatabase.getCoreStats (ctxt, conn.getUserInformation (meter));
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
			if (cs.levelInfo != null)
				loadLevelInfo (cs.levelInfo);
		}
		
		/**
		 * Creates the levelups hashtable. Called by 
		 * {@link #setCoreStats(com.wanikani.androidnotifier.db.HistoryDatabase.CoreStats)},
		 * so there is no need to access the DB.
		 * @param levelInfo the level information
		 */
		private void loadLevelInfo (Map<Integer, HistoryDatabase.LevelInfo> levelInfo)
		{
			markers.clear ();

			for (Map.Entry<Integer, HistoryDatabase.LevelInfo> e : levelInfo.entrySet ()) {
				markers.put (e.getValue ().day, newMarker (e.getValue ().day, e.getKey ()));
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
		@Override
		public void setCoreStats (HistoryDatabase.CoreStats cs)
		{
			super.setCoreStats (cs);
			
			maxY = cs.maxRadicals + cs.maxKanji + cs.maxVocab;
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
			segment.data = new float [2][pseg.srsl.size ()];
			i = 0;
			for (SRSDistribution srs : pseg.srsl) {
				segment.data [0][i] = srs.apprentice.total;
				segment.data [1][i++] = srs.burned.total;
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
				segment.data [0][i] = srs.apprentice.total;
				segment.data [1][i] = srs.guru.total;
				segment.data [2][i] = srs.master.total;
				segment.data [3][i] = srs.enlighten.total;
				segment.data [4][i] = srs.burned.total;
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
		
		public boolean scrolling (boolean strict)
		{
			return false;
		}
		
		protected void updateIfComplete ()
		{
			if (dd != null && cs != null)
				update ();
		}
		
		protected abstract void update ();
		
		protected int getVacation ()
		{
			HistoryDatabase.LevelInfo li;
			int i, ans;
			
			if (dd == null || cs == null || cs.levelInfo == null)
				return 0;
			
			ans = 0;
			for (i = 1; i <= dd.level; i++) {
				li = cs.levelInfo.get (i);
				if (li != null)
					ans += li.vacation;
			}
			
			return ans;
		}
		
		protected Map<Integer, Integer> getDays ()
		{
			Map<Integer, Integer> ans;
			HistoryDatabase.LevelInfo li;
			Integer lday;
			int i;

			lday = 0;
			ans = new Hashtable<Integer, Integer> ();
			for (i = 1; i <= dd.level; i++) {
				li = cs.levelInfo.get (i);
				if (li != null && lday != null && lday < li.day)
					ans.put (i - 1, li.day - lday);
				lday = li != null ? (li.day + li.vacation) : null;
			}
			
			/* We don't want L50 to grow... */
			for (i = ALL_THE_LEVELS; i <= dd.level; i++)
				ans.remove (i);

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
			int vity, vacation;
			boolean show;
			
			days = getDays ();
			vacation = getVacation ();
			show = false;
			
			show |= updateL50 (days, vacation);
			show |= updateNextLevel (days);
			vity = show ? View.VISIBLE : View.GONE;
		
			parent.findViewById (R.id.ct_eta).setVisibility (vity);
			parent.findViewById (R.id.ctab_eta). setVisibility (vity);
		}
		
		private boolean updateL50 (Map<Integer, Integer> days, int vacation)
		{
			HistoryDatabase.LevelInfo li;
			TextView tw;
			Float delay;
			Calendar cal;

			delay = weight (days, WEXP_L50);
			tw = (TextView) parent.findViewById (R.id.tv_eta_l50);
			if (delay != null && dd.level < ALL_THE_LEVELS) {
				
				li = cs != null && cs.levelInfo != null ? cs.levelInfo.get (dd.level) : null;
				cal = Calendar.getInstance ();

				cal.setTime (dd.creation);				
				if (li != null) {
					/* Algorithm 1: last_levelup_time + avg_time * (50 - current_level)  
					 * More accurate but may fail if for some reason we have no leveup info for the current level
					 */
					cal.add (Calendar.DATE, li.day + li.vacation);
					cal.add (Calendar.DATE, (int) (delay * (ALL_THE_LEVELS - dd.level)));
				} else {
					/* Algorithm 2: subscription_date + total_vacation + 50 * avg_time */   
					cal.add (Calendar.DATE, (int) (delay * ALL_THE_LEVELS) + vacation);
				}
				
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
			HistoryDatabase.LevelInfo lastli;
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
				lastli = cs.levelInfo.get (dd.level);
				if (lastli != null) {
					cal = Calendar.getInstance ();
					cal.setTime (normalize (dd.creation));
					cal.add (Calendar.DATE, lastli.day + lastli.vacation);
					delay -= ((float) System.currentTimeMillis () - cal.getTimeInMillis ()) / (24 * 3600 * 1000);
					delay -= 0.5F;	/* This compensates the fact that granularity is one day */										

					if (delay <= avgl && delay >= 0) {
						nltag = (TextView) parent.findViewById (R.id.tag_eta_next);
						nlw.setVisibility (View.VISIBLE);
						nltag.setText (R.string.tag_eta_next_future);
						s = main.getString (R.string.fmt_eta_next_future, beautify (delay));
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
						
		private Date normalize (Date date)
		{
			Calendar cal;
			
			cal = Calendar.getInstance ();
			cal.setTime (date);
			cal.set (Calendar.HOUR_OF_DAY, 0);
			cal.set (Calendar.MINUTE, 0);
			cal.set (Calendar.SECOND, 0);
			cal.set (Calendar.MILLISECOND, 0);
			
			return cal.getTime ();
		}
		
		private List<Date> getExpectedDates (ItemLibrary<? extends Item> lib, int aint, Date uldate)
		{
			List<Date> dates;
			Calendar cal;
			int slevel;
			
			dates = new Vector<Date> ();
			for (Item i : lib.list) {
				cal = Calendar.getInstance ();
				if (i.stats == null || i.stats.srs == null) {
					slevel = -1;
					cal.setTime (uldate);
				} else if (i.stats.srs == SRSLevel.APPRENTICE) {
					cal.setTime (i.stats.availableDate);
					slevel = Math.min (i.stats.meaning.currentStreak, i.stats.reading.currentStreak); 
				} else
					slevel = 3;
				
				switch (slevel) {
				case -1:
					cal.add (Calendar.HOUR, 4);
				case 0:
					cal.add (Calendar.HOUR, 8);
				case 1:
					cal.add (Calendar.DATE, 1);
				case 2:
					cal.add (Calendar.DATE, aint);
				}
				dates.add (cal.getTime ());
			}
			
			Collections.sort (dates);
			
			return dates;
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
	
	private class LevelupSource extends GenericChart implements View.OnClickListener {

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
			int i, slups;
			
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
			
			if (!bars.isEmpty ()) {
				chart.setVisibility (View.VISIBLE);			
				slups = DatabaseFixup.getSuspectLevels (main, dd.level, cs);
				if (slups > 0)
					chart.alert (getResources ().getString (R.string.alert_suspect_levels, slups), this);
			} else
				chart.setVisibility (View.GONE);
		}
		
		@Override
		public void onClick (View view)
		{
			if (isVisible())
				main.dbFixup ();
		}
		
		public boolean scrolling (boolean strict)
		{
			return ((HistogramChart) parent.findViewById (R.id.hi_levels)).scrolling (strict);
		}		
	}
	
	public class Callback implements LowPriorityScrollView.Callback {
		
		@Override
		public boolean canScroll (LowPriorityScrollView lpsw)
		{
			return !scrolling (true);
		}
		
	}
	
	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;
	
	/// The network engine
	NetworkEngine netwe;
	
	/// All the TY charts. This is needed to lock and unlock scrolling
	List<TYChart> charts;
	
	/// All the generic (non TY) charts.
	List<GenericChart> gcharts;
	
	/// All the charts that must be flushed when requested
	List<IconizableChart> fcharts;
	
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
	
	/// The review timeline charts
	ReviewsTimelineChart timeline;
	
	int preserved [] = new int [] { 
			R.id.pc_srs, R.id.ty_srs, 
			R.id.pc_vocab, R.id.ty_vocab,
			R.id.pc_kanji, R.id.ty_kanji,
			R.id.hi_levels };
	
	int semiPreserved [] = new int [] {
			R.id.ct_age_distribution,
			R.id.os_jlpt, R.id.os_joyo,
			R.id.os_kanji_levels, R.id.os_levels,
			R.id.os_review_timeline_srs, R.id.os_review_timeline_item
	};
	
	private Map<Integer, Boolean> semiPreservedState;
	
	/// Overall number of kanji
	private static final int ALL_THE_KANJI = 1679;
	
	/// Overall number of vocab items
	private static final int ALL_THE_VOCAB = 5029;
	
	/// Overall number of levels
	private static final int ALL_THE_LEVELS = 50;
	
	/// The database
	HistoryDatabaseCache hdbc;
	
	/// The object that listens for reconstruction events
	ReconstructListener rlist;
		
	/// Level source
	LevelupSource levels;
	
	public static final String PREFIX = StatsFragment.class.getName () + ".";
	
	public static final String KEY_OPEN = PREFIX + "POPEN.";
	
	private static final String KLIB_JLPT_1 =
		"氏統保第結派案策基価提挙応企検藤沢裁証援施井護展態鮮視条幹独宮率衛張監環審義訴株姿閣衆評影松撃佐核整融製票渉響推請器士討攻崎督授催及憲離激摘系批郎健盟従修隊織拡故振弁就異献厳維浜遺塁邦素遣抗模雄益緊標" +
		"宣昭廃伊江僚吉盛皇臨踏壊債興源儀創障継筋闘葬避司康善逮迫惑崩紀聴脱級博締救執房撤削密措志載陣我為抑幕染奈傷択秀徴弾償功拠秘拒刑塚致繰尾描鈴盤項喪伴養懸街契掲躍棄邸縮還属慮枠恵露沖緩節需射購揮充貢鹿却端" +
		"賃獲郡併徹貴衝焦奪災浦析譲称納樹挑誘紛至宗促慎控智握宙俊銭渋銃操携診託撮誕侵括謝駆透津壁稲仮裂敏是排裕堅訳芝綱典賀扱顧弘看訟戒祉誉歓奏勧騒閥甲縄郷揺免既薦隣華範隠徳哲杉釈己妥威豪熊滞微隆症暫忠倉彦肝喚" +
		"沿妙唱阿索誠襲懇俳柄驚麻李浩剤瀬趣陥斎貫仙慰序旬兼聖旨即柳舎偽較覇詳抵脅茂犠旗距雅飾網竜詩繁翼潟敵魅嫌斉敷擁圏酸罰滅礎腐脚潮梅尽僕桜滑孤炎賠句鋼頑鎖彩摩励縦輝蓄軸巡稼瞬砲噴誇祥牲秩帝宏唆阻泰賄撲堀菊絞" +
		"縁唯膨矢耐塾漏慶猛芳懲剣彰棋丁恒揚冒之倫陳憶潜梨仁克岳概拘墓黙須偏雰遇諮狭卓亀糧簿炉牧殊殖艦輩穴奇慢鶴謀暖昌拍朗寛覆胞泣隔浄没暇肺貞靖鑑飼陰銘随烈尋稿丹啓也丘棟壌漫玄粘悟舗妊熟旭恩騰往豆遂狂岐陛緯培衰" +
		"艇屈径淡抽披廷錦准暑磯奨浸剰胆繊駒虚霊帳悔諭惨虐翻墜沼据肥徐糖搭盾脈滝軌俵妨擦鯨荘諾雷漂懐勘栽拐駄添冠斜鏡聡浪亜覧詐壇勲魔酬紫曙紋卸奮欄逸涯拓眼獄尚彫穏顕巧矛垣欺釣萩粛栗愚嘉遭架鬼庶稚滋幻煮姫誓把践呈" +
		"疎仰剛疾征砕謡嫁謙后嘆菌鎌巣頻琴班棚潔酷宰廊寂辰霞伏碁俗漠邪晶墨鎮洞履劣那殴娠奉憂朴亭淳怪鳩酔惜穫佳潤悼乏該赴桑桂髄虎盆晋穂壮堤飢傍疫累痴搬晃癒桐寸郭尿凶吐宴鷹賓虜陶鐘憾猪紘磁弥昆粗訂芽庄傘敦騎寧循忍" +
		"怠如寮祐鵬鉛珠凝苗獣哀跳匠垂蛇澄縫僧眺亘呉凡憩媛溝恭刈睡錯伯笹穀陵霧魂弊妃舶餓窮掌麗綾臭悦刃縛暦宜盲粋辱毅轄猿弦稔窒炊洪摂飽冗桃狩朱渦紳枢碑鍛刀鼓裸猶塊旋弓幣膜扇腸槽慈楊伐駿漬糾亮墳坪紺娯椿舌羅峡俸厘" +
		"峰圭醸蓮弔乙汁尼遍衡薫猟羊款閲偵喝敢胎酵憤豚遮扉硫赦窃泡瑞又慨紡恨肪扶戯伍忌濁奔斗蘭迅肖鉢朽殻享秦茅藩沙輔媒鶏禅嘱胴迭挿嵐椎絹陪剖譜郁悠淑帆暁傑楠笛玲奴錠拳翔遷拙侍尺峠篤肇渇叔雌亨堪叙酢吟逓嶺甚喬崇漆" +
		"岬癖愉寅礁乃洲屯樺槙姻巌擬塀唇睦閑胡幽峻曹詠卑侮鋳抹尉槻隷禍蝶酪茎帥逝汽琢匿襟蛍蕉寡琉痢庸朋坑藍賊搾畔遼唄孔橘漱呂拷嬢苑巽杜渓翁廉謹瞳湧欣窯褒醜升殉煩巴禎劾堕租稜桟倭婿慕斐罷矯某囚魁虹鴻泌於赳漸蚊葵厄" +
		"藻禄孟嫡尭嚇巳凸暢韻霜硝勅芹杏棺儒鳳馨慧愁楼彬匡眉欽薪褐賜嵯綜繕栓翠鮎榛凹艶惣蔦錬隼渚衷逐斥稀芙詔皐雛惟佑耀黛渥憧宵妄惇脩甫酌蚕嬉蒼暉頒只肢檀凱彗謄梓丑嗣叶汐絢朔伽畝抄爽黎惰蛮冴旺萌偲壱瑠允侯蒔鯉弧遥" +
		"舜瑛附彪卯但綺芋茜凌皓洸毬婆緋鯛怜邑倣碧啄穣酉悌倹柚繭亦詢采紗賦眸玖弐錘諄倖痘笙侃裟洵爾耗昴銑莞伶碩宥滉晏伎朕迪綸且竣晨吏燦麿頌箇楓琳梧哉澪匁晟衿凪梢丙颯茄勺恕蕗瑚遵瞭燎虞柊侑謁斤嵩捺蓉茉袈燿誼冶栞墾" +
		"勁菖旦椋叡紬胤凜亥爵脹麟莉汰瑶瑳耶椰絃丞璃奎塑昂柾熙菫諒鞠崚濫捷";

	private static final String KLIB_JLPT_2 =
		"党協総区領県設改府査委軍団各島革村勢減再税営比防補境導副算輸述線農州武象域額欧担準賞辺造被技低復移個門課脳極含蔵量型況針専谷史階管兵接細効丸湾録省旧橋岸周材戸央券編捜竹超並療採森競介根販歴将幅般貿講林" +
		"装諸劇河航鉄児禁印逆換久短油暴輪占植清倍均億圧芸署伸停爆陸玉波帯延羽固則乱普測豊厚齢囲卒略承順岩練軽了庁城患層版令角絡損募裏仏績築貨混昇池血温季星永著誌庫刊像香坂底布寺宇巨震希触依籍汚枚複郵仲栄札板骨" +
		"傾届巻燃跡包駐弱紹雇替預焼簡章臓律贈照薄群秒奥詰双刺純翌快片敬悩泉皮漁荒貯硬埋柱祭袋筆訓浴童宝封胸砂塩賢腕兆床毛緑尊祝柔殿濃液衣肩零幼荷泊黄甘臣浅掃雲掘捨軟沈凍乳恋紅郊腰炭踊冊勇械菜珍卵湖喫干虫刷湯溶" +
		"鉱涙匹孫鋭枝塗軒毒叫拝氷乾棒祈拾粉糸綿汗銅湿瓶咲召缶隻脂蒸肌耕鈍泥隅灯辛磨麦姓筒鼻粒詞胃畳机膚濯塔沸灰菓帽枯涼舟貝符憎皿肯燥畜挟曇滴伺";
	
	private static final String KLIB_JLPT_3 =
		"政議民連対部合市内相定回選米実関決全表戦経最現調化当約首法性要制治務成期取都和機平加受続進数記初指権支産点報済活原共得解交資予向際勝面告反判認参利組信在件側任引求所次昨論官増係感情投示変打直両式確果容" +
		"必演歳争談能位置流格疑過局放常状球職与供役構割費付由説難優夫収断石違消神番規術備宅害配警育席訪乗残想声念助労例然限追商葉伝働形景落好退頭負渡失差末守若種美命福望非観察段横深申様財港識呼達良候程満敗値突" +
		"光路科積他処太客否師登易速存飛殺号単座破除完降責捕危給苦迎園具辞因馬愛富彼未舞亡冷適婦寄込顔類余王返妻背熱宿薬険頼覚船途許抜便留罪努精散静婚喜浮絶幸押倒等老曲払庭徒勤遅居雑招困欠更刻賛抱犯恐息遠戻願絵" +
		"越欲痛笑互束似列探逃遊迷夢君閉緒折草暮酒悲晴掛到寝暗盗吸陽御歯忘雪吹娘誤洗慣礼窓昔貧怒泳祖杯疲皆鳴腹煙眠怖耳頂箱晩寒髪忙才靴恥偶偉猫幾";
	
	private static final String KLIB_JLPT_4 =
		"会同事自社発者地業方新場員立開手力問代明動京目通言理体田主題意不作用度強公持野以思家世多正安院心界教文元重近考画海売知道集別物使品計死特私始朝運終台広住真有口少町料工建空急止送切転研足究楽起着店病質待" +
		"試族銀早映親験英医仕去味写字答夜音注帰古歌買悪図週室歩風紙黒花春赤青館屋色走秋夏習駅洋旅服夕借曜飲肉貸堂鳥飯勉冬昼茶弟牛魚兄犬妹姉漢";
	
	private static final String KLIB_JLPT_5 =
		"日一国人年大十二本中長出三時行見月後前生五間上東四今金九入学高円子外八六下来気小七山話女北午百書先名川千水半男西電校語土木聞食車何南万毎白天母火右読友左休父雨";
	
	private static final String KLIB_JOYO_1 =
		"一右雨円王音下火花貝学気九休玉金空月犬見五口校左三山子四糸字耳七車手十出女小上森人水正生青夕石赤千川先早草足村大男竹中虫町天田土二日入年白八百文木本名目立力林六";

	private static final String KLIB_JOYO_2 =
		"引羽雲園遠何科夏家歌画回会海絵外角楽活間丸岩顔汽記帰弓牛魚京強教近兄形計元言原戸古午後語工公広交光考行高黄合谷国黒今才細作算止市矢姉思紙寺自時室社弱首秋週春書少場色食心新親図数西声星晴切雪船線前組走多" +
		"太体台地池知茶昼長鳥朝直通弟店点電刀冬当東答頭同道読内南肉馬売買麦半番父風分聞米歩母方北毎妹万明鳴毛門夜野友用曜来里理話";
	
	private static final String KLIB_JOYO_3 =
		"悪安暗医委意育員院飲運泳駅央横屋温化荷界開階寒感漢館岸起期客究急級宮球去橋業曲局銀区苦具君係軽血決研県庫湖向幸港号根祭皿仕死使始指歯詩次事持式実写者主守取酒受州拾終習集住重宿所暑助昭消商章勝乗植申身神" +
		"真深進世整昔全相送想息速族他打対待代第題炭短談着注柱丁帳調追定庭笛鉄転都度投豆島湯登等動童農波配倍箱畑発反坂板皮悲美鼻筆氷表秒病品負部服福物平返勉放味命面問役薬由油有遊予羊洋葉陽様落流旅両緑礼列練路和";
	
	private static final String KLIB_JOYO_4 =
		"愛案以衣位囲胃印英栄塩億加果貨課芽改械害街各覚完官管関観願希季紀喜旗器機議求泣救給挙漁共協鏡競極訓軍郡径型景芸欠結建健験固功好候航康告差菜最材昨札刷殺察参産散残士氏史司試児治辞失借種周祝順初松笑唱焼象" +
		"照賞臣信成省清静席積折節説浅戦選然争倉巣束側続卒孫帯隊達単置仲貯兆腸低底停的典伝徒努灯堂働特得毒熱念敗梅博飯飛費必票標不夫付府副粉兵別辺変便包法望牧末満未脈民無約勇要養浴利陸良料量輪類令冷例歴連老労録";
	
	private static final String KLIB_JOYO_5 =
		"圧移因永営衛易益液演応往桜恩可仮価河過賀快解格確額刊幹慣眼基寄規技義逆久旧居許境均禁句群経潔件券険検限現減故個護効厚耕鉱構興講混査再災妻採際在財罪雑酸賛支志枝師資飼示似識質舎謝授修述術準序招承証条状常" +
		"情織職制性政勢精製税責績接設舌絶銭祖素総造像増則測属率損退貸態団断築張提程適敵統銅導徳独任燃能破犯判版比肥非備俵評貧布婦富武復複仏編弁保墓報豊防貿暴務夢迷綿輸余預容略留領";
	
	private static final String KLIB_JOYO_6 =
		"異遺域宇映延沿我灰拡革閣割株干巻看簡危机揮貴疑吸供胸郷勤筋系敬警劇激穴絹権憲源厳己呼誤后孝皇紅降鋼刻穀骨困砂座済裁策冊蚕至私姿視詞誌磁射捨尺若樹収宗就衆従縦縮熟純処署諸除将傷障城蒸針仁垂推寸盛聖誠宣専" +
		"泉洗染善奏窓創装層操蔵臓存尊宅担探誕段暖値宙忠著庁頂潮賃痛展討党糖届難乳認納脳派拝背肺俳班晩否批秘腹奮並陛閉片補暮宝訪亡忘棒枚幕密盟模訳郵優幼欲翌乱卵覧裏律臨朗論";
	
	private static final String KLIB_JOYO_S =
		"亜哀挨曖握扱宛嵐依威為畏尉萎偉椅彙違維慰緯壱逸茨芋咽姻淫陰隠韻唄鬱畝浦詠影鋭疫悦越謁閲炎怨宴媛援煙猿鉛縁艶汚凹押旺欧殴翁奥岡憶臆虞乙俺卸穏佳苛架華菓渦嫁暇禍靴寡箇稼蚊牙瓦雅餓介戒怪拐悔皆塊楷潰壊懐諧劾" +
		"崖涯慨蓋該概骸垣柿核殻郭較隔獲嚇穫岳顎掛潟括喝渇葛滑褐轄且釜鎌刈甘汗缶肝冠陥乾勘患貫喚堪換敢棺款閑勧寛歓監緩憾還環韓艦鑑含玩頑企伎岐忌奇祈軌既飢鬼亀幾棋棄毀畿輝騎宜偽欺儀戯擬犠菊吉喫詰却脚虐及丘朽臼糾" +
		"嗅窮巨拒拠虚距御凶叫狂享況峡挟狭恐恭脅矯響驚仰暁凝巾斤菌琴僅緊錦謹襟吟駆惧愚偶遇隅串屈掘窟熊繰勲薫刑茎契恵啓掲渓蛍傾携継詣慶憬稽憩鶏迎鯨隙撃桁傑肩倹兼剣拳軒圏堅嫌献遣賢謙鍵繭顕懸幻玄弦舷股虎孤弧枯雇誇" +
		"鼓錮顧互呉娯悟碁勾孔巧甲江坑抗攻更拘肯侯恒洪荒郊香貢控梗喉慌硬絞項溝綱酵稿衡購乞拷剛傲豪克酷獄駒込頃昆恨婚痕紺魂墾懇佐沙唆詐鎖挫采砕宰栽彩斎債催塞歳載埼剤崎削柵索酢搾錯咲刹拶撮擦桟惨傘斬暫旨伺刺祉肢施" +
		"恣脂紫嗣雌摯賜諮侍滋慈餌璽鹿軸𠮟疾執湿嫉漆芝赦斜煮遮邪蛇酌釈爵寂朱狩殊珠腫趣寿呪需儒囚舟秀臭袖羞愁酬醜蹴襲汁充柔渋銃獣叔淑粛塾俊瞬旬巡盾准殉循潤遵庶緒如叙徐升召匠床抄肖尚昇沼宵症祥称渉紹訟掌晶焦硝粧詔" +
		"奨詳彰憧衝償礁鐘丈冗浄剰畳縄壌嬢錠譲醸拭殖飾触嘱辱尻伸芯辛侵津唇娠振浸紳診寝慎審震薪刃尽迅甚陣尋腎須吹炊帥粋衰酔遂睡穂随髄枢崇据杉裾瀬是井姓征斉牲凄逝婿誓請醒斥析脊隻惜戚跡籍拙窃摂仙占扇栓旋煎羨腺詮践" +
		"箋潜遷薦繊鮮禅漸膳繕狙阻租措粗疎訴塑遡礎双壮荘捜挿桑掃曹曽爽喪痩葬僧遭槽踪燥霜騒藻憎贈即促捉俗賊遜汰妥唾堕惰駄耐怠胎泰堆袋逮替滞戴滝択沢卓拓託濯諾濁但脱奪棚誰丹旦胆淡嘆端綻鍛弾壇恥致遅痴稚緻畜逐蓄秩窒" +
		"嫡沖抽衷酎鋳駐弔挑彫眺釣貼超跳徴嘲澄聴懲勅捗沈珍朕陳鎮椎墜塚漬坪爪鶴呈廷抵邸亭貞帝訂逓偵堤艇締諦泥摘滴溺迭哲徹撤添塡殿斗吐妬途渡塗賭奴怒到逃倒凍唐桃透悼盗陶塔搭棟痘筒稲踏謄藤闘騰洞胴瞳峠匿督篤栃凸突屯" +
		"豚頓貪鈍曇丼那奈梨謎鍋軟尼弐匂虹尿妊忍寧捻粘悩濃把覇婆罵杯排廃輩培陪媒賠伯拍泊迫剝舶薄漠縛爆箸肌鉢髪伐抜罰閥氾帆汎伴阪畔般販斑搬煩頒範繁藩蛮盤妃彼披卑疲被扉碑罷避尾眉微膝肘匹泌姫漂苗描猫浜賓頻敏瓶扶怖" +
		"阜附訃赴浮符普腐敷膚賦譜侮舞封伏幅覆払沸紛雰噴墳憤丙併柄塀幣弊蔽餅壁璧癖蔑偏遍哺捕舗募慕簿芳邦奉抱泡胞俸倣峰砲崩蜂飽褒縫乏忙坊妨房肪某冒剖紡傍帽貌膨謀頰朴睦僕墨撲没勃堀奔翻凡盆麻摩磨魔昧埋膜枕又抹慢漫" +
		"魅岬蜜妙眠矛霧娘冥銘滅免麺茂妄盲耗猛網黙紋冶弥厄躍闇喩愉諭癒唯幽悠湧猶裕雄誘憂融与誉妖庸揚揺溶腰瘍踊窯擁謡抑沃翼拉裸羅雷頼絡酪辣濫藍欄吏痢履璃離慄柳竜粒隆硫侶虜慮了涼猟陵僚寮療瞭糧厘倫隣瑠涙累塁励戻鈴" +
		"零霊隷齢麗暦劣烈裂恋廉錬呂炉賂露弄郎浪廊楼漏籠麓賄脇惑枠湾腕";
	
	/**
	 * Constructor
	 */
	public StatsFragment ()
	{
		charts = new Vector<TYChart> ();
		gcharts = new Vector<GenericChart> ();
		fcharts = new Vector<IconizableChart> ();
		hdbc = new HistoryDatabaseCache ();
		
		semiPreservedState = new Hashtable<Integer, Boolean> ();
		netwe = new NetworkEngine ();
		
		timeline = new ReviewsTimelineChart (netwe, R.id.os_review_timeline_item,
											 R.id.os_review_timeline_srs, MeterSpec.T.OTHER_STATS);
		netwe.add (timeline);
		
		netwe.add (new ItemDistributionChart (netwe, R.id.os_kanji_levels, MeterSpec.T.OTHER_STATS, EnumSet.of (Item.Type.KANJI)));
		netwe.add (new ItemDistributionChart (netwe, R.id.os_levels, MeterSpec.T.MORE_STATS, EnumSet.allOf (Item.Type.class)));

		netwe.add (new ItemAgeChart (netwe, R.id.ct_age_distribution, MeterSpec.T.OTHER_STATS, EnumSet.allOf (Item.Type.class)));

		netwe.add (new KanjiProgressChart (netwe, R.id.os_jlpt, MeterSpec.T.OTHER_STATS, R.string.jlpt5, KLIB_JLPT_5));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_jlpt, MeterSpec.T.OTHER_STATS, R.string.jlpt4, KLIB_JLPT_4));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_jlpt, MeterSpec.T.OTHER_STATS, R.string.jlpt3, KLIB_JLPT_3));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_jlpt, MeterSpec.T.OTHER_STATS, R.string.jlpt2, KLIB_JLPT_2));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_jlpt, MeterSpec.T.OTHER_STATS, R.string.jlpt1, KLIB_JLPT_1));		

		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo1, KLIB_JOYO_1));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo2, KLIB_JOYO_2));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo3, KLIB_JOYO_3));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo4, KLIB_JOYO_4));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo5, KLIB_JOYO_5));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyo6, KLIB_JOYO_6));		
		netwe.add (new KanjiProgressChart (netwe, R.id.os_joyo, MeterSpec.T.OTHER_STATS, R.string.joyoS, KLIB_JOYO_S));		
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
		
		if (getActivity () != null) {
			srsds.setCoreStats (cs);
			kanjids.setCoreStats (cs);
			vocabds.setCoreStats (cs);
			for (TYChart tyc : charts)
				tyc.refresh ();
		
			for (GenericChart gc : gcharts)
				gc.setCoreStats (cs);
		}
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
		
		parent = inflater.inflate (R.layout.stats, container, false);
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
		gcharts.add (levels = new LevelupSource ());
		
		if (cs != null)
			setCoreStats (cs);
		else if (task == null) {
			task = new GetCoreStatsTask (main, main.getConnection ());
			task.execute ();
		}
		
		((LowPriorityScrollView) parent).setCallback (new Callback ());
				
		return parent;
    }
	
	@Override
	public void onActivityCreated (Bundle bundle)
	{
		super.onActivityCreated (bundle);
		
		fcharts = new Vector<IconizableChart> ();
		fcharts.add ((IconizableChart) parent.findViewById (R.id.ct_age_distribution));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_kanji_levels));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_levels));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_jlpt));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_joyo));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_review_timeline_item));
		fcharts.add ((IconizableChart) parent.findViewById (R.id.os_review_timeline_srs));
						
		netwe.bind (main, parent);
	}
	
	@Override
	public void onDestroyView ()
	{
		super.onDestroyView ();

		netwe.unbind ();
	}
	
	@Override
	public void onStart ()
	{
		super.onStart ();
		
		SharedPreferences prefs;
		IconizableChart ic;
		Boolean value;
		int i;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (main);
		for (i = 0; i < preserved.length; i++) {
			ic = (IconizableChart) parent.findViewById (preserved [i]);
			ic.setOpen (prefs.getBoolean (KEY_OPEN + preserved [i], false));
		}
		
		for (i = 0; i < semiPreserved.length; i++) {
			ic = (IconizableChart) parent.findViewById (semiPreserved [i]);
			value = semiPreservedState.get (semiPreserved [i]);
			ic.setOpen (value != null ? value : false);
		}					
	}

	@Override
	public void onStop ()
	{
		super.onStop ();
		
		Editor prefs;
		IconizableChart ic;
		int i;
		
		prefs = PreferenceManager.getDefaultSharedPreferences (main).edit ();
		for (i = 0; i < preserved.length; i++) {
			ic = (IconizableChart) parent.findViewById (preserved [i]);
			prefs.putBoolean (KEY_OPEN + preserved [i], ic.isOpen ());
		}
		prefs.commit ();
		
		for (i = 0; i < semiPreserved.length; i++) {
			ic = (IconizableChart) parent.findViewById (semiPreserved [i]);
			semiPreservedState.put (semiPreserved [i], ic.isOpen ());
		}					
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
		setFixup (main.dbfixup);
	}
	
	public void setFixup (MainActivity.FixupState state)
	{
		HistogramChart hc;
		Resources res;
		
		res = getResources ();
		hc = (HistogramChart) parent.findViewById (R.id.hi_levels);
		switch (state) {
		case NOT_RUNNING:
			break;
			
		case RUNNING:
			hc.alert (res.getString (R.string.db_fixup_text));
			break;
		
		case FAILED:
			if (levels != null)
				hc.alert (res.getString (R.string.db_fixup_fail), levels);
			break;
			
		case DONE:
			hc.hideAlert ();
			new GetCoreStatsTask (main, main.getConnection ()).execute ();
		}
	}
	
	@Override
	public int getName ()
	{
		return R.string.tag_stats;
	}
	
	@Override
	public void refreshComplete (DashboardData dd)
	{
		List<DataSet> ds;
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
				ds = getSRSDataSets (dd.od.srs);
				pc.setData (ds, getInfoSet (ds), 5);
			} else
				pc.setVisibility (View.GONE);

			pc = (PieChart) parent.findViewById (R.id.pc_kanji);
			ds = getKanjiProgDataSets (dd.od.srs);
			pc.setData (ds, getInfoSet (ds), 5);
				
			pc = (PieChart) parent.findViewById (R.id.pc_vocab);
			ds = getVocabProgDataSets (dd.od.srs);
			pc.setData (ds, getInfoSet (ds), 5);
			
			for (TYChart chart : charts)
				chart.setOrigin (dd.creation);
				
			timeline.refresh (main.getConnection (), dd);
			
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
		
		ds = new DataSet (res.getString (R.string.tag_burned),
				  		  res.getColor (R.color.burned),
				  		  srs.burned.total);

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

	/**
	 * Creates the infoset needed by the any progression distribution plot
	 * @param ds the input datasets 
	 * @return a list of information dataset 
	 */
	protected List<InfoSet> getInfoSet (List<DataSet> dses)
	{
		List<InfoSet> ans;
		Resources res;
		InfoSet ds;
		float count;
		
		res = getResources ();
		ans = new Vector<InfoSet> ();

		count = dses.get (1).value +	/* guru */
				dses.get (2).value +	/* master */
				dses.get (3).value;		/* enlightened */
		if (dses.size () > 4)
			count += dses.get (4).value; /* burned */
		
		ds = new InfoSet (res.getString (R.string.tag_unlocked), count + dses.get (0).value);
		ans.add (ds);

		ds = new InfoSet (res.getString (R.string.tag_learned), count);
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
	public void flush (Tab.RefreshType rtype, boolean fg)
	{
		switch (rtype) {
		case FULL_EXPLICIT:			
			if (fg) {
				netwe.flush ();
				for (IconizableChart chart : fcharts)
					chart.flush ();
			}
			/* Fall through */
		case FULL_IMPLICIT:
			/* Fall through */			
		case MEDIUM:
			/* Fall through */
		case LIGHT:
			/* Fall through */ 
		}
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
		return scrolling (false);
	}
	
	public boolean scrolling (boolean strict)
	{
		for (TYChart chart : charts)
			if (chart.scrolling (strict))
				return true;
		
		for (GenericChart chart : gcharts)
			if (chart.scrolling (strict))
				return true;
		
		if (netwe.scrolling (strict))
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
			
			flushDatabase ();
			
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
	
	@Override
	public void flushDatabase ()
	{
		hdbc.flush ();		
		setCoreStats (cs);
	}
	
}
