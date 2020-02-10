package endexcase.scanmachine.fragments;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.uart.SportInterface;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

/**
 * Created by Zack on 15/6/16.
 */
public class EngineerMenuPageFragment extends BaseFragment implements
		View.OnClickListener, OnTouchListener, IDialogValueListener ,OnCheckedChangeListener{

	private Button btn_speed_param_setting, btn_ccd_param_setting,
			btn_engineer_param_setting,btn_ver_and_machine;
	private ToggleButton btn_label_test, btn_extension_test;
	
	private Button btn_debug;
	private RelativeLayout btn_header_close;
	
	private Dialog mPwdDialog;
	private Handler mHandler = new Handler();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_engineering_mian_menu,
				container, false);
		init(view);
		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.engineer_page_menu_title_engineer_page);
		btn_header_close = (RelativeLayout) view.findViewById(R.id.btn_header_close);
		btn_speed_param_setting = (Button) view.findViewById(R.id.btn_engineering_menu_speed_param_setting);
		btn_ccd_param_setting = (Button) view.findViewById(R.id.btn_engineering_menu_ccd_param_setting);
		btn_engineer_param_setting = (Button) view.findViewById(R.id.btn_engineering_menu_engineering_param_setting);
		btn_ver_and_machine = (Button) view.findViewById(R.id.btn_engineering_menu_version_and_machine_info);
		btn_label_test = (ToggleButton) view.findViewById(R.id.btn_label_test);
		btn_extension_test = (ToggleButton) view.findViewById(R.id.btn_extension_test);
		
		btn_label_test.setOnTouchListener(this);
		btn_extension_test.setOnTouchListener(this);
		btn_label_test.setOnCheckedChangeListener(this);
		btn_extension_test.setOnCheckedChangeListener(this);
		btn_label_test.setClickable(false);
		btn_extension_test.setClickable(false);
		
		btn_header_close.setOnClickListener(this);
		btn_speed_param_setting.setOnClickListener(this);
		btn_ccd_param_setting.setOnClickListener(this);
		btn_engineer_param_setting.setOnClickListener(this);
		btn_ver_and_machine.setOnClickListener(this);
		
		mActivity.addFrag2map(R.id.btn_engineering_menu_speed_param_setting,
				new EngineerSpeedParamSettingFragment());
		mActivity.addFrag2map(R.id.btn_engineering_menu_ccd_param_setting,
				new EngineerCCDParamSettingFragment());
		mActivity.addFrag2map(R.id.btn_engineering_menu_engineering_param_setting,
				new EngineerParamSettingFragment());
		mActivity.addFrag2map(R.id.btn_engineering_menu_version_and_machine_info,
				new EngineerVerAndMachineInfoFragment());
		
		mPwdDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_password)
				.setValueRetListener(this)
				.build();
		
		btn_debug = (Button) view.findViewById(R.id.btn_debug);
		btn_debug.setOnClickListener(this);
		mActivity.addFrag2map(R.id.btn_debug, new DebugFragment());
		if(SportInterface.USE_NATIVE_UART){
			btn_debug.setEnabled(false);
			btn_debug.setVisibility(View.INVISIBLE);
		}
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void onResume() {
		super.onResume();

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_engineering_menu_speed_param_setting:
			mActivity.showFragment(R.id.btn_engineering_menu_speed_param_setting);
			break;
		case R.id.btn_engineering_menu_ccd_param_setting:
			mActivity.showFragment(R.id.btn_engineering_menu_ccd_param_setting);
			break;
		case R.id.btn_engineering_menu_engineering_param_setting:
			mActivity.showFragment(R.id.btn_engineering_menu_engineering_param_setting);
			break;
		case R.id.btn_engineering_menu_version_and_machine_info:
			if(mActivity.LoginState==1)
				mPwdDialog.show();
			else
				mActivity.showFragment(R.id.btn_engineering_menu_version_and_machine_info);
			break;
		case R.id.btn_debug:
			mActivity.showFragment(R.id.btn_debug);
			break;
		case R.id.btn_header_close:
			mActivity.showFragment(R.id.btn_main_side_system_setting);
			break;
			
		}
	}

	@Override
	public void onReturnValue(String ret) {
		mActivity.showFragment(R.id.btn_engineering_menu_version_and_machine_info);
		mActivity.LoginState=2;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(v.getId()){
		case R.id.btn_label_test:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				btn_label_test.setPressed(true);
				if(btn_label_test.isChecked()){
					btn_label_test.setChecked(false);
				}else{
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if(!btn_extension_test.isChecked())
								btn_label_test.setChecked(true);
						}
					}, 5000);
				}
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				btn_label_test.setPressed(false);
				mHandler.removeCallbacksAndMessages(null);
			}
			break;
		case R.id.btn_extension_test:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				btn_extension_test.setPressed(true);
				if(btn_extension_test.isChecked()){
					btn_extension_test.setChecked(false);
				}else{
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if(!btn_label_test.isChecked())
								btn_extension_test.setChecked(true);
						}
					}, 5000);
				}
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				btn_extension_test.setPressed(false);
				mHandler.removeCallbacksAndMessages(null);
			}
			break;
		}
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.btn_extension_test:
			mActivity.setExtensionTestEnable(btn_extension_test.isChecked());
			break;
		case R.id.btn_label_test:
			mActivity.setLabelTestModeEnable(btn_label_test.isChecked());
			break;
		}
	}
}
