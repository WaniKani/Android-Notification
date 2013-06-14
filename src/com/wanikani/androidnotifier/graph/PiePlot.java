package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PiePlot extends View {

	public static class DataSet {
		
		public int color;
		
		public float value;
		
		Path path;
		
		Paint paint;
		
		public DataSet (int color, float value)
		{
			this.color = color;
			this.value = value;
		}
	}
	
	private List<DataSet> dsets;
	
	private RectF rect;
	
	private static final int START_ANGLE = 110;
	
	/**
	 * Constructor.
	 * @param ctxt context
	 * @param attrs attribute set
	 */
	public PiePlot (Context ctxt, AttributeSet attrs)
	{
		super (ctxt, attrs);
		
		dsets = new Vector<DataSet> (0);
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
			height = measureExact (width, height, hMode);
		else if (hMode == MeasureSpec.EXACTLY)
			width = measureExact (height, width, wMode);
		else if (wMode == MeasureSpec.AT_MOST) {
			height = measureExact (width, height, hMode);
			width = Math.min (width, height);
		} else if (hMode == MeasureSpec.AT_MOST) {
			width = measureExact (height, width, wMode);
			height = Math.min (width, height);
		} else			
			width = height = 100;
		
		setMeasuredDimension (width, height);
	}
	
	protected int measureExact (int a, int b, int bmode)
	{
		switch (bmode) {
		case MeasureSpec.EXACTLY:
			return b;
			
		case MeasureSpec.AT_MOST:
			return Math.min (a, b);
			
		case MeasureSpec.UNSPECIFIED:
			return a;
			
		default:
			return b;
		}
	}
	
	@Override
	protected void onDraw (Canvas canvas)
	{
		for (DataSet ds : dsets)
			canvas.drawPath (ds.path, ds.paint);
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		rect = new RectF (0, 0, width, height);
		
		recalc ();
	}
	
	protected void recalc ()
	{
		float angle, sweep;
		float total;
		float ox, oy;
		
		if (dsets.isEmpty ())
			return;

		total = 0;
		for (DataSet ds : dsets)
			total += ds.value;
		
		ox = rect.centerX ();
		oy = rect.centerY ();
		
		angle = START_ANGLE;
		for (DataSet ds : dsets) {
			while (angle < 0)
				angle += 360;
			if (angle > 360)
				angle %= 360;
			
			sweep = ds.value * 360 / total;

			ds.path = new Path ();
			ds.path.moveTo (ox, oy);
			ds.path.arcTo (rect, angle, sweep);
			ds.path.close ();
			
			ds.paint = new Paint ();
			ds.paint.setColor (ds.color);
			
			angle += sweep;			
		}
	}
	
	public void setData (List<DataSet> dsets)
	{
		this.dsets = dsets;
		recalc ();
		invalidate ();
	}
}
