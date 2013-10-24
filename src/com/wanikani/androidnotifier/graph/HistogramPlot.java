package com.wanikani.androidnotifier.graph;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.wanikani.androidnotifier.R;

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

public class HistogramPlot extends View {

	public static class Series {
		
		public String name;
		
		public int color;
		
		public Series (String name, int color)
		{
			this.name = name;
			this.color = color;
		}
		
		public Series (int color)
		{
			this.color = color;
		}
		
	}
	
	public static class Sample {
		
		public Series series;
		
		public long value;
		
		public Sample (Series series, long value)
		{
			this.series = series;
			this.value = value;
		}

		public Sample (Series series)
		{
			this.series = series;
		}
		
		public Sample ()
		{
			/* empty */
		}		
	}
	
	public static class Samples {
		
		public String tag;
		
		public List<Sample> samples;
		
		public Samples (String tag)
		{
			this.tag = tag;
			samples = new Vector<Sample> ();
		}
		
	}
		
	/**
	 * The listener that intercepts motion and fling gestures.
	 */
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onDown (MotionEvent mev)
		{
			scroller.forceFinished (true);
			ViewCompat.postInvalidateOnAnimation (HistogramPlot.this);
			
			return true;
		}
		
		@Override
		public boolean onScroll (MotionEvent mev1, MotionEvent mev2, float dx, float dy)
		{
			vp.scroll ((int) dx, (int) dy);
			ViewCompat.postInvalidateOnAnimation (HistogramPlot.this);
			
			return true;
		}

		@Override
		public boolean onFling (MotionEvent mev1, MotionEvent mev2, float vx, float vy)
		{			
			scroller.forceFinished (true);
			scroller.fling (vp.getAbsPosition (), 0, (int) -vx, 0, 0, 
							vp.barToAbsPosition (vp.bars) + 2000000, 0, 0);
			ViewCompat.postInvalidateOnAnimation (HistogramPlot.this);
			
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
		
		/// Default number of pixels per bar
		public float DEFAULT_DIP_PER_BAR = 20;
				
		/// Default gap between bars
		public float DEFAULT_GAP = 8;
		
		/// Default label font size
		public float DEFAULT_DATE_LABEL_FONT_SIZE = 12;
		
		/// Default axis width
		public int DEFAULT_AXIS_WIDTH = 2;
		
		/// Default space between axis and label
		public int DEFAULT_HEADROOM = 10;
		
		/// Default number of items represented by a vertical mark
		public int DEFAULT_YAXIS_GRID = 100;

		/// The plot area
		public RectF plotArea;
		
		/// The complete view area
		public RectF viewArea;
		
		/// Actual margin around the diagram
		public float margin;
		
		/// Actual number of pixels per bar
		public float dipPerBar;
		
		/// Actual gap between bars
		public float gap;
		
		/// Actual label font size
		public float axisWidth;
		
		/// Actual space between axis and label
		public int headroom = 10;

		/// Actual font siz
		public float dateLabelFontSize;
		
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
			TypedArray a;
			
			a = ctxt.obtainStyledAttributes (attrs, R.styleable.HistogramPlot);
				 								
			dm = ctxt.getResources ().getDisplayMetrics ();
			
			margin = DEFAULT_MARGIN;
			dipPerBar = DEFAULT_DIP_PER_BAR;
			gap = DEFAULT_GAP;
			axisWidth = DEFAULT_AXIS_WIDTH;
			headroom = DEFAULT_HEADROOM;
			dateLabelFontSize = DEFAULT_DATE_LABEL_FONT_SIZE;
			yaxisGrid = a.getInteger (R.styleable.HistogramPlot_ticks, DEFAULT_YAXIS_GRID);
			
			dateLabelFontSize = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_SP, 
					 									   dateLabelFontSize, dm);
			
			a.recycle ();		

			updateSize (new RectF ());
		}
		
		public void updateLabelPaint (Paint paint)
		{
			dipPerBar = paint.measureText (" 999 ");
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
			margin = Math.max (DEFAULT_MARGIN, mm + headroom);
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
		
		/// Paint used to draw labels
		Paint labelPaint;
		
		/// Paint used to draw total amount
		Paint levelupPaint;
		
		/// Paint used to draw total amount, when drawn inside the bar
		Paint levelupPaintInside;

		/// Series to paint map
		Map<Series, Paint> series;
		
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
			float points [];
			
			axisPaint = new Paint ();
			axisPaint.setColor (Color.BLACK);
			axisPaint.setStrokeWidth (meas.axisWidth);
			
			points = new float [] { 1, 1 };
			gridPaint = new Paint ();
			gridPaint.setColor (Color.BLACK);
			gridPaint.setPathEffect (new DashPathEffect (points, 0));
			
			levelupPaint = new Paint ();
			levelupPaint.setColor (res.getColor (R.color.levelup));
			levelupPaint.setTextAlign (Paint.Align.CENTER);
			levelupPaint.setTextSize ((int) meas.dateLabelFontSize);
			levelupPaint.setAntiAlias (true);
			
			levelupPaintInside = new Paint ();
			levelupPaintInside.setColor (Color.WHITE);
			levelupPaintInside.setTextAlign (Paint.Align.CENTER);
			levelupPaintInside.setTextSize ((int) meas.dateLabelFontSize);
			levelupPaintInside.setAntiAlias (true);

			labelPaint = new Paint ();
			labelPaint.setColor (Color.BLACK);
			labelPaint.setTextAlign (Paint.Align.CENTER);
			labelPaint.setTextSize ((int) meas.dateLabelFontSize);
			labelPaint.setAntiAlias (true);
			meas.updateLabelPaint (labelPaint);
			
			fm = labelPaint.getFontMetrics ();
			meas.ensureFontMargin ((int) (fm.bottom - fm.ascent));			
			
			series = new Hashtable<Series, Paint> ();
		}		
		
		/**
		 * Called when the series set changes. Recreates the mapping between
		 * series and paint objects
		 * @param series the new series
		 */
		public void setSeries (List<Series> series)
		{
			Paint p;
			
			this.series.clear ();
			for (Series s : series) {
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
		
		/// The measure object
		Measures meas;
		
		/// The first (leftmost) tag 
		float t0;
		
		/// The last (rightmost) tag
		float t1;
		
		/// Size of interval (<tt>t1-t0</tt>) 
		float interval;
		
		/// Max value of Y axis
		long yMax;
		
		/// Y scale
		float yScale;
		
		/// Number of bars
		int bars;
		
		/**
		 * Constructor
		 * @param meas the measure object
		 * @param bars the number of bars
		 * @param yMax max value of Y axis
		 */
		public Viewport (Measures meas, int bars, long yMax)
		{			
			this.meas = meas;
			this.bars = bars;
			this.yMax = yMax;
			
			t1 = bars;
			
			updateSize ();
		}
				
		/**
		 * Called when the plot area changes
		 */
		public void updateSize ()
		{
			interval = meas.plotArea.width () / (meas.dipPerBar + meas.gap);
			yScale = meas.plotArea.height () / yMax;
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
			else if (t0 > bars - interval)
				t0 = bars - interval;

			t1 = t0 + interval;
		}
		
		/**
		 * Returns the number of pixels between the left margin of the viewport
		 * and the first bar. Of course these pixels are not displayed because they
		 * are outside the viewport.
		 * @return the number of pixels
		 */
		public int getAbsPosition ()
		{
			return barToAbsPosition (t0);
		}
		
		/**
		 * Moves the viewport, putting its left margin at a number of pixels to
		 * the right of first bar
		 * @param pos the new position
		 */
		public void setAbsPosition (int pos)
		{
			t0 = absPositionToBar (pos);
			t1 = t0 + interval;
			adjust ();
		}
		
		/**
		 * Returns the number of pixels between the leftmost bar and a given bar
		 * @param bar a bar
		 * @return the number of pixels
		 */
		public int getRelPosition (int bar)
		{
			return (int) ((bar - t0) * (meas.dipPerBar + meas.gap));
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
		 * @param bar a bar
		 * @return the number of pixels
		 */
		public int barToAbsPosition (float bar)
		{
			return (int) (bar * (meas.dipPerBar + meas.gap));
		}
		
		/**
		 * Returns the day, given the number of pixels from subscription day
		 * @param pos number of pixels
		 * @return the bar number
		 */
		public float absPositionToBar (int pos)
		{
			return ((float) pos) / (meas.dipPerBar + meas.gap);
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
		 * Returns the rightmost complete bar represented in this viewport.
		 * This differs from {@link #t1} because it is an integer
		 * @return the bar
		 */
		public int rightmostBar ()
		{
			return floor (t1);
		}
		
		/**
		 * Returns the leftmost complete bar represented in this viewport.
		 * This differs from {@link #t0} because it is an integer
		 * @return the bar
		 */
		public int leftmostBar ()
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
	
	/// <tt>true</tt> during fling gestures
	private boolean scrolling;
	
	/// The actual data
	private List<Samples> bars;
	
	/**
	 * Constructor
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	public HistogramPlot (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		scroller = new Scroller (ctxt);
		glist = new GestureListener ();
		gdect = new GestureDetector (ctxt, glist);
		
		loadAttributes (ctxt, attrs);
	}
		
	/**
	 * Constructs the objects that use attributes.
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{				
		meas = new Measures (ctxt, attrs);
		pas = new PaintAssets (getResources (), attrs, meas);		
	}
		
	/**
	 * Sets the data samples.
	 * @param series a list of series that will be referenced by <tt>data</tt> 
	 * @param bars a list of samples, each representing a bar
	 * @param cap maximum Y value admitted (may be smaller if bars are smaller than that)
	 */
	public void setData (List<Series> series, List<Samples> bars, long cap)
	{
		pas.setSeries (series);
		vp = new Viewport (meas, bars.size (), getMaxY (bars, cap));
		
		this.bars = bars;
		
		invalidate ();
	}
	
	static private long getMaxY (List<Samples> bars, long cap)
	{
		long ans, current, rmax;
		
		ans = rmax = 0;
		for (Samples bar : bars) {
			current = 0;
			for (Sample s : bar.samples)
				current += s.value;
			if (cap <= 0 || current < cap)
				ans = Math.max (ans, current);
			rmax = Math.max (rmax, current);
		}
		
		return ans != 0 ? ans : rmax;
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
		vp.updateSize ();
		
		invalidate ();
	}

	@Override
    public void computeScroll () 
	{
        super.computeScroll ();
        
        if (scroller.computeScrollOffset ()) {
        	vp.setAbsPosition (scroller.getCurrX ());
			ViewCompat.postInvalidateOnAnimation (this);
        }
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{			
		float left, right, tagLabelBaseline;
		int d, lo, hi, ascent;
		Samples bar;
		
		canvas.drawLine (meas.plotArea.left, meas.plotArea.bottom,
				         meas.plotArea.right, meas.plotArea.bottom, pas.axisPaint);
		lo = vp.leftmostBar () - 1;	/* We want broken bars too :) */
		hi = vp.rightmostBar () + 1;
				
		ascent = (int) pas.labelPaint.getFontMetrics ().ascent;
		
		tagLabelBaseline = meas.plotArea.bottom - ascent + meas.headroom / 2;
		
		for (d = lo; d <= hi; d++) {
			
			if (d < 0)
				continue;
			
			if (d >= bars.size ())
				break;
			
			left = vp.getRelPosition (d);
			right = left + vp.meas.dipPerBar;
			bar = bars.get (d);

			drawBar (canvas, bar, left, right);
			
			canvas.drawText (bar.tag, (left + right) / 2, tagLabelBaseline, pas.labelPaint);
		}
		
		if (meas.yaxisGrid > 0) {
			for (d = meas.yaxisGrid; vp.getY (d) >= meas.plotArea.top; d += meas.yaxisGrid) {
				canvas.drawLine (meas.plotArea.left, vp.getY (d), 
							 	meas.plotArea.right, vp.getY (d), pas.gridPaint);
			}
		}
	}
	
	protected void drawBar (Canvas canvas, Samples bar, float left, float right)
	{
		long base, height;
		float top, tbl;
		Paint lpaint;
		Path path;
		RectF rect;
		
		top = vp.getY (vp.yMax);
		base = 0;
		for (Sample sample : bar.samples) {
			if (sample.value > 0) {
				height = sample.value;
				
				if (base > vp.yMax)
					break;
				else if (base + height > vp.yMax) {
					path = new Path ();
					path.moveTo (left, vp.getY (base));
					path.lineTo (left, top);
					path.lineTo (left + (right - left) / 3, top - 10);
					path.lineTo (left + (right - left) * 2 / 3, top + 5);
					path.lineTo (right, top);
					path.lineTo (right, vp.getY (base));
					path.close ();
					canvas.drawPath (path, pas.series.get (sample.series));
				} else {				
					rect = new RectF (left, vp.getY (base + height), right, vp.getY (base));
					rect.intersect (meas.plotArea);
					canvas.drawRect (rect, pas.series.get (sample.series));
				}
				base += height;
				
			}
		}
		
		if (base <= vp.yMax) {
			lpaint = pas.levelupPaint;
			tbl = vp.getY (base) - meas.headroom / 2;
		} else {
			lpaint = pas.levelupPaintInside; 
			tbl = vp.getY (vp.yMax) + meas.margin;
		}
			
		canvas.drawText (Long.toString (base), (left + right) / 2, tbl, lpaint);
	}
	
	/**
	 * True if scrolling 
	 * @return <tt>true</tt> if scrolling
	 */
	public boolean scrolling ()
	{
		return scrolling;
	}
}