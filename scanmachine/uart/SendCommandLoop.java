package endexcase.scanmachine.uart;

import java.util.concurrent.TimeoutException;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.database.SharedPrefConstants;
import endexcase.scanmachine.service.ControlService;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Hex;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * 負責消費Queue內的指令傳送至UART
 * 並取出收到的回傳值
 * 以Broadcast的形式回傳系統
 */
public class SendCommandLoop{
	private static final AppLog smAppLog = new AppLog("uart",SendCommandLoop.class);
	private boolean mShowLog=true;
	
	private boolean mIsLoopStart = false;
	private boolean mAskFlag = true;
	private boolean mPauseLoop = false;
	
	private boolean mEchoFlag = false;
	
	private ControlService mService;
	
	// 以下Queue優先權由高而低
	private final CommandQueue mUrgentAskQueue;
	private final CommandQueue mNormalAskQueue;
	
	private static int COMMAND_LOOP_SLEEP_TIME_IN_MILLS = 1;
	private static final int RECV_COMMAND_LOOP_SLEEP_TIME_IN_MILLS = 20;
	
	private Runnable mRunnable = new Runnable(){
		@SuppressWarnings("unused")// unused is smAppLog 
		public void run() {
			while(mIsLoopStart){
				
				SystemClock.sleep(COMMAND_LOOP_SLEEP_TIME_IN_MILLS);// 1 (ms/cycle)
				
				if(mAskFlag && !mPauseLoop){
					
					if(mUrgentAskQueue.getCount()>0){
						mEchoFlag = false;
						byte[] readySend = mUrgentAskQueue.getCommand();

						if(mShowLog) smAppLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
						mAskFlag=false;// 確保一問一答
						
						//Send Command
						SportInterface.sendBytes(readySend,readySend.length);
						
						while(!mEchoFlag) {
							SystemClock.sleep(50);
						}
						
						if(mShowLog) smAppLog.d("Get Echo = "+ mEchoFlag);
						mAskFlag = true; 
					}
					
					if(mNormalAskQueue.getCount()>0){
						mEchoFlag = false;
						byte[] readySend = mNormalAskQueue.getCommand();

						if(mShowLog) smAppLog.d("Cmd = "+ Hex.hexBytesToString(readySend));
						mAskFlag=false;// 確保一問一答
						
						//Send Command
						SportInterface.sendBytes(readySend,readySend.length);
						
						while(!mEchoFlag) {
							SystemClock.sleep(50);
						}
						if(mShowLog) smAppLog.d("Get Echo = "+ mEchoFlag);
						mAskFlag = true;
					}
				}
			}
			smAppLog.d("CmdLoopThread is dead");
		}
	};
	
	private Runnable mRecvRunnable = new Runnable(){
		@SuppressWarnings("unused")// unused is smAppLog 
		public void run() {
			while(mIsLoopStart){
				SystemClock.sleep(RECV_COMMAND_LOOP_SLEEP_TIME_IN_MILLS);// 20 (ms/cycle)
				
				if(!mPauseLoop){
					
					//Get Command first
					boolean haveGetData = false;
					
					byte[] result = null;
					//Get Result
					try{
						result = SportInterface.catchNBytes(1);
						haveGetData = true;
					}catch(TimeoutException e){
						haveGetData = false;
					}catch(NullPointerException e){
						haveGetData = false;
					}
					
					if(haveGetData) {
						if(mShowLog) smAppLog.d("Get Value result = "+Hex.hexBytesToString(result));
						if(result[0] != EndexScanProtocols.COMMAND_TYPE_ECHO) {
							byte cmd = EndexScanProtocols.checkCommandType(result[0]);
							if(cmd == EndexScanProtocols.COMMAND_TYPE_BYTE) {
								recvByteCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_WORD) {
								recvWORDCommand();
							}
							else if(cmd == EndexScanProtocols.COMMAND_TYPE_DWORD) {
								recvDWORDCommand();
							}
							else {
								if(mShowLog) smAppLog.d("Unknow cmd");
							}
						}
						else {
							mEchoFlag = true;
						}
					}
				}
			}
			smAppLog.d("recvCmdLoopThread is dead");
		}
	};
	
	public SendCommandLoop(ControlService service,CommandQueue urgentCmdQueue,CommandQueue normalCmdQueue){
		smAppLog.d("SendCommandLoop");
		mService = service;
		mUrgentAskQueue = urgentCmdQueue;
		mNormalAskQueue = normalCmdQueue;
//		if(!SportInterface.USE_NATIVE_UART)
//			COMMAND_LOOP_SLEEP_TIME_IN_MILLS = PreferenceManager.getDefaultSharedPreferences(mService).getInt(SharedPrefConstants.PREF_KEY_CMD_DELAY, 50);
	}

	public synchronized void start(){
		smAppLog.d("start");
		mIsLoopStart = true;
		Thread thread = new Thread(mRunnable,"CmdLoopThread");
		thread.start();
		
		Thread recvthread = new Thread(mRecvRunnable,"CmdLoopThread");
		recvthread.start();
	}
	
	public synchronized void stop(){
		mIsLoopStart = false;
	}
	
	public synchronized void pause(boolean flag){
		mPauseLoop = flag;
	}
	
	public void recvByteCommand() {
		byte[] result = null;
		byte[] echoCmd = EndexScanProtocols.getEchoCommand();
		int addr=0;
		int data=0;
		
		try{
			result = SportInterface.catchNBytes(4);
		}catch(TimeoutException e){
			smAppLog.e(e.toString());
			return;
		}catch(NullPointerException e){
			smAppLog.e(e.toString());
			return;
		}
		if(mShowLog) smAppLog.d("Get Value result = "+Hex.hexBytesToString(result));
		SportInterface.sendBytes(echoCmd,echoCmd.length);
		
		addr = ((result[0]<<8)&0xFF00) + (result[1]&0xFF);
		data = result[2]&0xFF;
		
		if(addr==EndexScanProtocols.BYTE_COMMAND_ADDR15){
			boolean LabEnable_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_LabEnable_);
			boolean BotSeparateEnable_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_);
			boolean TypingEnable_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_TypingEnable_);
			mService.saveByteData(addr, data);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_LabEnable_, LabEnable_);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_BotSeparateEnable_, BotSeparateEnable_);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR15_TypingEnable_, TypingEnable_);
		}else if(addr==EndexScanProtocols.BYTE_COMMAND_ADDR16){
			boolean BotDimChkOnOff_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_BotDimChkOnOff_);
			boolean LabEnable2_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_);
			boolean TypingEnable2_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_TypingEnable2_);
			mService.saveByteData(addr, data);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_BotDimChkOnOff_, BotDimChkOnOff_);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_LabEnable2_, LabEnable2_);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR16_TypingEnable2_, TypingEnable2_);
		}else if(addr==EndexScanProtocols.BYTE_COMMAND_ADDR17){
			boolean SysLabMissHalt_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR17_SysLabMissHalt_);
			mService.saveByteData(addr, data);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR17_SysLabMissHalt_, SysLabMissHalt_);
		}else if(addr==EndexScanProtocols.BYTE_COMMAND_ADDR18){
			boolean NoRibbonHalt_ = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR18_NoRibbonHalt_);
			mService.saveByteData(addr, data);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR18_NoRibbonHalt_, NoRibbonHalt_);
		}else if(addr==EndexScanProtocols.BYTE_COMMAND_ADDR20){
			boolean keep_selDev = AppData.getInstance().getBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR20_DetectTpye_SelDevice_);
			mService.saveByteData(addr, data);
			AppData.getInstance().setBitData(addr, EndexScanProtocols.BYTE_COMMAND_ADDR20_DetectTpye_SelDevice_, keep_selDev);
		}else{
			mService.saveByteData(addr, data);
		}
	}
	
	public void recvWORDCommand() {
		byte[] result = null;
		byte[] echoCmd = EndexScanProtocols.getEchoCommand();
		int addr=0;
		int data=0;
		
		//Get Result
		try{
			result = SportInterface.catchNBytes(5);
		}catch(TimeoutException e){
			smAppLog.e(e.toString());
			return;
		}catch(NullPointerException e){
			smAppLog.e(e.toString());
			return;
		}
		if(mShowLog) smAppLog.d("Get Value result = "+Hex.hexBytesToString(result));
		SportInterface.sendBytes(echoCmd,echoCmd.length);
		
		addr = ((result[0]<<8)&0xFF00) + (result[1]&0xFF);
		data = ((result[2]<<8)&0xFF00) + (result[3]&0xFF);
		
		if(addr == EndexScanProtocols.WORD_COMMAND_SysPage_) {
			mService.analyseSysPage(data);
		}
		else {
			mService.saveWordData(addr, data);
		}
	}
	
	public void recvDWORDCommand() {
		byte[] result = null;
		byte[] echoCmd = EndexScanProtocols.getEchoCommand();
		int addr=0;
		int data1=0;
		int data2=0;
		
		try{
			result = SportInterface.catchNBytes(7);
		}catch(TimeoutException e){
			smAppLog.e(e.toString());
			return;
		}catch(NullPointerException e){
			smAppLog.e(e.toString());
			return;
		}
		
		SportInterface.sendBytes(echoCmd,echoCmd.length);
		
		addr = ((result[0]<<8)&0xFF00) + (result[1]&0xFF);
		data1 = ((result[2]<<8)&0xFF00) + (result[3]&0xFF);
		data2 = ((result[4]<<8)&0xFF00) + (result[5]&0xFF);
		mService.saveDWordData(addr, data1, data2);
	}
	
	public void setCmdDelay(int delayMilliSec){
		if(!SportInterface.USE_NATIVE_UART)
			COMMAND_LOOP_SLEEP_TIME_IN_MILLS = delayMilliSec;
	}
}
