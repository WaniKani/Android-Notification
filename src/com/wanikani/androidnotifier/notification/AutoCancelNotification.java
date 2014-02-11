package com.wanikani.androidnotifier.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.wanikani.androidnotifier.DashboardData;
import com.wanikani.androidnotifier.DashboardFragment;
import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.SettingsActivity;
import com.wanikani.androidnotifier.notification.NotificationService.StateData;

class AutoCancelNotification implements NotificationInterface {

	/** The ID associated to the reviews notification icon */
	private static final int NOT_REVIEWS_ID = 1;
	
	/** The ID associated to the lessons notification icon */
	private static final int NOT_LESSONS_ID = 2;
	
	Context ctxt;
	
	public AutoCancelNotification (Context ctxt)
	{
		this.ctxt = ctxt;
	}
	
	@Override
	public void enable (StateData sd)
	{
		/* empty */
	}
	
	@Override
	public void disable ()
	{			
		hideReviews ();
		hideLessons ();
	}
	
	@Override
	public void update (StateData sd, ChangeType ctype)
	{
		switch (ctype) {
		case LESSONS:
			if (sd.hasLessons)
				showLessons (sd.lessons);
			else
				hideLessons ();
			break;
			
		case REVIEWS:
			if (sd.hasReviews)
				showReviews (sd.reviews);
			else
				hideReviews ();
			break;
			
		case DATA:
			setData (sd.dd);
		}
	}

	public void showLessons (int lessons)
	{
		NotificationManager nmanager;
		NotificationCompat.Builder builder;
		Notification not;
		PendingIntent pint;
		Intent intent;
		String text;

		nmanager = (NotificationManager) 
				ctxt.getSystemService (Context.NOTIFICATION_SERVICE);
			
		intent = new Intent (ctxt, NotificationService.class);
		intent.setAction (NotificationService.ACTION_LESSONS_TAP);
		
		pint = PendingIntent.getService (ctxt, 0, intent, 0);

		builder = new NotificationCompat.Builder (ctxt);
		builder.setSmallIcon (R.drawable.not_lessons);
		
		if (SettingsActivity.get42plus (ctxt) && lessons > DashboardFragment.LESSONS_42P)
			text = ctxt.getString (R.string.new_lessons_42plus, DashboardFragment.LESSONS_42P);
		else
			text = ctxt.getString (lessons == 1 ? 
						      R.string.new_lesson : R.string.new_lessons, lessons);
		builder.setContentTitle (ctxt.getString (R.string.app_name));
								 
		builder.setContentText (text);
		builder.setContentIntent (pint);
				
		not = builder.build ();
		not.flags |= Notification.FLAG_AUTO_CANCEL;
		
		nmanager.notify (NOT_LESSONS_ID, not);		
	}
	
	public void hideLessons ()
	{
		NotificationManager nmanager;
		
		nmanager = (NotificationManager) 
			ctxt.getSystemService (Context.NOTIFICATION_SERVICE);
		
		nmanager.cancel (NOT_LESSONS_ID);		
	}
	
	public void showReviews (int reviews)
	{
		NotificationManager nmanager;
		NotificationCompat.Builder builder;
		Notification not;
		PendingIntent pint;
		Intent intent;
		String text;

		nmanager = (NotificationManager) 
				ctxt.getSystemService (Context.NOTIFICATION_SERVICE);
			
		intent = new Intent (ctxt, NotificationService.class);
		intent.setAction (NotificationService.ACTION_TAP);
		
		pint = PendingIntent.getService (ctxt, 0, intent, 0);

		builder = new NotificationCompat.Builder (ctxt);
		builder.setSmallIcon (R.drawable.not_icon);
		
		if (SettingsActivity.get42plus (ctxt) && reviews > DashboardFragment.LESSONS_42P)
			text = ctxt.getString (R.string.new_reviews_42plus, DashboardFragment.LESSONS_42P);
		else
			text = ctxt.getString (reviews == 1 ? 
						      R.string.new_review : R.string.new_reviews, reviews);
		builder.setContentTitle (ctxt.getString (R.string.app_name));
								 
		builder.setContentText (text);
		builder.setContentIntent (pint);
		
		not = builder.build ();
		not.flags |= Notification.FLAG_AUTO_CANCEL;
		
		nmanager.notify (NOT_REVIEWS_ID, not);		
	}
	
	public void hideReviews ()
	{
		NotificationManager nmanager;
		
		nmanager = (NotificationManager) 
			ctxt.getSystemService (Context.NOTIFICATION_SERVICE);
		
		nmanager.cancel (NOT_REVIEWS_ID);
	}
	
	
	public void setData (DashboardData dd)
	{
		/* empty */
	}
}
