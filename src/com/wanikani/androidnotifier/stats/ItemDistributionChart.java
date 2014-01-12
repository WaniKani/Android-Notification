package com.wanikani.androidnotifier.stats;

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
import com.wanikani.wklib.Connection.Meter;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
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

public class ItemDistributionChart implements NetworkEngine.Chart {
	
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
		
		private int levels;
		
		/// The series
		private List<HistogramPlot.Series> series;

		/// The SRS to series mapping
		private EnumMap<SRSLevel, HistogramPlot.Series> map;
		
		/// The SRS to idx mapping
		private EnumMap<SRSLevel, Integer> imap;

		/// The actual data
		private List<HistogramPlot.Samples> bars;
		
		/// Item types collected so far
		private EnumSet<Item.Type> availableTypes;
		
		/// Item we are currently collecting
		private EnumSet<Item.Type> collectedTypes;
		
		/// Got an error
		private boolean error;
		
		public State (int levels)
		{
			this.levels = levels;

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
			
			initBars ();
			
		}
		
		private void initBars ()
		{
			HistogramPlot.Samples bar;
			int i;

			bars = new Vector<HistogramPlot.Samples> ();

			for (i = 1; i <= levels; i++) {
				bar = new HistogramPlot.Samples (Integer.toString (i));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.APPRENTICE)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.GURU)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.MASTER)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.ENLIGHTEN)));
				bar.samples.add (new HistogramPlot.Sample (map.get (SRSLevel.BURNED)));
				bars.add (bar);
			}			
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
				for (Item.Type t : types)
					if (!availableTypes.contains (t))
						return;
			}					
			
			updatePlot (this);
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
			if (i.stats != null)	// stats is null for locked items
				bars.get (i.level - 1).samples.get (imap.get (i.stats.srs)).value++;
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
	
	public ItemDistributionChart (NetworkEngine netwe, int id, MeterSpec.T mtype, EnumSet<Item.Type> types)
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
	public State startUpdate (int levels, EnumSet<Item.Type> type)
	{
		if (state != null)
			return null;
		
		if (nextState == null)
			nextState = new State (levels);
		
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
		
		if (state.error)
			chart.setError ();
		else		
			chart.setData (state.series, state.bars, -1);
	}

	public boolean scrolling ()
	{
		return chart != null && chart.scrolling ();
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
