package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.Interface.IOnErrorOccurred;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.DecimalInputFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

/**
 * Created by Zack on 15/6/16.
 */
public class EngineerCCDParamSettingFragment extends BaseFragment implements
		View.OnTouchListener, IEditTextDoneListener, View.OnClickListener, IOnErrorOccurred, IDialogValueListener {
	
	private String TAG = EngineerCCDParamSettingFragment.class.getSimpleName();
	private RelativeLayout btn_header_back;
	private EditText mDist1Left,  mDist2Left, l_et_label_detect;
	private EditText mDist1Right,  mDist2Right, r_et_label_detect;

	private static final int LABEL_SENSOR_AUTO_DETECT_LEFT = 1114;
	private static final int LABEL_SENSOR_AUTO_DETECT_RIGHT = 1115;
	
	private ToggleButton l_toggle_label_detect, r_toggle_label_detect;
	
	private int originSysPage;
	private boolean keepDetect;
	
	Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what){
			case LABEL_SENSOR_AUTO_DETECT_LEFT:
				if(keepDetect){
					sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_LEFT, 1000);
					mActivity.setAutoLeftLabSensor(true);
				}else{
					mActivity.setAutoLeftLabSensor(false);
					if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]!=originSysPage){
						sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_LEFT, 50);
					}else{
						l_toggle_label_detect.setChecked(false);
						mActivity.closeAutoProcessDialog();
						l_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtL_]));
					}
				}
				break;
			case LABEL_SENSOR_AUTO_DETECT_RIGHT:
				if(keepDetect){
					sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_RIGHT, 1000);
					mActivity.setAutoRightLabSensor(true);
					Log.d(TAG, "restart detect");
				}else{
					mActivity.setAutoRightLabSensor(false);
					if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]!=originSysPage){
						sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_RIGHT, 50);
						Log.w(TAG, "wait system page to 2");
					}else{
						r_toggle_label_detect.setChecked(false);
						mActivity.closeAutoProcessDialog();
						r_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtR_]));
					}
				}
				break;
			}
		}
		
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_engineering_ccd_param_setting,
				container, false);
		init(view);
		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.engineer_page_menu_title_ccd_param);
		btn_header_back = (RelativeLayout)view.findViewById(R.id.btn_header_back);
		btn_header_back.setOnTouchListener(this);
		
		mDist1Left = (EditText)view.findViewById(R.id.et_print_time_left);
		mDist2Left = (EditText)view.findViewById(R.id.et_title_print_speed_left);
		
		registerEdittextDoneListener(mDist1Left, this);
		registerEdittextDoneListener(mDist2Left, this);
		
		mDist1Right = (EditText)view.findViewById(R.id.et_print_time_right);
		mDist2Right = (EditText)view.findViewById(R.id.et_title_print_speed_right);
		
		registerEdittextDoneListener(mDist1Right, this);
		registerEdittextDoneListener(mDist2Right, this);
		
		DecimalInputFilter ccdFilter = getCharReceiveListenerFilter(new DecimalInputFilter(0.0, 600.00, 3, 2));
		mDist1Left.setFilters(new InputFilter[]{ccdFilter});
		mDist2Left.setFilters(new InputFilter[]{ccdFilter});
		mDist1Right.setFilters(new InputFilter[]{ccdFilter});
		mDist2Right.setFilters(new InputFilter[]{ccdFilter});
		
		l_toggle_label_detect = (ToggleButton)view.findViewById(R.id.toggle_label_detect_left);
    	r_toggle_label_detect = (ToggleButton)view.findViewById(R.id.toggle_label_detect_right);
    	l_toggle_label_detect.setOnClickListener(this);
    	r_toggle_label_detect.setOnClickListener(this);
    	l_et_label_detect = (EditText)view.findViewById(R.id.et_label_detect_left);
    	r_et_label_detect = (EditText)view.findViewById(R.id.et_label_detect_right);
    	
    	DecimalInputFilter filter3 = getCharReceiveListenerFilter(new DecimalInputFilter(0, 255, 3, 0));
        l_et_label_detect.setFilters(new InputFilter[]{filter3});
        r_et_label_detect.setFilters(new InputFilter[]{filter3});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void onResume() {
		super.onResume();
		mDist1Left.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ObjSenToPeelDistL_]/100));
		mDist2Left.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSenToPeelDistL_]/100));
		
		mDist1Right.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ObjSenToPeelDistR_]/100));
		mDist2Right.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSenToPeelDistR_]/100));
		
		l_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtL_]));
		r_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtR_]));
		
		if(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_MainBoardIONoMatch_)) {
	    	l_toggle_label_detect.setEnabled(false);
	    	r_toggle_label_detect.setEnabled(false);
	    }
	}

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		if(retValue == null || retValue.length() <= 0)
			return;

		switch(viewId) {
		case R.id.et_print_time_left:
			mDist1Left.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setLeftDist1((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_print_speed_left:
			mDist2Left.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setLeftDist2((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_print_time_right:
			mDist1Right.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setRightDist1((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_print_speed_right:
			mDist2Right.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setRightDist2((int) (Double.valueOf(retValue)*100));
			break;
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(v.getId()){
		case R.id.btn_header_back:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				 v.setPressed(true);
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				mActivity.showFragment(R.id.btn_header_secret);
				 v.setPressed(false);
				return true;
			}
			break;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.toggle_label_detect_right:
			mActivity.showAutoProcessDialog(this);
			originSysPage = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_];
			mHandler.sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_RIGHT, 1000);
			mActivity.setAutoRightLabSensor(true);
			keepDetect = true;
			break;
		case R.id.toggle_label_detect_left:
			mActivity.showAutoProcessDialog(this);
			originSysPage = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_];
			mHandler.sendEmptyMessageDelayed(LABEL_SENSOR_AUTO_DETECT_LEFT, 1000);
			mActivity.setAutoLeftLabSensor(true);
			keepDetect = true;
			break;
		}
	}

	@Override
	public void onReturnValue(String ret) {
		Log.d(TAG, "onReturnValue:"+ret);
		if(ret.equals("cancel")){
		}else if(ret.equals("onDialogTouch")){
			keepDetect = false;
			if(mHandler.hasMessages(LABEL_SENSOR_AUTO_DETECT_LEFT)){
				mHandler.removeMessages(LABEL_SENSOR_AUTO_DETECT_LEFT);
				mHandler.sendEmptyMessage(LABEL_SENSOR_AUTO_DETECT_LEFT);
			}else if(mHandler.hasMessages(LABEL_SENSOR_AUTO_DETECT_RIGHT)){
				mHandler.removeMessages(LABEL_SENSOR_AUTO_DETECT_RIGHT);
				mHandler.sendEmptyMessage(LABEL_SENSOR_AUTO_DETECT_RIGHT);
			}
			
		}
	}
	
	@Override
	public void onErrorOccurred() {
		cancelAllAutoDetectProcess();
	}

	private void cancelAllAutoDetectProcess() {
		mHandler.removeMessages(LABEL_SENSOR_AUTO_DETECT_LEFT);
		mHandler.removeMessages(LABEL_SENSOR_AUTO_DETECT_RIGHT);
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
		    	l_toggle_label_detect.setChecked(false);
		    	r_toggle_label_detect.setChecked(false);
			}
		});
		
	}

}
