package endexcase.scanmachine.util;

import android.util.Log;

public class AppLog {
	public static final boolean debugMode = true;
	
	private String mTagName;
	private String mClassName;

	public AppLog(String tagName,Class<?> cls){
		mTagName = tagName;
		mClassName = cls.getSimpleName();
	}

	public void d(String message){
		if(debugMode)
			Log.d(mTagName,mClassName+": "+message);
	}

	public void w(String message){
		if(debugMode)
			Log.w(mTagName, mClassName + ": " + message);
	}

	public void e(String message){
		if(debugMode)
			Log.e(mTagName, mClassName + ": " + message);
	}
}
