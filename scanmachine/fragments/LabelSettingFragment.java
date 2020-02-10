package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.Interface.IOnErrorOccurred;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.DecimalInputFilter;
import endexcase.scanmachine.util.Utils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.percent.PercentRelativeLayout;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.ToggleButton;
/**
 * Created by Zack on 15/6/16.
 */
public class LabelSettingFragment extends BaseFragment implements View.OnClickListener, IEditTextDoneListener ,IOnErrorOccurred, IDialogValueListener{
	
	private final static String TAG = LabelSettingFragment.class.getSimpleName();
	private PercentRelativeLayout prl_left, prl_right, prl_table_content_left, prl_table_content_right, prl_main_frame_table;	 
	private EditText et_input_botdiameter;
	private ToggleButton toggle_botdiameter;
	//left
	private EditText l_et_label_length, l_et_paste_speed, l_et_label_leave_length, l_et_paste_position, l_et_label_check, l_et_label_detect;
	private ToggleButton l_toggle_label_length;
	private Switch switch_label_setting_left;
	//right
	private EditText r_et_label_length, r_et_paste_speed, r_et_label_leave_length, r_et_paste_position, r_et_label_check, r_et_label_detect;
	private ToggleButton r_toggle_label_length;
	private Switch switch_label_setting_right;
	
	//date
	private int originSysPage;
	private boolean keepDetect;
	
	//handler what
	private static final int BOTTLE_DIA_AUTO_DETECT = 1111;
	private static final int LABEL_LENGTH_AUTO_DETECT_FEFT = 1112;
	private static final int LABEL_LENGTH_AUTO_DETECT_RIGHT = 1113;
	
	private RelativeLayout ll_header_bar;
	
	//Auto檢測流程
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
			case BOTTLE_DIA_AUTO_DETECT:
				if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]!=originSysPage){
					sendEmptyMessageDelayed(BOTTLE_DIA_AUTO_DETECT, 200);
				}else{
					et_input_botdiameter.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_]/10.0));
					toggle_botdiameter.setChecked(false);
					mActivity.closeAutoProcessDialog();
				}
				break;
			case LABEL_LENGTH_AUTO_DETECT_FEFT:
				if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]!=originSysPage){
					sendEmptyMessageDelayed(LABEL_LENGTH_AUTO_DETECT_FEFT, 200);
				}else{
					l_toggle_label_length.setChecked(false);
					mActivity.closeAutoProcessDialog();
					l_et_label_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthL_]/10.0));
				}
				break;
			case LABEL_LENGTH_AUTO_DETECT_RIGHT:
				if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]!=originSysPage){
					sendEmptyMessageDelayed(LABEL_LENGTH_AUTO_DETECT_RIGHT, 200);
				}else{
					r_toggle_label_length.setChecked(false);
					mActivity.closeAutoProcessDialog();
					r_et_label_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthR_]/10.0));
				}
				break;
			}
		}
		
	};
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lable_setting, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        super.setTitle(view, R.string.main_side_text_tag_setting);
        ll_header_bar = (RelativeLayout)view.findViewById(R.id.rl_fragment_header);
        prl_main_frame_table = (PercentRelativeLayout)view.findViewById(R.id.main_frame_table);
        prl_left = (PercentRelativeLayout)view.findViewById(R.id.prl_left_main_table);
        prl_right = (PercentRelativeLayout)view.findViewById(R.id.prl_right_main_table);
        prl_table_content_left = (PercentRelativeLayout)prl_left.findViewById(R.id.label_setting_table_content_left);
        prl_table_content_right = (PercentRelativeLayout)prl_right.findViewById(R.id.label_setting_table_content_right);
        initEditText(view);
        intiToggle(view);
    }

    private void intiToggle(View view) {
    	toggle_botdiameter = (ToggleButton)view.findViewById(R.id.toggle_botdiameter);
    	l_toggle_label_length = (ToggleButton)prl_left.findViewById(R.id.toggle_label_length);
    	r_toggle_label_length = (ToggleButton)prl_right.findViewById(R.id.toggle_label_length);
    	toggle_botdiameter.setOnClickListener(this);
    	l_toggle_label_length.setOnClickListener(this);
    	r_toggle_label_length.setOnClickListener(this);
    	
    	switch_label_setting_left = (Switch) prl_left.findViewById(R.id.switch_label_setting_left);
    	switch_label_setting_right = (Switch) prl_right.findViewById(R.id.switch_label_setting_right);
	}

	private void initEditText(View view) {
    	et_input_botdiameter = (EditText)view.findViewById(R.id.et_input_botdiameter);
        l_et_label_length = (EditText)prl_left.findViewById(R.id.et_label_length);
        l_et_paste_speed = (EditText)prl_left.findViewById(R.id.et_paste_speed);
        l_et_label_leave_length = (EditText)prl_left.findViewById(R.id.et_label_leave_length);
        l_et_paste_position = (EditText)prl_left.findViewById(R.id.et_paste_position);
        l_et_label_check = (EditText)prl_left.findViewById(R.id.et_label_check);
        l_et_label_detect = (EditText)prl_left.findViewById(R.id.et_label_detect);
        r_et_label_length = (EditText)prl_right.findViewById(R.id.et_label_length);
        r_et_paste_speed = (EditText)prl_right.findViewById(R.id.et_paste_speed);
        r_et_label_leave_length = (EditText)prl_right.findViewById(R.id.et_label_leave_length);
        r_et_paste_position = (EditText)prl_right.findViewById(R.id.et_paste_position);
        r_et_label_check = (EditText)prl_right.findViewById(R.id.et_label_check);
        r_et_label_detect = (EditText)prl_right.findViewById(R.id.et_label_detect);
        
        registerEdittextDoneListener(et_input_botdiameter, this);
        registerEdittextDoneListener(l_et_label_length, this);
        registerEdittextDoneListener(l_et_paste_speed, this);
        registerEdittextDoneListener(l_et_label_leave_length, this);
        registerEdittextDoneListener(l_et_paste_position, this);
        registerEdittextDoneListener(l_et_label_check, this);
        registerEdittextDoneListener(l_et_label_detect, this);
        
        registerEdittextDoneListener(r_et_label_length, this);
        registerEdittextDoneListener(r_et_paste_speed, this);
        registerEdittextDoneListener(r_et_label_leave_length, this);
        registerEdittextDoneListener(r_et_paste_position, this);
        registerEdittextDoneListener(r_et_label_check, this);
        registerEdittextDoneListener(r_et_label_detect, this);
        
        //input range
        et_input_botdiameter.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(1000, 0, 4, 1))});
        DecimalInputFilter filter1 = getCharReceiveListenerFilter(new DecimalInputFilter(3, 1));
        l_et_label_length.setFilters(new InputFilter[]{filter1});
        r_et_label_length.setFilters(new InputFilter[]{filter1});
        l_et_label_leave_length.setFilters(new InputFilter[]{filter1});
        r_et_label_leave_length.setFilters(new InputFilter[]{filter1});
        l_et_paste_position.setFilters(new InputFilter[]{filter1});
        r_et_paste_position.setFilters(new InputFilter[]{filter1});
        
        DecimalInputFilter filter2 = getCharReceiveListenerFilter(new DecimalInputFilter(3, 0));
        l_et_paste_speed.setFilters(new InputFilter[]{filter2});
        r_et_paste_speed.setFilters(new InputFilter[]{filter2});
        
        DecimalInputFilter filter3 = getCharReceiveListenerFilter(new DecimalInputFilter(0, 255, 3, 0));
        l_et_label_check.setFilters(new InputFilter[]{filter3});
        l_et_label_detect.setFilters(new InputFilter[]{filter3});
        r_et_label_check.setFilters(new InputFilter[]{filter3});
        r_et_label_detect.setFilters(new InputFilter[]{filter3});
	}

	private void refreshValue() {
		//switch
		switch_label_setting_left.setOnCheckedChangeListener(null);
    	switch_label_setting_right.setOnCheckedChangeListener(null);
		boolean isLeftCheck = mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_LabEnable_);
		boolean isRightCheck = mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_);
		switch_label_setting_left.setChecked(isLeftCheck);
		switch_label_setting_right.setChecked(isRightCheck);
		//activate view state
		updateLeftTableState(switch_label_setting_left.isChecked());
		updateRightTableState(switch_label_setting_right.isChecked());
		
		//Edittext
		et_input_botdiameter.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_]/10.0));
		
		l_et_label_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthL_]/10.0));
        l_et_paste_speed.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_]));
        l_et_label_leave_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLeaveLengthL_]/10.0));
        l_et_paste_position.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionL_]/10.0));
        l_et_label_check.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperSetL_]));
        l_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtL_]));
        
        r_et_label_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthR_]/10.0));
        r_et_paste_speed.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_]));
        r_et_label_leave_length.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLeaveLengthR_]/10.0));
        r_et_paste_position.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionR_]/10.0));
        r_et_label_check.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperSetR_]));
        r_et_label_detect.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtR_]));
        
        int multiHeadState = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CvcState_];
        if(multiHeadState == Consts.SINGLE_PRINT_HEAD){	//0x0300 單頭
        	updateLeftTableState(false);
        	switch_label_setting_left.setEnabled(false);
        }
        
        //關閉自動檢測
        if(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_MainBoardIONoMatch_)) {
        	toggle_botdiameter.setEnabled(false);
        	l_toggle_label_length.setEnabled(false);
        	r_toggle_label_length.setEnabled(false);
        }
        switch_label_setting_left.setOnCheckedChangeListener(onSwitchChangeListener);
    	switch_label_setting_right.setOnCheckedChangeListener(onSwitchChangeListener);
       
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

	@Override
    public void onResume() {
        super.onResume();
        refreshValue();
        if(mActivity.isMachineRunning()){
        	Utils.recursiveEnableView(prl_main_frame_table, false);
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_r_s);
        }else{
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_s);
        }
    }


    @Override
    public void onClick(View v) {
    	int parentId = ((PercentRelativeLayout) ((ViewGroup) v.getParent()).getParent()).getId();
		switch(v.getId()){
			case R.id.toggle_botdiameter:
					mActivity.showAutoProcessDialog(null);
					originSysPage = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_];
					mHandler.sendEmptyMessageDelayed(BOTTLE_DIA_AUTO_DETECT, 1000);
					mActivity.setAutoBottleDiameter();
				break;
			case R.id.toggle_label_length:
				mActivity.showAutoProcessDialog(null);
				originSysPage = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_];
				if(parentId==prl_table_content_left.getId()){
					mHandler.sendEmptyMessageDelayed(LABEL_LENGTH_AUTO_DETECT_FEFT, 1000);
					mActivity.setAutoLeftLabLength();
				}else{
					mHandler.sendEmptyMessageDelayed(LABEL_LENGTH_AUTO_DETECT_RIGHT, 1000);
					mActivity.setAutoRightLabLength();
				}
				break;
		}
    }
    
    private OnCheckedChangeListener onSwitchChangeListener = new OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			switch(buttonView.getId()){
				case R.id.switch_label_setting_left:
					updateLeftTableState(switch_label_setting_left.isChecked());
					mActivity.setLeftLabEnable(switch_label_setting_left.isChecked());
					break;
				case R.id.switch_label_setting_right:
					updateRightTableState(switch_label_setting_right.isChecked());
					mActivity.setRightLabEnable(switch_label_setting_right.isChecked());
					break;
			}
			
		}
    	
    };

	private void updateLeftTableState(boolean isChecked) {
		/**
		 * seems duplicateParentState only work for child's visual styling not their behavior
		 * so, we still need to setEnable to child views
		 */
		prl_table_content_left.setEnabled(isChecked);
		l_et_label_length.setEnabled(isChecked);
        l_et_paste_speed.setEnabled(isChecked);
        l_et_label_leave_length.setEnabled(isChecked);
        l_et_paste_position.setEnabled(isChecked);
        l_et_label_check.setEnabled(isChecked);
        l_toggle_label_length.setClickable(isChecked);
	}

	private void updateRightTableState(boolean isChecked) {
		prl_table_content_right.setEnabled(isChecked);
		r_et_label_length.setEnabled(isChecked);
        r_et_paste_speed.setEnabled(isChecked);
        r_et_label_leave_length.setEnabled(isChecked);
        r_et_paste_position.setEnabled(isChecked);
        r_et_label_check.setEnabled(isChecked);
        r_toggle_label_length.setClickable(isChecked);
	}

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
			switch(viewId){
				case R.id.et_input_botdiameter:
					float BotDiameter = Float.valueOf(et_input_botdiameter.getText().toString());
					mActivity.setManualBottleDiameter((int)(BotDiameter*10));
					et_input_botdiameter.setText(String.format("%.1f",mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_]/10.0));
					break;
				case R.id.et_label_length:
					if(parentId==prl_table_content_left.getId()){
						float val = Float.valueOf(retValue);
						l_et_label_length.setText(String.format("%.1f", val));
						mActivity.setLeftLabLength((int)(val*10));
					}else{
						float val = Float.valueOf(retValue);
						r_et_label_length.setText(String.format("%.1f", val));
						mActivity.setRightLabLength((int)(val*10));
					}
					break;
				case R.id.et_paste_speed:
					if(parentId==prl_table_content_left.getId()){
						int val = Integer.valueOf(retValue);
						l_et_paste_speed.setText(String.format("%d",val));
						mActivity.setLeftLabSpeed(val);
					}else{
						int val = Integer.valueOf(retValue);
						r_et_paste_speed.setText(String.format("%d",val));
						mActivity.setRightLabSpeed(val);
					}
					break;
				case R.id.et_label_leave_length:
					if(parentId==prl_table_content_left.getId()){
						float val = Float.valueOf(retValue);
						l_et_label_leave_length.setText(String.format("%.1f", val));
						mActivity.setLeftLabLeaveLength((int)(val*10));
					}else{
						float val = Float.valueOf(retValue);
						r_et_label_leave_length.setText(String.format("%.1f", val));
						mActivity.setRightLabLeaveLength((int)(val*10));
					}
					break;
				case R.id.et_paste_position:
					if(parentId==prl_table_content_left.getId()){
						float val = Float.valueOf(retValue);
						l_et_paste_position.setText(String.format("%.1f", val));
						mActivity.setLeftLabPosition((int)(val*10));
					}else{
						float val = Float.valueOf(retValue);
						r_et_paste_position.setText(String.format("%.1f", val));
						mActivity.setRightLabPosition((int)(val*10));
					}
					break;
				case R.id.et_label_check:
					if(parentId==prl_table_content_left.getId()){
						int val = Integer.valueOf(retValue);
						l_et_label_check.setText(String.format("%d", val));
						mActivity.setLeftLabPaper(val);
					}else{
						int val = Integer.valueOf(retValue);
						r_et_label_check.setText(String.format("%d", val));
						mActivity.setRightLabPaper(val);
					}
					break;
			}
			
	}
	
	

	@Override
	public void onErrorOccurred() {
		cancelAllAutoDetectProcess();
	}

	private void cancelAllAutoDetectProcess() {
		mHandler.removeMessages(BOTTLE_DIA_AUTO_DETECT);
		mHandler.removeMessages(LABEL_LENGTH_AUTO_DETECT_FEFT);
		mHandler.removeMessages(LABEL_LENGTH_AUTO_DETECT_RIGHT);
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				toggle_botdiameter.setChecked(false);
		    	l_toggle_label_length.setChecked(false);
		    	r_toggle_label_length.setChecked(false);
			}
		});
		
	}
	
	@Override
	public void onReturnValue(String ret) {
		Log.d(TAG, "onReturnValue:"+ret);
		if(ret.equals("cancel")){
		}else if(ret.equals("onDialogTouch")){
			keepDetect = false;
		}
	}
}
