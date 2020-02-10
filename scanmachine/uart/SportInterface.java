/**
 * Sport Device control Java interface.
 */

package endexcase.scanmachine.uart;

import java.util.concurrent.TimeoutException;

import com.gemminer.Sport;

public class SportInterface {
	public static final boolean USE_NATIVE_UART = true;

	/**
	 * Constant for Set Download_Pin to Low Level.
	*/
	public static final int DOWNLOAD_PIN_LOW  = 0;

	/**
	 * Constant for Set Download_Pin to High Level.
	*/
	public static final int DOWNLOAD_PIN_HIGH = 1;


	public static void downloadPin(int isHigh){
		if(USE_NATIVE_UART){
			Sport.downloadPin(isHigh);	
		}else{
			Sport_Dev.downloadPin(isHigh);	
		}
	}

	public static int sendBytes(byte[] data, int len){
		if(USE_NATIVE_UART){
			return Sport.sendBytes(data, len);
		}else{
			return Sport_Dev.sendBytes(data, len);
		}
	}

	public static byte[] getBytes(){
		if(USE_NATIVE_UART){
			return Sport.getBytes();
		}else{
			return Sport_Dev.getBytes();
		}
	}

	public static byte[] getNBytes(int len){
		if(USE_NATIVE_UART){
			return Sport.getNBytes(len);
		}else{
			return Sport_Dev.getNBytes(len);
		}
	}

	public static byte[] catchNBytes(int len) throws TimeoutException{
		if(USE_NATIVE_UART){
			return Sport.catchNBytes(len);
		}else{
			return Sport_Dev.catchNBytes(len);
		}
	}

	public static byte[] dumpNBytes(int len){
		if(USE_NATIVE_UART){
			return Sport.dumpNBytes(len);
		}else{
			return Sport_Dev.dumpNBytes(len);
		}
	}

	public static void setSpeed(int speed){
		if(USE_NATIVE_UART){
			Sport.setSpeed(speed);
		}else{
			Sport_Dev.setSpeed(speed);
		}
	}

	public static void setParity(byte parity){
		if(USE_NATIVE_UART){
			Sport.setParity(parity);
		}else{
			Sport_Dev.setParity(parity);
		}
	}

	public static void log(int level){
		if(USE_NATIVE_UART){
			Sport.log(level);
		}else{
			Sport_Dev.log(level);
		}
	}

	public static void init(){
		if(USE_NATIVE_UART){
			Sport.init();
		}else{
			Sport_Dev.init();
		}
	}

	public static void deinit(){
		if(USE_NATIVE_UART){
			Sport.deinit();
		}else{
			Sport_Dev.deinit();
		}
	}
}

