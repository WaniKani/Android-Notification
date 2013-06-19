package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Vector;

import com.wanikani.androidnotifier.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

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
 * A pie plot. The difference between this widget and {@link PieChart} is
 * that this class just implements the image, without title and legend. 
 */
public class PiePlot extends View {

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
		
		/// Path of the upper side of the slice
		Path tpath;

		/// Path of first vertical strip, if any 
		Path hpath1;
		
		/// Path of second vertical script, if any
		Path hpath2;
		
		/// Color context for the upper side of the slice
		Paint fpaint;
		
		/// Color context of the vertical strip
		Paint spaint;
		
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
	}
	
	/**
	 * An enum whose items reflect the different ways a slice may be displayed.
	 * Depending on the size of a slice and its initial angle, its vertical strip
	 * may be completely hidden, completely visible or partially visible.
	 * The items describe the different possibilities and are used to 
	 * create the correct paths.
	 */
	private enum Strip {
		
		/**
		 * The two edges of the slice are at the near side.
		 */
		FRONT {
			public void fillDataSet (DataSet ds, RectF rect, float h, 
									 float angle1, float angle2)
			{
				if (coz (angle1) < coz (angle2)) {
					//     ______________
					//    /              \
					//    \____1____2____/
					//    ####|      |####
					ds.hpath1 = strip (rect, h, angle1, 180);
					ds.hpath2 = strip (rect, h, 0, angle2);
				} else
					//     ______________
					//    /              \
					//    \____2____1____/
					//        |######|
					ds.hpath1 = strip (rect, h, angle1, angle2);
			}
		},
		
		/**
		 * The two edges of the slice are at the far side.
		 */
		BACK {
			
			public void fillDataSet (DataSet ds, RectF rect, float h, 
									 float angle1, float angle2)
			{
				if (coz (angle2) < coz (angle1))
					//     _____2___1____
					//    /              \
					//    \______________/
					//    ####|      |####
					ds.hpath1 = strip (rect, h, 0, 180);
			}
		},
		
		/**
		 * The first edge is not visible, the other one is visible.
		 */
		RIGHT {
			public void fillDataSet (DataSet ds, RectF rect, float h, 
									 float angle1, float angle2)
			{
				//     _________1____
				//    /              \
				//    \_________2____/
				//               |####
				ds.hpath1 = strip (rect, h, 0, angle2);
			}			
		},
		
		/**
		 * The first edge is visible, the other one is not visible.
		 */
		LEFT {
			public void fillDataSet (DataSet ds, RectF rect, float h, 
									 float angle1, float angle2)
			{
				//     _________2____
				//    /              \
				//    \_________1____/
				//     #########|				
				ds.hpath1 = strip (rect, h, angle1, 180);
			}						
		};

		/**
		 * Udates a datased, by filling the {@link DataSet#hpath1} and
		 * {@link DataSet#hpath2} fields. These are the paths that
		 * describe the vertical strip.
		 * @param ds the dataset to be filled
		 * @param rect the pie chart enclosing rect
		 * @param h the height of the pie plot
		 * @param angle1 the (clockwise) start angle of the slice
		 * @param angle2 the (clockwise) end angle of the slice
		 */
		public abstract void fillDataSet (DataSet ds, RectF rect, float h,
										  float angle1, float angle2);		

		/**
		 * An fake cosine function. The returned values are in the same
		 * relationship between the cosine of the respective arguments.  
		 * @param angle the angle
		 * @return the pseudo-cosine function
		 */
		private static float coz (float angle)
		{
			return angle < 180 ? 360 - angle : angle;
		}	
		
		/**
		 * Returns a path describing to a single vertical strip from a given
		 * angle to another angle, in a clockwise direction 
		 * @param rect the pie plot's enclosing rect
		 * @param h the pie plot height
		 * @param angle1 start angle
		 * @param angle2 stop angle
		 * @return the path
		 */
		private static Path strip (RectF rect, float h, float angle1, float angle2)
		{
			Path path;
			
			path = new Path ();
			path.arcTo (rect, angle2, angle1 - angle2);
			path.offset (0, h);
			path.arcTo (rect, angle1, angle2 - angle1);
			path.close ();
			
			return path;
		}
	}
	
	/// The current datasets
	private List<DataSet> dsets;
	
	/// The enclosing rect
	private RectF rect;
	
	/// Default angle of the first slice
	private static final int DEFAULT_START_ANGLE = -110;
	
	/// Default width/height ratio of the ellipse
	private static final float DEFAULT_RATIO = 2F;
	
	/// Default with/depth ratio
	private static final float DEFAULT_HRATIO = 10;
	
	/// Angle of the first slice
	private int startAngle;
	
	/// Width/height ratio of the ellipse
	private float ratio;
	
	/// Width/depth ratio
	private float hratio;
		
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public PiePlot (Context ctxt, AttributeSet attrs)
	{		
		super (ctxt, attrs);
		
		dsets = new Vector<DataSet> (0);
		
		loadAttributes (ctxt, attrs);
	}
	
	/**
	 * Performs the actual job of reading the attributes and updating 
	 * the look. Meant for cascading.
	 * @param ctxt the context
	 * @param attrs the attributes
	 */
	void loadAttributes (Context ctxt, AttributeSet attrs)
	{
		TypedArray a;
		
		a = ctxt.obtainStyledAttributes (attrs, R.styleable.PiePlot);
			 
		startAngle = a.getInteger (R.styleable.PiePlot_start_angle, DEFAULT_START_ANGLE);
		ratio = a.getFloat (R.styleable.PiePlot_ratio, DEFAULT_RATIO);
		hratio = a.getFloat (R.styleable.PiePlot_hratio, DEFAULT_HRATIO);
		
		a.recycle ();
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
		
		if (wMode == MeasureSpec.EXACTLY)
			height = measureExact (width, height, hMode, ratio);
		else if (hMode == MeasureSpec.EXACTLY)
			width = measureExact (height, width, wMode, 1F / ratio);
		else if (wMode == MeasureSpec.AT_MOST) {
			height = measureExact (width, height, hMode, ratio);
			width = Math.min (width, height);
		} else if (hMode == MeasureSpec.AT_MOST) {
			width = measureExact (height, width, wMode, 1F / ratio);
			height = Math.min (width, height);
		} else			
			width = height = 100;
		
		setMeasuredDimension (width, height);	
	}
	
	/**
	 * Given one of the dimensions of the pie, returns the other dimension,
	 * making sure the constraint encoded in <code>bmode</code> is respected 
	 * @param a one dimension
	 * @param b the other dimension
	 * @param bmode the constraint
	 * @param ratio the ideal ratio between the two dimensions 
	 * @return the best option for <code>b</code>
	 */
	protected int measureExact (float a, float b, int bmode, float ratio)
	{
		a /= ratio;
		
		switch (bmode) {
		case MeasureSpec.EXACTLY:
			return Math.round (b);
			
		case MeasureSpec.AT_MOST:
			return Math.round (Math.min (a, b));
			
		case MeasureSpec.UNSPECIFIED:
			return Math.round (a);
			
		default:
			return Math.round (b);
		}
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{
		for (DataSet ds : dsets) {
			canvas.drawPath (ds.tpath, ds.fpaint);
			if (ds.hpath1 != null)
				canvas.drawPath (ds.hpath1, ds.spaint);
			if (ds.hpath2 != null)
				canvas.drawPath (ds.hpath1, ds.spaint);
		}
		
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		rect = new RectF (0, 0, width, height);
		
		recalc ();
	}
	
	protected void fillTopPath (DataSet ds, RectF rect, float angle, float sweep)
	{
		Path path;
		
		path = new Path ();
		path.moveTo (rect.centerX (), rect.centerY ());
		path.arcTo (rect, angle, sweep);
		path.close ();
		
		ds.tpath = path;
	}
	
	private static boolean isVisible (float angle)
	{
		return angle < 180;
	}
	
	protected void fillHPath (DataSet ds, RectF rect, float h, float angle, float sweep)
	{
		float angle2;
		Strip s;
		
		angle2 = (angle + sweep) % 360;
		if (isVisible (angle))
			if (isVisible (angle2))
				s = Strip.FRONT;
			else
				s = Strip.LEFT;
		else
			if (isVisible (angle2))
				s = Strip.RIGHT;
			else
				s = Strip.BACK;
		
		s.fillDataSet(ds, rect, h, angle, angle2);
	}
	
	protected void recalc ()
	{
		RectF topr;
		float angle, sweep;
		float total;
		float h;
		
		if (dsets.isEmpty () || rect == null)
			return;
		
		h = rect.width () / hratio * (1 - 1F / ratio);
		topr = new RectF (0, 0, rect.width (), rect.height () - h);
		
		total = 0;
		for (DataSet ds : dsets)
			total += ds.value;
		
		angle = startAngle;
		for (DataSet ds : dsets) {
			while (angle < 0)
				angle += 360;
			if (angle > 360)
				angle %= 360;
			
			sweep = ds.value * 360 / total;
			
			fillTopPath (ds, topr, angle, sweep);
			fillHPath (ds, topr, h, angle, sweep);

			ds.fpaint = new Paint ();
			ds.fpaint.setStyle (Style.FILL);
			ds.fpaint.setColor (ds.color);
			ds.fpaint.setAntiAlias (true);
			
			ds.spaint = new Paint ();
			ds.spaint.setStyle (Style.FILL);
			ds.spaint.setColor (shadow (ds.color));
			ds.spaint.setAntiAlias (true);

			angle += sweep;			
		}
	}
	
	protected int shadow (int color)
	{
		float hsv [];

		hsv = new float [3];
		Color.colorToHSV (color, hsv);
		hsv [2] /= 2;
		
		return Color.HSVToColor (hsv);
	}
	
	public void setData (List<DataSet> dsets)
	{
		this.dsets = dsets;
		recalc ();
		invalidate ();
	}
}
