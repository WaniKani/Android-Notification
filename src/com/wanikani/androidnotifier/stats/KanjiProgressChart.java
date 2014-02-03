package com.wanikani.androidnotifier.stats;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Vector;

import android.content.res.Resources;
import android.view.View;

import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.MeterSpec;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.ProgressChart;
import com.wanikani.androidnotifier.graph.ProgressPlot;
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

public class KanjiProgressChart implements NetworkEngine.Chart {
	
	private class ResourceData {
		
		Meter meter;
		
		String title;
		
		EnumMap<SRSLevel, String> tags;
		
		EnumMap<SRSLevel, Integer> colors;
		
		String lockedTag;
		
		int lockedColor;
		
		public ResourceData (MainActivity main)
		{
			Resources res;
			
			meter = mtype.get (main);
			
			res = main.getResources ();
			
			title = res.getString (titleId);
			
			tags = new EnumMap<SRSLevel, String> (SRSLevel.class);
			colors = new EnumMap<SRSLevel, Integer> (SRSLevel.class);
			
			tags.put (SRSLevel.APPRENTICE, res.getString (R.string.tag_apprentice));
			tags.put (SRSLevel.GURU, res.getString (R.string.tag_guru));
			tags.put (SRSLevel.MASTER, res.getString (R.string.tag_master));
			tags.put (SRSLevel.ENLIGHTEN, res.getString (R.string.tag_enlightened));
			tags.put (SRSLevel.BURNED, res.getString (R.string.tag_burned));
			lockedTag = res.getString (R.string.tag_locked);
			
			colors.put (SRSLevel.APPRENTICE, res.getColor (R.color.apprentice));
			colors.put (SRSLevel.GURU, res.getColor (R.color.guru));
			colors.put (SRSLevel.MASTER, res.getColor (R.color.master));
			colors.put (SRSLevel.ENLIGHTEN, res.getColor (R.color.enlightened));
			colors.put (SRSLevel.BURNED, res.getColor (R.color.burned));
			lockedColor = res.getColor (R.color.locked);
		}
		
	}
	
	private class State implements NetworkEngine.State {
		
		EnumMap<SRSLevel, ProgressPlot.DataSet> slds;
		
		ProgressPlot.DataSet rds;

		Vector<ProgressPlot.DataSet> dses;
		
		boolean error;
		
		public State ()
		{
			slds = new EnumMap<SRSLevel, ProgressPlot.DataSet> (SRSLevel.class);
			slds.put (SRSLevel.APPRENTICE, new ProgressPlot.DataSet (0));
			slds.put (SRSLevel.GURU, new ProgressPlot.DataSet (0));
			slds.put (SRSLevel.MASTER, new ProgressPlot.DataSet (0));
			slds.put (SRSLevel.ENLIGHTEN, new ProgressPlot.DataSet (0));
			slds.put (SRSLevel.BURNED, new ProgressPlot.DataSet (0));			
			rds = new ProgressPlot.DataSet (library.length ());			
		}
						
		public void loadResources (ResourceData rd)
		{
			for (SRSLevel srs : EnumSet.allOf (SRSLevel.class))
				slds.get (srs).set (rd.tags.get (srs), rd.colors.get (srs));
			rds.set (rd.lockedTag, rd.lockedColor);			
		}

		@Override
		public void done (boolean ok)
		{
			error = !ok;
			
			dses = new Vector<ProgressPlot.DataSet> (slds.values ());
			dses.add (rds);

			updatePlot (this);
		}

		public void newRadical (ItemLibrary<Radical> radicals)
		{
			/* empty */
		}

		public void newKanji (ItemLibrary<Kanji> kanji)
		{
			for (Kanji k : kanji.list) {
				if (k.stats != null && library.contains (k.character)) {
					slds.get (k.stats.srs).value++;
					rds.value--;
				}
			}		
		}

		public void newVocab (ItemLibrary<Vocabulary> vocabs)
		{
			/* empty */
		}		
			
	}
	
	NetworkEngine netwe;
	
	ProgressChart chart;
	
	ProgressChart.SubPlot plot;
	
	String library;
	
	ResourceData rd;
	
	int id;
	
	int titleId;
	
	MeterSpec.T mtype;
	
	State state;
	
	EnumSet<Item.Type> types;
	
	public KanjiProgressChart (NetworkEngine netwe, int id, MeterSpec.T mtype, int titleId, String library)
	{
		this.netwe = netwe;
		this.id = id;
		this.mtype = mtype;
		this.library = library;
		this.titleId = titleId;
		
		types = EnumSet.of (Item.Type.KANJI);
	}

	@Override
	public void bind (MainActivity main, View view)
	{
		if (rd == null)
			rd = new ResourceData (main);
		
		chart = (ProgressChart) view.findViewById (id);		
		chart.setDataSource (this);
		plot = chart.addData (rd.title);

		if (state != null)
			updatePlot (state);		
	}
	
	@Override
	public void unbind ()
	{
		chart = null;
		plot = null;		
	}
	
	@Override
	public State startUpdate (UserInformation ui, EnumSet<Item.Type> type)
	{
		return type.contains (Item.Type.KANJI) ? new State () : null;
	}
	
	public boolean scrolling (boolean strict)
	{
		return false;
	}
	
	private void updatePlot (State state)
	{
		this.state = state;

		/* Not bound */
		if (chart == null)
			return;
		
		if (rd != null)
			state.loadResources (rd);
		
		if (state.error)
			chart.setError ();
		else		
			plot.setData (state.dses);				
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
