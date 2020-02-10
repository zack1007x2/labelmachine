package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.DecimalInputFilter;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * Created by Zack on 15/6/16.
 */
public class EngineerSpeedParamSettingFragment extends BaseFragment implements
		View.OnTouchListener, IEditTextDoneListener{
	private static final AppLog smAppLog = new AppLog("EngineerSpeedParamSettingFragment",EngineerSpeedParamSettingFragment.class);
	
	private RelativeLayout btn_header_back;
	private EditText mMaxSpdLeft, mStartSpdLeft, mStopSpdLeft, mParAccLeft, mStepAccLeft, mParDecLeft, mStepDecLeft;
	private EditText mMaxSpdRight, mStartSpdRight, mStopSpdRight, mParAccRight, mStepAccRight, mParDecRight, mStepDecRight;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_engineering_speed_param_setting,
				container, false);
		init(view);

		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.engineer_page_menu_title_speed_param);
		btn_header_back = (RelativeLayout)view.findViewById(R.id.btn_header_back);
		btn_header_back.setOnTouchListener(this);
		
		mMaxSpdLeft = (EditText)view.findViewById(R.id.et_max_spd_left);
		mStartSpdLeft = (EditText)view.findViewById(R.id.et_start_spd_left);
		mStopSpdLeft = (EditText)view.findViewById(R.id.et_stop_spd_left);
		mParAccLeft = (EditText)view.findViewById(R.id.et_par_acc_left);
		mStepAccLeft = (EditText)view.findViewById(R.id.et_step_acc_left);
		mParDecLeft = (EditText)view.findViewById(R.id.et_par_dec_left);
		mStepDecLeft = (EditText)view.findViewById(R.id.et_step_dec_left);
		
		registerEdittextDoneListener(mMaxSpdLeft, this);
		registerEdittextDoneListener(mStartSpdLeft, this);
		registerEdittextDoneListener(mStopSpdLeft, this);
		registerEdittextDoneListener(mParAccLeft, this);
		registerEdittextDoneListener(mStepAccLeft, this);
		registerEdittextDoneListener(mParDecLeft, this);
		registerEdittextDoneListener(mStepDecLeft, this);
		
		mMaxSpdRight = (EditText)view.findViewById(R.id.et_max_spd_right);
		mStartSpdRight = (EditText)view.findViewById(R.id.et_start_spd_right);
		mStopSpdRight = (EditText)view.findViewById(R.id.et_stop_spd_right);
		mParAccRight = (EditText)view.findViewById(R.id.et_par_acc_right);
		mStepAccRight = (EditText)view.findViewById(R.id.et_step_acc_right);
		mParDecRight = (EditText)view.findViewById(R.id.et_par_dec_right);
		mStepDecRight = (EditText)view.findViewById(R.id.et_step_dec_right);
		
		registerEdittextDoneListener(mMaxSpdRight, this);
		registerEdittextDoneListener(mStartSpdRight, this);
		registerEdittextDoneListener(mStopSpdRight, this);
		registerEdittextDoneListener(mParAccRight, this);
		registerEdittextDoneListener(mStepAccRight, this);
		registerEdittextDoneListener(mParDecRight, this);
		registerEdittextDoneListener(mStepDecRight, this);
		
		DecimalInputFilter filterMaxSpd = getCharReceiveListenerFilter(new DecimalInputFilter(0, 20000, 5, 0));//1000~20000
		mMaxSpdLeft.setFilters(new InputFilter[]{filterMaxSpd});
		mMaxSpdRight.setFilters(new InputFilter[]{filterMaxSpd});
		DecimalInputFilter filterStartStopSpd = getCharReceiveListenerFilter(new DecimalInputFilter(0, 2000, 4, 0));//100~2000
		mStartSpdLeft.setFilters(new InputFilter[]{filterStartStopSpd});
		mStopSpdLeft.setFilters(new InputFilter[]{filterStartStopSpd});
		mStartSpdRight.setFilters(new InputFilter[]{filterStartStopSpd});
		mStopSpdRight.setFilters(new InputFilter[]{filterStartStopSpd});
		DecimalInputFilter filterPar = getCharReceiveListenerFilter(new DecimalInputFilter(0, 20, 2, 0));
		mParAccLeft.setFilters(new InputFilter[]{filterPar});
		mParAccRight.setFilters(new InputFilter[]{filterPar});
		mParDecLeft.setFilters(new InputFilter[]{filterPar});
		mParDecRight.setFilters(new InputFilter[]{filterPar});
		DecimalInputFilter filterStep = getCharReceiveListenerFilter(new DecimalInputFilter(0, 500, 3, 0));//5~500
		mStepAccLeft.setFilters(new InputFilter[]{filterStep});
		mStepAccRight.setFilters(new InputFilter[]{filterStep});
		mStepDecLeft.setFilters(new InputFilter[]{filterStep});
		mStepDecRight.setFilters(new InputFilter[]{filterStep});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void onResume() {
		super.onResume();
		
		mMaxSpdLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1TopSpd_]));
		mStartSpdLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1StartSpd_]));
		mStopSpdLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1StopSpd_]));
		mParAccLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1UpRate_]));
		mStepAccLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1UpScale_]));
		mParDecLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1DnRate_]));
		mStepDecLeft.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S1DnScale_]));
		
		mMaxSpdRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2TopSpd_]));
		mStartSpdRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2StartSpd_]));
		mStopSpdRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2StopSpd_]));
		mParAccRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2UpRate_]));
		mStepAccRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2UpScale_]));
		mParDecRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2DnRate_]));
		mStepDecRight.setText(Integer.toString(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_S2DnScale_]));
	}


	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		if(retValue == null || retValue.length() <= 0)
			return;

		switch(viewId) {
		case R.id.et_max_spd_left:
			if(Integer.parseInt(retValue)<1000)
				retValue = "1000";
			mMaxSpdLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1TopSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_start_spd_left:
			if(Integer.parseInt(retValue)<100)
				retValue = "100";
			mStartSpdLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1StartSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_stop_spd_left:
			if(Integer.parseInt(retValue)<100)
				retValue = "100";
			mStopSpdLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1StopSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_par_acc_left:
			mParAccLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1UpRate(Integer.parseInt(retValue));
			break;
		case R.id.et_step_acc_left:
			if(Integer.valueOf(retValue)<5){
				retValue="5";
			}
			mStepAccLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1UpScale(Integer.parseInt(retValue));
			break;
		case R.id.et_par_dec_left:
			mParDecLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1DnRate(Integer.parseInt(retValue));
			break;
		case R.id.et_step_dec_left:
			if(Integer.valueOf(retValue)<5){
				retValue="5";
			}
			mStepDecLeft.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS1DnScale(Integer.parseInt(retValue));
			break;
		case R.id.et_max_spd_right:
			if(Integer.parseInt(retValue)<1000)
				retValue = "1000";
			mMaxSpdRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2TopSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_start_spd_right:
			if(Integer.parseInt(retValue)<100)
				retValue = "100";
			mStartSpdRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2StartSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_stop_spd_right:
			if(Integer.parseInt(retValue)<100)
				retValue = "100";
			mStopSpdRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2StopSpeed(Integer.parseInt(retValue));
			break;
		case R.id.et_par_acc_right:
			mParAccRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2UpRate(Integer.parseInt(retValue));
			break;
		case R.id.et_step_acc_right:
			if(Integer.valueOf(retValue)<5){
				retValue="5";
			}
			mStepAccRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2UpScale(Integer.parseInt(retValue));
			break;
		case R.id.et_par_dec_right:
			mParDecRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2DnRate(Integer.parseInt(retValue));
			break;
		case R.id.et_step_dec_right:
			if(Integer.valueOf(retValue)<5){
				retValue="5";
			}
			mStepDecRight.setText(String.format("%d",Integer.parseInt(retValue)));
			mActivity.setS2DnScale(Integer.parseInt(retValue));
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

}
