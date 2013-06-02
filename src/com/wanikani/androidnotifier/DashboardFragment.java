package com.wanikani.androidnotifier;

import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
 * The main activity, which is displayed on application launch.  We
 * display a simple GUI that show some stats (need some work on here)
 * and (most important) allow the user change his/her preferences.
 * <p> 
 * The stats are refreshed automatically every 
 * {@link #T_INT_AUTOREFRESH} milliseconds.  This value should be kept quite
 * large to avoid needless traffic and power consumption; if the user
 * really needs to update the stats, we provide also a "refresh" menu.
 */
public class DashboardFragment extends Fragment implements Tab { 

	/**
	 * A listener that intercepts review button clicks.
	 * It starts the {@link WebReviewActivity}.
	 */
	private class ReviewClickListener implements View.OnClickListener {
		
		@Override
		public void onClick (View v)
		{
			main.review ();
		}	
	}

	MainActivity main;
	
	View parent;
	
	DashboardData dd;
	
	public void setMainActivity (MainActivity main)
	{
		this.main = main;
	}
	
	@Override
	public void onCreate (Bundle bundle)
	{
		super.onCreate (bundle);
	}

	/**
	 * Registers all the click listeners.
	 * Currently they are:
	 * <ul>
	 * 	<li>The listener that handles "Available now" web link
	 *  <li>The listener of the "Review button"
	 * </ul>
	 */
	private void registerListeners ()
	{
		View view;
		
		view = parent.findViewById (R.id.btn_review);
		view.setOnClickListener (new ReviewClickListener ());
	}
	
	@Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
            				  Bundle savedInstanceState) 
    {
    	parent = inflater.inflate(R.layout.dashboard, container, false);
    	registerListeners ();
    	
    	if (dd != null)
    		refreshComplete (dd);
    	
    	return parent;
    }
		
	protected void setText (int id, String text)
	{
		TextView view;
		
		view = (TextView) parent.findViewById (id);
		view.setText (text);
	}
	
	protected void setText (int id, int sid)
	{
		TextView view;
		
		view = (TextView) parent.findViewById (id);
		view.setText (sid);
	}
	
	protected void setVisibility (int id, int flag)
	{
		View view;
		
		view = parent.findViewById (id);
		view.setVisibility (flag);
	}
	
	/**
	 * Called by {@link RefreshTask} when asynchronous data 
	 * retrieval is completed.
	 * @param dd the retrieved data
	 */
	public void refreshComplete (DashboardData dd)
	{
		ProgressBar pb;
		ImageView iw;
		TextView tw;
		String s;

		this.dd = dd;
		
		if (parent == null || getActivity () == null)
			return;
		
		iw = (ImageView) parent.findViewById (R.id.iv_gravatar);
		if (dd.gravatar != null)
			iw.setImageBitmap (mask (dd.gravatar));

		setText (R.id.tv_username, dd.username);
		setText (R.id.tv_level, String.format (getString (R.string.fmt_level), dd.level));
		setText (R.id.tv_title, String.format (getString (R.string.fmt_title), dd.title));
		setText (R.id.reviews_val, Integer.toString (dd.reviewsAvailable));
		setVisibility (R.id.tr_r_now, dd.reviewsAvailable > 0 ? View.VISIBLE : View.GONE);
		setText (R.id.tv_next_review, R.string.tag_next_review);
		
		if (dd.reviewsAvailable > 0) {
			setVisibility (R.id.tv_next_review, View.GONE);
			setVisibility (R.id.tv_next_review_val, View.GONE);
			setVisibility (R.id.btn_review, View.VISIBLE);
		} else {
			setText (R.id.tv_next_review_val, niceInterval (dd.nextReviewDate));
			setVisibility (R.id.tv_next_review, View.VISIBLE);
			setVisibility (R.id.tv_next_review_val, View.VISIBLE);
			setVisibility (R.id.btn_review, View.GONE);
		}
		
		tw = (TextView) parent.findViewById (R.id.lessons_available);
		if (dd.lessonsAvailable > 1) {
			s = String.format (getString (R.string.fmt_lessons), dd.lessonsAvailable);
			tw.setText (Html.fromHtml (s));
			tw.setMovementMethod (LinkMovementMethod.getInstance ());
		} else if (dd.lessonsAvailable == 1) {
			tw.setText (Html.fromHtml (getString (R.string.fmt_one_lesson)));
			tw.setMovementMethod (LinkMovementMethod.getInstance ());
		}		
		tw.setVisibility (dd.lessonsAvailable > 0 ? View.VISIBLE : View.GONE);
		
		setText (R.id.next_hour_val, Integer.toString (dd.reviewsAvailableNextHour));
		setText (R.id.next_day_val, Integer.toString (dd.reviewsAvailableNextDay));
		
		/* Now the optional stuff */
		switch (dd.od.lpStatus) {
		case RETRIEVING:
			if (dd.od.lpStatus != DashboardData.OptionalDataStatus.RETRIEVING)
				setVisibility (R.id.pb_w_section, View.VISIBLE);
			setVisibility (R.id.progress_w_section, View.VISIBLE);
			break;
			
		case RETRIEVED:
			pb = (ProgressBar) parent.findViewById (R.id.pb_radicals);
			pb.setProgress (100 * dd.od.lp.radicalsProgress / dd.od.lp.radicalsTotal);

			pb = (ProgressBar) parent.findViewById (R.id.pb_kanji);
			pb.setProgress (100 * dd.od.lp.kanjiProgress / dd.od.lp.kanjiTotal);

			setVisibility (R.id.progress_w_section,View.GONE);
			setVisibility (R.id.progress_section, View.VISIBLE);
			break;
			
		case FAILED:
			/* Just hide the spinner. 
			 * If we already have some data, it is displayed anyway */
			setVisibility (R.id.progress_w_section, View.GONE);			
		}
	}
	

	/**
	 * Pretty-prints a date. This implementation tries to mimic the WaniKani website,
	 * by returning an approximate interval. 
	 * @param date the date to format
	 * @return a string to be displayed
	 */
	private String niceInterval (Date date)
	{
		float days, hours, minutes;
		boolean forward;
		Resources res;
		long delta;
		int x;
			
		res = getResources ();
		if (date == null)
			return res.getString (R.string.fmt_no_reviews);
		
		delta = date.getTime () - new Date ().getTime ();
		forward = delta > 0;
		if (!forward)
			return res.getString (R.string.tag_available_now);
		
		minutes = delta / (60 * 1000);
		hours = minutes / 60;
		days = hours / 24;
		
		x = Math.round (days);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_days), x);
		else if (x == 1)
			return String.format (res.getString (R.string.fmt_one_day));

		x = Math.round (hours);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_hours), x);
		else if (x == 1 && hours >= 1)
			return String.format (res.getString (R.string.fmt_one_hour));

		x = Math.round (minutes);
		if (x > 1)
			return String.format (res.getString (R.string.fmt_X_minutes), x);
		else if (x == 1)
			return String.format (res.getString (R.string.fmt_one_minute));
		
		return String.format (res.getString (R.string.fmt_seconds));	
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
		
		if (parent != null) {
			pb = (ProgressBar) parent.findViewById (R.id.pb_status);
			pb.setVisibility (enable ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
		}
	}
	
	public int getName ()
	{
		return R.string.tag_dashboard;
	}
}
	
