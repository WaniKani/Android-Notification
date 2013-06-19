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

public class StatsFragment extends Fragment implements Tab {

	View parent;
		
	MainActivity main;
	
	private static final int ALL_THE_KANJI = 1700;
	
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
	
	public void refreshComplete (DashboardData dd)
	{
		PieChart pc;

		if (dd == null || !isResumed ())
			return;
		
		if (dd.od != null && dd.od.srs != null) {
			pc = (PieChart) parent.findViewById (R.id.pc_srs);			
			pc.setData (getSRSDataSets (dd.od.srs));

			pc = (PieChart) parent.findViewById (R.id.pc_kanji);			
			pc.setData (getKanjiProgDataSets (dd.od.srs));
			
			pc = (PieChart) parent.findViewById (R.id.pc_vocab);			
			pc.setData (getVocabProgDataSets (dd.od.srs));
		}
	}
	
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
	
	public void spin (boolean enable)
	{
		/* empty */
	}
	
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
	
	 public boolean scrollLock ()
	 {
		return false; 
	 }

}
