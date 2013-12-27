package com.wanikani.androidnotifier.stats;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.provider.ContactsContract.Contacts.Data;
import android.view.View;

import com.wanikani.androidnotifier.DashboardData;
import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.MeterSpec;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.HistogramChart;
import com.wanikani.androidnotifier.graph.HistogramPlot;
import com.wanikani.androidnotifier.graph.HistogramPlot.Samples;
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.wklib.Connection;
import com.wanikani.wklib.Connection.Meter;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.Vocabulary;

public class ReviewsTimelineChart implements NetworkEngine.Chart {
	
	private class ResourceData {
		
		Meter meter;
		
		EnumMap<Item.Type, String> itags;
				
		EnumMap<Item.Type, Integer> icolors;
		
		EnumMap<SRSLevel, String> stags;
		
		EnumMap<SRSLevel, Integer> scolors;

		public ResourceData (MainActivity main)
		{
			Resources res;
			
			meter = mtype.get (main);
			
			res = main.getResources ();
			
			itags = new EnumMap<Item.Type, String> (Item.Type.class);
			icolors = new EnumMap<Item.Type, Integer> (Item.Type.class);
						
			itags.put (Item.Type.RADICAL, res.getString (R.string.tag_radicals));
			itags.put (Item.Type.KANJI, res.getString (R.string.tag_kanji));
			itags.put (Item.Type.VOCABULARY, res.getString (R.string.tag_vocab));
			
			icolors.put (Item.Type.RADICAL, res.getColor (R.color.radical));
			icolors.put (Item.Type.KANJI, res.getColor (R.color.kanji));
			icolors.put (Item.Type.VOCABULARY, res.getColor (R.color.vocabulary));
			
			stags = new EnumMap<SRSLevel, String> (SRSLevel.class);
			scolors = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
						
			stags.put (SRSLevel.APPRENTICE, res.getString (R.string.tag_apprentice));
			stags.put (SRSLevel.GURU, res.getString (R.string.tag_guru));
			stags.put (SRSLevel.MASTER, res.getString (R.string.tag_master));
			stags.put (SRSLevel.ENLIGHTEN, res.getString (R.string.tag_enlightened));
			
			scolors.put (SRSLevel.APPRENTICE, res.getColor (R.color.apprentice));
			scolors.put (SRSLevel.GURU, res.getColor (R.color.guru));
			scolors.put (SRSLevel.MASTER, res.getColor (R.color.master));
			scolors.put (SRSLevel.ENLIGHTEN, res.getColor (R.color.enlightened));
		}
		
	}
	
	private class LevelData {
		
		EnumMap<SRSLevel, Integer> srsd;
		
		EnumMap<Item.Type, Integer> itemd;
		
		public LevelData ()
		{
			srsd = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			for (SRSLevel srs : EnumSet.allOf (SRSLevel.class))
				srsd.put (srs, 0);
			
			itemd = new EnumMap<Item.Type, Integer> (Item.Type.class);
			for (Item.Type it : EnumSet.allOf (Item.Type.class))
				itemd.put (it, 0);
		}
		
		public void put (Item i)
		{
			if (i.stats != null && i.stats.srs != null)
				srsd.put (i.stats.srs, srsd.get (i.stats.srs) + 1);
			itemd.put (i.type, itemd.get (i.type) + 1);			
		}
		
		public int size ()
		{
			int ans;
			
			ans = 0;
			for (Integer i : srsd.values ())
				ans += i;
			
			return ans;
		}
	}
	
	private class TimelineData {

		Map<Integer, LevelData> levels;
		
		long time;
		
		String tag;
		
		public TimelineData (long time, String tag)
		{
			this.time = time;
			this.tag = tag;
			
			levels = new Hashtable<Integer, LevelData> ();
		}
		
		public void put (Item i)
		{
			LevelData ld;
			
			ld = (LevelData) levels.get (i.level);
			if (ld == null) {
				ld = new LevelData ();
				levels.put (i.level, ld);
			}
			
			ld.put (i);
		}		
		
		public void purge (int laa [])
		{
			int i;
			
			for (i = 0; i < laa.length; i++)
				levels.remove (laa [i]);
		}
				
		public int size ()
		{
			int ans;
			
			ans = 0;
			for (LevelData ld : levels.values ())
				ans += ld.size ();
			
			return ans;
		}
	}
	
	private class State implements NetworkEngine.State {
		
		/// The data
		private List<TimelineData> data;
		
		/// Item types collected so far
		private EnumSet<Item.Type> availableTypes;
		
		/// Item we are currently collecting
		private EnumSet<Item.Type> collectedTypes;
		
		/// Got an error
		private boolean error;
	
		/// Date format
		private SimpleDateFormat df;
		
		public State ()
		{
			Calendar cal;
			String tag;
			int i, min;
		
			df = new SimpleDateFormat ("HH:mm", Locale.US);
			
			availableTypes = EnumSet.noneOf (Item.Type.class);

			error = false;
			data = new Vector<TimelineData> ();
			
			cal = Calendar.getInstance ();
			cal.set (Calendar.SECOND, 0);
			cal.set (Calendar.MILLISECOND, 0);
			min = cal.get (Calendar.MINUTE);
			min -= min % 15;			
			cal.set (Calendar.MINUTE, min);
			
			for (i = 0; i < INTERVALS; i++) {
				tag = cal.get (Calendar.MINUTE) % 30 == 0 ? df.format (cal.getTime ()) : "";
				data.add (new TimelineData (cal.getTime ().getTime (), tag));
				cal.add (Calendar.MINUTE, 15);
			}
		}
						
		@Override
		public void done (boolean ok)
		{
			error = !ok;
		
			if (ok) {
				availableTypes.addAll (collectedTypes);
				for (Item.Type t : EnumSet.allOf (Item.Type.class))
					if (!availableTypes.contains (t))
						return;
			}					
			
			updatePlots (this);
		}	
		
		public void newRadical (ItemLibrary<Radical> radicals)
		{
			if (availableTypes.contains (Item.Type.RADICAL))
				return;
			
			for (Item i : radicals.list)
				put (i);
		}
		
		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			if (availableTypes.contains (Item.Type.KANJI))
				return;

			for (Item i : kanji.list)
				put (i);
		}

		public void newVocab (ItemLibrary<Vocabulary> vocabs)
		{
			if (availableTypes.contains (Item.Type.VOCABULARY))
				return;

			for (Item i : vocabs.list)
				put (i);			
		}
		
		private void put (Item i)
		{
			TimelineData data;
			Date time;
			
			time = i.getAvailableDate ();
			if (time == null || i.stats == null || i.stats.burned)
				return;
			
			data = dateToData (time);
			if (data == null)
				return;
			
			data.put (i);
		}
		
		private TimelineData dateToData (Date date)
		{
			long i;
			
			if (data.isEmpty ())
				return null;
			
			i = (date.getTime () - data.get (0).time) / (15 * 60 * 1000);
			if (i < 0)
				i = 0;
			
			return i < data.size () ? data.get ((int) i) : null; 
		}		
		
		public boolean compatible (DashboardData dd)
		{			
			int nextDay, nextHour;
			long now, dayLimit, hourLimit;
			
			nextDay = nextHour = 0;
			now = System.currentTimeMillis ();
			hourLimit = now + 3600 * 1000;
			dayLimit = now + 24 * 3600 * 1000;
			for (TimelineData tl : data) {
				if (tl.time < hourLimit)
					nextHour += tl.size ();

				if (tl.time < dayLimit)
					nextDay += tl.size ();
				else
					break;								
			}
			
			return (dd.reviewsAvailableNextDay + dd.reviewsAvailable) == nextDay &&
				   (dd.reviewsAvailableNextHour + dd.reviewsAvailable) == nextHour;
		}
		
		public void relocate (int laa [], ItemLibrary<Item> lib, int idx)
		{
			while (idx-- > 0 && data.size () > 0)
				data.remove (0);
			
			for (TimelineData td : data)
				td.purge (laa);
			
			for (Item i : lib.list)
				put (i);
		}
		
	}
	
	private abstract class Histogram {
		
		/// The plot id
		int id;
		
		/// The chart
		HistogramChart chart;
		
		/// The series
		protected List<HistogramPlot.Series> series;
		
		public Histogram (int id)
		{
			this.id = id;
		}
		
		public void bind (View view)
		{
			chart = (HistogramChart) view.findViewById (id);		
			chart.setDataSource (ReviewsTimelineChart.this);
		}	
		
		public void unbind ()
		{
			chart = null;
		}
		
		public void updatePlot (State state)
		{
			/* Not bound */
			if (chart == null)
				return;
			
			if (state.error)
				chart.setError ();
			else		
				chart.setData (series, getBars (state), -1, true);
		}
		
		protected abstract List<Samples> getBars (State state);

		public boolean scrolling ()
		{
			return chart != null && chart.scrolling ();
		}
	}
	
	private class SRSHistogram extends Histogram {
		
		/// The SRS level to series mapping
		private EnumMap<SRSLevel, HistogramPlot.Series> map;
		
		/// The SRS level to idx mapping
		private EnumMap<SRSLevel, Integer> imap;

		public SRSHistogram (int id)
		{
			super (id);
			
			map = new EnumMap<SRSLevel, HistogramPlot.Series> (SRSLevel.class);
			imap = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			series = new Vector<HistogramPlot.Series> ();
			
			imap.put (SRSLevel.APPRENTICE, 0);
			imap.put (SRSLevel.GURU, 1);
			imap.put (SRSLevel.MASTER, 2);
			imap.put (SRSLevel.ENLIGHTEN, 3);
			
			add (SRSLevel.APPRENTICE);
			add (SRSLevel.GURU);
			add (SRSLevel.MASTER);						
			add (SRSLevel.ENLIGHTEN);						
		}
		
		private void add (SRSLevel srs)		
		{			
			HistogramPlot.Series s;
			
			s = new HistogramPlot.Series ();			
			series.add (s);
			map.put (srs, s);
		}
		
		public void loadResources (ResourceData rd)
		{
			for (SRSLevel srs : EnumSet.allOf (SRSLevel.class))
				if (srs != SRSLevel.BURNED)
					map.get (srs).set (rd.stags.get (srs), rd.scolors.get (srs));
				
		}	
		
		@Override
		protected List<Samples> getBars (State state)
		{
			HistogramPlot.Samples bar;
			List<Samples> bars;
			Iterator<TimelineData> i;
			TimelineData td;
			long now;

			bars = new Vector<HistogramPlot.Samples> ();
			
			i = state.data.iterator ();
			if (!i.hasNext ())
				return bars;
			td = i.next ();
			
			bar = new HistogramPlot.Samples (td.tag);
			bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.APPRENTICE)));
			bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.GURU)));
			bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.MASTER)));
			bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.ENLIGHTEN)));
			bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.BURNED)));
			bars.add (bar);

			now = System.currentTimeMillis ();

			while (td.time < now) {
				for (LevelData ld : td.levels.values ()) {
					bar.samples.get (imap.get (SRSLevel.APPRENTICE)).value += ld.srsd.get (SRSLevel.APPRENTICE);
					bar.samples.get (imap.get (SRSLevel.GURU)).value += ld.srsd.get (SRSLevel.GURU);
					bar.samples.get (imap.get (SRSLevel.MASTER)).value += ld.srsd.get (SRSLevel.MASTER);
					bar.samples.get (imap.get (SRSLevel.ENLIGHTEN)).value += ld.srsd.get (SRSLevel.ENLIGHTEN);
				}
				bar.tag = td.tag;
				td = i.hasNext () ? i.next () : null;
				if (td == null)
					break;
			}

			while (td != null && bars.size () < DISPLAYED_INTERVALS) {
				bar = new HistogramPlot.Samples (td.tag);
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.APPRENTICE)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.GURU)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.MASTER)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.ENLIGHTEN)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.BURNED)));
				bars.add (bar);				

				for (LevelData ld : td.levels.values ()) {
					bar.samples.get (imap.get (SRSLevel.APPRENTICE)).value += ld.srsd.get (SRSLevel.APPRENTICE);
					bar.samples.get (imap.get (SRSLevel.GURU)).value += ld.srsd.get (SRSLevel.GURU);
					bar.samples.get (imap.get (SRSLevel.MASTER)).value += ld.srsd.get (SRSLevel.MASTER);
					bar.samples.get (imap.get (SRSLevel.ENLIGHTEN)).value += ld.srsd.get (SRSLevel.ENLIGHTEN);
				}
				td = i.hasNext () ? i.next () : null;
			}
			
			return bars;
		}		
	}
	
	private class TypeHistogram extends Histogram {
		
		/// The item type to series mapping
		private EnumMap<Item.Type, HistogramPlot.Series> map;
		
		/// The item type to idx mapping
		private EnumMap<Item.Type, Integer> imap;

		public TypeHistogram (int id)		
		{
			super (id);
			
			map = new EnumMap<Item.Type, HistogramPlot.Series> (Item.Type.class);
			imap = new EnumMap<Item.Type, Integer> (Item.Type.class);
			series = new Vector<HistogramPlot.Series> ();
			
			imap.put (Item.Type.RADICAL, 0);
			imap.put (Item.Type.KANJI, 1);
			imap.put (Item.Type.VOCABULARY, 2);
			
			add (Item.Type.RADICAL);
			add (Item.Type.KANJI);
			add (Item.Type.VOCABULARY);			
		}
		
		private void add (Item.Type type)		
		{			
			HistogramPlot.Series s;
			
			s = new HistogramPlot.Series ();			
			series.add (s);
			map.put (type, s);
		}
		
		public void loadResources (ResourceData rd)
		{
			for (Item.Type type : EnumSet.allOf (Item.Type.class))
				map.get (type).set (rd.itags.get (type), rd.icolors.get (type));	
		}
		
		@Override
		protected List<Samples> getBars (State state)
		{
			HistogramPlot.Samples bar;
			List<Samples> bars;
			Iterator<TimelineData> i;
			TimelineData td;
			long now;

			bars = new Vector<HistogramPlot.Samples> ();
			
			i = state.data.iterator ();
			if (!i.hasNext ())
				return bars;
			td = i.next ();
			
			bar = new HistogramPlot.Samples (td.tag);
			bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.RADICAL)));
			bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.KANJI)));
			bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.VOCABULARY)));
			bars.add (bar);

			now = System.currentTimeMillis ();

			while (td.time < now) {
				for (LevelData ld : td.levels.values ()) {
					bar.samples.get (imap.get (Item.Type.RADICAL)).value += ld.itemd.get (Item.Type.RADICAL);
					bar.samples.get (imap.get (Item.Type.KANJI)).value += ld.itemd.get (Item.Type.KANJI);
					bar.samples.get (imap.get (Item.Type.VOCABULARY)).value += ld.itemd.get (Item.Type.VOCABULARY);
				}
				bar.tag = td.tag;
				td = i.hasNext () ? i.next () : null;
				if (td == null)
					break;
			}			

			while (td != null && bars.size () < DISPLAYED_INTERVALS) {
				bar = new HistogramPlot.Samples (td.tag);
				bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.RADICAL)));
				bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.KANJI)));
				bar.samples.add (new HistogramPlot.Sample (map.get (Item.Type.VOCABULARY)));
				bars.add (bar);
				for (LevelData ld : td.levels.values ()) {
					bar.samples.get (imap.get (Item.Type.RADICAL)).value += ld.itemd.get (Item.Type.RADICAL);
					bar.samples.get (imap.get (Item.Type.KANJI)).value += ld.itemd.get (Item.Type.KANJI);
					bar.samples.get (imap.get (Item.Type.VOCABULARY)).value += ld.itemd.get (Item.Type.VOCABULARY);
				}
				td = i.hasNext () ? i.next () : null;
			}
			
			return bars;
		}		
	}
	
	private class RelocateTask extends AsyncTask<Void, Void, ItemLibrary<Item>> {

		/// The connection
		private Connection conn;
		
		/// The user level
		private int level;
		
		/// The meter
		private Meter meter;
		
		/// Timeline count
		private int idx;

		/// Chart states
		State state;
		
		/// The relocated levels
		int laa [];
		
		public RelocateTask (Connection conn, int level, Meter meter, State state)
		{
			this.conn = conn;
			this.level = level;
			this.meter = meter;
			this.state = state;					
		}
				
		@Override
		protected ItemLibrary<Item> doInBackground (Void... v)
		{
			Set<Integer> levels;
			ItemLibrary <Item> lib;
			Iterator<Integer> i;
			int j;
			
			levels = new HashSet<Integer> (); 
			levels.add (level);	/* In case some new items appeared after a lessons session */
			for (TimelineData d : state.data) {
				if (d.time >= System.currentTimeMillis ())
					break;
				idx++;
				for (Integer l : d.levels.keySet ())
					levels.add (l);
			}
			
			/* We want to keep the last bar before now */
			if (idx > 0)
				idx--;
			
			laa = new int [levels.size ()];
			j = 0;
			for (i = levels.iterator (); i.hasNext (); j++) 
				laa [j] = i.next ();
			
			lib = new ItemLibrary<Item> ();
			try {
				if (rt == this)
					lib.add (conn.getRadicals (meter, laa));
				
				if (rt == this)
					lib.add (conn.getKanji (meter, laa));
				
				if (rt == this)
					lib.add (conn.getVocabulary (meter, laa));
			} catch (IOException e) {
				return null;
			}
			
			return lib;
		}
		
		@Override
		protected void onPostExecute (ItemLibrary<Item> lib)
		{
			if (lib != null && rt == this && this.state == ReviewsTimelineChart.this.state) {
				state.relocate (laa, lib, idx);
				updatePlots (state);
			}
		}		
	}
	

	NetworkEngine netwe;
	
	ResourceData rd;
	
	MeterSpec.T mtype;
	
	/// The datasrouce
	IconizableChart.DataSource ds;
	
	State state;
	
	State nextState;
	
	/// The SRS histogram factory
	private SRSHistogram srsh;
	
	/// The item type histogram factory
	private TypeHistogram typeh;
	
	/// Current relocate task
	volatile RelocateTask rt;
	
	/// Number of bars. One hour is four bars */
	public static int INTERVALS = 4 * 48;
	
	/// Number of displayed bars. One hour is four bars */
	public static int DISPLAYED_INTERVALS = 4 * 24;

	public ReviewsTimelineChart (NetworkEngine netwe, int iid, int sid, MeterSpec.T mtype)
	{
		this.netwe = netwe;
		this.mtype = mtype;
		
		typeh = new TypeHistogram (iid);
		srsh = new SRSHistogram (sid);
	}
	
	@Override
	public void bind (MainActivity main, View view)
	{
		if (rd == null) {
			rd = new ResourceData (main);
			typeh.loadResources (rd);
			srsh.loadResources (rd);
		}
		
		typeh.bind (view);
		srsh.bind (view);
		
		if (state != null)
			updatePlots (state);
	}
	
	@Override
	public void unbind ()
	{
		typeh.unbind ();
		srsh.unbind ();
	}
		
	@Override
	public State startUpdate (int levels, EnumSet<Item.Type> type)
	{
		if (state != null)
			return null;
		
		if (nextState == null)
			nextState = new State ();
		
		nextState.collectedTypes = type;
			
		return nextState; 
	}

	private void updatePlots (State state)
	{
		this.state = state;
		
		if (nextState == state)
			nextState = null;

		srsh.updatePlot (state);
		typeh.updatePlot (state);
	}

	public boolean scrolling ()
	{
		return srsh.scrolling () || typeh.scrolling ();
	}

	@Override
	public void flush ()
	{
		state = null;
	}
	
	@Override
	public void loadData ()
	{
		if (state != null)
			updatePlots (state);
		else if (rd != null)
			netwe.request (rd.meter, EnumSet.allOf (Item.Type.class));
	}
	
	private void relocate (Connection conn, int level)
	{
		long avail;
		
		if (state.data.size () > 0)
			avail = state.data.get (state.data.size () - 1).time - System.currentTimeMillis ();
		else
			avail = 0;
		
		if (avail < ((long) DISPLAYED_INTERVALS) * 15 * 60 * 1000) {
			state = null;
			netwe.flush ();
		} else if (rd != null)
			(rt = new RelocateTask (conn, level, rd.meter, state)).execute ();
	}
	
	public void refresh (Connection conn, DashboardData dd)
	{			
		if (state != null) {			
			if (state.compatible (dd)) {
				srsh.updatePlot (state);
				typeh.updatePlot (state);
			} else
				relocate (conn, dd.level);
		}
	}
}
