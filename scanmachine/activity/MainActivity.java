package endexcase.scanmachine.activity;

import java.util.Locale;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.MachineType;
import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.Interface.IOnErrorOccurred;
import endexcase.scanmachine.Interface.IOnReceiveCharListener;
import endexcase.scanmachine.service.ControlService;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.uart.SportInterface;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.DecimalInputFilter;
import endexcase.scanmachine.util.Utils;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import endexcase.scanmachine.widget.MainMonitorButton;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputFilter;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.TextView.OnEditorActionListener;
import endexcase.scanmachine.database.DatabaseProxy;
import endexcase.scanmachine.fragments.BaseFragment;
import endexcase.scanmachine.fragments.LoadMemoryFragment;
import endexcase.scanmachine.fragments.PrintSettingFragment;
import endexcase.scanmachine.fragments.SaveMemoryFragment;
import endexcase.scanmachine.fragments.SystemSettingFragment;
import endexcase.scanmachine.fragments.LabelSettingFragment;

public class MainActivity extends FragmentActivity implements
		View.OnClickListener, OnTouchListener, IOnReceiveCharListener {

	private static final AppLog smAppLog = new AppLog("activity",MainActivity.class);
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private SparseArray<BaseFragment> navigateMap = new SparseArray<BaseFragment>();
    private PercentRelativeLayout btn_main_side_auto_check, btn_main_side_load_memo, btn_main_side_save_memo,
        btn_main_side_tag_setting, btn_main_side_print_setting, btn_main_side_system_setting;
    private RelativeLayout main_view;
    private MainMonitorButton btStart, btStop;
    private EditText etProduceAmount, etLabelRemain, etProduceSpeed;
    private TextView tvProduceAmount, tvLabelRemain, tvProduceSpeed,
    tvMachineType, tvCurMemoId, tv_model_title, tv_current_memory_num_title,
    tv_current_machine_status_title,tv_current_machine_status;
    private String[] mMachineTypeArr;

    private TextView mSideAutoCheckText, mSideLoadMemoText, mSideSaveMemoText, mSideTagSettingText, mSidePrintSettingText, mSideSystemSettingText;
    private TextView mOutputText, mRemainingText, mSpeedText;
    
    private boolean isDoneClicked;
    private String mCurEditTextStr;
    
    InputMethodManager imm;
    
    private View cover_view;
    private LinearLayout ll_side_menu_upper_area, ll_side_menu_lower_area;
    private boolean lockScreen=false;
    
    private ToggleButton toggle_separate_motion_delay;

    // Service Binder
	@Nullable private ControlService mBindService = null;
	@Nullable private ControlServiceConnection mServiceConn = null;
	
	//Database
	private AppData mAppdata = null;
	private DatabaseProxy mDatabase;

	private int CurFragment;
	private static final int START_LABEL_WRAPER = 1001;
	private static final int STOP_LABEL_WRAPER = 1002;
	private static final int REGULAR_REFRESH = 1003;// only run after setActivateView();
	private static final int CHECK_MACHINE_STATE = 1004;
	private static final int AUTO_ADJUSTMENT_PROCESS_DETECT = 1005;
	
	private Dialog mAutoProcessDialog;
	private boolean NotEditYet;
	
	private RelativeLayout rl_title_bar;
	private ImageView btn_speed_plus, btn_speed_minus;
	private int CurCnySetSpd;
	//for fragment
	public int LoginState=SportInterface.USE_NATIVE_UART?0:2;
	private boolean isChangingToStart;//UI state not machine actually state
	private boolean isActivateUI;
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case START_LABEL_WRAPER:
				removeMessages(REGULAR_REFRESH);
				if(!btStart.isWaiting()){
					btStart.setIsWaiting(true);
					btStart.setClickable(false);
					btStop.setEnabled(false);
					btStop.setClickable(false);
					etProduceAmount.setEnabled(false);
			    	etLabelRemain.setEnabled(false);
			    	etProduceSpeed.setEnabled(false);
			    	btn_main_side_auto_check.setEnabled(false);
			    	btn_main_side_load_memo.setEnabled(false);
			    	btn_main_side_save_memo.setEnabled(false);
			    	btn_main_side_print_setting.setEnabled(false);
			    	btn_main_side_tag_setting.setEnabled(false);
			    	btn_main_side_system_setting.setEnabled(false);
				}
				setMachineStartRunning(true);
				sendEmptyMessage(CHECK_MACHINE_STATE);
				CurCnySetSpd = Integer.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CnySetSpd_]);
				break;
			case STOP_LABEL_WRAPER:
				removeMessages(REGULAR_REFRESH);
				if(!btStop.isWaiting()){
					btStop.setIsWaiting(true);
					btStop.setClickable(false);
					btStart.setEnabled(false);
					btStart.setClickable(false);
					btn_speed_plus.setEnabled(false);
					btn_speed_minus.setEnabled(false);
					btn_main_side_auto_check.setEnabled(false);
			    	btn_main_side_load_memo.setEnabled(false);
			    	btn_main_side_save_memo.setEnabled(false);
			    	btn_main_side_print_setting.setEnabled(false);
			    	btn_main_side_tag_setting.setEnabled(false);
			    	btn_main_side_system_setting.setEnabled(false);
				}
				setMachineStartRunning(false);
				setMachineStartRunning(false);
				sendEmptyMessage(CHECK_MACHINE_STATE);
				break;
			case REGULAR_REFRESH:
				if(isMachineRunning()){
					refreshValue();
					sendEmptyMessageDelayed(REGULAR_REFRESH, 100);
				}else{
					if(isActivateUI){
						//緊急停止
						removeMessages(REGULAR_REFRESH);
						setInactivateView();
						etProduceSpeed.setText("0.0");
					}else{
						//正常停止前更新速度值流程
						if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSpd_]!=0){
							sendEmptyMessageDelayed(REGULAR_REFRESH, 100);
						}
						refreshValue();
					}
				}
				break;
			case CHECK_MACHINE_STATE:
				if(isChangingToStart){
					//停止時啟動
					if(isMachineRunning()){
						//already start
						removeMessages(CHECK_MACHINE_STATE);
						setActivateView();
						btStart.setIsWaiting(false);//clear flag
						sendEmptyMessage(REGULAR_REFRESH);
					}else{
						//wait for start
						if(SportInterface.USE_NATIVE_UART){
							sendEmptyMessageDelayed(CHECK_MACHINE_STATE, 100);
						}else{
							mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_CnyChkOnOff_, true);
							sendEmptyMessageDelayed(CHECK_MACHINE_STATE,2000);
						}
					}
				}else{
					//運轉中停止
					if(!isMachineRunning()){
						//already stop
						removeMessages(CHECK_MACHINE_STATE);
						btStop.setIsWaiting(false);//clear flag
						setInactivateView();
						sendEmptyMessage(REGULAR_REFRESH);
					}else{
						//wait for stop
						if(SportInterface.USE_NATIVE_UART){
							sendEmptyMessageDelayed(CHECK_MACHINE_STATE, 100);
						}else{
							mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_CnyChkOnOff_, false);
							sendEmptyMessageDelayed(CHECK_MACHINE_STATE,2000);
						}
					}
				}
				
				break;
			case AUTO_ADJUSTMENT_PROCESS_DETECT:
				if(!mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_AutoMesAllEnable_)){
					this.removeMessages(AUTO_ADJUSTMENT_PROCESS_DETECT);
					closeAutoProcessDialog();
					sendEmptyMessage(START_LABEL_WRAPER);
				}else{
					this.sendEmptyMessageDelayed(AUTO_ADJUSTMENT_PROCESS_DETECT, 500);
				}
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Initial DB
		mAppdata = AppData.getInstance();
		mDatabase = DatabaseProxy.getInstance(this);
		if(!mDatabase.isDatabaseCreate()) {
			mDatabase.initMachineMemData();
			MachineType.setDefValue(MachineType.KK806U, mDatabase);
			mDatabase.saveAppAllParameter();
		}
		else {
			mDatabase.loadAppParameter();
			mDatabase.getMachineMemData(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]);
			mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]=2;
			mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_SysPage_);
		}
		
		languageInit();
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		
		setContentView(R.layout.activity_main);
		imm = (InputMethodManager)(MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE));
		initFragment();
		initView();
		if(savedInstanceState==null){
			// start the service
			Intent intent = new Intent(this, ControlService.class);
			startService(intent);
		}
	}

	private void refreshValue(){
		int CurPuoduceAmount, CurLabelRemain, CurProduceSpeed;
		mMachineTypeArr = getResources().getStringArray(R.array.machine_type_arr);
		tvCurMemoId.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_]));
		String machineTypeStr = mMachineTypeArr[mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]];
		tvMachineType.setText(machineTypeStr);
		
		toggle_separate_motion_delay.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_));
		toggle_separate_motion_delay.setText(toggle_separate_motion_delay.isChecked()?R.string.system_setting_item_title_bot_separate_on:R.string.system_setting_item_title_bot_separate_off);
		
		int PuoduceAmountH = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyH_];
		int PuoduceAmountL = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyL_];
		int LabelRemainH = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyH_];
		int LabelRemainL = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyL_];
		
		CurPuoduceAmount = (PuoduceAmountH << 16) + PuoduceAmountL;
		CurLabelRemain = (LabelRemainH << 16) + LabelRemainL;
		CurProduceSpeed = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSpd_];
		
		if(etProduceAmount.getVisibility()==View.VISIBLE)etProduceAmount.setText(String.valueOf(CurPuoduceAmount));
		if(etLabelRemain.getVisibility()==View.VISIBLE)etLabelRemain.setText(String.valueOf(CurLabelRemain));
		if(etProduceSpeed.getVisibility()==View.VISIBLE)etProduceSpeed.setText(String.format("%.1f",Double.valueOf(CurProduceSpeed/10.0)));
		
		if(tvProduceAmount.getVisibility()==View.VISIBLE)tvProduceAmount.setText(String.valueOf(CurPuoduceAmount));
		if(tvLabelRemain.getVisibility()==View.VISIBLE)tvLabelRemain.setText(String.valueOf(CurLabelRemain));
		if(tvProduceSpeed.getVisibility()==View.VISIBLE)tvProduceSpeed.setText(String.format("%.1f",Double.valueOf(CurProduceSpeed/10.0)));
		
		if(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_MainBoardIONoMatch_)) {
			onMainBoardIONoMatchError();
		}
	};
	private void initView() {
		cover_view = (View)findViewById(R.id.cover_view);
		cover_view.setOnTouchListener(this);
		main_view = (RelativeLayout)findViewById(R.id.main_view);
		//monitor panel
		btStart = (MainMonitorButton)findViewById(R.id.btn_activate);
		btStop = (MainMonitorButton)findViewById(R.id.btn_inactivate);
		btStart.setInitStringId(R.string.main_monitor_btn_start);
		btStart.setWaitingStringId(R.string.main_monitor_btn_waiting_start);
		btStop.setInitStringId(R.string.main_monitor_btn_stop);
		btStop.setWaitingStringId(R.string.main_monitor_btn_waiting_stop);
		toggle_separate_motion_delay = (ToggleButton)findViewById(R.id.toggle_separate_motion_delay);
		
		tvProduceAmount = (TextView)findViewById(R.id.tv_produce_amount);
		tvLabelRemain = (TextView)findViewById(R.id.tv_label_remained);
		tvProduceSpeed = (TextView)findViewById(R.id.tv_produce_speed);	
		etProduceAmount = (EditText) findViewById(R.id.et_produce_amount);
		etLabelRemain = (EditText) findViewById(R.id.et_label_remained);
		etProduceSpeed = (EditText) findViewById(R.id.et_produce_speed);
		
		etProduceAmount.setOnEditorActionListener(onEditorActionListener);
		etLabelRemain.setOnEditorActionListener(onEditorActionListener);
		etProduceSpeed.setOnEditorActionListener(onEditorActionListener);
		etProduceAmount.setOnFocusChangeListener(onFocusChangeListener);
		etLabelRemain.setOnFocusChangeListener(onFocusChangeListener);
		etProduceSpeed.setOnFocusChangeListener(onFocusChangeListener);
		
		DecimalInputFilter intFilter = new DecimalInputFilter(7, 0);
		intFilter.setOnCharReceiveListener(this);
		etProduceAmount.setFilters(new InputFilter[]{intFilter});
		etLabelRemain.setFilters(new InputFilter[]{intFilter});
		DecimalInputFilter filter = new DecimalInputFilter(3, 1) {};
		etProduceSpeed.setFilters(new InputFilter[]{filter});
		
		mOutputText = (TextView) findViewById(R.id.tv_produce_amount_title);
		mRemainingText = (TextView) findViewById(R.id.tv_label_remained_title);
		mSpeedText = (TextView) findViewById(R.id.tv_produce_speed_title);
		
		btStart.setOnClickListener(this);
		btStop.setOnClickListener(this);
		toggle_separate_motion_delay.setOnClickListener(this);
		
		btn_speed_plus = (ImageView)findViewById(R.id.btn_speed_plus);
		btn_speed_minus = (ImageView)findViewById(R.id.btn_speed_minus);
		btn_speed_plus.setOnTouchListener(this);
		btn_speed_minus.setOnTouchListener(this);
		
		//side menu
		ll_side_menu_upper_area = (LinearLayout)findViewById(R.id.ll_side_menu_upper_area);
		ll_side_menu_lower_area	= (LinearLayout)findViewById(R.id.ll_side_menu_lower_area);	
		btn_main_side_auto_check = (PercentRelativeLayout) findViewById(R.id.btn_main_side_auto_check);
		btn_main_side_auto_check.setOnTouchListener(this);
		btn_main_side_load_memo = (PercentRelativeLayout) findViewById(R.id.btn_main_side_load_memo);
		btn_main_side_load_memo.setOnClickListener(this);
		btn_main_side_save_memo = (PercentRelativeLayout) findViewById(R.id.btn_main_side_save_memo);
		btn_main_side_save_memo.setOnClickListener(this);
		btn_main_side_tag_setting = (PercentRelativeLayout) findViewById(R.id.btn_main_side_tag_setting);
		btn_main_side_tag_setting.setOnClickListener(this);
		btn_main_side_print_setting = (PercentRelativeLayout) findViewById(R.id.btn_main_side_print_setting);
		btn_main_side_print_setting.setOnClickListener(this);
		btn_main_side_system_setting = (PercentRelativeLayout) findViewById(R.id.btn_main_side_system_setting);
		btn_main_side_system_setting.setOnClickListener(this);
		
		mSideAutoCheckText = (TextView) findViewById(R.id.text_side_text_auto_check);
		mSideLoadMemoText = (TextView) findViewById(R.id.text_side_text_load_memo);
		mSideSaveMemoText = (TextView) findViewById(R.id.text_side_text_save_memo);
		mSideTagSettingText = (TextView) findViewById(R.id.text_side_text_tag_setting);
		mSidePrintSettingText = (TextView) findViewById(R.id.text_side_text_print_setting);
		mSideSystemSettingText = (TextView) findViewById(R.id.text_side_text_system_setting);
		
		//header bar
		rl_title_bar = (RelativeLayout)findViewById(R.id.main_header);
		tv_model_title = (TextView)findViewById(R.id.tv_model_title);
		tv_current_memory_num_title = (TextView)findViewById(R.id.tv_current_memory_num_title);
		tvMachineType = (TextView)findViewById(R.id.tv_model_content);
		tvCurMemoId = (TextView)findViewById(R.id.tv_current_memory_num);
		tv_current_machine_status_title = (TextView)findViewById(R.id.tv_current_machine_status_title);
		tv_current_machine_status = (TextView)findViewById(R.id.tv_current_machine_status);
	}

	@Override
	protected void onStart() {
		super.onStart();
		smAppLog.d("onStart()@"+hashCode());

		// bindService
		if(mServiceConn==null){
			mServiceConn = new ControlServiceConnection();
			bindService(new Intent(this,ControlService.class), mServiceConn, Context.BIND_AUTO_CREATE);
		}
	}
	
	@Override
    protected void onStop(){
        super.onStop();
        if(mServiceConn!=null){
        	unbindService(mServiceConn);
        	mServiceConn=null;
        }
    }
	
	private class ControlServiceConnection implements ServiceConnection{
		boolean isValid = true;
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if(isValid){
				smAppLog.d("ControlServiceConnection#onServiceConnected()");
				mBindService = ((ControlService.ControlServiceBinder)service).getControlService();
				mBindService.setBoundActivity(MainActivity.this);
			}else{
				smAppLog.e("ControlServiceConnection#onServiceConnected() called but this is invalid");
			}
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			smAppLog.e("ControlServiceConnection#onServiceConnected() called but this is invalid");
		}
	}
		
	private void languageInit() {
		String languageToLoad = null;
		if(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_] == AppData.LANGUAGE_ENGLISH)
			languageToLoad = "en";
		else
			languageToLoad = "zh";
		
		Locale locale = new Locale(languageToLoad); 
		Locale.setDefault(locale);
		android.content.res.Configuration config = new android.content.res.Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
	}
	
	private void initFragment() {

		fragmentManager = getSupportFragmentManager();

		navigateMap.clear();
		mapNaviToFragment(R.id.btn_main_side_load_memo,
				new LoadMemoryFragment());
		mapNaviToFragment(R.id.btn_main_side_save_memo,
				new SaveMemoryFragment());
		mapNaviToFragment(R.id.btn_main_side_tag_setting,
				new LabelSettingFragment());
		mapNaviToFragment(R.id.btn_main_side_print_setting,
				new PrintSettingFragment());
		mapNaviToFragment(R.id.btn_main_side_system_setting,
				new SystemSettingFragment());
	}

	private void mapNaviToFragment(int id, BaseFragment fragment) {
		View view = findViewById(id);
		view.setOnClickListener(this);
		navigateMap.put(id, fragment);
	}
	
	public void addFrag2map(int id, BaseFragment fragment){
		navigateMap.put(id, fragment);
	}

	public void showFragment(int viewid) {
		String tag = String.valueOf(viewid);
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.contentView, navigateMap.get(viewid), tag);
		fragmentTransaction.commit();
		main_view.setVisibility(View.INVISIBLE);
		CurFragment = viewid;
	}
	
	public void finishFragment() {
		fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.remove(navigateMap.get(CurFragment));
		fragmentTransaction.commit();
		main_view.setVisibility(View.VISIBLE);
		
		if(CurFragment == R.id.btn_main_side_system_setting || CurFragment==R.id.btn_main_side_load_memo) {
			setLanguage();
		}
		String machineTypeStr = mMachineTypeArr[mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]];
		tvMachineType.setText(machineTypeStr);
		tvCurMemoId.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_]));
		CurFragment = -1;
		refreshValue();
		if(isMachineRunning()){
			setActivateView();
		}else{
			setInactivateView();
		}
	}

	private void setLanguage() {
		mSideAutoCheckText.setText(R.string.main_side_text_auto_check);
		mSideLoadMemoText.setText(R.string.main_side_text_load_memo);
		mSideSaveMemoText.setText(R.string.main_side_text_save_memo);
		mSideTagSettingText.setText(R.string.main_side_text_tag_setting);
		mSidePrintSettingText.setText(R.string.main_side_text_print_setting);
		mSideSystemSettingText.setText(R.string.main_side_text_system_setting);
		
		mOutputText.setText(R.string.main_monitor_table_title_produce_amount);
		mRemainingText.setText(R.string.main_monitor_table_title_label_remained);
		mSpeedText.setText(R.string.main_monitor_table_title_produce_speed);
		
		btStart.setText(R.string.main_monitor_btn_start);
		btStop.setText(R.string.main_monitor_btn_stop);
		
		tv_model_title.setText(R.string.main_titlebar_machine_model_title);
		tv_current_memory_num_title.setText(R.string.main_titlebar_current_memory_num_title);
		tv_current_machine_status_title.setText(R.string.main_titlebar_current_machine_status_title);
		
		toggle_separate_motion_delay.setChecked(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_));
		toggle_separate_motion_delay.setText(toggle_separate_motion_delay.isChecked()?R.string.system_setting_item_title_bot_separate_on:R.string.system_setting_item_title_bot_separate_off);
	}
	@Override
	public void onClick(View view) {
		int viewId = view.getId();
		switch (viewId) {
		case R.id.toggle_separate_motion_delay:
			setBotSeparateEnable(toggle_separate_motion_delay.isChecked());
			break;
		case R.id.btn_activate:
			mHandler.sendEmptyMessage(START_LABEL_WRAPER);
			break;
		case R.id.btn_inactivate:
			mHandler.sendEmptyMessage(STOP_LABEL_WRAPER);
			break;
		case R.id.btn_main_side_load_memo:
		case R.id.btn_main_side_save_memo:
		case R.id.btn_main_side_tag_setting:
		case R.id.btn_main_side_print_setting:
		case R.id.btn_main_side_system_setting:
			hideKeyBoard(getCurrentFocus());
			showFragment(viewId);
			break;
		}
	}
	private boolean isSetSpeedRunnableRun;
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.btn_speed_plus:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				mHandler.postDelayed(mSpeedPlusRunnable, 1000);//+10 process
				v.setPressed(true);
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				v.setPressed(false);
				mHandler.removeCallbacks(mSpeedPlusRunnable);
				if(!isSetSpeedRunnableRun){
					CurCnySetSpd++;
					if(CurCnySetSpd>=255){
						CurCnySetSpd=255;
						btn_speed_plus.setEnabled(false);
					}
					if(!btn_speed_minus.isEnabled()){
						btn_speed_minus.setEnabled(true);
					}
					setPdSpeed(CurCnySetSpd);
				}else{
					isSetSpeedRunnableRun = false;
				}
				return true;
			}
			break;
		case R.id.btn_speed_minus:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				mHandler.postDelayed(mSpeedMinusRunnable, 1000);//-10 process
				v.setPressed(true);
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				v.setPressed(false);
				mHandler.removeCallbacks(mSpeedMinusRunnable);
				if(!isSetSpeedRunnableRun){
					CurCnySetSpd--;
					if(CurCnySetSpd<=0){
						CurCnySetSpd=0;
						btn_speed_minus.setEnabled(false);
					}
					if(!btn_speed_plus.isEnabled()){
						btn_speed_plus.setEnabled(true);
					}
					setPdSpeed(CurCnySetSpd);
				}else{
					isSetSpeedRunnableRun = false;
				}
				return true;
			}
			break;
		case R.id.btn_main_side_auto_check:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				mHandler.postDelayed(mAutoDetectRunnable, 2000);
				 v.setPressed(true);
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				mHandler.removeCallbacks(mAutoDetectRunnable);
				 v.setPressed(false);
				return true;
			}
		case R.id.cover_view:
			if(lockScreen)
				return true;
			if(imm.isAcceptingText()){
				View a = null;
				while(MainActivity.this.getCurrentFocus()!=null){
					a = MainActivity.this.getCurrentFocus();
					a.clearFocus();
					if(a instanceof EditText){
						break;
					}
				}
        		hideKeyBoard(v);
			}
			break;
		}
		return false;
	}
	
	private Runnable mAutoDetectRunnable = new Runnable() {
		
		@Override
		public void run() {
			int multiHeadState = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CvcState_];
	        if(multiHeadState == Consts.SINGLE_PRINT_HEAD){
	        	if(!mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_)){
	        		mBindService.analyseSysPage(AppData.ERROR_MESSAGE_ID_75);
	        		return;
	        	}
	        }else if(multiHeadState == Consts.DUAL_PRINT_HEAD){
	        	if(!mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_LabEnable_)
	        		&& !mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_)){
	        		mBindService.analyseSysPage(AppData.ERROR_MESSAGE_ID_75);
	        		return;
	        	}
	        }
	        
	        showAutoProcessDialog(null);
			setAutoAdjustment();
			mHandler.sendEmptyMessageDelayed(AUTO_ADJUSTMENT_PROCESS_DETECT, 500);
		}
	};
	
	private Runnable mSpeedPlusRunnable = new Runnable() {
		@Override
		public void run() {
			isSetSpeedRunnableRun = true;
			CurCnySetSpd+=10;
			if(CurCnySetSpd>=255){
				CurCnySetSpd=255;
				btn_speed_plus.setEnabled(false);
				mHandler.removeCallbacks(mSpeedPlusRunnable);
			}else{
				mHandler.postDelayed(this, 500);
			}
			if(!btn_speed_minus.isEnabled()){
				btn_speed_minus.setEnabled(true);
			}
			setPdSpeed(CurCnySetSpd);
		}
	};
	
	private Runnable mSpeedMinusRunnable = new Runnable() {
		@Override
		public void run() {
			isSetSpeedRunnableRun = true;
			CurCnySetSpd-=10;
			if(CurCnySetSpd<=0){
				CurCnySetSpd=0;
				btn_speed_minus.setEnabled(false);
				mHandler.removeCallbacks(mSpeedMinusRunnable);
			}else{
				mHandler.postDelayed(this, 500);
			}
			if(!btn_speed_plus.isEnabled()){
				btn_speed_plus.setEnabled(true);
			}
			setPdSpeed(CurCnySetSpd);
		}
	};
	
	public void backtoActivity(View view) {
	    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		finishFragment();
	}
	
	public void backtoEngineerMenu(View view){
		showFragment(R.id.btn_header_secret);
	}

	private void showKeyBoard(View view){
		if (view != null) {
			synchronized(imm){
				imm.showSoftInput(view, 0);
			}
		    
		}
	}
	private void hideKeyBoard(View view){
		if (view != null) {
			synchronized(imm){
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		    
		}
	}
	
	//TODO Edittext Control
	
	@Override
	public void onReceiveKey(String receiveChar) {
		String Prev_text;
		if(NotEditYet){
			View v = this.getCurrentFocus();
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
	
	private OnEditorActionListener onEditorActionListener = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            int result = actionId & EditorInfo.IME_MASK_ACTION;
            switch(result) {
            case EditorInfo.IME_ACTION_DONE:
                // done stuff
            	View v = MainActivity.this.getCurrentFocus();
            	if(v!=null && v instanceof EditText){
            		//clear focus to trigger onFocusChangeListener
            		isDoneClicked = true;
            		v.clearFocus();
            	}
                break;
            }
            return false;
        }
    };
    
	private OnFocusChangeListener onFocusChangeListener = new OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			//focused -> !focused
			if(v instanceof EditText && !hasFocus){
				NotEditYet = false;
				hideKeyBoard(v);
        		if(!isDoneClicked){
        			//recover original value
        			if(v.getId() != etProduceSpeed.getId()){
        				((EditText)v).setText(mCurEditTextStr);
        			}
        		}else{
        			//success, clear flag, do update value
        			isDoneClicked = false;
        			if(!((EditText)v).getText().toString().matches("-?\\d+(\\.\\d+)?")){
            			//format error recover original value
        				((EditText)v).setText(mCurEditTextStr);
            		}else{
            			//success update value
            			String val = String.format("%d", Integer.valueOf(((EditText)v).getText().toString()));
            			((EditText)v).setText(val);
            			switch(v.getId()){
        				case R.id.et_produce_amount:
        					int CurPuoduceAmount = Integer.parseInt(etProduceAmount.getText().toString());
        					setOutoutNumber(CurPuoduceAmount);
        					break;
        				case R.id.et_label_remained:
        					int CurLabelRemain = Integer.parseInt(etLabelRemain.getText().toString());
        					setRemainingNumber(CurLabelRemain);
        					break;
        				case R.id.et_produce_speed:
        					int CurProduceSpeed = Integer.parseInt(etProduceSpeed.getText().toString());
        					setPdSpeed(CurProduceSpeed);
        					break;
        				}
            		}
        		}
        		//etProduceSpeed change back to display WORD_COMMAND_LabSpd_
        		if(v.getId() == etProduceSpeed.getId()){
    				DecimalInputFilter filter = new DecimalInputFilter(9999, 0, 3, 1);
        			etProduceSpeed.setFilters(new InputFilter[]{filter});
        			etProduceSpeed.setText(String.format("%.1f", Double.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSpd_]/10.0)));
    			}
        	//!focused -> focused
        	}else{
        		//etProduceSpeed change to display WORD_COMMAND_CnySetSpd_
        		if(v.getId() == etProduceSpeed.getId()){
        			DecimalInputFilter intFilter = new DecimalInputFilter(255, 0, 3, 0);
        			intFilter.setOnCharReceiveListener(MainActivity.this);
            		etProduceSpeed.setFilters(new InputFilter[]{intFilter});
            		etProduceSpeed.setText(String.format("%d", Integer.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CnySetSpd_])));
        		}
        		
        		
        		//cash current data when catch focus
        		if(v instanceof EditText){
        			NotEditYet = true;
        			showKeyBoard(v);
        			mCurEditTextStr = ((EditText)v).getText().toString();
        			final EditText et = ((EditText)v);
        			et.post(new Runnable() {
						@Override
						public void run() {
							et.setSelection(mCurEditTextStr.length());
						}
					});
    			}
        	}
		}
	};
	
	 @Override
		protected void onResume() {
			super.onResume();
			if(isMachineRunning()){
				setActivateView();
			}else{
				setInactivateView();
			}
		}
	
    private void setInactivateView() {
    	rl_title_bar.setBackgroundResource(R.drawable.top_bg_g);
    	tv_current_machine_status.setText(R.string.main_monitor_btn_stop);
		etProduceAmount.setVisibility(View.VISIBLE);
		etLabelRemain.setVisibility(View.VISIBLE);
		etProduceSpeed.setVisibility(View.VISIBLE);
		tvProduceAmount.setVisibility(View.INVISIBLE);
		tvLabelRemain.setVisibility(View.INVISIBLE);
		tvProduceSpeed.setVisibility(View.INVISIBLE);
		btn_speed_plus.setVisibility(View.INVISIBLE);
		btn_speed_minus.setVisibility(View.INVISIBLE);
		Utils.recursiveEnableView(ll_side_menu_upper_area, true);
		Utils.recursiveEnableView(ll_side_menu_lower_area, true);
		
    	btStart.setEnabled(true);
    	btStart.setClickable(true);
    	btStop.setEnabled(false);
    	btStop.setClickable(false);
    	btStart.setIsWaiting(false);
		btStop.setIsWaiting(false);
    	etProduceAmount.setEnabled(true);
    	etLabelRemain.setEnabled(true);
    	etProduceSpeed.setEnabled(true);
    	refreshValue();
    	isActivateUI = false;
	}

	private void setActivateView() {
		rl_title_bar.setBackgroundResource(R.drawable.top_bg_r);
		tv_current_machine_status.setText(R.string.main_monitor_btn_start);
		tvProduceAmount.setVisibility(View.VISIBLE);
		tvLabelRemain.setVisibility(View.VISIBLE);
		tvProduceSpeed.setVisibility(View.VISIBLE);
		btn_speed_plus.setVisibility(View.VISIBLE);
		btn_speed_minus.setVisibility(View.VISIBLE);
		etProduceAmount.setVisibility(View.INVISIBLE);
		etLabelRemain.setVisibility(View.INVISIBLE);
		etProduceSpeed.setVisibility(View.INVISIBLE);
		Utils.recursiveEnableView(ll_side_menu_upper_area, false);
		Utils.recursiveEnableView(ll_side_menu_lower_area, true);
    	
    	btStart.setEnabled(false);
    	btStart.setClickable(false);
    	btStop.setEnabled(true);
    	btStop.setClickable(true);
    	btStart.setIsWaiting(false);
		btStop.setIsWaiting(false);
    	if(CurCnySetSpd==255)
    		btn_speed_plus.setEnabled(false);
    	else
    		btn_speed_plus.setEnabled(true);
    	if(CurCnySetSpd==0)
    		btn_speed_minus.setEnabled(false);
    	else
    		btn_speed_minus.setEnabled(true);
    	
    	refreshValue();
    	isActivateUI = true;
	}
	
	public boolean isMachineRunning(){
		return mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_CnyChkOnOff_);
	}
    
	/*
	 * 設定生產數量
	 */
	public void setOutoutNumber(int data) {
		if(mBindService!=null)
			mBindService.setOutoutNumber(data);
	}
	
	/*
	 * 設定標籤剩量
	 */
	public void setRemainingNumber(int data) {
		if(mBindService!=null)
			mBindService.setRemainingNumber(data);
	}
	
	/*
	 * 設定生產速度
	 */
	public void setPdSpeed(int data) {
		if(mBindService!=null)
			mBindService.setPdSpeed(data);
	}
	
	/*
	 * 啟動/停止運轉
	 */
	public void setMachineStartRunning(boolean flag) {
		isChangingToStart = flag;
		if(mBindService!=null)
			mBindService.setMachineStartRunning(flag);
	}
	
	/*
	 * 自動檢測
	 */
	
	public void setAutoAdjustment() {
		if(mBindService!=null)
			mBindService.setAutoAdjustment();
	}
	
	/*
	 * Auto 測物長度
	 */
	public void setAutoBottleDiameter() {
		if(mBindService!=null)
			mBindService.setAutoBottleDiameter();
	}

	/*
	 * Manual 測物長度
	 */
	public void setManualBottleDiameter(int data) {
		if(mBindService!=null)
			mBindService.setManualBottleDiameter(data);
	}
	
	/*
	 * 貼標頭狀態 Left
	 */
	public void setLeftLabEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setLeftLabEnable(flag);
	}
	
	/*
	 * 貼標頭狀態 Right
	 */
	public void setRightLabEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setRightLabEnable(flag);
	}
	
	/*
	 * 左標籤長度
	 */
	public void setLeftLabLength(int data) {
		if(mBindService!=null)
			mBindService.setLeftLabLength(data);
	}
	
	/*
	 * 左標籤長度 Auto
	 */
	public void setAutoLeftLabLength() {
		if(mBindService!=null)
			mBindService.setAutoLeftLabLength();
	}
	
	/*
	 * 右標籤長度
	 */
	public void setRightLabLength(int data) {
		if(mBindService!=null)
			mBindService.setRightLabLength(data);
	}
	
	/*
	 *右標籤長度 Auto
	 */
	public void setAutoRightLabLength() {
		if(mBindService!=null)
			mBindService.setAutoRightLabLength();
	}
	
	/*
	 * 左標籤速度
	 */
	public void setLeftLabSpeed(int data) {
		if(mBindService!=null)
			mBindService.setLeftLabSpeed(data);
	}
	
	/*
	 * 右標籤速度
	 */
	public void setRightLabSpeed(int data) {
		if(mBindService!=null)
			mBindService.setRightLabSpeed(data);
	}
	
	/*
	 * 左出標長度
	 */
	public void setLeftLabLeaveLength(int data) {
		if(mBindService!=null)
			mBindService.setLeftLabLeaveLength(data);
	}
	
	/*
	 * 右出標長度
	 */
	public void setRightLabLeaveLength(int data) {
		if(mBindService!=null)
			mBindService.setRightLabLeaveLength(data);
	}
	
	/*
	 * 左貼標位置
	 */
	public void setLeftLabPosition(int data) {
		if(mBindService!=null)
			mBindService.setLeftLabPosition(data);
	}
	
	/*
	 * 右貼標位置
	 */
	public void setRightLabPosition(int data) {
		if(mBindService!=null)
			mBindService.setRightLabPosition(data);
	}
	
	/*
	 * 左標籤檢測
	 */
	public void setLeftLabPaper(int data) {
		if(mBindService!=null)
			mBindService.setLeftLabPaper(data);
	}
	
	/*
	 * 右標籤檢測
	 */
	public void setRightLabPaper(int data) {
		if(mBindService!=null)
			mBindService.setRightLabPaper(data);
	}
	
	/*
	 * 左標籤感應
	 */
	public void setAutoLeftLabSensor(boolean enble) {
		if(mBindService!=null)
			mBindService.setAutoLeftLabSensor(enble);
	}
	
	/*
	 * 右標籤感應
	 */
	public void setAutoRightLabSensor(boolean enble) {
		if(mBindService!=null)
			mBindService.setAutoRightLabSensor(enble);
	}
	
	/*
	 * 左打印頭狀態
	 */
	public void setLeftTypingEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setLeftTypingEnable(flag);
	}
	
	/*
	 * 右打印頭狀態
	 */
	public void setRightTypingEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setRightTypingEnable(flag);
	}
	
	/*
	 * 打印時間
	 */
	public void setTypingPrintTime(int data) {
		if(mBindService!=null)
			mBindService.setTypingPrintTime(data);
	}
	
	/*
	 * 打印速度
	 */
	public void setTypingHasten(int data) {
		if(mBindService!=null)
			mBindService.setTypingHasten(data);
	}
	
	/*
	 * 加速開關
	 */
	public void setTypingAccelerate(int data) {
		if(mBindService!=null)
			mBindService.setTypingAccelerate(data);
	}
	
	/*
	 * 檢測打印功能
	 */
	public void setDetectTypeSelect(int data) {
		if(mBindService!=null)
			mBindService.setDetectTypeSelect(data);
	}
	
	/*
	 * 檢測打印選擇
	 */
	public void setDetectTypeSelDevice(boolean data) {
		if(mBindService!=null)
			mBindService.setDetectTypeSelDevice(data);
	}
	
	/*
	 * 檢測打印停機數量
	 */
	public void setTypeCheck(int data) {
		if(mBindService!=null)
			mBindService.setTypeCheck(data);
	}
	
	/*
	 * PWEAN - [切換按鈕]
	 */
	public void setPWEAN(int data) {
		if(mBindService!=null)
			mBindService.setPWEAN(data);
	}
	
	/*
	 * MODE - [復歸按鈕]
	 */
	public void setFixPointType(int data) {
		if(mBindService!=null)
			mBindService.setFixPointType(data);
	}
	
	/*
	 * V1SD
	 */
	public void setV1SD(int data) {
		if(mBindService!=null)
			mBindService.setV1SD(data);
	}
	
	/*
	 * V1ED
	 */
	public void setV1ED(int data) {
		if(mBindService!=null)
			mBindService.setV1ED(data);
	}
	
	/*
	 * 動作延遲
	 */
	public void setBackLogOnDelay(int data) {
		if(mBindService!=null)
			mBindService.setBackLogOnDelay(data);
	}
	
	/*
	 * 重置延遲
	 */
	public void setBackLogOffDelay(int data) {
		if(mBindService!=null)
			mBindService.setBackLogOffDelay(data);
	}
	
	/*
	 * 漏標偵測
	 */
	public void setSysLabMissHalt(boolean flag) {
		if(mBindService!=null)
			mBindService.setSysLabMissHalt(flag);
	}
	
	/*
	 * 色帶偵測
	 */
	public void setNoRibbonHalf(boolean flag) {
		if(mBindService!=null)
			mBindService.setNoRibbonHalf(flag);
	}
	
	/*
	 * 倒瓶偵測
	 */
	public void setBotDimChkOnOff(boolean flag) {
		if(mBindService!=null)
			mBindService.setBotDimChkOnOff(flag);
	}
	
	/*
	 * 夾貼距離
	 */
	public void setTypeAccelerate(int data) {
		if(mBindService!=null)
			mBindService.setTypeAccelerate(data);
	}
	
	/*
	 * 分瓶開關
	 */
	public void setBotSeparateEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setBotSeparateEnable(flag);
	}
	
	/*
	 * 分瓶速度
	 */
	public void setBotSeparateSpd(int data) {
		if(mBindService!=null)
			mBindService.setBotSeparateSpd(data);
	}
	
	/*
	 * English
	 */
	public void setLanguageMode(int data) {
		if(mBindService!=null)
			mBindService.setLanguageMode(data);
	}
	
	/*
	 * Version
	 */
	public void getVersion() {
		if(mBindService!=null)
			mBindService.getVersion();
	}
	
	/*
	 * S1 Max Spd.
	 */
	public void setS1TopSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS1TopSpeed(data);
	}
	
	/*
	 * S1 Start Spd.
	 */
	public void setS1StartSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS1StartSpeed(data);
	}
	
	/*
	 * S1 Stop Spd.
	 */
	public void setS1StopSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS1StopSpeed(data);
	}
	
	/*
	 * S1 Par Acc.
	 */
	public void setS1UpRate(int data) {
		if(mBindService!=null)
			mBindService.setS1UpRate(data);
	}
	
	/*
	 * S1 Step Acc.
	 */
	public void setS1UpScale(int data) {
		if(mBindService!=null)
			mBindService.setS1UpScale(data);
	}
	
	/*
	 * S1 Par Dec.
	 */
	public void setS1DnRate(int data) {
		if(mBindService!=null)
			mBindService.setS1DnRate(data);
	}
	
	/*
	 * S1 Step Dec.
	 */
	public void setS1DnScale(int data) {
		if(mBindService!=null)
			mBindService.setS1DnScale(data);
	}
	
	/*
	 * S2 Max Spd.
	 */
	public void setS2TopSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS2TopSpeed(data);
	}
	
	/*
	 * S2 Start Spd.
	 */
	public void setS2StartSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS2StartSpeed(data);
	}
	
	/*
	 * S2 Stop Spd.
	 */
	public void setS2StopSpeed(int data) {
		if(mBindService!=null)
			mBindService.setS2StopSpeed(data);
	}
	
	/*
	 * S2 Par Acc.
	 */
	public void setS2UpRate(int data) {
		if(mBindService!=null)
			mBindService.setS2UpRate(data);
	}
	
	/*
	 * S2 Step Acc.
	 */
	public void setS2UpScale(int data) {
		if(mBindService!=null)
			mBindService.setS2UpScale(data);
	}
	
	/*
	 * S2 Par Dec.
	 */
	public void setS2DnRate(int data) {
		if(mBindService!=null)
			mBindService.setS2DnRate(data);
	}
	
	/*
	 * S2 Step Dec.
	 */
	public void setS2DnScale(int data) {
		if(mBindService!=null)
			mBindService.setS2DnScale(data);
	}
	
	/*
	 * 左電眼 DIST 1
	 */
	public void setLeftDist1(int data) {
		if(mBindService!=null)
			mBindService.setLeftDist1(data);
	}
	
	/*
	 * 左電眼 DIST 2
	 */
	public void setLeftDist2(int data) {
		if(mBindService!=null)
			mBindService.setLeftDist2(data);
	}
	
	/*
	 * 右電眼 DIST 1
	 */
	public void setRightDist1(int data) {
		if(mBindService!=null)
			mBindService.setRightDist1(data);
	}
	
	/*
	 * 右電眼 DIST 2
	 */
	public void setRightDist2(int data) {
		if(mBindService!=null)
			mBindService.setRightDist2(data);
	}
	
	/*
	 * 左/右電眼 ObjPresDist
	 */
	public void setObjPresDist(int data) {
		if(mBindService!=null)
			mBindService.setObjPresDist(data);
	}
	
	/*
	 * LD1
	 */
	public void setLD1(int data) {
		if(mBindService!=null)
			mBindService.setLD1(data);
	}
	
	/*
	 * LD2
	 */
	public void setLD2(int data) {
		if(mBindService!=null)
			mBindService.setLD2(data);
	}
	
	/*
	 * WADI
	 */
	public void setWADI(int data) {
		if(mBindService!=null)
			mBindService.setWADI(data);
	}
	
	/*
	 * WECD
	 */
	public void setWECD(int data) {
		if(mBindService!=null)
			mBindService.setWECD(data);
	}
	
	/*
	 * ECD
	 */
	public void setECD(int data) {
		if(mBindService!=null)
			mBindService.setECD(data);
	}
	
	/*
	 * ROT1
	 */
	public void setROT1(int data) {
		if(mBindService!=null)
			mBindService.setROT1(data);
	}
	
	/*
	 * ROT2
	 */
	public void setROT2(int data) {
		if(mBindService!=null)
			mBindService.setROT2(data);
	}
	
	/*
	 * CD1
	 */
	public void setCD1(int data) {
		if(mBindService!=null)
			mBindService.setCD1(data);
	}
	
	/*
	 * ROOLEN
	 */
	public void setRoolEn(int data) {
		if(mBindService!=null)
			mBindService.setRoolEn(data);
	}
	/*
	 *貼標測試 
	 */
	public void setLabelTestModeEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setLabelTestModeEnable(flag);
	}
	
	/*
	 *出標測試 
	 */
	public void setExtensionTestEnable(boolean flag) {
		if(mBindService!=null)
			mBindService.setExtensionTestEnable(flag);
	}
	
	public void insertCmd(byte[] cmd) {
		if(mBindService!=null)
			mBindService.insertCmd(cmd);
	}
	
	public void setCmdDlay(int delayMilliSec) {
		if(mBindService!=null)
			mBindService.setCmdDlay(delayMilliSec);
	}
	
	public void initSysPageParam(){
		if(mBindService!=null)
			mBindService.initSysPageParam();
	}
	
	public void initParameter(int machineType){
		if(mBindService!=null) {
			mBindService.showLoadingDialog();
			mBindService.pauseUartCommandLoop(true);
			mBindService.clearUartCommandQueue();
		}
		
		MachineType.setDefValue(machineType, mDatabase);
		mDatabase.saveAppAllParameter();
		
		if(mBindService!=null) {
			mBindService.reStartMachineInit();
			//mBindService.pauseUartCommandLoop(false);
		}
	};
	
	public void initParameterAndMemory(int machineType){
		if(mBindService!=null) {
			mBindService.showLoadingDialog();
			mBindService.pauseUartCommandLoop(true);
			mBindService.clearUartCommandQueue();
		}
		
		mDatabase.clearMachineMemData();
		MachineType.setDefValue(machineType, mDatabase);
		mDatabase.saveAppAllParameter();
		
		if(mBindService!=null) {
			mBindService.reStartMachineInit();
			//mBindService.pauseUartCommandLoop(false);
		}
	};
	//for load memory
	public void reInitParameter(){
		if(mBindService!=null) {
			mBindService.showLoadingDialog();
			mBindService.pauseUartCommandLoop(true);
			mBindService.clearUartCommandQueue();
			mBindService.reStartMachineInit();
		}
	}
	
	public void onMainBoardIONoMatchError(){
		btn_main_side_auto_check.setEnabled(false);
    	btStart.setEnabled(false);
    	btStop.setEnabled(false);
	}
	
	public void upgradeAPPorFW(boolean flag){
		if(mBindService!=null) {
			if(flag) {
				mBindService.pauseUartCommandLoop(true);
				mBindService.clearUartCommandQueue();
			}
			//else {
			//	mBindService.pauseUartCommandLoop(false);
			//}
		}
	}

	public void onErrorOccurred(int errorType) {
		//if auto detecting stop it
		mHandler.removeMessages(AUTO_ADJUSTMENT_PROCESS_DETECT);
		//Auto檢測流程dialog也須中斷
		closeAutoProcessDialog();
		
		//if fragment is auto detecting stop it
		Fragment curFrag = fragmentManager.findFragmentByTag(String.valueOf(CurFragment));
		if(curFrag instanceof IOnErrorOccurred){
			((IOnErrorOccurred) curFrag).onErrorOccurred();
		}
	}
	
	public void showAutoProcessDialog(IDialogValueListener listener){
		mAutoProcessDialog = CustomDialogBuilder.getInstance()
				.setContext(this)
				.setValueRetListener(listener)
				.setInflateLayout(R.layout.custom_dialog_auto_detect_process)
				.build();
		mAutoProcessDialog.show();
	}
	
	public void closeAutoProcessDialog(){
		if(mAutoProcessDialog!=null)mAutoProcessDialog.cancel();
	}
	
	public void onUartInitFinish(){
		main_view.setFocusableInTouchMode(true);
		lockScreen = true;
		main_view.post(new Runnable() {
	          public void run() {
	        	  main_view.requestFocusFromTouch();
	        	  main_view.requestFocus();
	        	  showKeyBoard(main_view);
	        	  main_view.clearFocus();
			      hideKeyBoard(main_view);
			      lockScreen = false;
	          }
	      });
	}

	
}
