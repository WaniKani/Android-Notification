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
import com.wanikani.androidnotifier.graph.IconizableChart;
import com.wanikani.androidnotifier.graph.ProgressChart;
import com.wanikani.androidnotifier.graph.ProgressPlot;
import com.wanikani.wklib.Item;
import com.wanikani.wklib.ItemLibrary;
import com.wanikani.wklib.Kanji;
import com.wanikani.wklib.Radical;
import com.wanikani.wklib.SRSLevel;
import com.wanikani.wklib.Vocabulary;

public class KanjiProgressChart implements NetworkEngine.Chart {
	
	ProgressChart chart;
	
	ProgressChart.SubPlot plot;
	
	String library;
	
	EnumMap<SRSLevel, ProgressPlot.DataSet> slds;
	
	ProgressPlot.DataSet rds;
	
	boolean dataAvailable;
	
	IconizableChart.DataSource ds;
	
	int id;
	
	int titleId;
	
	String title;
	
	MeterSpec.T mtype;
	
	public KanjiProgressChart (int id, MeterSpec.T mtype, int titleId, String library)
	{
		this.id = id;
		this.mtype = mtype;
		this.library = library;
		this.titleId = titleId;
		
		slds = new EnumMap<SRSLevel, ProgressPlot.DataSet> (SRSLevel.class);
		slds.put (SRSLevel.APPRENTICE, new ProgressPlot.DataSet (0));
		slds.put (SRSLevel.GURU, new ProgressPlot.DataSet (0));
		slds.put (SRSLevel.MASTER, new ProgressPlot.DataSet (0));
		slds.put (SRSLevel.ENLIGHTEN, new ProgressPlot.DataSet (0));
		slds.put (SRSLevel.BURNED, new ProgressPlot.DataSet (0));			
		rds = new ProgressPlot.DataSet (library.length ());
		
	}

	@Override
	public void bind (NetworkEngine netwe, MainActivity main, View view)
	{
		Resources res;
		
		res = main.getResources ();
		
		slds.get (SRSLevel.APPRENTICE).set (res.getString (R.string.tag_apprentice), 
						  					res.getColor (R.color.apprentice));
		slds.get (SRSLevel.GURU).set (res.getString (R.string.tag_guru), 
						  			  res.getColor (R.color.guru));
		slds.get (SRSLevel.MASTER).set (res.getString (R.string.tag_master), 
						  				res.getColor (R.color.master));
		slds.get (SRSLevel.ENLIGHTEN).set (res.getString (R.string.tag_enlightened), 
						  				   res.getColor (R.color.enlightened));
		slds.get (SRSLevel.BURNED).set (res.getString (R.string.tag_burned), 
						  				res.getColor (R.color.burned));
		
		rds.set (res.getString (R.string.tag_locked),
				 res.getColor (R.color.locked));
		
		title = res.getString (titleId);

		chart = (ProgressChart) view.findViewById (id);
		
		if (ds == null)
			ds = netwe.getDataSource (mtype.get (main), EnumSet.of (Item.Type.KANJI));
		
		chart.setDataSource (ds);

		plot = chart.addData (title);
		
		updatePlot ();		
	}
	
	@Override
	public void unbind ()
	{
		plot = null;		
	}
	
	public void startUpdate (int levels)
	{
		/* empty */
	}
	
	public void update (EnumSet<Item.Type> types)
	{
		dataAvailable |= types.contains (Item.Type.KANJI);
		
		updatePlot ();		
	}

	public void newRadical (ItemLibrary<Radical> radicals)
	{
		/* empty */
	}

	public void newKanji (ItemLibrary<Kanji> kanji)
	{
		if (dataAvailable)
			return;
		
		for (Kanji k : kanji.list) {
			if (k.stats != null && library.contains (k.character)) {
				slds.get (k.stats.srs).value++;
				rds.value--;
			}
		}		
	}

	public void newVocab (ItemLibrary<Vocabulary> vocabs)
	{
		
	}		
		
	private void updatePlot ()
	{
		List<ProgressPlot.DataSet> l;

		if (dataAvailable && plot != null) {
			l = new Vector<ProgressPlot.DataSet> (slds.values ());
			l.add (rds);
		
			if (plot != null)
				plot.setData (l);				
		}
	}
	
	public boolean scrolling ()
	{
		return false;
	}	
}
