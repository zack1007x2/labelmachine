package endexcase.scanmachine.service;

import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.MachineType;
import endexcase.scanmachine.activity.MainActivity;
import endexcase.scanmachine.database.DatabaseProxy;
import endexcase.scanmachine.database.SharedPrefConstants;
import endexcase.scanmachine.uart.CommandQueue;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.uart.SendCommandLoop;
import endexcase.scanmachine.uart.SportInterface;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.Hex;
import endexcase.scanmachine.widget.CustomDialogBuilder;

public class ControlService extends Service{
	private static final AppLog smAppLog = new AppLog("service",ControlService.class);
	private static final AppLog smUartLog = new AppLog("uart",ControlService.class);
	
	@Nullable private AlertDialog mUartInitDialog;
	@Nullable private AlertDialog mErrorDialog = null;
	private Dialog loadingDialog;
	
	//UART
	private boolean mJniInit = false;
	private final CommandQueue mUrgentQueue = new CommandQueue(5);
	private final CommandQueue mNormalQueue = new CommandQueue(20);
	private SendCommandLoop mCmdLoop;
	
	private int CMD_DELAY_MILLI_SEC = 10;

	//Database
	private AppData mAppdata = null;
	private DatabaseProxy mDatabase;
	
	// bind service
	private final ControlServiceBinder mBinder = new ControlServiceBinder();
	@Nullable private MainActivity mBoundActivity = null;

	@Override
	public void onCreate() {
		//startStrictMode();
		super.onCreate();
		
		startForeground();
		
		//Initial database
		mAppdata = AppData.getInstance();
		mDatabase = DatabaseProxy.getInstance(this);
		
		//Initial Uart
		mCmdLoop = new SendCommandLoop(this, mUrgentQueue, mNormalQueue);
		showUartInitDialog();
		mJniInit = uartInit();
		initSysPageParam();
		startMachineInit();
		
		loadingDialog = CustomDialogBuilder.getInstance()
				.setContext(ControlService.this)
				.setInflateLayout(R.layout.custom_dialog_loading)
				.build();
		
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void startForeground(){
		int id = (int)(System.currentTimeMillis());
		Notification notification = new Notification.Builder(this)
			.setContentTitle("EndexBackgroundService")
			.setSmallIcon(R.mipmap.ic_launcher)
			.build();
		startForeground(id, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		smAppLog.d("Service#onStartCommand()");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public void setBoundActivity(MainActivity activity){
		if(activity==null)
			throw new NullPointerException("setBoundActivity can not be used with a null activity.If you want to clear bound activity,use clearBoundActivity() instead.");
		mBoundActivity = activity;
	}

	public class ControlServiceBinder extends Binder{
		public ControlService getControlService(){
			return ControlService.this;
		}
	}
	
	private boolean uartInit() {
		boolean uartInit = false;
		while(!uartInit){
			long JniInitStartTime = System.currentTimeMillis();
			long JniInitReTryTime = System.currentTimeMillis();
			try{
				SportInterface.init();
				SportInterface.setSpeed(115200);// progard
				SportInterface.log(1);
				byte a = 0x44;
				SportInterface.setParity(a);
				SportInterface.downloadPin(SportInterface.DOWNLOAD_PIN_LOW);
				uartInit = true ;
				smAppLog.d("uartInit finish");
				return uartInit;
			}catch(ExceptionInInitializerError e){
				smAppLog.e(e.toString());
				uartInit = false;
				continue;
			}catch(NoClassDefFoundError e){
				smAppLog.e(e.toString());
				uartInit = false;
				continue;
			}catch(Exception e){
				smAppLog.e(e.toString());
				uartInit = false;
				JniInitReTryTime = System.currentTimeMillis();
				if(JniInitReTryTime-JniInitStartTime>=5000){
					// Time Out
					smAppLog.e("Uart device init time out !");
					smAppLog.e("Uart connecting failed");
					return uartInit;
				}
				SportInterface.setSpeed(57600);// progard
				smAppLog.d("Sport.DOWNLOAD_PIN_LOW = " + SportInterface.DOWNLOAD_PIN_LOW); //progard
				smAppLog.d("Sport.DOWNLOAD_PIN_HIGH = "+SportInterface.DOWNLOAD_PIN_HIGH); //progard
				SportInterface.getBytes();// progard
				SportInterface.dumpNBytes(0);// progard
				SportInterface.deinit();// progard
				continue;
			}
		}
		return uartInit;
	}
	
	/**開始設定各項參數*/
	public void startMachineInit() {
		Thread machineInitThread = new Thread(new Runnable(){
			private final Handler mCallbackHandler = new Handler();
			@Override
			public void run() {
				byte[] result = null;
				byte[] readySend = null;
				//Send Word Parameters to GW
				for( int i=0; i<Consts.MACHINE_WORD_PARAMETER_SIZE; i++) {
					SystemClock.sleep(CMD_DELAY_MILLI_SEC);
					readySend = EndexScanProtocols.getWordRequestCommand(i, mAppdata.mWordPara[i]);
					SportInterface.sendBytes(readySend,readySend.length);
					
					while(true) { //等待接收Echo指令
						SystemClock.sleep(10);
						smUartLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
						try{
							result = SportInterface.catchNBytes(1);
						}catch(TimeoutException e){
							smAppLog.e(e.toString());
							showError();
							return;
						}catch(NullPointerException e){
							smAppLog.e(e.toString());
							showError();
							return;
						}
						
						smUartLog.d("Get Value result = "+Hex.hexBytesToString(result));
						if(!EndexScanProtocols.isEchoCommand(result[0])) {
							byte cmd = EndexScanProtocols.checkCommandType(result[0]);
							if(cmd == EndexScanProtocols.COMMAND_TYPE_BYTE) {
								mCmdLoop.recvByteCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_WORD) {
								mCmdLoop.recvWORDCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_DWORD) {
								mCmdLoop.recvDWORDCommand();
							}
							else {
								smAppLog.d("Not Echo ");
								showError();
								return;
							}
						}
						else {
							break;
						}
					}
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte) mAppdata.mAddr15) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR17, (byte) mAppdata.mAddr17) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR19, (byte) mAppdata.mAddr19) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR20, (byte) mAppdata.mAddr20) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if(mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_MainBoardIONoMatch_)) {
					mCallbackHandler.post(new Runnable(){
						@Override
						public void run() {
							final String msgText = getString(R.string.system_error_machine_notnatch);
							closeUartInitDialog();
							showErrorDialog(msgText, true);
							if(mBoundActivity!=null)
								mBoundActivity.onMainBoardIONoMatchError();
						}
					});
					return;
				}
				
				closeDialog();
				mCmdLoop.start();
				
				// 如果是KK 956-U 和CVC 430-U 的機種，APP 開機完成後，必需去判讀CheckRollPaste_旗標來決定RollPasteEnable_數值。
				int machineType = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_];
				if(machineType == MachineType.KK956U || machineType==MachineType.CVC430U){
					boolean check = mAppdata.getBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_CheckRollPaste_);
					setRollPasteEnable(check);
				}
			}
			
			private void showError() {
				mCallbackHandler.post(new Runnable(){
					@Override
					public void run() {
						closeUartInitDialog();
						showErrorDialog(getString(R.string.str_uart_error), false);
						if(mBoundActivity!=null)
							mBoundActivity.onErrorOccurred(R.string.str_uart_error);
					}
				});
			}
			
			private void closeDialog() {
				mCallbackHandler.post(new Runnable(){
					@Override
					public void run() {
						closeUartInitDialog();
					}
				});
			}
			
			private boolean sendByteParams(int addr, byte data) {
				byte[] result = null;
				byte[] readySend = null;
				
				readySend = EndexScanProtocols.getByteRequestCommand( addr, (byte) data);
				
				smUartLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
				
				SportInterface.sendBytes(readySend,readySend.length);
				
				while(true) { //等待接收Echo指令
					SystemClock.sleep(10);
					
					try{
						result = SportInterface.catchNBytes(1);
					}catch(TimeoutException e){
						smAppLog.e(e.toString());
						return false;
					}catch(NullPointerException e){
						smAppLog.e(e.toString());
						return false;
					}
					
					smUartLog.d("Get Value result = "+Hex.hexBytesToString(result));
					if(!EndexScanProtocols.isEchoCommand(result[0])) {
						byte cmd = EndexScanProtocols.checkCommandType(result[0]);
						if(cmd == EndexScanProtocols.COMMAND_TYPE_BYTE) {
							mCmdLoop.recvByteCommand();
						}
						else if(cmd == EndexScanProtocols.COMMAND_TYPE_WORD) {
							mCmdLoop.recvWORDCommand();
						}
						else if(cmd == EndexScanProtocols.COMMAND_TYPE_DWORD) {
							mCmdLoop.recvDWORDCommand();
						}
						else {
							smAppLog.d("Not Echo ");
							return false;
						}
					}
					else {
						smAppLog.d("Is Echo ");
						break;
					}
				}
				return true;
			}
		});
		machineInitThread.start();
	}
	
	public void reStartMachineInit() {
		Thread machineInitThread = new Thread(new Runnable(){
			private final Handler mCallbackHandler = new Handler();
			@Override
			public void run() {
				SystemClock.sleep(1000);
				byte[] result = null;
				byte[] readySend = null;
				//Send Word Parameters to GW
				for( int i=0; i<Consts.MACHINE_WORD_PARAMETER_SIZE; i++) {
					SystemClock.sleep(CMD_DELAY_MILLI_SEC);
					readySend = EndexScanProtocols.getWordRequestCommand(i, mAppdata.mWordPara[i]);
					SportInterface.sendBytes(readySend,readySend.length);
					
					while(true) { //等待接收Echo指令
						SystemClock.sleep(10);
						smAppLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
						try{
							result = SportInterface.catchNBytes(1);
						}catch(TimeoutException e){
							smAppLog.e(e.toString());
							showError();
							return;
						}catch(NullPointerException e){
							smAppLog.e(e.toString());
							showError();
							return;
						}
						
						smAppLog.d("Get Value result = "+Hex.hexBytesToString(result));
						if(!EndexScanProtocols.isEchoCommand(result[0])) {
							byte cmd = EndexScanProtocols.checkCommandType(result[0]);
							if(cmd == EndexScanProtocols.COMMAND_TYPE_BYTE) {
								mCmdLoop.recvByteCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_WORD) {
								mCmdLoop.recvWORDCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_DWORD) {
								mCmdLoop.recvDWORDCommand();
							}
							else {
								smAppLog.d("Not Echo ");
								showError();
								return;
							}
						}
						else {
							break;
						}
					}
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte) mAppdata.mAddr15) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR17, (byte) mAppdata.mAddr17) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR19, (byte) mAppdata.mAddr19) ) {
					showError();
					return;
				}
				
				SystemClock.sleep(CMD_DELAY_MILLI_SEC);
				if( !sendByteParams(EndexScanProtocols.BYTE_COMMAND_ADDR20, (byte) mAppdata.mAddr20) ) {
					showError();
					return;
				}
				dismissLoadingDialog();
				pauseUartCommandLoop(false);
			}
			
			private void showError() {
				mCallbackHandler.post(new Runnable(){
					@Override
					public void run() {
						//closeUartInitDialog();
						dismissLoadingDialog();
						showErrorDialog(getString(R.string.str_uart_error), false);
						if(mBoundActivity!=null)
							mBoundActivity.onErrorOccurred(R.string.str_uart_error);
					}
				});
			}
			
			private boolean sendByteParams(int addr, byte data) {
				byte[] result = null;
				byte[] readySend = null;
				
				readySend = EndexScanProtocols.getByteRequestCommand( addr, (byte) data);
				
				smAppLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
				
				SportInterface.sendBytes(readySend,readySend.length);
				
				while(true) { //等待接收Echo指令
					SystemClock.sleep(10);
					
					try{
						result = SportInterface.catchNBytes(1);
					}catch(TimeoutException e){
						smAppLog.e(e.toString());
						return false;
					}catch(NullPointerException e){
						smAppLog.e(e.toString());
						return false;
					}
					
					smAppLog.d("Get Value result = "+Hex.hexBytesToString(result));
					if(!EndexScanProtocols.isEchoCommand(result[0])) {
						byte cmd = EndexScanProtocols.checkCommandType(result[0]);
						if(cmd == EndexScanProtocols.COMMAND_TYPE_BYTE) {
							mCmdLoop.recvByteCommand();
						}
						else if(cmd == EndexScanProtocols.COMMAND_TYPE_WORD) {
							mCmdLoop.recvWORDCommand();
						}
						else if(cmd == EndexScanProtocols.COMMAND_TYPE_DWORD) {
							mCmdLoop.recvDWORDCommand();
						}
						else {
							smAppLog.d("Not Echo ");
							return false;
						}
					}
					else {
						smAppLog.d("Is Echo ");
						break;
					}
				}
				return true;
			}
		});
		machineInitThread.start();
	}
	
	private void showUartInitDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		mUartInitDialog = builder.create();
		mUartInitDialog.setMessage(getString(R.string.str_uart_init));
		mUartInitDialog.setCancelable(false);
		mUartInitDialog.setCanceledOnTouchOutside(false);
		mUartInitDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mUartInitDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		mUartInitDialog.show();
		
		TextView messageView = (TextView)mUartInitDialog.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.CENTER);
		messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
	}
	
	private void closeUartInitDialog(){
		if(mUartInitDialog!=null) {
			if(mUartInitDialog.isShowing()) mUartInitDialog.dismiss();
			mUartInitDialog=null;
			if(mBoundActivity!=null)mBoundActivity.onUartInitFinish();
		}
	}
	
	private void showErrorDialog(String text, boolean outsideTouchable){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		if(mErrorDialog == null || !mErrorDialog.isShowing()) {
			mErrorDialog = builder.create();
			mErrorDialog.setMessage(text);
			mErrorDialog.setCancelable(false);
			mErrorDialog.setCanceledOnTouchOutside(outsideTouchable);
			mErrorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mErrorDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mErrorDialog.show();
			
			TextView messageView = (TextView)mErrorDialog.findViewById(android.R.id.message);
			messageView.setGravity(Gravity.CENTER);
			messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
		}
	}
	
	private void showSysErrorDialog(String text, boolean outsideTouchable) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		if(mErrorDialog == null || !mErrorDialog.isShowing()) {
			mErrorDialog = builder.create();
			mErrorDialog.setMessage(text);
			mErrorDialog.setCancelable(false);
			mErrorDialog.setCanceledOnTouchOutside(outsideTouchable);
			mErrorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			mErrorDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			mErrorDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					byte[] cmd = null;
					mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_] = 2;
					cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_SysPage_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]);
					mNormalQueue.insertCommand(cmd);
					mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_SysPage_);
					mErrorDialog=null;
				}
				
			});
			mErrorDialog.show();
			
			TextView messageView = (TextView)mErrorDialog.findViewById(android.R.id.message);
			messageView.setGravity(Gravity.CENTER);
			messageView.setTextSize(getResources().getDimension(R.dimen.text_size_m));
		}
	}
	
	private void closeErrorDialog(){
		if(mErrorDialog!=null) {
			if(mErrorDialog.isShowing()) mErrorDialog.dismiss();
			mErrorDialog=null;
		}
	}

	/**This method is for Performance Debug Only*/
	private void startStrictMode(){
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork()   // or .detectAll() for all detectable problems
			.penaltyLog()
			.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.detectLeakedClosableObjects()
			.penaltyLog()
			.penaltyDeath()
			.build());
	}
	
	public void startTest(){
		Thread testThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while(true){
					try{
						byte[] cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte)0x11, 
								EndexScanProtocols.BYTE_COMMAND_ADDR15_AutoManual_, true);
						mUrgentQueue.insertCommand(cmd);
						Thread.sleep(500);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		});
		testThread.start();
	}
	
	public void pauseUartCommandLoop(boolean flag) {
		mCmdLoop.pause(flag);
	}
	
	public void clearUartCommandQueue() {
		mUrgentQueue.clean();
		mNormalQueue.clean();
	}
	
	public void saveByteData(int addr, int data) {
		switch(addr) {
		case EndexScanProtocols.BYTE_COMMAND_ADDR15:
			mAppdata.mAddr15 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR16:
			mAppdata.mAddr16 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR17:
			mAppdata.mAddr17 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR18:
			mAppdata.mAddr18 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR19:
			mAppdata.mAddr19 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR20:
			mAppdata.mAddr20 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR21:
			mAppdata.mAddr21 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR22:
			mAppdata.mAddr22 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR23:
			mAppdata.mAddr23 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR24:
			mAppdata.mAddr24 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR25:
			mAppdata.mAddr25 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR26:
			mAppdata.mAddr26 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR27:
			mAppdata.mAddr27 = data;
			break;
		case EndexScanProtocols.BYTE_COMMAND_ADDR28:
			mAppdata.mAddr28 = data;
			break;
		}
		mDatabase.saveAppByteParameter();
	}
	
	public void saveWordData(int addr, int data) {
		smAppLog.d("JACK addr: " + addr);
		smAppLog.d("JACK data: " + data);
		mAppdata.mWordPara[addr] = data;
		mDatabase.saveAppWordParameter(addr);
	}
	
	public void saveDWordData(int addr, int data1, int data2) {
		//TODO
	}
	
	public void analyseSysPage(int data) {
		String mesg = null;
		switch(data) {
		case AppData.ERROR_MESSAGE_ID_64:
			mesg = getString(R.string.system_error_message_64);
			break;
		case AppData.ERROR_MESSAGE_ID_65:
			mesg = getString(R.string.system_error_message_65);
			break;
		case AppData.ERROR_MESSAGE_ID_66:
			mesg = getString(R.string.system_error_message_66);
			break;
		case AppData.ERROR_MESSAGE_ID_67:
			mesg = getString(R.string.system_error_message_67);
			break;
		case AppData.ERROR_MESSAGE_ID_68:
			mesg = getString(R.string.system_error_message_68);
			break;
		case AppData.ERROR_MESSAGE_ID_69:
			mesg = getString(R.string.system_error_message_69);
			break;
		case AppData.ERROR_MESSAGE_ID_70:
			mesg = getString(R.string.system_error_message_70);
			break;
		case AppData.ERROR_MESSAGE_ID_75:
			mesg = getString(R.string.system_error_message_75);
			break;
		case AppData.ERROR_MESSAGE_ID_76:
			mesg = getString(R.string.system_error_message_76);
			break;
		case AppData.ERROR_MESSAGE_ID_77:
			mesg = getString(R.string.system_error_message_77);
			break;
		case AppData.ERROR_MESSAGE_ID_81:
			mesg = getString(R.string.system_error_message_81);
			break;
		case AppData.ERROR_MESSAGE_ID_83:
			mesg = getString(R.string.system_error_message_83);
			break;
		case AppData.ERROR_MESSAGE_ID_85:
			mesg = getString(R.string.system_error_message_85);
			break;
		}

		if(mesg != null) {
			final String msgText = mesg;
			
			if(mBoundActivity != null) {
				mBoundActivity.onErrorOccurred(data);
				mBoundActivity.runOnUiThread(new Runnable() {
					public void run() {
						showSysErrorDialog(msgText, true);
					}
				});
			}
		}else{
			closeErrorDialog();
		}
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_] = data;
		
	}
	
	public void setOutoutNumber(int data) {
		byte[] cmd = null;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyH_] = (data >> 16) & 0xFFFF;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyL_] = data & 0xFFFF;
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditLabQtyEnable_, true);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditRsvQtyEnable_, false);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabQtyH_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyH_]);
		mNormalQueue.insertCommand(cmd);
		
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabQtyL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyL_]);
		mNormalQueue.insertCommand(cmd);
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditLabQtyEnable_, false);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditRsvQtyEnable_, false);
		
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabQtyH_);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabQtyL_);
		mDatabase.saveAppByteParameter();
	}
	
	public void setRemainingNumber(int data) {
		byte[] cmd = null;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyH_] = (data >> 16) & 0xFFFF;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyL_] = data & 0xFFFF;
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditLabQtyEnable_, false);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditRsvQtyEnable_, true);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RsvQtyH_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyH_]);
		mNormalQueue.insertCommand(cmd);
		
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RsvQtyL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyL_]);
		mNormalQueue.insertCommand(cmd);
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditLabQtyEnable_, false);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditRsvQtyEnable_, false);
		
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RsvQtyH_);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RsvQtyL_);
		mDatabase.saveAppByteParameter();
	}
	
	public void setPdSpeed(int data) {
		byte[] cmd = null;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CnySetSpd_] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_CnySetSpd_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_CnySetSpd_]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_CnySetSpd_);
	}
	
	public void setMachineStartRunning(boolean flag) {
		byte[] cmd = null;
		if(!flag) {
//			mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSpd_] = 0;
//			cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabSpd_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabSpd_]);
//			mNormalQueue.insertCommand(cmd);
//			
//			mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabSpd_);
		}else{
			initSysPageParam();
		}
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditLabQtyEnable_, false);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_EditRsvQtyEnable_, false);
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_RunStopKey_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setAutoAdjustment() {
		byte[] cmd = null;
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_] = 49;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_SysPage_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]);
		mNormalQueue.insertCommand(cmd);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RecallMemory_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyL_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabQtyL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyL_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabQtyL_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyH_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabQtyH_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabQtyH_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabQtyH_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyL_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RsvQtyL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyL_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RsvQtyL_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyH_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RsvQtyH_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RsvQtyH_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RsvQtyH_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionL_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabPositionL_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionL_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabPositionL_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionR_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_LabPositionR_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabPositionR_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabPositionR_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PrintTime_] = 15;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_PrintTime_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PrintTime_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_PrintTime_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1SD_] = 80;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_V1SD_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1SD_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_V1SD_);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1ED_] = 15;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_V1ED_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_V1ED_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_V1ED_);
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_AutoMesAllEnable_, true);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
	}
	
	public void setAutoBottleDiameter() {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_BotAutoMesEnable_, true);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_RecallMemory_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
		mDatabase.saveAppByteParameter();
		
	}
	
	public void setManualBottleDiameter(int data) {
		byte[] cmd = null;
		double pi = 3.1415926;
		long ConveyEncoderRes = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ConveyEncoderRes_];
		long ConveyGearDiam = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_ConveyGearDiam_];
		long BotDiameterCnyCnt = 0;
		
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_BotDiameter_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_]);
		mNormalQueue.insertCommand(cmd);
		
		BotDiameterCnyCnt = (long) ((ConveyEncoderRes*4*10*data)/(pi*ConveyGearDiam));
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameterCnyCnt_] = (int) BotDiameterCnyCnt;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_BotDiameterCnyCnt_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameterCnyCnt_]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_BotDiameter_);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_BotDiameterCnyCnt_);
	}
	
	public void setLeftLabEnable(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_LabEnable_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte) mAppdata.mAddr15);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setRightLabEnable(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setLeftLabLength(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabLengthL_);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
	}
	
	public void setRightLabLength(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_LabLengthR_);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
	}
	
	public void setAutoLeftLabLength() {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabLenLAutoMesEnable_, true);
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
		mDatabase.saveAppByteParameter();
	}
	
	public void setAutoRightLabLength() {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR17, EndexScanProtocols.BYTE_COMMAND_ADDR17_LabLenRAutoMesEnable_, true);
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_RecallMemory_] = 0;
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR17, (byte) mAppdata.mAddr17);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RecallMemory_);
		mDatabase.saveAppByteParameter();
	}
	
	public void setLeftLabSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabFeedSpdL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightLabSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabFeedSpdR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLeftLabLeaveLength(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLeaveLengthL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightLabLeaveLength(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLeaveLengthR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLeftLabPosition(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabPositionL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightLabPosition(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabPositionR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLeftLabPaper(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_PaperSetL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightLabPaper(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_PaperSetR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setAutoLeftLabSensor(boolean enble) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabSenLAutoMesEnable_, enble);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setAutoRightLabSensor(boolean enble) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabSenRAutoMesEnable_, enble);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setLeftTypingEnable(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_TypingEnable_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte) mAppdata.mAddr15);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setRightTypingEnable(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_TypingEnable2_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setTypingPrintTime(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_PrintTime_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setTypingHasten(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_TypeHasten_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setTypingAccelerate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_TypeAccelerate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setDetectTypeSelect(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_DetectTpye_Select_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setDetectTypeSelDevice(boolean data) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR20, EndexScanProtocols.BYTE_COMMAND_ADDR20_DetectTpye_SelDevice_, data);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR20, (byte) mAppdata.mAddr20);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setTypeCheck(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_TypeChk_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setPWEAN(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_FixPointPasteEnable_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setFixPointType(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_FixPositionType_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setV1SD(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_V1SD_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setV1ED(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_V1ED_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setBackLogOnDelay(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_BacklogOnDelay_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setBackLogOffDelay(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_BacklogOffDelay_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setSysLabMissHalt(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR17, EndexScanProtocols.BYTE_COMMAND_ADDR17_SysLabMissHalt_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR17, (byte) mAppdata.mAddr17);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setNoRibbonHalf(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_NoRibbonHalt_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setBotDimChkOnOff(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR16, EndexScanProtocols.BYTE_COMMAND_ADDR16_BotDimChkOnOff_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR16, (byte) mAppdata.mAddr16);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setTypeAccelerate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_TypeAccelerate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setBotSeparateEnable(boolean flag) {
		byte[] cmd = null;
		
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR15, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR15, (byte) mAppdata.mAddr15);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppByteParameter();
	}
	
	public void setBotSeparateSpd(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_BotSepaSetSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLanguageMode(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LanguageMode_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void getVersion() {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_SysPage_;
		
		mAppdata.mWordPara[wordCmdAddr] = 44;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
	}
	
	public void setS1TopSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1TopSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1StartSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1StartSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1StopSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1StopSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1UpRate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1UpRate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1UpScale(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1UpScale_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1DnRate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1DnRate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS1DnScale(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S1DnScale_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2TopSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2TopSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2StartSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2StartSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2StopSpeed(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2StopSpd_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2UpRate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2UpRate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2UpScale(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2UpScale_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2DnRate(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2DnRate_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setS2DnScale(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_S2DnScale_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLeftDist1(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_ObjSenToPeelDistL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLeftDist2(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabSenToPeelDistL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightDist1(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_ObjSenToPeelDistR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRightDist2(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabSenToPeelDistR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setObjPresDist(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_ObjSenToPresDist_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLD1(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabelWheelDiamL_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setLD2(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabelWheelDiamR_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setWADI(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_RollPasteWheelDiam_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setWECD(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_RollPasteEncoderRes_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setECD(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_ConveyEncoderRes_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setROT1(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_RollPasteMaGearNum_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setROT2(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_RollPasteSlvGearNum_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setCD1(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_ConveyGearDiam_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void setRoolEn(int data) {
		byte[] cmd = null;
		int wordCmdAddr = EndexScanProtocols.WORD_COMMAND_RollPasteEnable_;
		
		mAppdata.mWordPara[wordCmdAddr] = data;
		cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
		mNormalQueue.insertCommand(cmd);
		
		mDatabase.saveAppWordParameter(wordCmdAddr);
	}
	
	public void insertCmd(byte[] cmd){
		mNormalQueue.insertCommand(cmd);
	}
	
	public void showLoadingDialog(){
		if(!loadingDialog.isShowing())
			loadingDialog.show();
	}
	
	public void dismissLoadingDialog(){
		if(loadingDialog.isShowing())
			loadingDialog.dismiss();
	}

	public void setCmdDlay(int delayMilliSec) {
		mCmdLoop.setCmdDelay(delayMilliSec);
	}
	
	public void initSysPageParam(){
		byte[] cmd = null;
		mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]=2;
		cmd = EndexScanProtocols.getWordRequestCommand(EndexScanProtocols.WORD_COMMAND_SysPage_, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_SysPage_);
	}
	
	public void setRollPasteEnable(boolean flag) {
		byte[] cmd = null;
		int addr = EndexScanProtocols.WORD_COMMAND_RollPasteEnable_;
		mAppdata.mWordPara[addr] = flag?1:0;
		cmd = EndexScanProtocols.getWordRequestCommand(addr, mAppdata.mWordPara[addr]);
		mNormalQueue.insertCommand(cmd);
		mDatabase.saveAppWordParameter(EndexScanProtocols.WORD_COMMAND_RollPasteEnable_);
	}
	
	public void setLabelTestModeEnable(boolean flag){
		byte[] cmd = null;
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_LabelTestModeEnable_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
	}
	
	public void setExtensionTestEnable(boolean flag){
		byte[] cmd = null;
		mAppdata.setBitData(EndexScanProtocols.BYTE_COMMAND_ADDR18, EndexScanProtocols.BYTE_COMMAND_ADDR18_LabelTestModeEnable_, flag);
		cmd = EndexScanProtocols.getByteRequestCommand(EndexScanProtocols.BYTE_COMMAND_ADDR18, (byte) mAppdata.mAddr18);
		mNormalQueue.insertCommand(cmd);
		int wordCmdAddr;
		if(flag){
			//		LabLengthL_(0014) = 65535
			//		LabLengthR_(0015) = 65535
			wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthL_;
			mAppdata.mWordPara[wordCmdAddr] = 65535;
			cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
			mNormalQueue.insertCommand(cmd);
			
			wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthR_;
			mAppdata.mWordPara[wordCmdAddr] = 65535;
			cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
			mNormalQueue.insertCommand(cmd);
		}else{
			//		LabLengthL_(0014) = 按下前的值
			//		LabLengthR_(0015) = 按下前的值
			wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthL_;
			mAppdata.mWordPara[wordCmdAddr] = mDatabase.getWordParamData(wordCmdAddr);
			cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
			mNormalQueue.insertCommand(cmd);
			
			wordCmdAddr = EndexScanProtocols.WORD_COMMAND_LabLengthR_;
			mAppdata.mWordPara[wordCmdAddr] = mDatabase.getWordParamData(wordCmdAddr);
			cmd = EndexScanProtocols.getWordRequestCommand(wordCmdAddr, mAppdata.mWordPara[wordCmdAddr]);
			mNormalQueue.insertCommand(cmd);
		}
		
	}
}
