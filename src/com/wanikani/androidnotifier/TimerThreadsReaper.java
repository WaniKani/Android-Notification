package com.wanikani.androidnotifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.os.Handler;

public class TimerThreadsReaper {

	private static class AudioTagWrapper implements Comparable<AudioTagWrapper>  {

		Method method;

		Object tag;
		
		int tcount;

		public AudioTagWrapper (int tcount, Object tag) 
		{
			this.tcount = tcount;
			this.tag = tag;
		}

		private boolean stopTimer ()
		{
			Timer timer;
			
			try {
				timer = (Timer) TimerThreadsReaper.getField (tag, "mTimer", Timer.class);
				if (timer == null)
					return false;
				
				timer.cancel ();
				
				return true;
			} catch (Throwable t) {
				return false;
			}
		}
		
		private boolean resetMediaPlayer ()
		{
			try {
				method = tag.getClass().getDeclaredMethod ("resetMediaPlayer", new Class [0]);
				method.setAccessible (true);
				method.invoke (tag, new Object [0]);
			} catch (Throwable t) {
				return false;
			}
			
			return true;
		}

		public boolean release ()
		{
			return stopTimer () || resetMediaPlayer ();
		}
		

		@Override
		public boolean equals (Object w) 
		{
			return tag == ((AudioTagWrapper) w).tag;
		}

		@Override
		public int hashCode () 
		{
			return tag.hashCode();
		}
		
		@Override
		public int compareTo (AudioTagWrapper w)
		{
			return tcount - w.tcount;
		}
	}
	
	public interface ReaperTaskListener {
		
		public void reaped (int count, int total);
		
	}
	
	public class ReaperTask implements Runnable {
		
		Handler handler;
		
		int grace;
		
		long period;
		
		boolean active;
		
		ReaperTaskListener listener;
		
		int total;
		
		private ReaperTask (Handler handler, int grace, long period)
		{			
			this.handler = handler;
			this.grace = grace;
			this.period = period;
		}
		
		public void setListener (ReaperTaskListener listener)
		{
			this.listener = listener;
		}
		
		public void resume ()
		{
			active = true;
			run ();
		}
		
		public void pause ()
		{
			active = false;
		}
		
		public void run ()
		{
			int count;
			
			if (!active)
				return;
			
			count = killDelta (grace);
			total += count;
			if (listener != null)
				listener.reaped (count, total);
			
			handler.postDelayed (this, period);
		}		
	}
	
	List<AudioTagWrapper> reaped;
	
	public TimerThreadsReaper ()
	{
		reaped = new Vector<AudioTagWrapper> ();
	}
		
	public ReaperTask createTask (Handler handler, int grace, long period)
	{
		return new ReaperTask (handler, grace, period);
	}
	
	public int stopAll () 
	{
		return killDelta (0);
	}
	
	public int killDelta (int grace)
	{
		List<AudioTagWrapper> current;
		AudioTagWrapper w;
		int count, i;

		current = snapshot ();

		count = 0;
		for (i = 0; i < current.size () - grace; i++) {
			w = current.get (i);
			if (!reaped.contains (w) && current.get (i).release ())
				count++;
			reaped.add (w);
		}
		
		return count;
	}

	public List<AudioTagWrapper> snapshot () 
	{
		List<AudioTagWrapper> ans;
		ThreadGroup group;
		Thread thread [];
		int i, len;

		ans = new Vector<AudioTagWrapper> ();

		group = Thread.currentThread ().getThreadGroup ();
		thread = new Thread [group.activeCount()];
		while (group.enumerate (thread, true) == thread.length)
			thread = new Thread[thread.length * 2];
		len = group.enumerate(thread, true);
		for (i = 0; i < len; i++) {
			addWrapper(ans, thread[i]);
		}
		
		Collections.sort (ans);
		
		return ans;
	}

	private void addWrapper (List<AudioTagWrapper> wrappers, Thread thread) 
	{
		TimerTask tasks[];
		Object obj;
		Runnable r;
		String s;
		int i, size, tcount;

		s = thread.getName();
		if (!s.startsWith("Timer-"))
			return;
		
		try {
			tcount = Integer.parseInt (s.substring (6));
		} catch (NumberFormatException e) {
			return;
		}

		s = thread.getClass().toString();

		r = (Runnable) getField(thread, "target", Runnable.class);
		if (r == null)
			r = thread;

		obj = getField(r, "tasks", Object.class);
		if (obj == null)
			return;

		tasks = (TimerTask[]) getField(obj, "timers", TimerTask[].class);
		if (tasks == null)
			return;

		obj = getField(obj, "size", Integer.class);
		if (obj == null)
			return;

		size = (Integer) obj;
		for (i = 0; i < size; i++) {
			s = tasks[i].getClass().getCanonicalName();
			if (tasks[i].getClass().getCanonicalName()
					.equals("android.webkit.HTML5Audio.TimeupdateTask")) {
				obj = getField(tasks[i], "this$0", Object.class);
				if (obj != null)
					wrappers.add(new AudioTagWrapper (tcount, obj));
			}
		}
	}

	private static Object getField(Object parent, String name, Class clazz) 
	{
		Class pclass;
		Field field;
		Object obj;

		pclass = parent.getClass();
		try {
			field = pclass.getDeclaredField(name);
			if (field == null)
				return null;

			field.setAccessible(true);

			obj = field.get(parent);
			if (obj == null)
				return null;

			if (!clazz.isInstance(obj))
				return null;

			return obj;

		} catch (Throwable t) {
			return null;
		}
	}

}
