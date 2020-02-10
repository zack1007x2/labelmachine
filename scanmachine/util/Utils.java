package endexcase.scanmachine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.endex.ce60.R;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import endexcase.scanmachine.AppData;
import endexcase.scanmachine.database.DatabaseProxy;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.widget.CustomToast;

/**
 * Created by Zack on 2016/1/30.
 */
public class Utils {
	private static AppData mAppdata;
	private static DatabaseProxy mDatabase;
	private static boolean isFileTrans;

    public static int getDimens2Pixel(Context context, int dimenId){
        return  context.getResources().getDimensionPixelSize(dimenId);
    }
    
    /**
     * format yyyy/MM/dd
     */
    public static String TransYearDate(int year, int date){
    	if(year!=0 && date!=0){
    		String ret = year+"/"+date/100+"/"+date%100;
    		return ret;
    	}else{
    		return "0";
    	}
    	
    }
    
    public static void recursiveEnableView(ViewGroup view, boolean b) {
		view.setEnabled(b);
		
	    for (int i = 0; i < view.getChildCount(); i++) {
	        View child = view.getChildAt(i);
	        if (child instanceof ViewGroup) {
	        	recursiveEnableView((ViewGroup) child, b);
	        } else {
	            child.setEnabled(b);
	        }
	    }
	}
    
    public static void recursiveEnableVisiableView(ViewGroup view, boolean b) {
		view.setEnabled(b);
		
	    for (int i = 0; i < view.getChildCount(); i++) {
	        View child = view.getChildAt(i);
	        if (child instanceof ViewGroup) {
	        	recursiveEnableVisiableView((ViewGroup) child, b);
	        } else {
	            child.setEnabled(b);
	            child.setVisibility(b?View.VISIBLE:View.INVISIBLE);
	        }
	    }
	}
    
    public static boolean isUsbExist() {
		File usbFile = new File(Consts.USB_PATH);
		if(!usbFile.exists()||!usbFile.isDirectory())
			return false;
		
		return true;
	}
    
	public synchronized static void copyHoleMemoToUsb(Context context, int machineType){
		if(isFileTrans)
			return;
		isFileTrans = true;
		mAppdata = AppData.getInstance();
		CustomToast Toast = CustomToast.getCustomToast(context);
		final String curMachine = Consts.MACHINE_NAME_ARRAY[machineType];
		File srcFolder = new File(context.getFilesDir().toString());
		//if no matched memories return
		FilenameFilter filenamefilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.contains(curMachine))return true;
				return false;
			}
		};
		if(srcFolder.listFiles(filenamefilter).length==0){
			Toast.show(R.string.toast_message_data_not_found);
			isFileTrans = false;
			return;
		}
		
		File dstFolder = new File(Consts.USB_MEMO_DATA_PATH + Consts.MACHINE_NAME_ARRAY[machineType]);
//		Log.d("Export", "src folder : "+ srcFolder.getPath());
//		Log.d("Export", "dst folder : "+ dstFolder.getPath()+" r: "+dstFolder.canWrite());
		if(!dstFolder.exists() || !dstFolder.isDirectory() || !dstFolder.canWrite()){
			//新增該機種資料夾
			dstFolder.mkdirs();
		}else if(dstFolder.listFiles().length!=0){
			if(!dstFolder.canWrite()){
				isFileTrans = false;
				return;
			}
			//避免exception 先備份
//			File[] machineMemos = dstFolder.listFiles();
//			for(File curFile : machineMemos){
//				if(curFile.getName().contains(Consts.MACHINE_NAME_ARRAY[machineType])){
//					try {
//						backupMemoFiles(context, curFile);
//					} catch (IOException e) {
//						e.printStackTrace();
//						return;
//					}
//				}
//			}
//			移除重建dst folder
			DeleteRecursive(dstFolder);
			dstFolder.mkdirs();
		}
		//開始匯出
		for(File srcFile : srcFolder.listFiles()){
			if(srcFile.getName().contains(Consts.MACHINE_NAME_ARRAY[machineType])){
				try {
					int index = context.getFilesDir().toString().length();
					//   /USB_PATH/endex/scanmachine/MACHINE_NAME/xxx_xxx_xxx.txt
					File outputFile = new File(dstFolder.getPath()+srcFile.getPath().substring(index));
					copyFile(srcFile.getPath(),outputFile.getPath());
				} catch (IOException e) {
					e.printStackTrace();
					isFileTrans = false;
					return;
				}
			}
		}
		Utils.sync("copyHoleMemoToUsb");
//		匯出完成移除備份資料夾
//		File backupFolder = new File(Consts.USB_MEMO_DATA_BACKUP_PATH);
//		if(backupFolder.exists() && backupFolder.isDirectory())
//			DeleteRecursive(backupFolder);
		Toast.show(R.string.toast_message_export_done);
		isFileTrans = false;
	}

	public static void copyHoleMemoToLocal(Context context, int machineType){
		if(isFileTrans)
			return;
		isFileTrans = true;
		mAppdata = AppData.getInstance();
		mDatabase = DatabaseProxy.getInstance(context);
		CustomToast Toast = CustomToast.getCustomToast(context);
		File srcFolder = new File(Consts.USB_MEMO_DATA_PATH + Consts.MACHINE_NAME_ARRAY[machineType]);
		if(!srcFolder.exists() || !srcFolder.isDirectory() || srcFolder.listFiles().length==0){
			Toast.show(R.string.toast_message_data_not_found);
			isFileTrans = false;
			return;
		}
		
		File dstFolder = new File(context.getFilesDir().toString()); //sys folder
		
//		Log.d("Import", "src folder : "+ srcFolder.getPath());
//		Log.d("Import", "dst folder : "+ dstFolder.getPath());
		
		File[] machineMemos = dstFolder.listFiles();
		//backup and delete
		for(File curFile : machineMemos){
			if(curFile.getName().contains(Consts.MACHINE_NAME_ARRAY[machineType])){
				try {
					backupMemoFiles(context, curFile);
					curFile.delete();
				} catch (IOException e) {
					e.printStackTrace();
					isFileTrans = false;
					return;
				}
				
			}
		}
		
		//clear db memo data
		for (int i = 0; i < 50; i++) {
			int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (i / 8);
			int bitNo = i % 8;
			mAppdata.setBitData(byteAddr, bitNo, false);
		}
		
		//開始匯入
		for(File srcFile : srcFolder.listFiles()){
			if(srcFile.getName().contains(Consts.MACHINE_NAME_ARRAY[machineType])){
				try {
					int index = Consts.USB_MEMO_DATA_PATH.length()+Consts.MACHINE_NAME_ARRAY[machineType].length();
					File outputFile = new File(dstFolder.getPath()+srcFile.getPath().substring(index));
//					Log.d("Import", "outputFile ->"+outputFile.getPath());
					copyFile(srcFile.getPath(), outputFile.getPath());
					
					//update db 
					String MemoIdStr = srcFile.getPath().substring(index).split("_")[1];
					int MemoId = Integer.valueOf(MemoIdStr)-1;
//					Log.d("Import", "MemoId ==> "+MemoId);
					int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (MemoId / 8);
					int bitNo = MemoId % 8;
					mAppdata.setBitData(byteAddr, bitNo, true);
				} catch (IOException e) {
					e.printStackTrace();
					isFileTrans = false;
					return;
				}
			}
		}
		mDatabase.updateMachineMemData(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_],
				mAppdata.mAddr29, mAppdata.mAddr30, mAppdata.mAddr31, mAppdata.mAddr32, mAppdata.mAddr33,
				mAppdata.mAddr34, mAppdata.mAddr35);
		
		//finish delete backup folder
		File backupFolder = new File(context.getFilesDir().toString()+"backup");
		if(backupFolder.exists() && backupFolder.isDirectory())
			DeleteRecursive(backupFolder);
		
		Utils.sync("copyHoleMemoToLocal");
		Toast.show(R.string.toast_message_import_done);
		isFileTrans = false;
	}
	
	private static void copyFile(String srcPath, String dstPath) throws IOException{
		InputStream  src = new FileInputStream(srcPath);
        OutputStream dst = new FileOutputStream(dstPath);
        byte[] buf = new byte[1024];
        int len;
        while ((len = src.read(buf)) > 0) {
            dst.write(buf, 0, len);
            dst.flush();
        }
        src.close();
        dst.close();
	}
	
	public static void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);

	    fileOrDirectory.delete();
	}
	
	private static void backupMemoFiles(Context context,File curFile) throws IOException{
		if(curFile.getPath().contains(Consts.USB_MEMO_DATA_PATH)){//backup before export
			File backupFolder = new File(Consts.USB_MEMO_DATA_BACKUP_PATH);
			if(!backupFolder.exists() || !backupFolder.isDirectory()){
				backupFolder.mkdir();
			}
			File outputFile = new File(Consts.USB_MEMO_DATA_BACKUP_PATH + curFile.getPath().split("/")[curFile.getPath().split("/").length-1]);
//			Log.d("Export", "backup src : "+curFile.getPath());
//			Log.d("Export", "backup dst : "+outputFile.getPath());
			if(curFile.exists()){
				copyFile(curFile.getPath(), outputFile.getPath());
            }
		}else{//backup before import
			File backupFolder = new File(context.getFilesDir().toString()+"backup");
			if(!backupFolder.exists() || !backupFolder.isDirectory()){
				backupFolder.mkdir();
			}
			int index = context.getFilesDir().toString().length();
			File outputFile = new File(backupFolder.getPath() + curFile.getPath().substring(index));
//			Log.d("Import", "backup src : "+curFile.getPath());
//			Log.d("Import", "backup dst : "+outputFile.getPath());
			if(curFile.exists()){
				copyFile(curFile.getPath(), outputFile.getPath());
            }
		}
		
	}
	
	public static void sync(String... from){
		try {
			Runtime runtime = Runtime.getRuntime();
			runtime.exec("sync");
//			Log.d("SYNC", "-----------sync-"+from[0]+"-------------");
		} catch (IOException e) {
			e.printStackTrace();
		}catch(NullPointerException e){
//			Log.d("SYNC", "-----------sync--------------");
		}
	}
	
}
