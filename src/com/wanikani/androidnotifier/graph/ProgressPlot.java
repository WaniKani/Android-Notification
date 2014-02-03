package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

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

/**
 * A progress plot. This behaves more or less as a pie plot; it just displays its data
 * as a bar. 
 */
public class ProgressPlot extends View {

	/**
	 * One item to be displayed. This class contains both public fields
	 * (filled by users of this widget) and private fields that are used
	 * internally for rendering purposes.   
	 */
	public static class DataSet {
		
		/// The legend description
		public String description;
		
		/// The color
		public int color;
		
		/// The value
		public float value;
		
		/// Show even if value is zero
		public boolean showAlways;

		/// Path of the side
		Path fpath;

		/// Path of the section 
		Path spath;
		
		/// Color context for the side
		Paint fpaint;
		
		/// Color context for the section
		Paint spaint;
		
		/// Is this a legend only item
		boolean legendOnly;
		
		/**
		 * Constructor.
		 * @param description the legend description
		 * @param color the color
		 * @param value the value
		 */
		public DataSet (String description, int color, float value)
		{
			this.description = description;
			this.color = color;
			this.value = value;
		}
				
		/**
		 * Constructor for legend-only items.
		 * @param description the legend description
		 * @param value the value
		 */
		public DataSet (String description, float value)
		{
			this.description = description;
			this.value = value;
			
			legendOnly = true;
		}

		/**
		 * Constructor.
		 * @param value the value
		 */
		public DataSet (float value)
		{
			this.value = value;
		}
		
		/**
		 * Updates its internal fields
		 * @param description the legend description
		 * @param color the color
		 */
		public void set (String description, int color)
		{
			this.description = description;
			this.color = color;
		}		
		
		public static void setDifferential (List<DataSet> dsets)
		{
			float pos;
			
			pos = 0;
			for (DataSet ds : dsets) {
				ds.value -= pos;
				if (ds.value > 0)
					pos += ds.value;
				else
					ds.value = 0;
			}				
		}
	}
	
	public static class Marker {
		
		/// The legend description
		public String description;
		
		/// The color
		public int color;
		
		/// The value
		public float value;

		/// Color context
		Paint paint;
		
		/// Is there a bar separator too?
		boolean separator;
		
		/// The separator path
		Path path;
		
		float left, anchor, right, baseline;
		
		boolean hidden;
		
		public Marker (String description, int color, float value)
		{
			this (description, color, value, false);
		}
		
		public Marker (String description, int color, float value, boolean separator)
		{
			this.description = description;
			this.color = color;
			this.value = value;
			this.separator = separator;
		}
	}
	
	/// The current displayed datasets
	private List<DataSet> dsets;
	
	/// The markers
	private List<Marker> markers;

	/// The enclosing rect
	private RectF rect;
	
	/// Default width/height ratio of the ellipse
	private static final float DEFAULT_RATIO = .5F;
	
	/// Default height. Set to the android rythm
	private static final int DEFAULT_HEIGHT = 48;
	
	/// Default marker font size
	public static float DEFAULT_MARKER_FONT_SIZE = 12;
	
	/// Default space between marker and plot
	public static int DEFAULT_MARKER_SPACE = 10;

	/// Width/height ratio of the ellipse
	private float ratio;
	
	/// The marker font size (converted from sp)
	private float markerFontSize;
	
	/// Is the view ready to display
	private boolean readyToDraw; 
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public ProgressPlot (Context ctxt, AttributeSet attrs)
	{		
		super (ctxt, attrs);
		
		dsets = new Vector<DataSet> (0);		
		
		loadAttributes (ctxt, attrs);
	}
	
	/**
	 * Constructor.
	 * @param ctxt context
	 */
	public ProgressPlot (Context ctxt)
	{		
		super (ctxt);
		
		dsets = new Vector<DataSet> (0);		
	}

	/**
	 * Performs the actual job of reading the attributes and updating 
	 * the look. Meant for cascading.
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		DisplayMetrics dm;
		TypedArray a;
		
		dm = ctxt.getResources ().getDisplayMetrics ();
		
		a = ctxt.obtainStyledAttributes (attrs, R.styleable.PiePlot);
		ratio = a.getFloat (R.styleable.PiePlot_ratio, DEFAULT_RATIO);
		
		a.recycle ();
		
		markerFontSize = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_SP, DEFAULT_MARKER_FONT_SIZE, dm);
	}
	
	@Override
	public void onMeasure (int widthSpec, int heightSpec)
	{
		int width, height;
		int wMode, hMode;
		
		width = MeasureSpec.getSize (widthSpec);
		height = MeasureSpec.getSize (heightSpec);
		
		wMode = MeasureSpec.getMode (widthSpec);
		hMode = MeasureSpec.getMode (heightSpec);
		
		if (wMode == MeasureSpec.UNSPECIFIED)
			width = 100;
		
		switch (hMode) {
		case MeasureSpec.AT_MOST:
			height = Math.min (width, DEFAULT_HEIGHT);
			break;
			
		case MeasureSpec.EXACTLY:
			break;
			
		case MeasureSpec.UNSPECIFIED:
			height = DEFAULT_HEIGHT;
		}
		
		setMeasuredDimension (width, height);	
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{
		DataSet nzds;
		
		if (!readyToDraw)
			return;
		
		nzds = null;
		for (DataSet ds : dsets) {
			canvas.drawPath (ds.fpath, ds.fpaint);
			if (ds.value > 0)
				nzds = ds;
		}
		if (nzds != null && ratio > 0) {
			canvas.drawPath (nzds.fpath, nzds.fpaint);
			canvas.drawPath (nzds.spath, nzds.spaint);
		}
		
		if (markers != null) {
			for (Marker m : markers) {
				if (!m.hidden)
					canvas.drawText (m.description, m.anchor, m.baseline, m.paint);
				if (m.separator)
					canvas.drawPath (m.path, m.paint);
			}
		}
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		rect = new RectF (0, 0, width, height);

		rect.left += getPaddingLeft ();
		rect.right -= getPaddingRight ();
		rect.top += getPaddingTop ();
		rect.bottom -= getPaddingBottom ();
		
		recalc ();
	}
	
	/**
	 * Creates the path describing the side of a bar.
	 * @param ds the dataset to be filled
	 * @param rect the enclosing rect
	 * @param orect the oval rect (which is moved right of an amount equal to the rect width)
	 */
	protected void fillSidePath (DataSet ds, RectF rect, RectF orect)
	{
		Path path;
		
		path = new Path ();
		if (orect.width () > 0) {
			path.moveTo (rect.left, rect.bottom);
			path.arcTo (orect, 90, 180);
			path.lineTo (rect.right, rect.top);
			orect.offset (rect.width (), 0);
			path.arcTo (orect, 270, -180);
			path.close ();
		} else
			path.addRect (rect, Direction.CW);
		
		ds.fpath = path;
	}

	/**
	 * Creates the path describing the section of a bar.
	 * @param ds the dataset to be filled
	 * @param orect the oval rect
	 */
	protected void fillSectionPath (DataSet ds, RectF orect)
	{
		Path path;
		
		path = new Path ();
		path.addOval (orect, Path.Direction.CW);
		path.close ();
		
		ds.spath = path;
	}
	
	protected void prepareMarkers (RectF rect, float unit)
	{
		float height, hwidth, space, pos, baseline;
		Marker m1, m2;
		FontMetrics fm;
		Paint paint;
		int i, j;
		
		if (markers == null)
			return;
		
		fm = null;
		height = 0;
		baseline = 0;
		for (Marker ds : markers) {
			
			paint = new Paint ();
			paint.setStyle (Style.FILL_AND_STROKE);
			paint.setStrokeWidth (1);
			paint.setColor (ds.color);
			paint.setTextSize ((int) markerFontSize);
			paint.setAntiAlias (true);
			
			pos = unit * ds.value;
			hwidth = paint.measureText (ds.description) / 2;
			space = paint.measureText (" ") / 2;
			if (pos < hwidth) {
				ds.anchor = 0;
				ds.left = 0;
				ds.right = 2 * hwidth + space;
				paint.setTextAlign (Paint.Align.LEFT);
			} else if (pos + hwidth > rect.width ()) {
				ds.anchor = rect.width ();
				ds.right = rect.width ();
				ds.left = ds.right - 2 * hwidth - space;
				paint.setTextAlign (Paint.Align.RIGHT);				
			} else {
				ds.anchor = pos;
				ds.left = ds.anchor - hwidth - space;
				ds.right = ds.anchor + hwidth + space;
				paint.setTextAlign (Paint.Align.CENTER);
			}
			
			if (fm == null) {
				fm = paint.getFontMetrics ();
				height = fm.bottom - fm.ascent;
				baseline = rect.top - fm.ascent;
				rect.top = height + DEFAULT_MARKER_FONT_SIZE;
			}				
			ds.anchor += rect.left;
			ds.baseline = baseline;			
			ds.paint = paint;					
			
			if (ds.separator) {
				ds.path = new Path ();				
				ds.path.moveTo (rect.left + pos, rect.top);
				ds.path.lineTo (rect.left + pos, rect.bottom);
			}				
		}
		
		for (i = 0; i < markers.size (); i++) {
			m1 = markers.get (i);
			m1.hidden = false;
			for (j = 0; j < i; j++) {
				m2 = markers.get (j);
				if ((m1.left >= m2.left && m1.left <= m2.right) ||
					(m1.right >= m2.left && m1.right <= m2.right)) {
					m1.hidden = true;
					break;
				}
			}
		}
		
	}
	
	/**
	 * Recreates all the private fields of a dataset, to make the redraw operation
	 * as quick as possible.
	 */
	protected void recalc ()
	{
		RectF rect, orect;
		float total, unit;
		
		readyToDraw = false;
		
		if (dsets.isEmpty () || this.rect == null)
			return;
			
		total = 0;
		for (DataSet ds : dsets)
			total += ds.value;
		
		if (total == 0)
			return;
		
		rect = new RectF (this.rect);
		prepareMarkers (rect, rect.width () / total);				
		orect = new RectF (rect);
		
		orect.right = orect.left + orect.height () * ratio;
		if (rect.width () <= orect.width ())
			return;
		
		rect.left += orect.width () / 2;
		rect.right -= orect.width () / 2;
					
		unit = rect.width () / total;
		
		for (DataSet ds : dsets) {
			
			rect.right = rect.left + unit * ds.value;  
			
			fillSidePath (ds, rect, orect);
			fillSectionPath (ds, orect);
			
			ds.fpaint = new Paint ();
			ds.fpaint.setStyle (Style.FILL_AND_STROKE);
			ds.fpaint.setColor (ds.color);
			ds.fpaint.setAntiAlias (true);
			
			ds.spaint = new Paint ();
			ds.spaint.setStyle (Style.FILL_AND_STROKE);
			ds.spaint.setColor (shadow (ds.color));
			ds.spaint.setAntiAlias (true);			
						
			rect.left = rect.right;
		}
		
		readyToDraw = true;
	}
	
	/**
	 * Makes a color a little bit darker, to give a shadow effect
	 * @param color a color
	 * @return a darker nuance
	 */
	protected int shadow (int color)
	{
		float hsv [];

		hsv = new float [3];
		Color.colorToHSV (color, hsv);
		hsv [2] /= 2;
		
		return Color.HSVToColor (hsv);
	}

	/**
	 * Updates the datasets and the markers
	 * @param dsets a data set
	 * @param markers the markers
	 */
	public void setData (List<DataSet> dsets, List<Marker> markers)
	{
		this.dsets = dsets;
		this.markers = markers;
		recalc ();
		invalidate ();
	}
}
