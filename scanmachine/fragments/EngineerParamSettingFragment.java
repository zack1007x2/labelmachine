package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.DecimalInputFilter;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;

/**
 * Created by Zack on 15/6/16.
 */
public class EngineerParamSettingFragment extends BaseFragment implements
		View.OnClickListener, IEditTextDoneListener,View.OnTouchListener {

	private RelativeLayout btn_header_back;
	private EditText mEdLd1,  mEdLd2,  mEdWad1;
	private EditText mEdRot1,  mEdRot2,  mEdCd1;
	private EditText mEdWecd,  mEdEcd;
	private Switch mSwPwean, mSwRoolen;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_engineering_param_setting,
				container, false);
		init(view);

		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.engineer_page_menu_title_engineering_param);
		btn_header_back = (RelativeLayout)view.findViewById(R.id.btn_header_back);
		btn_header_back.setOnTouchListener(this);
		
		DecimalInputFilter filter1 = getCharReceiveListenerFilter(new DecimalInputFilter(2,2));
		DecimalInputFilter filter2 = getCharReceiveListenerFilter(new DecimalInputFilter(4, 0));
		
		mEdLd1 = (EditText)view.findViewById(R.id.et_title_ld1);
		mEdLd2 = (EditText)view.findViewById(R.id.et_title_ld2);
		mEdWad1 = (EditText)view.findViewById(R.id.et_title_wad1);
		
		registerEdittextDoneListener(mEdLd1, this);
		registerEdittextDoneListener(mEdLd2, this);
		registerEdittextDoneListener(mEdWad1, this);
		
		mEdLd1.setFilters(new InputFilter[]{filter1});
		mEdLd2.setFilters(new InputFilter[]{filter1});
		mEdWad1.setFilters(new InputFilter[]{filter1});
		
		mEdRot1 = (EditText)view.findViewById(R.id.et_title_rot1);
		mEdRot2 = (EditText)view.findViewById(R.id.et_title_rot2);
		mEdCd1 = (EditText)view.findViewById(R.id.et_title_cd1);
		
		registerEdittextDoneListener(mEdRot1, this);
		registerEdittextDoneListener(mEdRot2, this);
		registerEdittextDoneListener(mEdCd1, this);
		
		mEdRot1.setFilters(new InputFilter[]{filter2});
		mEdRot2.setFilters(new InputFilter[]{filter2});
		mEdCd1.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0.0, 300.00, 3, 2))});
		
		mEdWecd = (EditText)view.findViewById(R.id.et_title_wecd);
		mEdEcd = (EditText)view.findViewById(R.id.et_title_ecd);
		
		registerEdittextDoneListener(mEdWecd, this);
		registerEdittextDoneListener(mEdEcd, this);
		
		mEdWecd.setFilters(new InputFilter[]{filter2});
		mEdEcd.setFilters(new InputFilter[]{filter2});
		
		mSwPwean = (Switch)view.findViewById(R.id.switch_pwean);
		mSwRoolen = (Switch)view.findViewById(R.id.switch_roolen);
		mSwPwean.setOnClickListener(this);
		mSwRoolen.setOnClickListener(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void onResume() {
		super.onResume();
		
		mEdLd1.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabelWheelDiamL_]/100));
		mEdLd2.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabelWheelDiamR_]/100));
		mEdWad1.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RollPasteWheelDiam_]/100));
		
		mEdRot1.setText(String.format("%d", mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RollPasteMaGearNum_]));
		mEdRot2.setText(String.format("%d", mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RollPasteSlvGearNum_]));
		mEdCd1.setText(String.format("%.2f", (double)mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ConveyGearDiam_]/100));
		
		mEdWecd.setText(String.format("%d", mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RollPasteEncoderRes_]));
		mEdEcd.setText(String.format("%d", mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ConveyEncoderRes_]));
		
		mSwPwean.setChecked(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_FixPointPasteEnable_] == 0?false:true);
		mSwRoolen.setChecked(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RollPasteEnable_] == 0?false:true);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.switch_pwean:
				mActivity.setPWEAN(mSwPwean.isChecked()?1:0);
				break;
			case R.id.switch_roolen:
				mActivity.setRoolEn(mSwRoolen.isChecked()?1:0);
				break;
		}
	}

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		if(retValue == null || retValue.length() <= 0)
			return;

		switch(viewId) {
		case R.id.et_title_ld1:
			mEdLd1.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setLD1((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_ld2:
			mEdLd2.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setLD2((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_wad1:
			mEdWad1.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setWADI((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_rot1:
			mEdRot1.setText(String.format("%d", Integer.valueOf(retValue)));
			mActivity.setROT1(Integer.valueOf(retValue));
			break;
		case R.id.et_title_rot2:
			mEdRot2.setText(String.format("%d", Integer.valueOf(retValue)));
			mActivity.setROT2(Integer.valueOf(retValue));
			break;
		case R.id.et_title_cd1:
			mEdCd1.setText(String.format("%.2f", Double.valueOf(retValue)));
			mActivity.setCD1((int) (Double.valueOf(retValue)*100));
			break;
		case R.id.et_title_wecd:
			mEdWecd.setText(String.format("%d", Integer.valueOf(retValue)));
			mActivity.setWECD(Integer.valueOf(retValue));
			break;
		case R.id.et_title_ecd:
			mEdEcd.setText(String.format("%d", Integer.valueOf(retValue)));
			mActivity.setECD(Integer.valueOf(retValue));
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
