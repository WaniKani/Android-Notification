package com.wanikani.androidnotifier;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.MenuItem;

import com.wanikani.androidnotifier.db.CSVFormat;

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
 * This class contains some code shared between all the activities that 
 * support an options menu. The menu need not be the same for all, as long
 * as the ids are congruent.
 */
public class MenuHandler {

	/**
	 * A listener implemented by classes that whish to be notified when some
	 * menu-related event happens. Parent methods need not be called since
	 * their behaviour is to do nothing.
	 */
	public static class Listener {
			
		/// The context we are running in
		protected Context ctxt;
		
		/**
		 * Constructor
		 * @param ctxt the context we are running in
		 */
		public Listener (Context ctxt)
		{
			this.ctxt = ctxt;
		}
		
		/**
		 * The user has requested data to be refreshed.
		 */
		public void refresh ()
		{
			/* empty */
		}
		
		/**
		 * Settings are changed.
		 */
		public void settingsChanged ()
		{
			/* empty */
		}
		
		/**
		 * Dashboard item called
		 */
		public void dashboard ()
		{
			/* empty */
		}
		
		/**
		 * Export item called
		 */
		public void export ()
		{
			DataExporter.INSTANCE.export (ctxt, CSVFormat.newInstance (ctxt));
		}
		
		/**
		 * Ignore button
		 */
		public void ignore ()
		{
			/* empty */
		}
		
		/**
		 * Toggle override fonts
		 */
		public void fonts ()
		{
			/* empty */
		}
		
		/**
		 * Start meters activity
		 */
		public void meters ()
		{
			Intent intent;
			
			intent = new Intent (ctxt, MetersActivity.class);
			
			ctxt.startActivity (intent);			
		}
	}
	
	/**
	 * This receiver listens to the {@link SettingsActivity@ACT_CHANGED} event
	 * and delivers it to the listener.
	 *
	 */
	private class SettingsReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive (Context ctxt, Intent i)
		{
			if (i.getAction ().equals (SettingsActivity.ACT_CHANGED))
				listener.settingsChanged ();
		}
			
	}

	/** The context we are attached to */
	Context ctxt;
	
	/** The listener */
	Listener listener;
	
	/** The receiver */
	SettingsReceiver recv;
	
	/**
	 * Constructor
	 * @param ctxt the context
	 * @param listener the event listener
	 */
	public MenuHandler (Context ctxt, Listener listener)
	{
		this.ctxt = ctxt;
		this.listener = listener;
		
		IntentFilter filter;

		recv = new SettingsReceiver ();
		
		filter = new IntentFilter (SettingsActivity.ACT_CHANGED);
		ctxt.registerReceiver (recv, filter);		
	}

	
	/**
	 * Must be called when the handler is not needed any more, to prevent
	 * leakages
	 */
	public void unregister (Context ctxt)
	{
		ctxt.unregisterReceiver (recv);
	}
	
	/**
	 * Activities should call this method in their 
	 * implementation of {@link Activity#onOptionsItemSelected}. 
	 * @param item the selected item
	 * @return <tt>true</tt> if the event has been consumed
	 */
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId ()) {
		case R.id.em_refresh:
			listener.refresh ();
			break;
			
		case R.id.em_settings:
			settings ();
			break;

		case R.id.em_dashboard:
			listener.dashboard ();
			break;
			
		case R.id.em_export:
			listener.export ();
			break;
			
		case R.id.em_ignore:
			listener.ignore ();
			break;
			
		case R.id.em_fonts:
			listener.fonts ();
			break;
			
		case R.id.em_meters:
			listener.meters ();
			break;
			
		default:
			return false;
		}
		
		return true;
	}
	
	/**
	 * Settings menu implementation. Actually, it simply stacks the 
	 * {@link SettingsActivity} activity.
	 */
	public void settings ()
	{
		Intent intent;
		
		intent = new Intent (ctxt, SettingsActivity.class);
		ctxt.startActivity (intent);
	}	
}
