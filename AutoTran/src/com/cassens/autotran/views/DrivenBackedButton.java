package com.cassens.autotran.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.cassens.autotran.R;

@SuppressLint("ClickableViewAccessibility")
public class DrivenBackedButton extends Button implements View.OnTouchListener {
	public static enum Orientation {
		DRIVEN("D"), BACKED("B");
		private final String orientation;

		private Orientation(String o) {
			this.orientation = o;
		}

		public String toString() {
			if (this.orientation.equals("D")) {
				return "Driven";
			} else {
				return "Backed";
			}
		}

		public String getValue() {
			return this.orientation;
		}
	};
	private Orientation state;

	public DrivenBackedButton(Context context) {
        this(context, null);
    }
	
	public DrivenBackedButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.DrivenBackedButton, 0, 0);

		try {
			int stateAttr = a.getInteger(R.styleable.DrivenBackedButton_vehicle_orientation, 0);
			this.state = stateAttr == 1 ? Orientation.BACKED
					: Orientation.DRIVEN;
		} catch (Exception e) {
			this.state = Orientation.DRIVEN;
		} finally {
			a.recycle();
		}

		this.setText(this.state.toString());
		
		this.setOnTouchListener(this);
	}
	
	public Orientation getOrientation() {
		return this.state;
	}
	
	private void render() {
		String format; 
		
		if (this.state == Orientation.BACKED) {
			format = "%s / <b>%s</b>";
		} else {
			format = "<b>%s</b> / %s";
		}
		
		this.setText(Html.fromHtml(String.format(format, Orientation.DRIVEN.toString(), Orientation.BACKED.toString())));
	}

	public void setOrientation(Orientation o) {
		this.state = o;
		this.render();
	}
	
	public void setOrientation(String o) {
		this.state = (o != null && o.equals("B")) ? Orientation.BACKED
				: Orientation.DRIVEN;
		this.render();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
	    switch (event.getAction()) {
	    case MotionEvent.ACTION_UP:
	    	setOrientation(state == Orientation.BACKED ? Orientation.DRIVEN : Orientation.BACKED);
	        break;
	    default:
	        break;
	    }
	    return false;
	}
	
	@Override
	public void setOnTouchListener(OnTouchListener l) {
		if (l.equals(this)) {
			super.setOnTouchListener(l);
		} else {
			throw new IllegalStateException();
		}
    }
}
