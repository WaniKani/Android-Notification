package com.wanikani.androidnotifier.graph;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.wanikani.androidnotifier.R;
import com.wanikani.androidnotifier.graph.Pager.DataSet;

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

public class TYPlot extends View {

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onDown (MotionEvent mev)
		{
			scroller.forceFinished (true);
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
			
			return true;
		}
		
		@Override
		public boolean onScroll (MotionEvent mev1, MotionEvent mev2, float dx, float dy)
		{
			vp.scroll ((int) dx, (int) dy);
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
			
			return true;
		}

		@Override
		public boolean onFling (MotionEvent mev1, MotionEvent mev2, float vx, float vy)
		{			
			scroller.forceFinished (true);
			scroller.fling (vp.getAbsPosition (), 0, (int) -vx, 0, 0, 
							vp.dayToAbsPosition (taxis.today) + 2000000, 0, 0);
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
			
			return true;
		}
	}
	
	private static class Measures {
		
		public float DEFAULT_MARGIN = 24;
		
		public float DEFAULT_DIP_PER_DAY = 8;
				
		public int DEFAULT_LOOKAHEAD = 7;
		
		public float DEFAULT_DATE_LABEL_FONT_SIZE = 12;
		
		public int DEFAULT_AXIS_WIDTH = 2;
		
		public int DEFAULT_TICK_SIZE = 10;
		
		public int DEFAULT_YAXIS_GRID = 100;
		
		public RectF rect;
		
		public RectF plotArea;
		
		public float margin;
		
		public float dipPerDay;
		
		public int lookAhead;
		
		public float axisWidth;
		
		public float dateLabelFontSize;
		
		public int tickSize;
		
		public int yaxisGrid;
				
		public Measures (Context ctxt, AttributeSet attrs)
		{
			DisplayMetrics dm;
			
			dm = ctxt.getResources ().getDisplayMetrics ();
			
			margin = DEFAULT_MARGIN;
			dipPerDay = DEFAULT_DIP_PER_DAY;
			lookAhead = DEFAULT_LOOKAHEAD;
			axisWidth = DEFAULT_AXIS_WIDTH;
			dateLabelFontSize = DEFAULT_DATE_LABEL_FONT_SIZE;
			tickSize = DEFAULT_TICK_SIZE;
			yaxisGrid = DEFAULT_YAXIS_GRID;
			
			dateLabelFontSize = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_SP, 
					 									   dateLabelFontSize, dm);
			
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
	
	private static class PaintAssets {
		
		Paint axisPaint;
		
		Paint gridPaint;
		
		Paint dateLabels;
		
		Paint partial;
		
		Map<Pager.Series, Paint> series;
		
		public PaintAssets (Resources res, AttributeSet attrs, Measures meas)
		{
			Bitmap bmp;
			Shader shader;
			float points [];
			
			axisPaint = new Paint ();
			axisPaint.setColor (Color.BLACK);
			axisPaint.setStrokeWidth (meas.axisWidth);
			
			points = new float [] { 1, 1 };
			gridPaint = new Paint ();
			gridPaint.setColor (Color.BLACK);
			gridPaint.setPathEffect (new DashPathEffect (points, 0));
			
			dateLabels = new Paint ();
			dateLabels.setColor (Color.BLACK);
			dateLabels.setTextAlign (Paint.Align.CENTER);
			dateLabels.setTextSize ((int) meas.dateLabelFontSize);
			dateLabels.setAntiAlias (true);
			
			bmp = BitmapFactory.decodeResource (res, R.drawable.partial);
			
			partial = new Paint (Paint.FILTER_BITMAP_FLAG);
			shader = new BitmapShader (bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			partial.setShader (shader);
			
			series = new Hashtable<Pager.Series, Paint> ();
		}		
		
		public void setSeries (List<Pager.Series> series)
		{
			Paint p;
			
			this.series.clear ();
			for (Pager.Series s : series) {
				p = new Paint ();
				p.setColor (s.color);
				p.setStyle (Paint.Style.FILL_AND_STROKE);
				p.setAntiAlias (true);
				this.series.put (s, p);
			}
		}
	}
	
	private static class Viewport {
		
		DataSink dsink;
		
		Measures meas;
		
		float t0;
		
		float t1;
		
		float interval;
		
		float yScale;
		
		int today;
		
		public Viewport (DataSink dsink, Measures meas, int today)
		{			
			this.dsink = dsink;
			this.meas = meas;
			this.today = today;
			
			t1 = today + meas.lookAhead;
			
			/* Will be updated as soon as we have a valid datasource */
			updateSize (100);
		}
		
		public void setToday (int today)
		{
			if (today == this.today)
				return;
			
			t1 += today - this.today;
			t0 = t1 - interval;
			
			this.today = today;
			adjust ();
			dsink.refresh ();
		}
		
		public void updateSize (float yMax)
		{
			interval = meas.plotArea.width () / meas.dipPerDay;
			yScale = meas.plotArea.height () / yMax;
			if (interval < meas.lookAhead)
				interval = meas.lookAhead;
			t0 = t1 - interval;
			adjust ();
		}
		
		public void adjust ()
		{
			if (t0 < 0)
				t0 = 0;
			else if (t0 > today)
				t0 = today;

			t1 = t0 + interval;
		}
		
		public int getAbsPosition ()
		{
			return dayToAbsPosition (t0);
		}
		
		public void setAbsPosition (int pos)
		{
			t0 = absPositionToDay (pos);
			t1 = t0 + interval;
			adjust ();
			dsink.refresh ();
		}
		
		public int getRelPosition (int day)
		{
			return (int) ((day - t0) * meas.dipPerDay);
		}
		
		public float getY (float y)
		{
			return meas.plotArea.bottom - y * yScale;
		}
		
		public void scroll (int dx, int dy)
		{
			setAbsPosition (getAbsPosition () + dx);
		}

		public int dayToAbsPosition (float day)
		{
			return (int) (day * meas.dipPerDay);
		}
		
		public float absPositionToDay (int pos)
		{
			return ((float) pos) / meas.dipPerDay;
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
		
		public Pager.Interval getInterval ()
		{
			return new Pager.Interval (floor (t0), ceil (t1));
		}		
	}
	
	private static class TimeAxis {
		
		/// The date mapped as "day zero"
		public Date origin;
		
		/// Now
		public Date now;
		
		public int today;
		
		public TimeAxis ()
		{
			now = new Date ();
			setOrigin (new Date (0));
		}
		
		private Calendar getNormalizedCalendar (Date date)
		{
			Calendar ans;
			
			ans = Calendar.getInstance ();
			ans.setTime (date);
			ans.set (Calendar.HOUR, 1);
			ans.set (Calendar.MINUTE, 2);
			ans.set (Calendar.SECOND, 3);
			ans.set (Calendar.MILLISECOND, 4);
			
			return ans;
		}
		
		public void setOrigin (Date date)
		{
			Calendar cal1, cal2;
			
			origin = date;
			cal1 = getNormalizedCalendar (origin);
			cal2 = getNormalizedCalendar (now);
			today = (int) ((cal2.getTimeInMillis () - cal1.getTimeInMillis ()) /
							(24 * 60 * 60 * 1000));
		}
		
		public Calendar dayToCalendar (int day)
		{
			Calendar cal;
			
			cal = Calendar.getInstance ();
			cal.setTime (origin);
			cal.add (Calendar.DATE, day);
			
			return cal;
		}	
	}
	
	private class DataSink implements Pager.DataSink {
		
		DataSet ds;
		
		public void refresh ()
		{
			if (pager != null && gotOrigin) {
				retrieving (true);
				pager.requestData (vp.getInterval ());
			} else
				invalidate ();
		}

		public void dataAvailable (DataSet ds)
		{
			if (ds.interval.equals (vp.getInterval ())) {				
				this.ds = ds;
				retrieving (false);
				invalidate ();
			}
		}		
	}
	
	private Scroller scroller;
	
	private GestureDetector gdect;
	
	private GestureListener glist;
	
	private Measures meas;
	
	private Viewport vp;
	
	private PaintAssets pas;
	
	private DateFormat datef;
	
	private DateFormat janf;
	
	private TimeAxis taxis;
	
	private Pager pager;
	
	private DataSink dsink;
	
	private boolean scrolling;
	
	private TYChart chart;

	boolean gotOrigin;	
	
	public TYPlot (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		scroller = new Scroller (ctxt);
		glist = new GestureListener ();
		gdect = new GestureDetector (ctxt, glist);
		
		datef = new SimpleDateFormat ("MMM", Locale.US);
		janf = new SimpleDateFormat ("MMM yyyy", Locale.US);
		taxis = new TimeAxis ();
		dsink = new DataSink ();
		
		loadAttributes (ctxt, attrs);
	}
	
	public void setTYChart (TYChart chart)
	{
		this.chart = chart;
		spin (true);
	}
	
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		meas = new Measures (ctxt, attrs);
		vp = new Viewport (dsink, meas, taxis.today);
		pas = new PaintAssets (getResources (), attrs, meas);		
	}
	
	public void setOrigin (Date date)
	{
		gotOrigin = true;
		taxis.setOrigin (date);
		vp.setToday (taxis.today);
	}
	
	public void setDataSource (Pager.DataSource dsource)
	{
		gotOrigin = false;
		pager = new Pager (dsource, dsink);
		pas.setSeries (dsource.getSeries ());
		vp.updateSize (dsource.getMaxY ());
		spin (true);
	}

	@Override
	public boolean onTouchEvent (MotionEvent mev)
	{
		boolean ans;

		switch (mev.getAction ()) {
		case MotionEvent.ACTION_DOWN:
			scrolling = true;
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			scrolling = false;
			break;
		}
		
		ans = gdect.onTouchEvent (mev);
		
		return ans || super.onTouchEvent (mev);
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		meas.updateSize (new RectF (0, 0, width, height));
		vp.updateSize (pager != null ? pager.dsource.getMaxY () : 100);
		dsink.refresh ();
	}

	@Override
    public void computeScroll () 
	{
        super.computeScroll ();
        
        if (scroller.computeScrollOffset ()) {
        	vp.setAbsPosition (scroller.getCurrX ());
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
        }
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{
		if (dsink != null && dsink.ds != null)
			drawPlot (canvas, dsink.ds);
		
		drawGrid (canvas);
	}
	
	protected void drawGrid (Canvas canvas)
	{
		float f, dateLabelBaseline;
		int d, lo, hi;
		DateFormat df;
		String s;
		Calendar cal;
		
		canvas.drawLine (meas.plotArea.left, meas.plotArea.bottom,
				         meas.plotArea.right, meas.plotArea.bottom, pas.axisPaint);
		f = vp.getRelPosition (0);		
		lo = vp.leftmostDay ();
		hi = vp.rightmostDay ();
		cal = taxis.dayToCalendar (lo);
		
		dateLabelBaseline = meas.plotArea.bottom + meas.dateLabelFontSize + meas.tickSize / 2;

		for (d = lo; d <= hi; d++) {
			f = vp.getRelPosition (d);
			
			if (d == 0 || d == taxis.today)
				canvas.drawLine (f, meas.plotArea.top, f, meas.plotArea.bottom, pas.axisPaint);
			else if (cal.get (Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
				canvas.drawLine (f, meas.plotArea.top, f, meas.plotArea.bottom, pas.gridPaint);
			
			if (cal.get (Calendar.DAY_OF_MONTH) == 1) {
				df = cal.get (Calendar.MONTH) == Calendar.JANUARY ? janf : datef;
				s = df.format (cal.getTime ());
				canvas.drawLine (f, meas.plotArea.bottom - meas.tickSize / 2,
								 f, meas.plotArea.bottom + meas.tickSize / 2, pas.axisPaint);
				canvas.drawText (s, f, dateLabelBaseline, pas.dateLabels);
			}			
			
			cal.add (Calendar.DATE, 1);
		}
		
		for (d = meas.yaxisGrid; vp.getY (d) >= meas.plotArea.top; d += meas.yaxisGrid)
			canvas.drawLine (meas.plotArea.left, vp.getY (d), 
							 meas.plotArea.right, vp.getY (d), pas.gridPaint);
	}
	
	protected void drawPlot (Canvas canvas, Pager.DataSet ds)
	{
		for (Pager.Segment segment : ds.segments)
			drawSegment (canvas, segment);
	}
	
	protected void drawSegment (Canvas canvas, Pager.Segment segment)
	{
		float f [];
		int i;
		
		switch (segment.type) {
		case MISSING:
			drawMissing (canvas, segment.interval);
			break;
			
		case VALID:
			f = new float [segment.interval.getSize ()];
			for (i = 0; i < segment.data.length; i++)
				drawPlot (canvas, segment.series.get (i), segment.interval, 
						  f, segment.data [i]);
				
			break;
		}
	}
	
	protected void drawMissing (Canvas canvas, Pager.Interval i)
	{
		int from, to;
		
		if (i.start >= vp.today)
			return;
		
		from = i.start;
		to = Math.min (i.stop + 1, vp.today);
		
		canvas.drawRect (vp.getRelPosition (from), meas.plotArea.top,
				   		 vp.getRelPosition (to), meas.plotArea.bottom, pas.partial);
	}
	
	protected void drawPlot (Canvas canvas, Pager.Series series, Pager.Interval interval,
							 float base [], float samples [])
	{
		Path path;
		Paint p;
		int i, n;
		
		p = pas.series.get (series);
		n = interval.stop - interval.start + 1;
		if (p == null || samples.length == 0 || n <= 0)
			return;

		path = new Path ();
		
		i = n - 1;
		path.moveTo (vp.getRelPosition (interval.start + i), vp.getY (base [i]));
		while (true) {
			base [i] += samples [i];
			if (i-- == 0)
				break;
			path.lineTo (vp.getRelPosition (interval.start + i), vp.getY (base [i]));
		}
			
		for (i = 0; i < n; i++)
			path.lineTo (vp.getRelPosition (interval.start + i), vp.getY (base [i]));

		path.close ();
		
		canvas.drawPath (path, p);
	}
	
	public boolean scrolling ()
	{
		return scrolling;
	}
	
	public void spin (boolean enable)
	{
		if (chart != null)
			chart.spin (enable);
	}

	public void retrieving (boolean enable)
	{
		if (chart != null)
			chart.retrieving (enable);
		if (!enable)
			spin (false);
	}
}