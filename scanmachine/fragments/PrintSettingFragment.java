package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.DecimalInputFilter;
import endexcase.scanmachine.util.Utils;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;
import android.support.percent.PercentRelativeLayout;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Created by Zack on 15/6/16.
 */
public class PrintSettingFragment extends BaseFragment implements OnClickListener, IEditTextDoneListener {

	private PercentRelativeLayout ll_main_frame_table;
	private ToggleButton toggle_printhead_status_left,toggle_printhead_status_right, toggle_speedup;
	private Button btn_print_function, btn_print_option;
	private EditText et_print_time, et_title_print_speed, et_title_printer_stop_amount;
	private int curDetectType;
	private boolean curPrintOpt;
	private String[] mDectectTypeArr;
	private int[] mDectectTypeDrawableArr={R.color.color_text_input_gray_n, R.drawable.g2_btn_stop, R.drawable.g2_btn_aler};
	private RelativeLayout rl_print_opt;
	private RelativeLayout ll_header_bar;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_print_setting, container, false);
        init(view);
        return view;
    }

    private void refreshValue() {
    	toggle_printhead_status_left.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_TypingEnable_));
        toggle_printhead_status_right.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_TypingEnable2_));
        toggle_speedup.setChecked((mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TypeAccelerate_]==1)?true:false);
        curPrintOpt = mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_DetectTpye_SelDevice_);
        btn_print_option.setText(curPrintOpt?R.string.common_right:R.string.common_left);

        et_print_time.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PrintTime_]));
        et_title_print_speed.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TypeHasten_]));
        et_title_printer_stop_amount.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TypeChk_]));
        
        updateBtnState();
        
        int multiHeadState = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CvcState_];
        if(multiHeadState == Consts.SINGLE_PRINT_HEAD){	//0x0300 單頭
        	toggle_printhead_status_left.setEnabled(false);
        	Utils.recursiveEnableView(rl_print_opt, false);
        }
	}

	private void updateBtnState() {
		btn_print_function.setText(mDectectTypeArr[curDetectType]);
        btn_print_function.setBackgroundResource(mDectectTypeDrawableArr[curDetectType]);
	}

	private void init(View view) {
        super.setTitle(view, R.string.main_side_text_print_setting);
        ll_header_bar = (RelativeLayout)view.findViewById(R.id.rl_fragment_header);
        ll_main_frame_table = (PercentRelativeLayout)view.findViewById(R.id.main_frame_table);
        curDetectType = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_DetectTpye_Select_];
        mDectectTypeArr = getResources().getStringArray(R.array.detect_type);
        
        //toggle
        toggle_printhead_status_left = (ToggleButton)view.findViewById(R.id.toggle_printhead_status_left);
        toggle_printhead_status_right = (ToggleButton)view.findViewById(R.id.toggle_printhead_status_right);
        toggle_speedup = (ToggleButton)view.findViewById(R.id.toggle_speedup);
        btn_print_option = (Button)view.findViewById(R.id.btn_print_option);
        
        toggle_printhead_status_left.setOnClickListener(this);
        toggle_printhead_status_right.setOnClickListener(this);
        toggle_speedup.setOnClickListener(this);
        btn_print_option.setOnClickListener(this);
        
        
        //edittext
        et_print_time = (EditText)view.findViewById(R.id.et_print_time);
        et_title_print_speed = (EditText)view.findViewById(R.id.et_title_print_speed);
        et_title_printer_stop_amount = (EditText)view.findViewById(R.id.et_title_printer_stop_amount);
        
        registerEdittextDoneListener(et_print_time, this);
        registerEdittextDoneListener(et_title_print_speed, this);
        registerEdittextDoneListener(et_title_printer_stop_amount, this);
        
      //input range
        et_print_time.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 999, 3, 0))});
        et_title_print_speed.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 50, 2, 0))});
        et_title_printer_stop_amount.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 20, 2, 0))});
        
        //button
        btn_print_function = (Button)view.findViewById(R.id.btn_print_function);
        btn_print_function.setOnClickListener(this);
        
        rl_print_opt = (RelativeLayout) view.findViewById(R.id.rl_print_option);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }

    public void onResume() {
        super.onResume();
        refreshValue();
        if(mActivity.isMachineRunning()){
        	Utils.recursiveEnableView(ll_main_frame_table, false);
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_r_s);
        }else{
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_s);
        }
    }

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		switch(viewId){
		case R.id.et_print_time:
			et_print_time.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setTypingPrintTime(Integer.parseInt(retValue));
			break;
		case R.id.et_title_print_speed:
			et_title_print_speed.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setTypingHasten(Integer.parseInt(retValue));
			break;
		case R.id.et_title_printer_stop_amount:
			et_title_printer_stop_amount.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setTypeCheck(Integer.parseInt(retValue));
			break;
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_print_function:
			curDetectType++;
			if(curDetectType>2){
				curDetectType=0;
			}
			updateBtnState();
			mActivity.setDetectTypeSelect(curDetectType);
			break;
		case R.id.toggle_printhead_status_left:
			mActivity.setLeftTypingEnable(toggle_printhead_status_left.isChecked());
			break;
		case R.id.toggle_printhead_status_right:
			mActivity.setRightTypingEnable(toggle_printhead_status_right.isChecked());
			break;
		case R.id.toggle_speedup:
			mActivity.setTypingAccelerate(toggle_speedup.isChecked()?1:0);
			break;
		case R.id.btn_print_option:
			curPrintOpt = !curPrintOpt;
			btn_print_option.setText(curPrintOpt?R.string.common_right:R.string.common_left);
			mActivity.setDetectTypeSelDevice(curPrintOpt);
			break;
		}
	}

}
