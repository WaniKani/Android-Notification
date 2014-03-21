package com.wanikani.androidnotifier.notification;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.wanikani.androidnotifier.DashboardFragment;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.SettingsActivity;
import com.wanikani.androidnotifier.notification.NotificationService.StateData;

class PersistentNotification implements NotificationInterface {

	enum State {
		
		NOTHING {
			
			@Override
			public int getIcon (StateData sd)
			{
				return R.drawable.not_dash;
			}
			
			@Override
			public String getIntent ()
			{
				return NotificationService.ACTION_NULL_TAP;
			}	
			
			@Override
			public String getText (Context ctxt, StateData sd)
			{
				return nextReviews (ctxt, sd);
			}
			
			@Override
			public String getAccessoryText (Context ctxt, StateData sd)
			{
				return reviewsNextHour (ctxt, sd);				
			}
		},
		
		LESSONS {
			
			@Override
			public int getIcon (StateData sd)
			{
				return R.drawable.not_lessons;
			}
			
			@Override
			public String getIntent ()
			{
				return NotificationService.ACTION_LESSONS_TAP;
			}
			
			@Override
			public String getText (Context ctxt, StateData sd)
			{
				return nextReviews (ctxt, sd);
			}

			@Override
			public String getAccessoryText (Context ctxt, StateData sd)
			{
				return lessonsAvailable (ctxt, sd);
			}
		},
		
		REVIEWS {
			
			@Override
			public int getIcon (StateData sd)
			{
				return sd.thisLevel ? R.drawable.not_g_icon : R.drawable.not_icon;
			}
			
			@Override
			public String getIntent ()
			{
				return NotificationService.ACTION_TAP;
			}
			
			@Override
			public String getText (Context ctxt, StateData sd)
			{
				return reviewsAvailable (ctxt, sd);
			}
			
			@Override
			public String getAccessoryText (Context ctxt, StateData sd)
			{
				if (sd.hasLessons)
					return lessonsAvailable (ctxt, sd);
				else
					return reviewsNextHour (ctxt, sd);
			}			
		};
		
		private static DateFormat hdf = new SimpleDateFormat ("HH:mm");
		
		private static DateFormat ddf = new SimpleDateFormat ("EEE, HH:mm");
		
		public abstract int getIcon (StateData sd);
		
		public abstract String getIntent ();
		
		public abstract String getText (Context ctxt, StateData sd);
		
		public abstract String getAccessoryText (Context ctxt, StateData sd);

		private static String nextReviews (Context ctxt, StateData sd)
		{			
			Calendar rcal, ncal;
			String nice;
			int ddelta;

			if (sd.dd == null)
				return ctxt.getString (R.string.app_name);
			
			if (sd.dd.reviewsAvailable > 0) /* This happens when reviewing or the threshold is > 0. Let the user know, anyhow */ 
				return reviewsAvailable (ctxt, sd.dd.reviewsAvailable);
						
			if (sd.dd.nextReviewDate != null && !sd.dd.vacation) {
				rcal = Calendar.getInstance ();
				ncal = Calendar.getInstance ();
				rcal.setTime (sd.dd.nextReviewDate);
				ddelta = ncal.get (Calendar.DATE) - rcal.get (Calendar.DATE);
				if (ddelta == 0)
					nice = ctxt.getString (R.string.fmt_not_next_review_b, hdf.format (sd.dd.nextReviewDate));
				else
					nice = ctxt.getString (R.string.fmt_not_next_review_d, ddf.format (sd.dd.nextReviewDate));
			} else
				nice = DashboardFragment.niceInterval (ctxt.getResources (), sd.dd.nextReviewDate, sd.dd.vacation);
			
			return nice;
		}
		
		private static String reviewsAvailable (Context ctxt, StateData sd)
		{
			if (!sd.hasReviews)
				return "";
			else
				return reviewsAvailable (ctxt, sd.reviews);
		}
		
		private static String reviewsAvailable (Context ctxt, int count)
		{
			if (SettingsActivity.get42plus (ctxt) && count > DashboardFragment.LESSONS_42P)
				return ctxt.getString (R.string.new_reviews_42plus, DashboardFragment.LESSONS_42P);
			else
				return ctxt.getString (count == 1 ? 
									   R.string.new_review : R.string.new_reviews, count);
		}

		private static String lessonsAvailable (Context ctxt, StateData sd)
		{
			if (!sd.hasLessons)
				return "";
			else if (SettingsActivity.get42plus (ctxt) && sd.lessons > DashboardFragment.LESSONS_42P)
				return ctxt.getString (R.string.new_lessons_42plus, DashboardFragment.LESSONS_42P);
			else
				return ctxt.getString (sd.lessons == 1 ? 
							      	   R.string.new_lesson : R.string.new_lessons, sd.lessons);				
		}
		
		private static String reviewsNextHour (Context ctxt, StateData sd)
		{
			if (sd.dd == null)
				return "";
			switch (sd.dd.reviewsAvailableNextHour) {
			case 0:
				return ctxt.getString (R.string.next_hour_no_review);
				
			case 1:
				return ctxt.getString (R.string.next_hour_one_review);
				
			default:				
				return ctxt.getString (R.string.next_hour_reviews, sd.dd.reviewsAvailableNextHour);
			}
		}
		
		public static State getState (StateData sd)
		{
			return	sd.hasReviews ? State.REVIEWS :
					sd.hasLessons ? State.LESSONS : State.NOTHING;			
		}
	}
		
	/** The ID associated to the notification icon */
	private static final int NOTIFICATION_ID = 1;
	
	Context ctxt;
	
	public PersistentNotification (Context ctxt)
	{
		this.ctxt = ctxt;
	}
	
	@Override
	public void enable (StateData sdata)
	{
		update (sdata);
	}
	
	@Override
	public void disable ()
	{
		hide ();
	}

	@Override
	public void update (StateData sd, ChangeType ctype)
	{
		update (sd);	/* I want to be sure ctype is not accidentally used */
	}
	
	private void update (StateData sd)
	{
		NotificationManager nmanager;
		NotificationCompat.Builder builder;
		Notification not;
		PendingIntent pint;
		Intent intent;
		State state;
		
		state = State.getState (sd);
		
		nmanager = (NotificationManager) 
				ctxt.getSystemService (Context.NOTIFICATION_SERVICE);

		if (state.getIntent () != null ) {
			intent = new Intent (ctxt, NotificationService.class);
			intent.setAction (state.getIntent ());		
			pint = PendingIntent.getService (ctxt, 0, intent, 0);
		} else
			pint = null;

		builder = new NotificationCompat.Builder (ctxt);
		builder.setSmallIcon (state.getIcon (sd));
				
		builder.setContentTitle (state.getText (ctxt, sd));
								 
		builder.setContentText (state.getAccessoryText (ctxt, sd));
		if (pint != null)
			builder.setContentIntent (pint);
				
		not = builder.build ();
		not.flags |= Notification.FLAG_NO_CLEAR;
		
		nmanager.notify (NOTIFICATION_ID, not);
	}
	
	protected void hide ()
	{
		NotificationManager nmanager;
		
		nmanager = (NotificationManager) 
			ctxt.getSystemService (Context.NOTIFICATION_SERVICE);
		
		nmanager.cancel (NOTIFICATION_ID);		
	}	
}
