package endexcase.scanmachine.widget;

import com.endex.ce60.R;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.Button;

public class MainMonitorButton extends Button{
	
	private static final int[] STATE_WAITING = {R.attr.state_waiting};
	
	private boolean mIsWaiting = false;
	private int waitingStringId;
	private int initStringId;
	
	private Handler mHandler = new Handler();
	
	public MainMonitorButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	public boolean isWaiting() {
		return mIsWaiting;
	}
	
	public void setWaitingStringId(int waitingStringId) {
		this.waitingStringId = waitingStringId;
	}

	public void setInitStringId(int initStringId) {
		this.initStringId = initStringId;
	}


	public void setIsWaiting(boolean mIsWaiting) {
		this.mIsWaiting = mIsWaiting;
		if(mIsWaiting){
			setText(waitingStringId);		
		}else{
			setText(initStringId);		
		}
		mHandler.post(new Runnable() {
            @Override
            public void run() {
                refreshDrawableState();
            }
        });
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	    if (mIsWaiting) {
	        mergeDrawableStates(drawableState, STATE_WAITING);
	    }
	    return drawableState;
	}
	
}
