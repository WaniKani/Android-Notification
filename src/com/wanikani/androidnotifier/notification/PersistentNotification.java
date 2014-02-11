package com.wanikani.androidnotifier.notification;

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
			public int getIcon ()
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
				String nice;

				if (sd.dd == null)
					return ctxt.getString (R.string.app_name);
				
				nice = DashboardFragment.niceInterval (ctxt.getResources (), sd.dd.nextReviewDate, sd.dd.vacation);
				/* This is somehow ugly. No. It's ugly. */
				if (sd.dd.nextReviewDate == null || sd.dd.vacation)
					return nice;
				else
					return ctxt.getString (R.string.next_review, nice);
			}
			
			@Override
			public String getAccessoryText (Context ctxt, StateData sd)
			{
				return reviewsNextHour (ctxt, sd);				
			}
		},
		
		LESSONS {
			
			@Override
			public int getIcon ()
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
				return lessonsAvailable (ctxt, sd);
			}

			@Override
			public String getAccessoryText (Context ctxt, StateData sd)
			{
				return reviewsNextHour (ctxt, sd);				
			}
		},
		
		REVIEWS {
			
			@Override
			public int getIcon ()
			{
				return R.drawable.not_icon;
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
				String lessons, reviews;
				
				lessons = lessonsAvailable (ctxt, sd);
				reviews = reviewsNextHour (ctxt, sd);
				if (lessons.length () != 0 && reviews.length () != 0)
					lessons = lessons + "; ";
				
				return lessons + reviews;
			}			
		};
		
		public abstract int getIcon ();
		
		public abstract String getIntent ();
		
		public abstract String getText (Context ctxt, StateData sd);
		
		public abstract String getAccessoryText (Context ctxt, StateData sd);

		private static String reviewsAvailable (Context ctxt, StateData sd)
		{
			if (!sd.hasReviews)
				return "";
			else if (SettingsActivity.get42plus (ctxt) && sd.reviews > DashboardFragment.LESSONS_42P)
				return ctxt.getString (R.string.new_reviews_42plus, DashboardFragment.LESSONS_42P);
			else
				return ctxt.getString (sd.reviews == 1 ? 
									   R.string.new_review : R.string.new_reviews, sd.reviews);
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
		builder.setSmallIcon (state.getIcon ());
				
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
