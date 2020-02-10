package endexcase.scanmachine.fragments;


import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.Interface.IOnReceiveCharListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.util.DecimalInputFilter;
import endexcase.scanmachine.util.Utils;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * Created by Zack on 15/6/16.
 */
public abstract class BaseFragment extends Fragment implements IOnReceiveCharListener {

    protected TextView tv_header_title;
    protected MainActivity mActivity;
    private SparseArray<IEditTextDoneListener> edittextListeners = new SparseArray<IEditTextDoneListener>();
    
    //Data
    protected AppData mAppdata;
    
    private boolean isDoneClicked, NotEditYet;
    private String mCurFocusStr;
    
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppdata = AppData.getInstance();
		mActivity = (MainActivity) getActivity();
	}
    
    /**
     * 文字控制說明：
     * xml 屬性  inputType決定是否帶負號 imeOptions決定軟件盤enter鍵功能
     * filter 分為小數與整數型filter
     */

	protected OnEditorActionListener onEditorActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            int result = actionId & EditorInfo.IME_MASK_ACTION;
            switch(result) {
            case EditorInfo.IME_ACTION_DONE:
                // done stuff
            	View v = null;
            	try {
            		v = getActivity().getCurrentFocus();
            		if(v instanceof EditText){
                		isDoneClicked = true;
                		v.clearFocus();
                		hideKeyBoard();
                	}
				} catch (Exception e) {
					e.printStackTrace();
				}
            	
                break;
            }
            return true;
        }
    };
    
    protected OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
        	if(v instanceof EditText && !hasFocus){
        		NotEditYet = false;
        		if(!isDoneClicked){
        			//recover original value
        			IEditTextDoneListener item = edittextListeners.get(v.getId());
            		item.onEditTextDone(v.getId(), ((ViewGroup) ((ViewGroup) v.getParent()).getParent()).getId(), mCurFocusStr);
        		}else{
        			//clear flag, and update value
        			isDoneClicked = false;
        			if(!((EditText)v).getText().toString().matches("-?\\d+(\\.\\d+)?")){
            			((EditText)v).setText(mCurFocusStr);
            		}
        			IEditTextDoneListener item = edittextListeners.get(v.getId());
            		item.onEditTextDone(v.getId(), ((ViewGroup) ((ViewGroup) v.getParent()).getParent()).getId(), ((EditText)v).getText().toString());
        		}
        	}else{
        		//cash current data when catch focus
        		if(v instanceof EditText){
        			NotEditYet = true;
        			mCurFocusStr = ((EditText)v).getText().toString();
        			final EditText et = ((EditText)v);
        			et.post(new Runnable() {
						@Override
						public void run() {
							et.setSelection(mCurFocusStr.length());
						}
					});
    			}
        	}
        }
    };

    private void hideKeyBoard(){
		View view=null;
		try {
			view = getActivity().getCurrentFocus();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (view != null) {  
		    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
    
    protected void setTitle(View inflateView, int strId){
        tv_header_title = (TextView) inflateView.findViewById(R.id.tv_header_title);
        tv_header_title.setText(strId);
    }
    
    protected int getDisplayHeight(){
    	Rect frame = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
//        int displayHeight = frame.height();
        int statusBarHeight= frame.top;
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        
        return metrics.heightPixels-statusBarHeight;
    }
    
    
    protected int getGridItemHeight(View view) {
    	RelativeLayout header = (RelativeLayout)view.findViewById(R.id.rl_fragment_header);
    	int headerHeight = header.getLayoutParams().height;
        int ret = Math.round((getDisplayHeight() - headerHeight - Utils.getDimens2Pixel(getActivity(), R.dimen.size_30)) / 5);
        return ret;
    }
    
    protected void registerEdittextDoneListener(EditText editText, IEditTextDoneListener listener){
    	edittextListeners.put(editText.getId(), listener);
    	editText.setOnFocusChangeListener(onFocusChangeListener);
    	editText.setOnEditorActionListener(onEditorActionListener);
    }
    
    protected void unregisterEdittextDoneListener(EditText editText){
    	edittextListeners.remove(editText.getId());
    	editText.setOnFocusChangeListener(null);
    	editText.setOnEditorActionListener(null);
    }
    
    protected DecimalInputFilter getCharReceiveListenerFilter(DecimalInputFilter filter){
    	filter.setOnCharReceiveListener(this);
    	return filter;
    }
    @Override
	public void onReceiveKey(String receiveChar) {
		String Prev_text;
		if(NotEditYet){
			View v = getActivity().getCurrentFocus();
			Prev_text = ((EditText)v).getText().toString();
			try{
				((EditText)v).setText(receiveChar);
				((EditText)v).setSelection(receiveChar.length());
				NotEditYet = false;
			}catch(IndexOutOfBoundsException e){
				//利用setText notMatch 會return null 造成 setSelection 失敗
				//來確認是否符合輸入格式
				e.printStackTrace();
				((EditText)v).setText(Prev_text);
				((EditText)v).setSelection(Prev_text.length());
				NotEditYet = true;
			}
		}
	}
}
