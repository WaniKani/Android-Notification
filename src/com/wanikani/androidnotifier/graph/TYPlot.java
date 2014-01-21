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
import android.graphics.Paint.FontMetrics;
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
import com.wanikani.wklib.UserInformation;

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
 * A time-value chart. This view represents just the diagram, while {@link TYChart}
 * shows some other useful widgets too. Though it is possible to insert this view
 * into a layout, it is advisable to use the chart, because otherwise e.g. data
 * retrieval won't cause the spinner to show up.
 */
public class TYPlot extends View {

	/**
	 * The listener that intercepts motion and fling gestures.
	 */
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
			strictScroll |= dx != 0;
			vp.scroll ((int) dx, (int) dy);
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
			
			return true;
		}

		@Override
		public boolean onFling (MotionEvent mev1, MotionEvent mev2, float vx, float vy)
		{			
			strictScroll |= vx != 0;

			scroller.forceFinished (true);
			scroller.fling (vp.getAbsPosition (), 0, (int) -vx, 0, 0, 
							vp.dayToAbsPosition (taxis.today) + 2000000, 0, 0);
			ViewCompat.postInvalidateOnAnimation (TYPlot.this);
			
			return true;
		}
	}

	/**
	 * A repository of all the sizes and measures. Currently no variables
	 * can be customized, however I've kept them separated from their default
	 * values, so allowing layouts to override these default is just a matter of adding
	 * an attributes parser.
	 */
	private static class Measures {
		
		/// Default margin around the diagram
		public float DEFAULT_MARGIN = 24;
		
		/// Default number of pixels per day
		public float DEFAULT_DIP_PER_DAY = 8;
				
		/// Default number of days after today that are displayed at startup
		public int DEFAULT_LOOKAHEAD = 7;
		
		/// Default label font size
		public float DEFAULT_DATE_LABEL_FONT_SIZE = 12;
		
		/// Default axis width
		public int DEFAULT_AXIS_WIDTH = 2;
		
		/// Default height of a day tick
		public int DEFAULT_TICK_SIZE = 10;
		
		/// Default number of items represented by a vertical mark
		public int DEFAULT_YAXIS_GRID = 100;

		/// The plot area
		public RectF plotArea;
		
		/// The complete view area
		public RectF viewArea;
		
		/// Actual margin around the diagram
		public float margin;
		
		/// Actual number of pixels per day
		public float dipPerDay;
		
		/// Actual number of days after today that are displayed at startup
		public int lookAhead;
		
		/// Actual label font size
		public float axisWidth;
		
		/// Actual height of a day tick
		public float dateLabelFontSize;
		
		/// Actual number of items represented by a vertical mark
		public int tickSize;
		
		/// Actual number of items represented by a vertical mark
		public int yaxisGrid;
				
		/**
		 * Constructor
		 * @param ctxt the context 
		 * @param attrs attributes of the plot. Currently ignored
		 */
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

		/**
		 * Called when the plot changes it size. Updates the inner plot rect
		 * @param rect the new plot size
		 */
		public void updateSize (RectF rect)
		{
			viewArea = new RectF (rect);
			plotArea = new RectF (rect);
			
			plotArea.top += margin;
			plotArea.bottom -= margin;
		}
		
		/**
		 * Makes sure that the margin is large enough to display the time axis labels.
		 * @param mm minimum margin
		 */
		public void ensureFontMargin (long mm)
		{
			margin = Math.max (DEFAULT_MARGIN, mm + tickSize);
			updateSize (viewArea);
		}
		
	}
	
	/**
	 * A collection of all the paint objects that will be needed when drawing
	 * on the canvas. We create them beforehand and recycle them for performance
	 * reasons.
	 */
	private static class PaintAssets {

		/// Paint used to draw the axis
		Paint axisPaint;

		/// Paint used to draw the grids		
		Paint gridPaint;
		
		/// Paint used to draw the labels
		Paint dateLabels;
		
		/// Paint used to draw the area where samples are not available
		Paint partial;
		
		/// Paint used to draw levelups
		Paint levelup;
		
		/// Series to paint map
		Map<Pager.Series, Paint> series;
		
		/**
		 * Constructor. Creates all the paints, using the chart attributes and
		 * measures
		 * @param res the resources
		 * @param attrs the chart attributes
		 * @param meas measures object
		 */
		public PaintAssets (Resources res, AttributeSet attrs, Measures meas)
		{
			FontMetrics fm;
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
			
			fm = dateLabels.getFontMetrics ();
			meas.ensureFontMargin ((int) (fm.bottom - fm.ascent));
			
			bmp = BitmapFactory.decodeResource (res, R.drawable.partial);
			
			partial = new Paint (Paint.FILTER_BITMAP_FLAG);
			shader = new BitmapShader (bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			partial.setShader (shader);
			
			levelup = new Paint ();
			levelup.setTextAlign (Paint.Align.CENTER);
			levelup.setTextSize ((int) meas.dateLabelFontSize);
			levelup.setAntiAlias (true);
			
			series = new Hashtable<Pager.Series, Paint> ();
		}		
		
		/**
		 * Called when the series set changes. Recreates the mapping between
		 * series and paint objects
		 * @param series the new series
		 */
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
	
	/**
	 * This object tracks the position of the interval of the plot which is
	 * currently visible.
	 */
	private static class Viewport {
		
		/// The connector to the datasource. Needed to refresh when the viewport moves
		DataSink dsink;
		
		/// The measure object
		Measures meas;
		
		/// The first (leftmost) day 
		float t0;
		
		/// The last (rightmost) day
		float t1;
		
		/// Size of interval (<tt>t1-t0</tt>) 
		float interval;
		
		/// Y scale
		float yScale;
		
		/// Number of days since subscription
		int today;
		
		/**
		 * Constructor
		 * @param dsink the datasink 
		 * @param meas the measure object
		 * @param today number of days since subscription
		 */
		public Viewport (DataSink dsink, Measures meas, int today)
		{			
			this.dsink = dsink;
			this.meas = meas;
			this.today = today;
			
			t1 = today + meas.lookAhead;
			
			/* Will be updated as soon as we have a valid datasource */
			updateSize (100);
		}
		
		/**
		 * Changes the current day. Must be called when changing the datasource
		 * @param today the new origin
		 */
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
		
		/**
		 * Called when the plot area changes
		 * @param yMax the Y max
		 */
		public void updateSize (float yMax)
		{
			interval = meas.plotArea.width () / meas.dipPerDay;
			yScale = meas.plotArea.height () / yMax;
			if (interval < meas.lookAhead)
				interval = meas.lookAhead;
			t0 = t1 - interval;
			adjust ();
		}
		
		/**
		 * Updates the lower and upper edges after the viewport is resized 
		 */
		private void adjust ()
		{
			if (t0 < 0)
				t0 = 0;
			else if (t0 > today)
				t0 = today;

			t1 = t0 + interval;
		}
		
		/**
		 * Returns the number of pixels between the left margin of the viewport
		 * and day zero. Of course these pixels are not displayed because they
		 * are outside the viewport.
		 * @return the number of pixels
		 */
		public int getAbsPosition ()
		{
			return dayToAbsPosition (t0);
		}
		
		/**
		 * Moves the viewport, putting its left margin at a number of pixels to
		 * the right of day zero
		 * @param pos the new position
		 */
		public void setAbsPosition (int pos)
		{
			t0 = absPositionToDay (pos);
			t1 = t0 + interval;
			adjust ();
			dsink.refresh ();
		}
		
		/**
		 * Returns the number of pixels between the leftmost day and a given day
		 * @param day a day
		 * @return the number of pixels
		 */
		public int getRelPosition (int day)
		{
			return (int) ((day - t0) * meas.dipPerDay);
		}
		
		/**
		 * Converts item numbers to pixel
		 * @param y item numbers
		 * @return the number of pixel
		 */
		public float getY (float y)
		{
			return meas.plotArea.bottom - y * yScale;
		}
		
		/**
		 * Scrolls the viewport by a given interval
		 * @param dx the horizontal interval
		 * @param dy the vertical interval (ignored)
		 */
		public void scroll (int dx, int dy)
		{
			setAbsPosition (getAbsPosition () + dx);
		}

		/**
		 * Returns the number of pixels between a given day and the
		 * day of subscription.
		 * @param day a day
		 * @return the number of pixels
		 */
		public int dayToAbsPosition (float day)
		{
			return (int) (day * meas.dipPerDay);
		}
		
		/**
		 * Returns the day, given the number of pixels from subscription day
		 * @param pos number of pixels
		 * @return the day number
		 */
		public float absPositionToDay (int pos)
		{
			return ((float) pos) / meas.dipPerDay;
		}
		
		/**
		 * A floor operation that always points to -inf.
		 * @param d a number 
		 * @return the floor
		 */
		private int floor (float d)
		{
			return (int) (d > 0 ? Math.floor (d) : Math.ceil (d));
		}
		
		/**
		 * A ceil operation that always points to +inf.
		 * @param d a number 
		 * @return the ceil
		 */
		private int ceil (float d)
		{
			return (int) (d > 0 ? Math.ceil (d) : Math.floor (d));
		}

		/**
		 * Returns the rightmost day represented in this viewport.
		 * This differs from {@link #t1} because it is an integer
		 * @return the day
		 */
		public int rightmostDay ()
		{
			return floor (t1);
		}
		
		/**
		 * Returns the leftmost day represented in this viewport.
		 * This differs from {@link #t0} because it is an integer
		 * @return the day
		 */
		public int leftmostDay ()
		{
			return ceil (t0);
		}
		
		/**
		 * Returns the current displayed interval
		 * @return the interval
		 */
		public Pager.Interval getInterval ()
		{
			return new Pager.Interval (floor (t0), ceil (t1));
		}		
		
		/**
		 * Tells whether a subset of this interval is visible in the viewport 
		 * @param i an interval
		 * @return <tt>true</tt> if it is visible
		 */
		public boolean visible (Pager.Interval i)
		{
			return i.start < today && i.stop > t0;
		}
	}
	
	/**
	 * The time axis state
	 */
	private static class TimeAxis {
		
		/// The date mapped as "day zero"
		public Date origin;
		
		/// Now as a date
		public Date now;
		
		/// Now as a day
		public int today;
		
		/**
		 * Constructor
		 */
		public TimeAxis ()
		{
			now = new Date ();
			setOrigin (new Date (0));
		}
		
		/**
		 * Moves the origin to a given date
		 * @param date the date
		 */
		public void setOrigin (Date date)
		{
			origin = date;
			today = UserInformation.getDay (origin, now);
		}
		
		/**
		 * Converts a day into the calendar
		 * @param day a day
		 * @return the calendar position
		 */
		public Calendar dayToCalendar (int day)
		{
			Calendar cal;
			
			cal = Calendar.getInstance ();
			cal.setTime (origin);
			cal.add (Calendar.DATE, day);
			
			return cal;
		}	
	}
	
	/**
	 * An implementation of the pager datasink. This is the object that
	 * requests data from the data source, and updates the plot when
	 * it receives the samples.
	 */
	private class DataSink implements Pager.DataSink {
		
		/// The current dataset
		DataSet ds;
		
		/**
		 * Called when the plot needs to be refreshed. Note that we always
		 * request data because caching is done at a lower layer.
		 */
		public void refresh ()
		{
			if (pager != null && gotOrigin) {
				refreshing (true);
				pager.requestData (vp.getInterval ());
			} else
				invalidate ();
		}

		/**
		 * Called when data is available. Refreshes the plot area
		 * @param ds the samples
		 */
		public void dataAvailable (DataSet ds)
		{
			if (ds.interval.equals (vp.getInterval ())) {				
				this.ds = ds;
				refreshing (false);
				invalidate ();
			}
		}		
	}
	
	/// The scroller object that tracks fling gestures
	private Scroller scroller;
	
	/// The android gesture detector
	private GestureDetector gdect;
	
	/// Our gesture listener
	private GestureListener glist;
	
	/// The measure object
	private Measures meas;
	
	/// The current viewport
	private Viewport vp;
	
	/// The paint objects
	private PaintAssets pas;
	
	/// Date format to display time labels
	private DateFormat datef;
	
	/// Date format to display january (we also print the year)
	private DateFormat janf;
	
	/// The time axis
	private TimeAxis taxis;
	
	/// The pager, if a datasource is available, <tt>null</tt> otherwise
	private Pager pager;
	
	/// Our datasink
	private DataSink dsink;
	
	/// <tt>true</tt> during fling gestures
	private boolean scrolling;
	
	private boolean strictScroll;
	
	/// A reference to the parent chart, if any
	private TYChart chart;
	
	/// <tt>true</tt> if we know where we are
	boolean gotOrigin;	
	
	/**
	 * Constructor
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
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
	
	/**
	 * Called by the parent chart, if any. Calling this method enables some
	 * extra features. 
	 * @param chart the chart
	 */
	void setTYChart (TYChart chart)
	{
		this.chart = chart;
	}
	
	/**
	 * Constructs the objects that use attributes.
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		meas = new Measures (ctxt, attrs);
		vp = new Viewport (dsink, meas, taxis.today);
		pas = new PaintAssets (getResources (), attrs, meas);		
	}
	
	/**
	 * Sets the time origin. Until this call is made, no data is ever shown,
	 * so it must be the first call after 
	 * {@link #setDataSource(com.wanikani.androidnotifier.graph.Pager.DataSource)}
	 * @param date the origin
	 */
	public void setOrigin (Date date)
	{
		gotOrigin = true;
		taxis.setOrigin (date);
		vp.setToday (taxis.today);
	}
	
	/**
	 * Sets the datasource. After this, callers should use {@link #setOrigin(Date)}
	 * @param dsource the new datasource
	 */
	public void setDataSource (Pager.DataSource dsource)
	{
		gotOrigin = false;
		pager = new Pager (dsource, dsink);
		pas.setSeries (dsource.getSeries ());
		vp.updateSize (dsource.getMaxY ());
	}

	@Override
	public boolean onTouchEvent (MotionEvent mev)
	{
		boolean ans;

		switch (mev.getAction ()) {
		case MotionEvent.ACTION_DOWN:
			scrolling = true;
			strictScroll = false;
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
		boolean partial;
		
		if (dsink != null && dsink.ds != null) {
			partial = drawPlot (canvas, dsink.ds);
			if (chart != null)
				chart.partialShown (partial);				
		}
			
		drawGrid (canvas);
	}
	
	/**
	 * Draws the grinds on the canvas. Since they are "over" the plot, this
	 * method should be called last
	 * @param canvas the canvas
	 */
	protected void drawGrid (Canvas canvas)
	{
		float f, dateLabelBaseline, levelupBaseline;
		Map<Integer, Pager.Marker> markers;
		Pager.Marker marker;
		int d, lo, hi, ascent;
		DateFormat df;
		String s;
		Calendar cal;
		
		canvas.drawLine (meas.plotArea.left, meas.plotArea.bottom,
				         meas.plotArea.right, meas.plotArea.bottom, pas.axisPaint);
		f = vp.getRelPosition (0);		
		lo = vp.leftmostDay ();
		hi = vp.rightmostDay ();
		cal = taxis.dayToCalendar (lo);
		
		markers = pager.dsource.getMarkers ();
		
		ascent = (int) pas.dateLabels.getFontMetrics ().ascent;
		
		dateLabelBaseline = meas.plotArea.bottom - ascent + meas.tickSize / 2;
		levelupBaseline = meas.plotArea.top - meas.tickSize / 2;
		
		for (d = meas.yaxisGrid; vp.getY (d) >= meas.plotArea.top; d += meas.yaxisGrid)
			canvas.drawLine (meas.plotArea.left, vp.getY (d), 
							 meas.plotArea.right, vp.getY (d), pas.gridPaint);

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
			
			marker = markers.get (d);
			if (marker != null) {
				pas.levelup.setColor (marker.color);
				canvas.drawLine (f, meas.plotArea.top, f, meas.plotArea.bottom, pas.levelup);
				canvas.drawText (marker.name, f, levelupBaseline, pas.levelup);
			}
			
			cal.add (Calendar.DATE, 1);
		}
	}
	
	/**
	 * Draws the plot
	 * @param canvas the canvas
	 * @param ds the samples
	 * @return <tt>true</tt> if some partial data is present
	 */
	protected boolean drawPlot (Canvas canvas, Pager.DataSet ds)
	{
		boolean ans;
		
		ans = false;
		
		for (Pager.Segment segment : ds.segments)
			ans |= drawSegment (canvas, segment);
		
		return ans;
	}
	
	/**
	 * Draws a segment of the plot.
	 * @param canvas the canvas
	 * @param segment the segment
	 * @return <tt>true</tt> if this is a partial segment (i.e. its type
	 * is {@link Pager.SegmentType#MISSING}
	 */
	protected boolean drawSegment (Canvas canvas, Pager.Segment segment)
	{
		float f [];
		int i;
		
		switch (segment.type) {
		case MISSING:
			return drawMissing (canvas, segment.interval);
			
		case VALID:
			f = new float [segment.interval.getSize ()];
			for (i = 0; i < segment.data.length; i++)
				drawPlot (canvas, segment.series.get (i), segment.interval, 
						  f, segment.data [i]);
				
			break;
		}
		
		return false;
	}
	
	/**
	 * Draws a segment without data
	 * @param canvas the canvas
	 * @param i the interval size
	 * @return <tt>true</tt> unless this part entirely outside the viewport
	 */
	protected boolean drawMissing (Canvas canvas, Pager.Interval i)
	{
		int from, to;
		
		if (!vp.visible (i))
			return false;
		
		from = i.start;
		to = Math.min (i.stop + 1, vp.today);
		
		canvas.drawRect (vp.getRelPosition (from), meas.plotArea.top,
				   		 vp.getRelPosition (to), meas.plotArea.bottom, pas.partial);
		
		return true;
	}
	
	/**
	 * Draws a segment containins samples
	 * @param canvas the canvas
	 * @param series the series
	 * @param interval the interval
	 * @param base a float array initially set to zero, and updated by this method
	 * @param samples the samples 
	 */
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
		
		path.moveTo (vp.getRelPosition (interval.start + n), vp.getY (base [n - 1]));
		
		for (i = n - 1; i >= 0; i--) {
			path.lineTo (vp.getRelPosition (interval.start + i), vp.getY (base [i]));
			base [i] += samples [i];
		}
			
		for (i = 0; i < n; i++)
			path.lineTo (vp.getRelPosition (interval.start + i), vp.getY (base [i]));
		
		path.lineTo (vp.getRelPosition (interval.start + n), vp.getY (base [n - 1]));

		path.close ();
		
		canvas.drawPath (path, p);
	}
	
	/**
	 * True if scrolling 
	 * @return <tt>true</tt> if scrolling
	 */
	public boolean scrolling (boolean strict)
	{
		return scrolling && (!strict || strictScroll);
	}
	
	public void refreshing (boolean enable)
	{
		if (chart != null) {
			if (enable)
				chart.startRefresh ();
			else
				chart.dataAvailable ();
		}
	}
	
	/**
	 * Starts data reconstruction
	 */
	public void fillPartial ()
	{
		if (pager != null) 
			pager.fillPartial ();
	}

	/**
	 * Called when the samples have been changed. Updates the Y scale and 
	 * requests fresh data to the data source.
	 */
	public void refresh ()
	{
		/* The size may have changed */
		if (pager != null)
			vp.updateSize (pager.dsource.getMaxY ());
		
		dsink.refresh ();
	}
}