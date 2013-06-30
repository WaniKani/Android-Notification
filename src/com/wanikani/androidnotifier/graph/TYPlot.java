package com.wanikani.androidnotifier.graph;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

public class TYPlot extends View {

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		
		
	}
	
	private class OnTouchListener implements View.OnTouchListener {
		
		@Override
		public boolean onTouch (View view, MotionEvent mev)
		{
			return gdect.onTouchEvent (mev);
		}
	}
	
	private static class Measures {
		
		public float DEFAULT_MARGIN = 24;
		
		public float DEFAULT_DIP_PER_DAY = 8;
		
		public int DEFAULT_LOOKAHEAD = 1;
		
		public RectF rect;
		
		public RectF plotArea;
		
		public float margin;
		
		public float dipPerDay;
		
		public int lookAhead;
		
		public Measures (AttributeSet attrs)
		{
			margin = DEFAULT_MARGIN;
			dipPerDay = DEFAULT_DIP_PER_DAY;
			lookAhead = DEFAULT_LOOKAHEAD;
			
			updateSize (new RectF ());
		}
		
		public void updateSize (RectF rect)
		{
			this.rect = rect;
			plotArea = new RectF (rect);
			
			plotArea.top += margin;
			plotArea.bottom -= margin;
		}
		
	}
	
	private static class Viewport {
		
		Measures meas;
		
		float t0;
		
		float t1;
		
		float interval;
		
		public Viewport (Measures meas)
		{
			this.meas = meas;
			
			t1 = meas.lookAhead;
			updateSize ();
		}
		
		public void updateSize ()
		{
			interval = meas.plotArea.width () / meas.dipPerDay;
			
			update ();
		}
		
		public void update ()
		{
			t0 = t1 - interval;
		}
		
		public float dayToPos (int day)
		{
			return (day - t0) * meas.dipPerDay;
		}
		
		public float posToDay (float pos)
		{
			return pos / meas.dipPerDay + t0;
		}
		
		private int floor (float d)
		{
			return (int) (d > 0 ? Math.floor (d) : Math.ceil (d));
		}
		
		private int ceil (float d)
		{
			return (int) (d > 0 ? Math.ceil (d) : Math.floor (d));
		}

		public int rightmostDay ()
		{
			return floor (t1);
		}
		
		public int leftmostDay ()
		{
			return ceil (t0);
		}
	}
	
	private static class PaintAssets {
		
		Paint axisPaint;
		
		Paint gridPaint;
		
		Paint background;
		
		public PaintAssets (AttributeSet attrs)
		{
			float points [];
			
			axisPaint = new Paint ();
			axisPaint.setColor (Color.BLACK);
			
			points = new float [] { 1, 1 };
			gridPaint = new Paint ();
			gridPaint.setColor (Color.BLACK);
			gridPaint.setPathEffect (new DashPathEffect (points, 0));
			
			background = new Paint ();
			background.setColor (Color.WHITE);
			background.setStyle (Paint.Style.FILL);
		}		
	}
	
	private Scroller scroller;
	
	private GestureDetector gdect;
	
	private GestureListener glist;
	
	private Measures meas;
	
	private Viewport vp;
	
	private PaintAssets pas;
	
	public TYPlot (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		scroller = new Scroller (ctxt);
		glist = new GestureListener ();
		gdect = new GestureDetector (ctxt, glist);
		setOnTouchListener (new OnTouchListener ());
		
		meas = new Measures (attrs);
		vp = new Viewport (meas);
		pas = new PaintAssets (attrs);
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		meas.updateSize (new RectF (0, 0, width, height));
		vp.updateSize ();
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{
		drawBackground (canvas);
		drawGrid (canvas);
	}
	
	protected void drawBackground (Canvas canvas)
	{		
		canvas.drawRect (meas.rect, pas.background);
	}

	protected Calendar dayToCalendar (int day)
	{
		Calendar c;

		c = Calendar.getInstance ();
		c.add (Calendar.DATE, day);
		
		return c;
	}

	protected Date dayToDate (int day)
	{
		return dayToCalendar (day).getTime ();
	}
	
	protected void drawGrid (Canvas canvas)
	{
		int d, lo, hi, weekday;
		float f;
		
		canvas.drawLine (meas.plotArea.left, meas.plotArea.bottom,
				         meas.plotArea.right, meas.plotArea.bottom, pas.axisPaint);
		f = vp.dayToPos (0);		
		lo = vp.leftmostDay ();
		hi = vp.rightmostDay ();
		weekday = dayToCalendar (lo).get (Calendar.DAY_OF_WEEK) - 1;
		if (weekday != 0)
			lo += 7 - weekday;
		for (d = lo; d <= hi; d += 7) {
			f = vp.dayToPos (d);
			canvas.drawLine (f, meas.plotArea.top, f, meas.plotArea.bottom, pas.gridPaint); 
		}
		if (lo <= 0 && 0 <= hi) {
			f = vp.dayToPos (0);
			canvas.drawLine (f, meas.plotArea.top, f, meas.plotArea.bottom, pas.axisPaint);
		}
	}
}
