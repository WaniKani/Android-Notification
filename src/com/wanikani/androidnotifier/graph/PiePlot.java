package com.wanikani.androidnotifier.graph;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class PiePlot extends View {

	public static class DataSet {
		
		public int color;
		
		public float value;
		
		Path tpath;

		Path hpath;
		
		Paint fpaint;
		
		Paint spaint;

		public DataSet (int color, float value)
		{
			this.color = color;
			this.value = value;
		}
	}
	
	private List<DataSet> dsets;
	
	private RectF rect;
	
	private static final int START_ANGLE = -110;
	
	private static final float RATIO = 2F;
	
	private static final float HRATIO = 10;
		
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
			height = measureExact (width, height, hMode, RATIO);
		else if (hMode == MeasureSpec.EXACTLY)
			width = measureExact (height, width, wMode, 1F / RATIO);
		else if (wMode == MeasureSpec.AT_MOST) {
			height = measureExact (width, height, hMode, RATIO);
			width = Math.min (width, height);
		} else if (hMode == MeasureSpec.AT_MOST) {
			width = measureExact (height, width, wMode, 1F / RATIO);
			height = Math.min (width, height);
		} else			
			width = height = 100;
		
		setMeasuredDimension (width, height);
	}
	
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
			if (ds.hpath != null)
				canvas.drawPath (ds.hpath, ds.spaint);
		}
		
	}
	
	@Override
	protected void onSizeChanged (int width, int height, int ow, int oh)
	{
		rect = new RectF (0, 0, width, height);
		
		recalc ();
	}
	
	protected Path getTopPath (RectF rect, float angle, float sweep)
	{
		Path path;
		
		path = new Path ();
		path.moveTo (rect.centerX (), rect.centerY ());
		path.arcTo (rect, angle, sweep);
		path.close ();
		
		return path;
	}
	
	protected Path getHPath (RectF rect, float h, float angle, float sweep)
	{
		Path path;
		
		if (angle < 180)
			sweep = Math.min (sweep, 180 - angle);
		else if (sweep > 360 - angle) {
			sweep -= 360 - angle;
			angle = 0;
		} else
			return null;
		
		path = new Path ();
		path.arcTo (rect, angle + sweep, -sweep);
		path.offset (0, h);
		path.arcTo (rect, angle, sweep);
		path.close ();
		
		return path;
	}
	
	protected void recalc ()
	{
		RectF topr;
		float angle, sweep;
		float total;
		float h;
		
		if (dsets.isEmpty () || rect == null)
			return;
		
		h = rect.width () / HRATIO * (1 - 1F / RATIO);
		topr = new RectF (0, 0, rect.width (), rect.height () - h);
		
		total = 0;
		for (DataSet ds : dsets)
			total += ds.value;
		
		angle = START_ANGLE;
		for (DataSet ds : dsets) {
			while (angle < 0)
				angle += 360;
			if (angle > 360)
				angle %= 360;
			
			sweep = ds.value * 360 / total;
			
			ds.tpath = getTopPath (topr, angle, sweep);
			ds.hpath = getHPath (topr, h, angle, sweep);

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
