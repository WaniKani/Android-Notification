package com.wanikani.androidnotifier.stats;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

import com.wanikani.androidnotifier.MainActivity;
import com.wanikani.androidnotifier.MeterSpec;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.HistogramChart;
import com.wanikani.androidnotifier.graph.HistogramPlot;
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.Vocabulary;

public class ItemDistributionChart implements NetworkEngine.Chart {

	int id;
	
	HistogramChart chart;
	
	MeterSpec.T mtype;
	
	/// The series
	private List<HistogramPlot.Series> series;

	/// The SRS to series mapping
	private EnumMap<SRSLevel, HistogramPlot.Series> map;
	
	/// The SRS to idx mapping
	private EnumMap<SRSLevel, Integer> imap;

	/// The actual data
	private List<HistogramPlot.Samples> bars;
	
	/// Item needed for this chart
	private EnumSet<Item.Type> types; 

	/// Item types collected so far
	private EnumSet<Item.Type> availableTypes;
	
	public ItemDistributionChart (int id, MeterSpec.T mtype, EnumSet<Item.Type> types)
	{
		this.id = id;
		this.mtype = mtype;
		this.types = types;
		
		availableTypes = EnumSet.noneOf (Item.Type.class);
		
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
	}
	
	private void set (Resources res, SRSLevel level, int string, int color)		
	{			
		map.get (level).set (res.getString (string), res.getColor (color));
	}

	
	@Override
	public void bind (NetworkEngine netwe, MainActivity main, View view)
	{
		Resources res;
		
		res = main.getResources ();
		
		chart = (HistogramChart) view.findViewById (id);
		
		set (res, SRSLevel.APPRENTICE, R.string.tag_apprentice, R.color.apprentice);
		set (res, SRSLevel.GURU, R.string.tag_guru, R.color.guru);
		set (res, SRSLevel.MASTER, R.string.tag_master, R.color.master);
		set (res, SRSLevel.ENLIGHTEN, R.string.tag_enlightened, R.color.enlightened);
		set (res, SRSLevel.BURNED, R.string.tag_burned, R.color.burned);

		chart.setDataSource (netwe.getDataSource (mtype.get (main), types));
	}
	
	@Override
	public void unbind ()
	{
		chart = null;
	}
		
	private void add (SRSLevel level)		
	{			
		HistogramPlot.Series s;
		
		s = new HistogramPlot.Series ();			
		series.add (s);
		map.put (level, s);
	}
	
	public void startUpdate (int levels)
	{
		if (bars == null)
			initBars (levels);
	}

	public void update (EnumSet<Item.Type> types)
	{
		availableTypes.addAll (types);
		
		for (Item.Type t : this.types)
			if (!availableTypes.contains (t))
				return;
		
		if (chart != null)
			chart.setData (series, bars, -1);
	}

	private void initBars (int levels)
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
			
	public void newRadical (ItemLibrary<Radical> radicals)
	{
		if (!types.contains (Item.Type.RADICAL) ||
			availableTypes.contains (Item.Type.RADICAL))
			return;
		
		for (Item i : radicals.list)
			put (i);
	}
	
	public void newKanji (ItemLibrary<Kanji> kanji)
	{
		if (!types.contains (Item.Type.KANJI) ||
			availableTypes.contains (Item.Type.KANJI))
			return;

		for (Item i : kanji.list)
			put (i);
	}

	public void newVocab (ItemLibrary<Vocabulary> vocabs)
	{
		if (!types.contains (Item.Type.VOCABULARY) ||
			availableTypes.contains (Item.Type.VOCABULARY))
			return;

		for (Item i : vocabs.list)
			put (i);			
	}

	private void put (Item i)
	{
		if (i.stats != null)	// stats is null for locked items
			bars.get (i.level - 1).samples.get (imap.get (i.stats.srs)).value++;
	}
	
	public boolean scrolling ()
	{
		return chart != null && chart.scrolling ();
	}
}
