package com.wanikani.androidnotifier;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wanikani.androidnotifier.graph.PieChart;
import com.wanikani.androidnotifier.graph.PiePlot.DataSet;
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

	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;
	
	/// Overall number of kanji
	private static final int ALL_THE_KANJI = 1700;
	
	/// Overall number of vocab items
	private static final int ALL_THE_VOCAB = 5000;
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;
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
	 * @param container the parent view
	 * @param savedInstance an (unused) bundle
	 */
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
		super.onCreateView (inflater, container, savedInstanceState);

		parent = inflater.inflate(R.layout.stats, container, false);
        
    	return parent;
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
		/* empty */
	}
	
	@Override
	public void flush ()
	{
		PieChart pc;
		
		if (parent != null) {
		
			pc = (PieChart) parent.findViewById (R.id.pc_srs);
			pc.spin (true);

			pc = (PieChart) parent.findViewById (R.id.pc_kanji);			
			pc.spin (true);
		
			pc = (PieChart) parent.findViewById (R.id.pc_vocab);			
			pc.spin (true);
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
		return false; 
	}

}
