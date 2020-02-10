package endexcase.scanmachine.uart;

import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import android.os.Handler;
import android.os.Looper;

/**
 * 隔離UART底層開發用 
 */
public class Sport_Dev {
	private static byte[] enterBytesCache;
	
	private static final Handler mUiHandler = new Handler(Looper.getMainLooper());
	
	public static void downloadPin(int isHigh){
	}

	public static int sendBytes(byte[] data, int len){
		enterBytesCache = data;
		return 0;
	}

	public static byte[] getBytes(){
		byte[] returnedBytes = new byte[6];
		Arrays.fill(returnedBytes,(byte)0x00);
		return returnedBytes;
	}

	public static byte[] getNBytes(int len){
		throw new UnsupportedOperationException("Not implement yet");
	}

	public static byte[] catchNBytes(int len) throws TimeoutException{
		byte[] returnNBytes = null;

		if(len == 1) {
			returnNBytes = new byte[1];
			returnNBytes[0] = EndexScanProtocols.COMMAND_TYPE_ECHO;
		}
		
		Sport_Dev.enterBytesCache = null;

		if(returnNBytes!=null)
			return Arrays.copyOf(returnNBytes, len);
		else
			throw new TimeoutException();
	}

	public static byte[] dumpNBytes(int len){
		byte[] returnedBytes = new byte[len];
		Arrays.fill(returnedBytes,(byte)0x00);
		return returnedBytes;
	}

	public static void setSpeed(int speed){
	}

	public static void setParity(byte parity){
	}

	public static void log(int level){
	}

	public static void init(){
		enterBytesCache = null;
		mUiHandler.removeCallbacksAndMessages(null);
	}

	public static void deinit(){
	}


	private static byte[] getResponseByteArray(byte cmdVarId,byte responseCode,byte requestByte1,byte requestByte2,byte requestByte3,byte requestByte4){
		byte[] bytes = new byte[7];
		bytes[0] = cmdVarId;
		bytes[1] = responseCode;
		bytes[2] = requestByte1;
		bytes[3] = requestByte2;
		bytes[4] = requestByte3;
		bytes[5] = requestByte4;

		byte checkSum = computeCheckSum(bytes[0],bytes[1],bytes[2],bytes[3],bytes[4],bytes[5]);
		bytes[6] = checkSum;
		return bytes;
	}

	private static byte computeCheckSum(byte...bytes){
		byte checkSum = (byte)(0x00);
		for(byte oneByte:bytes)
			checkSum^=oneByte;
		return checkSum;
	}
}
