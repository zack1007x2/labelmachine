package endexcase.scanmachine.fragments;


import java.util.Locale;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.task.VersionInfoRunnable;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.uart.SportInterface;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.DecimalInputFilter;
import endexcase.scanmachine.util.Utils;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.percent.PercentRelativeLayout;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Created by Zack on 15/6/16.
 */
public class SystemSettingFragment extends BaseFragment implements
		View.OnClickListener, OnTouchListener, IEditTextDoneListener, IDialogValueListener {
	private static final AppLog smAppLog = new AppLog("SystemSettingFragment",SystemSettingFragment.class);
	
	private Button mSecretBtn, mModeBtn, mLanguageBtn, mVersionBtn;
	private int currentMode = 0;
	private int currentLan = 0;
	private String[] modeArr;
	private String[] langArr;
	
	//Database
	private AppData mAppdata = null;
	
	private TextView mLanText, mVersionText;
	
	private TextView mPositionTitleText, mPweanText, mModeText, mV1sdText, mV1edText;
	
	private TextView mBacklogTitleText, mOnDelayText, mOffDelayText;
	
	private TextView mErrorCheckTitleText;
	
	private ToggleButton mLabelBtn, mHotStampBtn, mObjectBtn;
	private ToggleButton togglePwean, toggle_separate_motion_delay;
	
	private EditText etVisd, etVied,etTypeAccelerate, etBacklogOnDelay, etBacklogOffDelay, etBotSepaSetSpd;
	
	private TextView mPrintTitleText, mPrintTypeDistText;
	
	private TextView mBottleTitleText, mSwitchText, mDistText;
	
	private Dialog mSoftInfoDialog, mPwdDialog;
	
	private RelativeLayout ll_header_bar;
	
	private PercentRelativeLayout area_fix_point_paste_setting, 
	area_detect_switch_setting, area_squeeze_bottle_setting, area_bottle_separate_speed_setting,
	area_type_accelerate_setting;
	
	private Runnable mPwdDialogRunnable = new Runnable() {
		
		@Override
		public void run() {
			if(mActivity.LoginState==0)
				mPwdDialog.show();
			else
				mActivity.showFragment(R.id.btn_header_secret);
		}
	}; 
	
	private int[][] machineSettingPermission = { 
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 1, 1, 1, 1},
			new int[]{0, 1, 1, 1, 1},
			new int[]{0, 1, 1, 1, 1},
			new int[]{0, 1, 1, 1, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 0, 0, 1},
			new int[]{0, 0, 1, 1, 1},
			new int[]{1, 0, 1, 1, 1},
			new int[]{1, 0, 1, 1, 1},
			new int[]{1, 0, 1, 1, 1},
			new int[]{1, 0, 1, 1, 1}
			};
	
	private Handler mHandler = new Handler();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_system_setting,
				container, false);
		mAppdata = AppData.getInstance();
		init(view);
		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.main_side_text_system_setting);
		ll_header_bar = (RelativeLayout)view.findViewById(R.id.rl_fragment_header);
		
		//data
		modeArr = getResources().getStringArray(R.array.label_mode);
		langArr = getResources().getStringArray(R.array.language);
		
		currentLan = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_];
		currentMode = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_FixPositionType_];
		
		//layout
		area_fix_point_paste_setting = (PercentRelativeLayout)view.findViewById(R.id.area_fix_point_paste_setting);
		area_detect_switch_setting = (PercentRelativeLayout)view.findViewById(R.id.area_detect_switch_setting);
		area_squeeze_bottle_setting = (PercentRelativeLayout)view.findViewById(R.id.area_squeeze_bottle_setting);
		area_bottle_separate_speed_setting = (PercentRelativeLayout)view.findViewById(R.id.area_bottle_separate_speed_setting);
		area_type_accelerate_setting = (PercentRelativeLayout)view.findViewById(R.id.area_type_accelerate_setting);

		//btn
		mSecretBtn = (Button) view.findViewById(R.id.btn_header_secret);
		mSecretBtn.setEnabled(true);
		mSecretBtn.setVisibility(View.VISIBLE);
		mSecretBtn.setOnTouchListener(this);
		mActivity.addFrag2map(R.id.btn_header_secret, new EngineerMenuPageFragment());
		mModeBtn =  (Button) view.findViewById(R.id.btn_mode);
		mLanguageBtn =  (Button) view.findViewById(R.id.btn_title_language_select);
		mVersionBtn =  (Button) view.findViewById(R.id.btn_title_system_version);
		
		mModeBtn.setOnClickListener(this);
		mLanguageBtn.setOnClickListener(this);
		mVersionBtn.setOnClickListener(this);
		
		//textView
		mLanText =  (TextView) view.findViewById(R.id.tv_title_language_select);
		mVersionText =  (TextView) view.findViewById(R.id.tv_title_system_version);
		
		mPositionTitleText = (TextView) view.findViewById(R.id.tv_head_declare_title_fix_point_paste);
		mPweanText =  (TextView) view.findViewById(R.id.tv_title_pwean);
		mModeText =  (TextView) view.findViewById(R.id.tv_title_mode);
		mV1sdText =  (TextView) view.findViewById(R.id.tv_title_visd);
		mV1edText =  (TextView) view.findViewById(R.id.tv_title_vied);
		
		mBacklogTitleText =  (TextView) view.findViewById(R.id.tv_head_declare_title_squeeze_bottle);
		mOnDelayText =  (TextView) view.findViewById(R.id.tv_squeeze_motion_delay);
		mOffDelayText =  (TextView) view.findViewById(R.id.tv_squeeze_reset_delay);
		
		mErrorCheckTitleText =  (TextView) view.findViewById(R.id.tv_head_declare_detect_switch);
		
		mPrintTitleText =  (TextView) view.findViewById(R.id.tv_head_declare_title_type_accelerate);
		mPrintTypeDistText =  (TextView) view.findViewById(R.id.tv_type_accelerate_distance);
		
		mBottleTitleText =  (TextView) view.findViewById(R.id.tv_head_declare_title_separate_bottle_speed);
		mSwitchText =  (TextView) view.findViewById(R.id.tv_separate_motion_delay);
		mDistText =  (TextView) view.findViewById(R.id.tv_separate_reset_delay);
		
		//edittext
		etVisd = (EditText) view.findViewById(R.id.et_visd);
		etVied = (EditText) view.findViewById(R.id.et_title_vied);
		etTypeAccelerate = (EditText) view.findViewById(R.id.et_type_accelerate_distance);
		etBacklogOnDelay = (EditText) view.findViewById(R.id.et_squeeze_motion_delay);
		etBacklogOffDelay = (EditText) view.findViewById(R.id.et_squeeze_reset_delay);
		etBotSepaSetSpd = (EditText) view.findViewById(R.id.et_separate_reset_delay);
		
		registerEdittextDoneListener(etVisd, this);
        registerEdittextDoneListener(etVied, this);
        registerEdittextDoneListener(etTypeAccelerate, this);
        registerEdittextDoneListener(etBacklogOnDelay, this);
        registerEdittextDoneListener(etBacklogOffDelay, this);
        registerEdittextDoneListener(etBotSepaSetSpd, this);
        
        etVisd.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 600, 3, 0))});
        etVied.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 200, 3, 0))});
        etTypeAccelerate.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0.00, 600.00, 3, 2))});
        DecimalInputFilter filterDelay = getCharReceiveListenerFilter(new DecimalInputFilter(0.0, 10.0, 2, 1));
        etBacklogOnDelay.setFilters(new InputFilter[]{filterDelay});
        etBacklogOffDelay.setFilters(new InputFilter[]{filterDelay});
        etBotSepaSetSpd.setFilters(new InputFilter[]{getCharReceiveListenerFilter(new DecimalInputFilter(0, 255, 3, 0))});
        
        //toggle
        togglePwean = (ToggleButton)view.findViewById(R.id.toggle_pwean);
        toggle_separate_motion_delay = (ToggleButton)view.findViewById(R.id.toggle_separate_motion_delay);
        mLabelBtn =  (ToggleButton) view.findViewById(R.id.toggle_detect_label_miss);
		mHotStampBtn =  (ToggleButton) view.findViewById(R.id.toggle_detect_no_ribbon);
		mObjectBtn =  (ToggleButton) view.findViewById(R.id.toggle_detect_bottle_dim);
		
		togglePwean.setOnClickListener(this);
		toggle_separate_motion_delay.setOnClickListener(this);
        mLabelBtn.setOnClickListener(this);
        mHotStampBtn.setOnClickListener(this);
        mObjectBtn.setOnClickListener(this);
        
        mPwdDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_password)
				.setValueRetListener(this)
				.build();
        
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void onResume() {
		super.onResume();
		refreshValue();
		if(mActivity.isMachineRunning()){
        	Utils.recursiveEnableView(area_fix_point_paste_setting, false);
        	Utils.recursiveEnableView(area_detect_switch_setting, false);
        	Utils.recursiveEnableView(area_squeeze_bottle_setting, false);
        	Utils.recursiveEnableView(area_bottle_separate_speed_setting, true);
        	Utils.recursiveEnableView(area_type_accelerate_setting, true);
        	mSecretBtn.setEnabled(false);
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_r_s);
        }else{
        	ll_header_bar.setBackgroundResource(R.drawable.title_bar_s);
        }
	}
	
	private void refreshValue(){
		//set layout permission
		int CurMachineType = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_];
		int[] permissionRow = machineSettingPermission[CurMachineType];
		updatePermission(permissionRow);
		
		//edittext
		etVisd.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1SD_]));
        etVied.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1ED_]));
        etTypeAccelerate.setText(String.format("%.2f", mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ObjSenToPresDist_]/100.00));
        etBacklogOnDelay.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BacklogOnDelay_]/10.0));
        etBacklogOffDelay.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BacklogOffDelay_]/10.0));
        etBotSepaSetSpd.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotSepaSetSpd_]));
        
		//toggle
		togglePwean.setChecked((mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_FixPointPasteEnable_]==1));
        toggle_separate_motion_delay.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_));
        
        //btn
        mModeBtn.setText(modeArr[currentMode]);
		mLanguageBtn.setText(langArr[currentLan]);
        mLabelBtn.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR17, EndexScanProtocols.BYTE_COMMAND_ADDR17_SysLabMissHalt_));
        mHotStampBtn.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_NoRibbonHalt_));
        mObjectBtn.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_BotDimChkOnOff_));
	}

	private void updatePermission(int[] permissionRow) {
		PercentRelativeLayout[] viewArr = {area_fix_point_paste_setting, area_type_accelerate_setting, 
				area_bottle_separate_speed_setting,area_squeeze_bottle_setting, area_detect_switch_setting};
		
		for(int i=0; i<permissionRow.length; i++){
			Utils.recursiveEnableVisiableView(viewArr[i], (permissionRow[i]==1));
		}
	}
	
	@Override
	public void onClick(View v) {
		MainActivity activity = (MainActivity) getActivity();
		
		switch (v.getId()) {
			case R.id.toggle_pwean:
				mActivity.setPWEAN(togglePwean.isChecked()?1:0);
				break;
			case R.id.toggle_separate_motion_delay:
				mActivity.setBotSeparateEnable(toggle_separate_motion_delay.isChecked());
				break;
			case R.id.toggle_detect_label_miss:
				mActivity.setSysLabMissHalt(mLabelBtn.isChecked());
				break;
			case R.id.toggle_detect_no_ribbon:
				mActivity.setNoRibbonHalf(mHotStampBtn.isChecked());
				break;
			case R.id.toggle_detect_bottle_dim:
				mActivity.setBotDimChkOnOff(mObjectBtn.isChecked());
				break;
			case R.id.btn_mode:
				currentMode++;
				if(currentMode>2){
					currentMode=0;
				}
				mModeBtn.setText(modeArr[currentMode]);
				mActivity.setFixPointType(currentMode);
				break;
			case R.id.btn_title_language_select:
				currentLan++;
				if(currentLan>1){
					currentLan=0;
				}
				mLanguageBtn.setText(langArr[currentLan]);
				setLanguage(currentLan);
				activity.setLanguageMode(currentLan);
				break;
			case R.id.btn_title_system_version:
				mActivity.getVersion();
				showVersionInfoDialog();
				break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		v.performClick();
		if(event.getAction()==MotionEvent.ACTION_DOWN)
			mHandler.postDelayed(mPwdDialogRunnable, 3000);
		else if(event.getAction()==MotionEvent.ACTION_UP){
			mHandler.removeCallbacks(mPwdDialogRunnable);
		}
		return false;
	}

	private void setLanguage(int lang) {
		String languageToLoad = null;
		if(lang == AppData.LANGUAGE_ENGLISH)
			languageToLoad = "en";
		else
			languageToLoad = "zh";

		Locale locale = new Locale(languageToLoad); 
		Locale.setDefault(locale);
		android.content.res.Configuration config = new android.content.res.Configuration();
		config.locale = locale;
		getActivity().getBaseContext().getResources().updateConfiguration(config, getActivity().getBaseContext().getResources().getDisplayMetrics());
		
		modeArr = getResources().getStringArray(R.array.label_mode);
		langArr = getResources().getStringArray(R.array.language);
		
		mLanText.setText(R.string.system_setting_item_title_language_select);
		mVersionText.setText(R.string.system_setting_item_title_system_version);
		mVersionBtn.setText(R.string.system_setting_btn_title_system_version);
		
		mPositionTitleText.setText(R.string.system_setting_head_declare_title_fix_point_paste);
		mPweanText.setText(R.string.system_setting_item_title_pwean);
		mModeText.setText(R.string.system_setting_item_title_mode);
		mV1sdText.setText(R.string.system_setting_item_title_visd);
		mV1edText.setText(R.string.system_setting_item_title_vied);
		mModeBtn.setText(modeArr[currentMode]);
		
		mBacklogTitleText.setText(R.string.system_setting_head_declare_title_squeeze_bottle);
		mOnDelayText.setText(R.string.system_setting_item_title_motion_delay);
		mOffDelayText.setText(R.string.system_setting_item_title_reset_delay);
		
		mErrorCheckTitleText.setText(R.string.system_setting_head_declare_title_detect_switch);
		if(mLabelBtn.isChecked())
			mLabelBtn.setText(R.string.system_setting_item_title_detect_label_miss_on);
		else
			mLabelBtn.setText(R.string.system_setting_item_title_detect_label_miss_off);
		mLabelBtn.setTextOn(getString(R.string.system_setting_item_title_detect_label_miss_on));
		mLabelBtn.setTextOff(getString(R.string.system_setting_item_title_detect_label_miss_off));
		if(mHotStampBtn.isChecked())
			mHotStampBtn.setText(R.string.system_setting_item_title_detect_no_ribbon_on);
		else
			mHotStampBtn.setText(R.string.system_setting_item_title_detect_no_ribbon_off);
		mHotStampBtn.setTextOn(getString(R.string.system_setting_item_title_detect_no_ribbon_on));
		mHotStampBtn.setTextOff(getString(R.string.system_setting_item_title_detect_no_ribbon_off));
		if(mObjectBtn.isChecked())
			mObjectBtn.setText(R.string.system_setting_item_title_detect_bottle_dim_on);
		else
			mObjectBtn.setText(R.string.system_setting_item_title_detect_bottle_dim_off);
		mObjectBtn.setTextOn(getString(R.string.system_setting_item_title_detect_bottle_dim_on));
		mObjectBtn.setTextOff(getString(R.string.system_setting_item_title_detect_bottle_dim_off));
		
		mPrintTitleText.setText(R.string.system_setting_head_declare_title_type_accelerate);
		mPrintTypeDistText.setText(R.string.system_setting_item_title_type_accelerate_distance);
		
		mBottleTitleText.setText(R.string.system_setting_head_declare_title_bottle_separate_speed);
		mSwitchText.setText(R.string.system_setting_item_title_motion_delay);
		mDistText.setText(R.string.system_setting_item_title_reset_delay);
		
		mPwdDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_password)
				.setValueRetListener(this)
				.build();
		
		super.setTitle(ll_header_bar, R.string.main_side_text_system_setting);
	}

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		switch(viewId){
		case R.id.et_visd:
			etVisd.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setV1SD(Integer.parseInt(retValue));
			break;
		case R.id.et_title_vied:
			etVied.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setV1ED(Integer.parseInt(retValue));
			break;
		case R.id.et_type_accelerate_distance:
			etTypeAccelerate.setText(String.format("%.2f", Double.parseDouble(retValue)));
			mActivity.setObjPresDist((int)(Double.parseDouble(retValue)*100));
			break;
		case R.id.et_squeeze_motion_delay:
			etBacklogOnDelay.setText(String.format("%.1f", Double.parseDouble(retValue)));
			mActivity.setBackLogOnDelay((int)(Double.parseDouble(retValue)*10));
			break;
		case R.id.et_squeeze_reset_delay:
			etBacklogOffDelay.setText(String.format("%.1f", Double.parseDouble(retValue)));
			mActivity.setBackLogOffDelay((int)(Double.parseDouble(retValue)*10));
			break;
		case R.id.et_separate_reset_delay:
			etBotSepaSetSpd.setText(String.format("%d", Integer.parseInt(retValue)));
			mActivity.setBotSeparateSpd(Integer.parseInt(retValue));
			break;
		}
	}

	@Override
	public void onReturnValue(String ret) {
		if(ret.equals("cancel")){
			mActivity.initSysPageParam();
		}else if(ret.equals("PWD")){
			mActivity.showFragment(R.id.btn_header_secret);//engineer menu
			mActivity.LoginState=1;
		}
	}
	
	private void showVersionInfoDialog(){
		mSoftInfoDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_software_version)
				.setValueRetListener(this)
				.build();
		mSoftInfoDialog.show();
	}
}
