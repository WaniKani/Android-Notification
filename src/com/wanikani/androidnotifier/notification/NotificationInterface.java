package com.wanikani.androidnotifier.notification;

import com.wanikani.androidnotifier.notification.NotificationService.StateData;

public interface NotificationInterface {

	public enum ChangeType {
		
		LESSONS,
		
		REVIEWS,
		
		DATA
		
	}
	
	public void enable (StateData sd);
	
	public void disable ();
	
	public void update (StateData sd, ChangeType ctype);
	
}
