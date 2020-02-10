package endexcase.scanmachine.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.uart.STM32;
import endexcase.scanmachine.uart.SportInterface;
import endexcase.scanmachine.util.Utils;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Zack on 15/6/16.
 */
public class EngineerVerAndMachineInfoFragment extends BaseFragment implements
		View.OnClickListener,View.OnTouchListener , IEditTextDoneListener {

	private TextView tvMachineType;
	
	private TextView tvApp, tvGateway, tvMaster, tvSlave1, tvSlave2;
	
	private Button btn_init_system_param_only, btn_init_all, btn_read_software_ver,
	btn_update_frameware, btn_update_app;
	
	private EditText et_title_wk_hour, et_title_lf_hour;
	
	private RelativeLayout rl_btn_machine_type, btn_header_back;
	
	private Dialog mMachineTypeDialog;
	
	private String[] mMachineTypeArr;
	
	static final String RomReadPath = "/mnt/usb_storage/";
	static final String RomWritePath = "/storage/sdcard0/Download/";
	private AlertDialog mInstallFwDialog;
	static FirmwareUpgrade mFirmwareUpgradeProcess;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(
				R.layout.fragment_engineering_ver_and_machine_info, container,
				false);
		init(view);
		mFirmwareUpgradeProcess = new FirmwareUpgrade();
		return view;
	}

	private void refreshValue() {
		et_title_wk_hour.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TotalRunTimeHourL_]));
		et_title_lf_hour.setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LimitRunTime_]));
		
		String verName = "";
		try {
			verName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		tvApp.setText(verName);
		String strGatewayVer = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_GatewaySoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_GatewaySoftwareDate_]);
		String strMasterVer = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MasterSoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MasterSoftwareDate_]);
		String strSlave1Ver = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave1SoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave1SoftwareDate_]);
		String strSlave2Ver = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave2SoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave2SoftwareDate_]);
		
		tvGateway.setText(strGatewayVer);
		tvMaster.setText(strMasterVer);
		tvSlave1.setText(strSlave1Ver);
		tvSlave2.setText(strSlave2Ver);
		
		mMachineTypeArr = getResources().getStringArray(R.array.machine_type_arr);
		String machineTypeStr = mMachineTypeArr[mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]];
		tvMachineType.setText(machineTypeStr);
	}

	private void init(View view) {
		super.setTitle(view, R.string.engineer_page_menu_title_ver_and_machine);
		
		//edittext
		et_title_wk_hour = (EditText)view.findViewById(R.id.et_title_wk_hour);
		et_title_lf_hour = (EditText)view.findViewById(R.id.et_title_lf_hour);
		
		registerEdittextDoneListener(et_title_wk_hour, this);
		registerEdittextDoneListener(et_title_lf_hour, this);
		
		//btn
		btn_header_back = (RelativeLayout) view.findViewById(R.id.btn_header_back);
		rl_btn_machine_type = (RelativeLayout) view.findViewById(R.id.rl_btn_machine_type);
		btn_read_software_ver = (Button)view.findViewById(R.id.btn_read_software_ver);
		btn_init_system_param_only = (Button)view.findViewById(R.id.btn_init_system_param_only);
		btn_init_all = (Button)view.findViewById(R.id.btn_init_all);
		btn_update_frameware = (Button)view.findViewById(R.id.btn_update_faremware);
		btn_update_app = (Button)view.findViewById(R.id.btn_update_app);
		
		btn_header_back.setOnTouchListener(this);
		rl_btn_machine_type.setOnClickListener(this);
		btn_read_software_ver.setOnClickListener(this);
		btn_init_system_param_only.setOnClickListener(this);
		btn_init_all.setOnClickListener(this);
		btn_update_frameware.setOnClickListener(this);
		btn_update_app.setOnClickListener(this);
		
		//textView
		tvApp = (TextView)view.findViewById(R.id.tv_content_app);
		tvGateway = (TextView)view.findViewById(R.id.tv_content_gateway);
		tvMaster = (TextView)view.findViewById(R.id.tv_content_master);
		tvSlave1 = (TextView)view.findViewById(R.id.tv_content_slave1);
		tvSlave2 = (TextView)view.findViewById(R.id.tv_content_slave2);
		tvMachineType = (TextView)view.findViewById(R.id.tv_machine_type);
		
		mMachineTypeDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_machine_type)
				.setOnItemClickListener(itemListener)
				.build();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onResume() {
		super.onResume();
		refreshValue();
	}

	@Override
	public void onClick(View v) {
		int curLang;
		switch (v.getId()) {
			case R.id.rl_btn_machine_type:
				if (!mMachineTypeDialog.isShowing()) 
					mMachineTypeDialog.show();
				break;
			case R.id.btn_read_software_ver:
				mActivity.getVersion();
				refreshValue();
				break;
			case R.id.btn_init_system_param_only:
				curLang = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_];
				mActivity.initParameter(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]);
				mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_] = curLang;
				refreshValue();
				break;
			case R.id.btn_init_all:
				curLang = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_];
				mActivity.initParameterAndMemory(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]);
				mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_] = curLang;
				refreshValue();
				break;
			case R.id.btn_update_faremware:
				mActivity.upgradeAPPorFW(true);
				FirmwareInstall();
				break;
			case R.id.btn_update_app:
				mActivity.upgradeAPPorFW(true);
				AppInstall();
				break;
		}
	}
	
	private OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			tvMachineType.setText(mMachineTypeArr[position]);
			mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_] = position;
			mMachineTypeDialog.dismiss();
		}
	};

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		switch(viewId){
		case R.id.et_title_wk_hour:
			et_title_wk_hour.setText(String.format("%d", Integer.valueOf(retValue)));
			mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TotalRunTimeHourL_]=Integer.valueOf(retValue);
			break;
		case R.id.et_title_lf_hour:
			et_title_lf_hour.setText(String.format("%d", Integer.valueOf(retValue)));
			mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_TotalRunTimeHourL_]=Integer.valueOf(retValue);
			break;
		}
	}

	private void AppInstall() {
		File fileSet[] = new File("/mnt/usb_storage/").listFiles();
		String temp = "";
		AlertDialog mAlertDialog;
		
		if (fileSet == null) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			mAlertDialog = builder.create();
			mAlertDialog.setMessage(getString(R.string.machine_ver_info_update_app_failure));
			mAlertDialog.setCancelable(false);
			mAlertDialog.setCanceledOnTouchOutside(true);
			mAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mAlertDialog.show();
			
			TextView messageView = (TextView)mAlertDialog.findViewById(android.R.id.message);
			messageView.setGravity(Gravity.CENTER);
			messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
			return;
		}
		
		if (fileSet != null) {
			for (int i = 0; i < fileSet.length; i++) {

				if (fileSet[i].getName().indexOf("CVC_") == 0) // 確認檔名開頭為 CVC_
				{ // 確認檔案尾部為 .apk
					temp = fileSet[i].getName().substring(
							fileSet[i].getName().lastIndexOf("."));
					if (temp.toLowerCase().equals(".apk")) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						
						mAlertDialog = builder.create();
						mAlertDialog.setMessage(getString(R.string.machine_ver_info_title_update_app));
						mAlertDialog.setCancelable(false);
						mAlertDialog.setCanceledOnTouchOutside(false);
						mAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
						mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
						mAlertDialog.show();
						
						TextView messageView = (TextView)mAlertDialog.findViewById(android.R.id.message);
						messageView.setGravity(Gravity.CENTER);
						messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
						
						Intent intent = new Intent();
						intent.setAction("com.gemminer.intent.action.update");
						intent.putExtra("apk", "/mnt/usb_storage/" + fileSet[i].getName());
						getActivity().sendBroadcast(intent);
						return;
					}
				}
			}
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		mAlertDialog = builder.create();
		mAlertDialog.setMessage(getString(R.string.machine_ver_info_update_app_failure));
		mAlertDialog.setCancelable(false);
		mAlertDialog.setCanceledOnTouchOutside(true);
		mAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		mAlertDialog.show();
		
		TextView messageView = (TextView)mAlertDialog.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.CENTER);
		messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
	}
	
	private void showInstallDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		mInstallFwDialog = builder.create();
		mInstallFwDialog.setMessage(getString(R.string.machine_ver_info_update_app_failure));
		mInstallFwDialog.setCancelable(false);
		mInstallFwDialog.setCanceledOnTouchOutside(false);
		mInstallFwDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mInstallFwDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		mInstallFwDialog.show();
		
		TextView messageView = (TextView)mInstallFwDialog.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.CENTER);
		messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
	}
	
	private void closeInstallDialog(){
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				if(mInstallFwDialog!=null){
					if(mInstallFwDialog.isShowing()) mInstallFwDialog.dismiss();
					mInstallFwDialog=null;
				}
			}
		});
	}
	
	private void showUpgradeDialog(){
		getActivity().runOnUiThread(new Runnable() {
			public void run() {
				AlertDialog mAlertDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				mAlertDialog = builder.create();
				mAlertDialog.setMessage(getString(R.string.machine_ver_info_restart_message));
				mAlertDialog.setCancelable(false);
				mAlertDialog.setCanceledOnTouchOutside(false);
				mAlertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				mInstallFwDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				mAlertDialog.show();
				
				TextView messageView = (TextView)mAlertDialog.findViewById(android.R.id.message);
				messageView.setGravity(Gravity.CENTER);
				messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
			}
		});
	}
	
	static File getInstall_BinFile(){
		File fileSet[] = new File(RomReadPath).listFiles();
		String temp="";
		if(fileSet==null) {
			return null;
		}
		
		if (fileSet != null) {
			for (int i = 0; i < fileSet.length; i++) {
				if (fileSet[i].getName().indexOf("CVC_") == 0) //確認檔名開頭為 CVC_
				{
					temp = fileSet[i].getName().substring(fileSet[i].getName().lastIndexOf("."));
					if (temp.toLowerCase().equals(".bin")){
						//fileName = fileSet[i].getName();
						return fileSet[i];}
				}	
			}
		}
		return null;
	}
	
	private void FirmwareInstall() {
		File install = getInstall_BinFile(); 
		
		if (install==null) {
			Toast.makeText(getActivity(), "Firmware File Not Found", Toast.LENGTH_SHORT).show();
			return;
		}
		else {// SDcard data to /storage/sdcard0/Download/ 	
			try {
				InputStream is = new FileInputStream(install);
				OutputStream os = new FileOutputStream(RomWritePath+"temp.bin");
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) { os.write(buffer, 0, length);  }
				os.flush();
				os.close();
				is.close();
			}catch (Exception e) {
			}
		}

		if (!new File(RomWritePath+"temp.bin").exists()) {
			Toast.makeText(getActivity(), "Firmware File Not Found", Toast.LENGTH_SHORT).show();
			return;
		}
		
		showInstallDialog();
		
		mFirmwareUpgradeProcess.execute() ;
	}
	
	class FirmwareUpgrade extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			SystemClock.sleep(1000);
			SportInterface.downloadPin(SportInterface.DOWNLOAD_PIN_HIGH);
			if(!STM32.bootloader())
				return null;

			SystemClock.sleep(5000);
			try { 
				if(!STM32.Init()) {
					return null;
				}
				//STM32.Get();if(STM32.Nack()){ReStartDialog(true);ToastMessageHandler.obtainMessage(905, 0, 0).sendToTarget();return null;}
				
				if(!STM32.ReadUnlock()) {
					return null;
				}
				SystemClock.sleep(1000);
				
				if(!STM32.Init()) {
					return null;
				}
				
				if(!STM32.EraseAll()) {
					return null;
				}
				
				if(!STM32.WriteMemory(RomWritePath, "temp.bin")) {
					return null;
				}
				
				if(!STM32.ReadProtect()) {
					return null;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			SportInterface.downloadPin(SportInterface.DOWNLOAD_PIN_LOW);
			closeInstallDialog();
			showUpgradeDialog();
			super.onPostExecute(result);
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
