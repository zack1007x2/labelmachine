package endexcase.scanmachine;

import android.app.Application;
import endexcase.scanmachine.util.CrashHandler;

public class MyApplication extends Application {
    @Override
    public void onCreate() {  
        super.onCreate();  
    	CrashHandler crashHandler = CrashHandler.getInstance();  
    	crashHandler.init(this);  
    }  
}
