package vargovcik.peter.atasp_companionapp.uiaddons;

import vargovcik.peter.atasp_companionapp.helpers.HelperMethods;
import vargovcik.peter.atasp_companionapp.uiaddons.AnalogPadInterface.ANALOG_PAD;
import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class AnalogPad extends View {
	private static final double NOB_RADIUS_PROPORTION_SIZE = .25;
	private static final int BACKGROUND_OFSET = 10;
	private static final long NOB_MAX_OUT = 100;
	private static final long NOB_MIN_OUT = -100;
	private float nobRadius = 50;
	private float x = 30;
	private float y = 30;
	private float initialX;
	private float initialY;
	private float offsetX;
	private float offsetY;
	private Paint myPaint;
	private Paint backgroundStroke;
	private AnalogPadInterface analogPadInterface;
	private boolean initiated = false;
	private boolean isActive = false;
	private boolean enabled = true;
	private HelperMethods helperMethods;
	private Bitmap backgroundBMP,knobIdleBMP,knobActiveBMP, analogDisabled;
	private Context context;

	public AnalogPad(Context context, AttributeSet attrs) { 
		super(context, attrs);
		this.context = context;

		backgroundStroke = new Paint();
		backgroundStroke.setColor(Color.BLACK);
		backgroundStroke.setStyle(Paint.Style.STROKE);
		backgroundStroke.setStrokeWidth(5);
		myPaint = new Paint();
		myPaint.setColor(Color.RED);
		myPaint.setAntiAlias(true);

		initialX = getWidth() / 2;
		initialY = getHeight() / 2;
		
		helperMethods = HelperMethods.instance;		
	}

	public void setAnalogPadInterface(AnalogPadInterface analogPadInterface) {
		this.analogPadInterface = analogPadInterface;
	}
	
	
	public void setKnobActiveBitmap(Bitmap knobActiveBMP) {
		this.knobActiveBMP = knobActiveBMP;
	}
	
	public void setBackgroundBitmap(Bitmap backgroundBMP) {
		this.backgroundBMP = backgroundBMP;
	}
	
	public void setKnobIdleBitmap(Bitmap knobIdleBMP) {
		this.knobIdleBMP = knobIdleBMP;
	}
	
	public void setDisabledBitmap(Bitmap analogDisabled) {
		this.analogDisabled = analogDisabled;
	}	
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		invalidate();
	}

	public boolean onTouchEvent(MotionEvent event) {
		if(enabled){
			int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				initialX = x;
				initialY = y;
				offsetX = event.getX();
				offsetY = event.getY();
				isActive = true;
				break;
			case MotionEvent.ACTION_MOVE:
				x = initialX + event.getX() - offsetX;
				y = initialY + event.getY() - offsetY;			
				updateInterface();
				break;
			case MotionEvent.ACTION_UP:
				x = getWidth() / 2;
				y = getHeight() / 2;
				if(analogPadInterface !=null){
					analogPadInterface.analogPadKeyEvent(ANALOG_PAD.KEY_UP);
				}
				isActive = false;
				updateInterface();
				break;
			case MotionEvent.ACTION_CANCEL:
				x = initialX + event.getX() - offsetX;
				y = initialY + event.getY() - offsetY;
				// analogPadInterface.analogPadKeyEvent(ANALOG_PAD.KEY_UP);
				break;
			}	
			return (true);
		}else{
			return (false);
		}
	} 

	private void updateInterface() {
		float xOut = helperMethods.map((long) x,(long) (nobRadius),(long) (getWidth()-nobRadius), NOB_MIN_OUT, NOB_MAX_OUT);
		float yOut = helperMethods.map((long) y, (long) (nobRadius),(long) (getHeight()-nobRadius), NOB_MIN_OUT, NOB_MAX_OUT);
		
		if (xOut<NOB_MIN_OUT){xOut = NOB_MIN_OUT;}
		if (xOut>NOB_MAX_OUT){xOut = NOB_MAX_OUT;}
		if (yOut<NOB_MIN_OUT){yOut = NOB_MIN_OUT;}
		if (yOut>NOB_MAX_OUT){yOut = NOB_MAX_OUT;}
		
		if(analogPadInterface !=null){
			analogPadInterface.analogPadEvent(xOut, yOut);
		}
	}

	public void draw(Canvas canvas) {
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			
			if(enabled){		
				if(backgroundBMP ==null){
					canvas.drawCircle(width/2, height/2, ((width>height)?height:width)/2,
						backgroundStroke);
				}
				else{
					canvas.drawBitmap(backgroundBMP, null, new Rect(0, 0, width, height), null);
				}
				if(!initiated){
					Log.d("CALIBRATION", "getWidth: "+getWidth()+" getHeight: "+getHeight());
					x = getWidth() / 2;
					y = getHeight() / 2;
					
					nobRadius = (float) (getWidth() * NOB_RADIUS_PROPORTION_SIZE);
					initiated = true;
				}
				
				if(x>getWidth()-nobRadius){	x = (getWidth()-nobRadius)-BACKGROUND_OFSET; }		
				if(x<nobRadius){ x = nobRadius + BACKGROUND_OFSET;}		
				if(y>getHeight()-nobRadius){ y = (getHeight()-nobRadius)-BACKGROUND_OFSET; }
				if(y<nobRadius){ y = nobRadius + BACKGROUND_OFSET; }
				
				if(knobIdleBMP==null || knobActiveBMP==null){
					canvas.drawCircle(x, y, nobRadius, myPaint);
				}
				else{
					Rect rect = new Rect();
					rect.left = (int)(x-nobRadius);
					rect.top = (int)(y-nobRadius);
					rect.right = (int)(x+nobRadius);
					rect.bottom = (int)(y+nobRadius);
					
					if(isActive){
						canvas.drawBitmap(knobActiveBMP, null, rect, null);				
					}
					else{
						canvas.drawBitmap(knobIdleBMP, null, rect, null);				
					}
				}
				invalidate();
			}
			else{
				if(backgroundBMP ==null){
					canvas.drawCircle(width/2, height/2, ((width>height)?height:width)/2,
						backgroundStroke);
				}
				else{
					canvas.drawBitmap(analogDisabled, null, new Rect(0, 0, width, height), null);
				}				
			}
	}	
}
