package com.wanikani.androidnotifier.stats;

import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import android.content.res.Resources;
import android.view.View;

import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.MeterSpec;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.HistogramChart;
import com.wanikani.androidnotifier.graph.HistogramPlot;
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.androidnotifier.graph.HistogramPlot.Sample;
import com.wanikani.wklib.Connection.Meter;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.UserInformation;
import com.wanikani.wklib.Vocabulary;

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

public class ItemAgeChart implements NetworkEngine.Chart {
	
	private class ResourceData {
		
		Meter meter;
		
		EnumMap<SRSLevel, String> tags;
		
		EnumMap<SRSLevel, Integer> colors;
		
		public ResourceData (MainActivity main)
		{
			Resources res;
			
			meter = mtype.get (main);
			
			res = main.getResources ();
			
			tags = new EnumMap<SRSLevel, String> (SRSLevel.class);
			colors = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			
			tags.put (SRSLevel.APPRENTICE, res.getString (R.string.tag_apprentice));
			tags.put (SRSLevel.GURU, res.getString (R.string.tag_guru));
			tags.put (SRSLevel.MASTER, res.getString (R.string.tag_master));
			tags.put (SRSLevel.ENLIGHTEN, res.getString (R.string.tag_enlightened));
			tags.put (SRSLevel.BURNED, res.getString (R.string.tag_burned));
			
			colors.put (SRSLevel.APPRENTICE, res.getColor (R.color.apprentice));
			colors.put (SRSLevel.GURU, res.getColor (R.color.guru));
			colors.put (SRSLevel.MASTER, res.getColor (R.color.master));
			colors.put (SRSLevel.ENLIGHTEN, res.getColor (R.color.enlightened));
			colors.put (SRSLevel.BURNED, res.getColor (R.color.burned));
		}
		
	}
	
	private class State implements NetworkEngine.State {
		
		private UserInformation ui;
		
		/// The series
		private List<HistogramPlot.Series> series;

		/// The SRS to series mapping
		private EnumMap<SRSLevel, HistogramPlot.Series> map;
		
		/// The SRS to idx mapping
		private EnumMap<SRSLevel, Integer> imap;

		/// The actual (not scaled) data
		private List<HistogramPlot.Samples> bars;
		
		/// The scaled data, ready to be plotted
		private List<HistogramPlot.Samples> sbars; 
		
		/// Item types collected so far
		private EnumSet<Item.Type> availableTypes;
		
		/// Item we are currently collecting
		private EnumSet<Item.Type> collectedTypes;
		
		/// Got an error
		private boolean error;
		
		/// Time scale, in days
		private static final int SCALE = 7;

		private static final int MIN_SAMPLES = 20;
		
		private static final int MIN_BARS = 3;
		
		public State (UserInformation ui)
		{
			this.ui = ui;
			
			availableTypes = EnumSet.noneOf (Item.Type.class);

			error = false;
			series = new Vector<HistogramPlot.Series> ();
			map = new EnumMap<SRSLevel, HistogramPlot.Series> (SRSLevel.class);
			imap = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			
			add (SRSLevel.APPRENTICE);
			add (SRSLevel.GURU);
			add (SRSLevel.MASTER);
			add (SRSLevel.ENLIGHTEN);
			add (SRSLevel.BURNED);
			
			imap.put (SRSLevel.APPRENTICE, 0);
			imap.put (SRSLevel.GURU, 1);
			imap.put (SRSLevel.MASTER, 2);
			imap.put (SRSLevel.ENLIGHTEN, 3);
			imap.put (SRSLevel.BURNED, 4);
			
			bars = new Vector<HistogramPlot.Samples> ();			
		}
		
		private HistogramPlot.Samples getBar (int index)
		{
			HistogramPlot.Samples bar;

			while (bars.size () <= index) {				
				bar = new HistogramPlot.Samples (Integer.toString (bars.size ()));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.APPRENTICE)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.GURU)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.MASTER)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.ENLIGHTEN)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.BURNED)));
				bars.add (bar);
			}
			
			return bars.get (index);
		}
				
		private void add (SRSLevel level)		
		{			
			HistogramPlot.Series s;
			
			s = new HistogramPlot.Series ();			
			series.add (s);
			map.put (level, s);
		}
		
		public void loadResources (ResourceData rd)
		{
			for (SRSLevel srs : EnumSet.allOf (SRSLevel.class))
				map.get (srs).set (rd.tags.get (srs), rd.colors.get (srs));	
		}
		
		@Override
		public void done (boolean ok)
		{
			error = !ok;
		
			if (ok) {				
				availableTypes.addAll (collectedTypes);
				sbars = scale (bars);
				for (Item.Type t : types)
					if (!availableTypes.contains (t))
						return;
			}					
			
			updatePlot (this);
		}	
		
		private List<HistogramPlot.Samples> scale (List<HistogramPlot.Samples> bars)
		{
			List<HistogramPlot.Samples> ans;
			HistogramPlot.Samples scalb;
			HistogramPlot.Sample ls;
			float scale;
			long total, val;
			
			ans = new Vector<HistogramPlot.Samples> ();
			for (HistogramPlot.Samples bar : bars) {

				total = bar.getTotal ();
								
				scale = total >= MIN_SAMPLES ? 100f / total : 0;
				
				ans.add (scalb = new HistogramPlot.Samples (bar.tag));
				total = 0;
				ls = null;
				for (HistogramPlot.Sample s : bar.samples) {
					val = (long) (s.value * scale);
					if (val > 0)
						scalb.samples.add (ls = new HistogramPlot.Sample (s.series, val));
					else
						scalb.samples.add (new HistogramPlot.Sample (s.series));
					total += val;					
				}
				
				if (ls != null)
					ls.value += 100 - total;						
			}
			
			while (!ans.isEmpty () && ans.get (ans.size () - 1).isEmpty ())
				ans.remove (ans.size () - 1);
			
			if (ans.size () < MIN_BARS)
				ans.clear ();
			
			return ans;			
		}
		
		public void newRadical (ItemLibrary<Radical> radicals)
		{
			if (availableTypes.contains (Item.Type.RADICAL) || !types.contains (Item.Type.RADICAL))
				return;
			
			for (Item i : radicals.list)
				put (i);
		}
		
		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			if (availableTypes.contains (Item.Type.KANJI) || !types.contains (Item.Type.KANJI))
				return;

			for (Item i : kanji.list)
				put (i);
		}

		public void newVocab (ItemLibrary<Vocabulary> vocabs)
		{
			if (availableTypes.contains (Item.Type.VOCABULARY) || !types.contains (Item.Type.VOCABULARY))
				return;

			for (Item i : vocabs.list)
				put (i);			
		}

		private void put (Item i)
		{
			Date udate;
			int idx;
			
			udate = i.getUnlockedDate ();
			if (udate == null || ui == null)
				return;
			
			idx = (ui.getDay () - ui.getDay (udate)) / SCALE;
			
			if (i.stats != null)	// stats is null for locked items. Should not be, however...
				getBar (idx).samples.get (imap.get (i.stats.srs)).value++;
		}
	}
	
	NetworkEngine netwe;
	
	int id;
	
	ResourceData rd;
	
	HistogramChart chart;
	
	MeterSpec.T mtype;
	
	/// The datasrouce
	IconizableChart.DataSource ds;
	
	State state;
	
	State nextState;
	
	EnumSet<Item.Type> types;
	
	public ItemAgeChart (NetworkEngine netwe, int id, MeterSpec.T mtype, EnumSet<Item.Type> types)
	{
		this.netwe = netwe;
		this.id = id;
		this.mtype = mtype;
		this.types = types;
	}
	
	@Override
	public void bind (MainActivity main, View view)
	{
		if (rd == null)
			rd = new ResourceData (main);
		
		chart = (HistogramChart) view.findViewById (id);		
		chart.setDataSource (this);
		
		if (state != null)
			updatePlot (state);
	}
	
	@Override
	public void unbind ()
	{
		chart = null;
	}
		
	@Override
	public State startUpdate (UserInformation ui, EnumSet<Item.Type> type)
	{
		if (state != null)
			return null;
		
		if (nextState == null)
			nextState = new State (ui);
		
		nextState.collectedTypes = type;
			
		return nextState; 
	}

	private void updatePlot (State state)
	{			
		this.state = state;
		
		if (nextState == state)
			nextState = null;
		
		/* Not bound */
		if (chart == null)
			return;
		
		if (rd != null)
			state.loadResources (rd);
		
		chart.setVisibility (state.error || !state.sbars.isEmpty () ? View.VISIBLE : View.GONE);
		
		if (state.error)
			chart.setError ();
		else if (!state.sbars.isEmpty ())		
			chart.setData (state.series, state.sbars, -1);
	}

	public boolean scrolling (boolean strict)
	{
		return chart != null && chart.scrolling (strict);
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
			updatePlot (state);
		else if (rd != null)
			netwe.request (rd.meter, types);
	}	
}
