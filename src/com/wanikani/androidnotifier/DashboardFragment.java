package com.wanikani.androidnotifier;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wanikani.androidnotifier.graph.ProgressChart;
import com.wanikani.androidnotifier.graph.ProgressPlot;
import com.wanikani.androidnotifier.graph.ProgressChart.SubPlot;
import com.wanikani.androidnotifier.graph.ProgressPlot.DataSet;
import com.wanikani.wklib.Item;

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
 * The home fragment, which is displayed on application launch.  We
 * display a simple GUI that show some stats, and allows to perform reviews
 * or study lessons, if some is pending.
 * <p> 
 * The stats are refreshed automatically, after a configurable timeout.  
 * This value should be kept quite large to avoid needless traffic and 
 * power consumption; if the user really needs to update the stats, we 
 * provide also a "refresh" menu.
 */
public class DashboardFragment extends Fragment implements Tab { 

	/**
	 * A listener that intercepts review button clicks.
	 * It simply informs the main activity, which will choose what
	 * to do next
	 */
	private class ReviewClickListener implements View.OnClickListener {
		
		/**
		 * Called when the button is clicked.
		 * @param v the button 
		 */
		@Override
		public void onClick (View v)
		{
			main.review ();
		}	
	}

	/**
	 * A listener that intercepts lesson button clicks.
	 * It simply informs the main activity, which will choose what
	 * to do next
	 */
	private class LessonsClickListener implements View.OnClickListener {
		
		/**
		 * Called when the button is clicked.
		 * @param v the button 
		 */
		@Override
		public void onClick (View v)
		{
			main.lessons ();
		}	
	}
	
	/**
	 * Listener to clicks on the forum link pages.
	 */
	private class ChatClickListener implements View.OnClickListener {
		
		/**
		 * Called when the button is clicked.
		 * @param v the button 
		 */
		@Override
		public void onClick (View v)
		{
			main.chat ();
		}	
	}

	/**
	 * Listener to clicks on the review summary link pages.
	 */
	private class ReviewSummaryClickListener implements View.OnClickListener {
		
		/**
		 * Called when the button is clicked.
		 * @param v the button 
		 */
		@Override
		public void onClick (View v)
		{
			main.reviewSummary ();
		}	
	}
	
	/**
	 * Listener for clicks on total items link. Causes the pager to 
	 * switch to the item tab, and sets the item type filter.
	 */
	private class TotalClickListener implements View.OnClickListener {

		/// The item type (typically radicals or kanji)
		private Item.Type type;
		
		/**
		 * Constructor
		 * @param type the item type (typically radicals or kanji) 
		 */
		public TotalClickListener (Item.Type type)
		{
			this.type = type;
		}
		
		@Override
		public void onClick (View v)
		{
			main.showTotal (type);
		}	
	}

	/**
	 * Listener for clicks on remaining items link. Causes the pager to 
	 * switch to the item tab, and sets the apprentice items filter.
	 */
	private class RemainingClickListener implements View.OnClickListener {

		/// The item type (typically radicals or kanji)
		private Item.Type type;
		
		/**
		 * Constructor
		 * @param type the item type (typically radicals or kanji) 
		 */
		public RemainingClickListener (Item.Type type)
		{
			this.type = type;
		}
		
		@Override
		public void onClick (View v)
		{
			main.showRemaining (type);
		}	
	}
	
	/**
	 * Listener for clicks on critical items link. Causes the pager to 
	 * switch to the item tab, and sets the critical items filter.
	 */
	private class CriticalClickListener implements View.OnClickListener {

		@Override
		public void onClick (View v)
		{
			main.showCritical ();
		}	
	}
	
	/// The main activity
	MainActivity main;
	
	/// The root view of the fragment
	View parent;
	
	/// True if the spinner is (also virtually) visible
	boolean spinning;
	
	/// Number of reviews/lessons before switching to 42+ mode
	public static final int LESSONS_42P = 42;
	
	/// The Radicals progress subplot
	SubPlot radicalsProgress;
	
	/// The Kanji progress subplot
	SubPlot kanjiProgress;	
	
	@Override
	public void onAttach (Activity main)
	{
		super.onAttach (main);
		
		this.main = (MainActivity) main;
		
		this.main.register (this);
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
	 * Registers all the click listeners.
	 * Currently they are:
	 * <ul>
	 * 	<li>The listener that handles "Available now" web link
	 *  <li>The listener of the "Review button"
	 *  <li>Kanji and radicals left "hyperlink"
	 * </ul>
	 */
	private void registerListeners ()
	{		
		View view;
		
		view = parent.findViewById (R.id.btn_review);
		view.setOnClickListener (new ReviewClickListener ());

		view = parent.findViewById (R.id.btn_lessons_available);
		view.setOnClickListener (new LessonsClickListener ());
		
		view = parent.findViewById (R.id.btn_view_critical);
		view.setOnClickListener (new CriticalClickListener ());
		
		view = parent.findViewById (R.id.radicals_progression);
		view.setClickable (true);
		view.setOnClickListener (new TotalClickListener (Item.Type.RADICAL));

		view = parent.findViewById (R.id.kanji_progression);
		view.setClickable (true);
		view.setOnClickListener (new TotalClickListener (Item.Type.KANJI));

		view = parent.findViewById (R.id.btn_result);
		view.setOnClickListener (new ReviewSummaryClickListener ());
		
		view = parent.findViewById (R.id.btn_chat);
		view.setOnClickListener (new ChatClickListener ());		
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
		ProgressChart chart;
		
		super.onCreateView (inflater, container, savedInstanceState);

		parent = inflater.inflate(R.layout.dashboard, container, false);
		registerListeners ();
		
		chart = (ProgressChart) parent.findViewById (R.id.pb_radicals);
		radicalsProgress = chart.addData (parent.findViewById (R.id.rad_dropdown));
        
		chart = (ProgressChart) parent.findViewById (R.id.pb_kanji);
		kanjiProgress = chart.addData (parent.findViewById (R.id.kanji_dropdown));

		return parent;
    }
	
	/**
	 * Called when the view is resumed. We refresh the GUI, and start
	 * the spinner, if it should be visible.
	 */
	@Override
	public void onResume ()
	{
		super.onResume ();
		
		refreshComplete (main.getDashboardData ());
		spin (spinning);
	}
		
	/**
	 * Convenience method that changes the contents of a text view.
	 * @param id the text view id
	 * @param text the text to be displayed
	 */
	protected void setText (int id, String text)
	{
		TextView view;
		
		view = (TextView) parent.findViewById (id);
		view.setText (text);
	}
	
	/**
	 * Convenience method that changes the contents of a text view.
	 * @param id the text view id
	 * @param sid the string ID to be retrieved from the resources
	 */
	protected void setText (int id, int sid)
	{
		TextView view;
		
		view = (TextView) parent.findViewById (id);
		view.setText (sid);
	}
	
	/**
	 * Convenience method that changes the visibility of a view.
	 * @param id the view id
	 * @param flag any of {@link View#VISIBLE}, 
	 * {@link View#INVISIBLE} or {@link View#GONE}
	 */
	protected void setVisibility (int id, int flag)
	{
		View view;
		
		view = parent.findViewById (id);
		view.setVisibility (flag);
	}
	
	/**
	 * Update level progression info. The information to be displayed comprises:
	 * <ul>
	 * 	<li>Completion percentage
	 *  <li>Number of items to complete level
	 *  <li>Number of apprentice items
	 * </ul>
	 * This method is called twice (one for radicals progression, one for 
	 * kanji progression)
	 * @param pbid progess bar ID
	 * @param rid remaining items text view ID
	 * @param tid the progressions tag
	 * @param tsid the progressions string id
	 * @param prog number of non-apprentice items 
	 * @param total total number of items
	 */
	protected void setProgress (int pbid, int rid, int tid, int tsid, int prog, int total)
	{
		TextView tw;
		ProgressBar pb;
		int percent, rem, grace;
		String s;

		percent = prog * 100 / total;
		
		pb = (ProgressBar) parent.findViewById (pbid);
		pb.setSecondaryProgress (percent);
		
		tw = (TextView) parent.findViewById (rid);
		rem = total - prog;
		grace = total / 10;
		if (percent < 90)
			s = getString (R.string.fmt_to_go, rem - grace, rem);
		else if (prog < total)
			s = getString (R.string.fmt_remaining, rem);
		else
			s = null;
			
		if (s != null) {
			s = String.format ("<font color=\"blue\"><u>%s</u></font>", s);
			tw.setText (Html.fromHtml (s));
			tw.setVisibility (View.VISIBLE);
		} else
			tw.setVisibility (View.GONE);			
		
		tw = (TextView) parent.findViewById (tid);
		s = String.format ("<font color=\"blue\"><u>%s</u></font>", getString (tsid, total));
		tw.setText (Html.fromHtml (s));
	}
	
	protected void setProgressNew (SubPlot splot, int pid, int tsid, int guru, int unlocked, int total)
	{
		List<DataSet> dsets;
		TextView tw;
		int threshold;
		Resources res;
		String s;

		res = getResources ();
		
		tw = (TextView) parent.findViewById (pid);
		s = String.format ("<font color=\"blue\"><u>%s</u></font>", getString (tsid, total));
		tw.setText (Html.fromHtml (s));

		dsets = new Vector<DataSet> ();
		
		dsets.add (new DataSet (getString (R.string.tag_guru), res.getColor (R.color.guru), guru));				
		
		threshold = total - (total / 10);
		
		if (threshold > unlocked) {
			/* | Guru | Apprentice | Threshold | Grace | */ 
			dsets.add (new DataSet (getString (R.string.tag_apprentice), res.getColor (R.color.apprentice), 
									unlocked, unlocked - guru));
			dsets.add (new DataSet (null, res.getColor (R.color.remaining), threshold));
			dsets.add (new DataSet (null, res.getColor (R.color.grace), total));			
		} else {
			/* | Guru | Apprentice  | Apprentice above Threshold | Grace | */ 
			dsets.add (new DataSet (getString (R.string.tag_apprentice), res.getColor (R.color.apprentice), 
					                threshold, unlocked - guru));
			dsets.add (new DataSet (null, res.getColor (R.color.apprentice_above_threshold), 
								    unlocked - guru));
			dsets.add (new DataSet (null, res.getColor (R.color.grace), total));			
		}				
		dsets.add (new DataSet (getString (R.string.tag_remaining), threshold - guru));
		
		ProgressPlot.DataSet.setDifferential (dsets);
		
		splot.setData (dsets);
	}

	/**
	 * Called by @link MainActivity when asynchronous data
	 * retrieval is completed. If we already have a view on which
	 * to display it, we update the GUI. Otherwise we cache the info
	 * and display it when the fragment is resumed.
	 * @param dd the retrieved data
	 */
	public void refreshComplete (DashboardData dd)
	{
		Context ctxt;
		ImageView iw;
		String s;
		
		ctxt = getActivity ();

		if (!isResumed () || dd == null || ctxt == null)
			return;
		
		iw = (ImageView) parent.findViewById (R.id.iv_gravatar);
		if (dd.gravatar != null)
			iw.setImageBitmap (mask (dd.gravatar));

		setText (R.id.tv_username, dd.username);
		setText (R.id.tv_level, getString (R.string.fmt_level, dd.level));
		setText (R.id.tv_title, getString (R.string.fmt_title, dd.title));
		
		if (SettingsActivity.get42plus (ctxt) && dd.reviewsAvailable > LESSONS_42P)
			setText (R.id.reviews_val, LESSONS_42P + "+");
		else
			setText (R.id.reviews_val, Integer.toString (dd.reviewsAvailable));
		setVisibility (R.id.tr_r_now, dd.reviewsAvailable > 0 ? View.VISIBLE : View.GONE);
		setText (R.id.tv_next_review, R.string.tag_next_review);
		
		if (dd.reviewsAvailable > 0) {
			setVisibility (R.id.tv_next_review, View.INVISIBLE);
			setVisibility (R.id.tv_next_review_val, View.GONE);
			setVisibility (R.id.btn_result, View.GONE);
			setVisibility (R.id.btn_review, View.VISIBLE);
		} else {
			setText (R.id.tv_next_review_val, niceInterval (dd.nextReviewDate, dd.vacation));
			setVisibility (R.id.tv_next_review, View.VISIBLE);
			setVisibility (R.id.tv_next_review_val, View.VISIBLE);
			setVisibility (R.id.btn_result, View.VISIBLE);
			setVisibility (R.id.btn_review, View.GONE);
		}
		
		if (SettingsActivity.get42plus (ctxt) && dd.lessonsAvailable > LESSONS_42P)
			setText (R.id.lessons_available, 
					 getString (R.string.fmt_lessons_42p, LESSONS_42P));
		if (dd.lessonsAvailable > 1) {
			s = getString (R.string.fmt_lessons, dd.lessonsAvailable);
			setText (R.id.lessons_available, s);
		} else if (dd.lessonsAvailable == 1)
			setText (R.id.lessons_available, getString (R.string.fmt_one_lesson));
			
		/* If no more lessons, hide the message */
		setVisibility (R.id.lay_lessons_available, dd.lessonsAvailable > 0 ? View.VISIBLE : View.GONE);
		
		setText (R.id.next_hour_val, Integer.toString (dd.reviewsAvailableNextHour));
		setText (R.id.next_day_val, Integer.toString (dd.reviewsAvailableNextDay));
		
		/* Now the optional stuff */
		switch (dd.od.lpStatus) {
		case RETRIEVING:
			if (dd.od.lpStatus != DashboardData.OptionalDataStatus.RETRIEVING)
				setVisibility (R.id.pb_w_section, View.VISIBLE);
			break;
			
		case RETRIEVED:
			setVisibility (R.id.pb_w_section,View.GONE);
			setVisibility (R.id.lay_progress, View.VISIBLE);
			
			setProgressNew (radicalsProgress, R.id.radicals_progression, R.string.fmt_radicals_progression,  
					  		dd.od.elp.radicalsProgress, dd.od.elp.radicalsUnlocked, dd.od.elp.radicalsTotal);
			
			setProgressNew (kanjiProgress, R.id.kanji_progression, R.string.fmt_kanji_progression,
							dd.od.elp.kanjiProgress, dd.od.elp.kanjiUnlocked, dd.od.elp.kanjiTotal);

			setVisibility (R.id.progress_section, View.VISIBLE);			
			break;
			
		case FAILED:
			/* Just hide the spinner. 
			 * If we already have some data, it is displayed anyway, otherwise hide it */
			if (dd.od.elp == null)
				setVisibility (R.id.lay_progress, View.GONE);
			setVisibility (R.id.pb_w_section, View.GONE);			
		}
		
		switch (dd.od.ciStatus) {
		case RETRIEVING:
			break;
			
		case RETRIEVED:
			if (dd.od.criticalItems > 1) {
				s = getString (R.string.fmt_critical_items, dd.od.criticalItems);
				setText (R.id.critical_items, s);
			} else if (dd.od.criticalItems == 1)
				setText (R.id.critical_items, getString (R.string.fmt_one_critical_item));
			
			setVisibility (R.id.lay_critical_items, dd.od.criticalItems > 0 ? View.VISIBLE : View.GONE);
			
			break;
			
		case FAILED:
			break;
		}

		/* Show the alerts panel only if there are still alerts to be shown */  
		showAlertsLayout (dd.lessonsAvailable > 0 ||
						  (dd.od != null && dd.od.criticalItems > 0));
	}

	/**
	 * Shows or hides the alerts layout, depending on the state of the visibility
	 * of its children
	 * 	@param show if it shall be shown  
	 */
	protected void showAlertsLayout (boolean show)
	{
		ViewGroup lay;

		lay = (ViewGroup) parent.findViewById (R.id.lay_alerts);
		lay.setVisibility (show ? View.VISIBLE : View.GONE);
	}	

	/**
	 * Pretty-prints a date. This implementation tries to mimic the WaniKani website,
	 * by returning an approximate interval. 
	 * @param date the date to format
	 * @return a string to be displayed
	 */
	private String niceInterval (Date date, boolean vacation)
	{
		float days, hours, minutes;
		boolean forward;
		Resources res;
		long delta;
		int x;
			
		res = getResources ();
		if (vacation)
			return res.getString (R.string.fmt_vacation);
		if (date == null)
			return res.getString (R.string.fmt_no_reviews);
		
		delta = date.getTime () - new Date ().getTime ();
		forward = delta > 0;
		/* forward may be < 0 even if lessons are not available yet
		 * (may due to clock disalignment)
		 */
		if (!forward)
			delta = 1;
		
		minutes = delta / (60 * 1000);
		hours = minutes / 60;
		days = hours / 24;
		
		x = Math.round (days);
		if (x > 1)
			return res.getString (R.string.fmt_X_days, x);
		else if (x == 1)
			return res.getString (R.string.fmt_one_day);

		x = (int) Math.floor (hours);
		if (((long) minutes) % 60 < 2) {
			if (x > 1)
				return res.getString (R.string.fmt_X_hours, x);
			else if (x == 1 && hours >= 1)
				return res.getString (R.string.fmt_one_hour);
		} else {
			if (x > 1)
				return res.getString (R.string.fmt_X_hours_mins, x, ((long) minutes) % 60);
			else if (x == 1 && hours >= 1)
				return res.getString (R.string.fmt_one_hour_mins, ((long) minutes) % 60);			
		}

		x = Math.round (minutes);
		if (x > 1)
			return res.getString (R.string.fmt_X_minutes, x);
		else if (x == 1)
			return res.getString (R.string.fmt_one_minute);
		
		return res.getString (R.string.fmt_seconds);	
	}

	/**
	 * Apply a circular mask on the given bitmap. This method is
	 * used to display the avatar.
	 * @param bmp an input bitmap
	 * @param result the output (masked) bitmap
	 */
	private Bitmap mask (Bitmap bmp)
	{
		Bitmap result, mask;
		Drawable dmask;
		Canvas canvas;
		Paint paint;

		result = Bitmap.createBitmap (bmp.getWidth (), bmp.getHeight (),
									  Bitmap.Config.ARGB_8888);
		canvas = new Canvas (result);
		
		dmask = getResources ().getDrawable (R.drawable.gravatar_mask);
		mask = ((BitmapDrawable) dmask).getBitmap ();
		
		paint = new Paint (Paint.ANTI_ALIAS_FLAG);
		paint.setXfermode (new PorterDuffXfermode (PorterDuff.Mode.DST_IN));
		canvas.drawBitmap (bmp, 0, 0, null);
		canvas.drawBitmap (mask, 0, 0, paint);
		
		return result;
	}

	/**
	 * Show or hide the spinner.
	 * @param enable true if should be shown
	 */
	public void spin (boolean enable)
	{
		ProgressBar pb;

		spinning = enable;
		if (parent != null) {
			pb = (ProgressBar) parent.findViewById (R.id.pb_status);
			pb.setVisibility (enable ? ProgressBar.VISIBLE : ProgressBar.GONE);
		}
	}
	
	/**
	 * Returns the tab name ID.
	 * @param the <code>tag_dashboard</code> ID
	 */
	public int getName ()
	{
		return R.string.tag_dashboard;
	}
	
	/**
	 * Does nothing. Needed just to implement the @link Tab interface, but
	 * we don't keep any cache.
	 */
	public void flush (Tab.RefreshType rtype, boolean fg)
	{
		/* empty */
	}
	
	/**
	 * This item has no scroll view.
	 * @return false
	 */
	public boolean scrollLock ()
	{
		 return false;
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
		return c == Contents.DASHBOARD;
	}
	
	@Override
	public void flushDatabase ()
	{
		/* empty */
	}
}
