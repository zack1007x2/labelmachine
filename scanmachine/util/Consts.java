package endexcase.scanmachine.util;

import java.io.File;

import android.util.Log;

public class Consts {
	private static String DB_SUB_NAME = ".txt";
	
	public static final String USB_PATH = "/mnt/usb_storage/";
	public static final String USB_MEMO_DATA_PATH = USB_PATH+"endex/scanmachine/";
	public static final String USB_MEMO_DATA_BACKUP_PATH = USB_MEMO_DATA_PATH+"backup/";
	
	public static final int SINGLE_PRINT_HEAD = 768;
	public static final int DUAL_PRINT_HEAD = 771;
	
	public final static String PASSWORD = "9012";
	
	public static String[] MACHINE_NAME_ARRAY = {
		"KK806U", "CVC200U", "KK906U", "KK916U", "CVC300U",
		"CVC302U", "CVC330U", "KK926U", "CVC400U", "KK956U",
		"CVC430U", "KK996U", "CVC350U", "KK998U", "CVC310U",
		"KK936U", "CVC220U", "CVC210U", "CVC230U", "KK906UP",
		"KK916UP", "CVC300UP", "CVC330UP"
	};
	
	public static int MACHINE_WORD_PARAMETER_SIZE = 120;
	
	public static String getMemDataFileName(String path, String editText, int machineId, int memIndex) {
		String fileName = null;
		fileName = path + "/" + MACHINE_NAME_ARRAY[machineId] + "_" + String.format("%03d", memIndex + 1) + "_" + editText + DB_SUB_NAME;
		
		return fileName;
	}
	
	public static String findMemDataFileName(String path, int machineId, int memIndex) {
		String compareName = MACHINE_NAME_ARRAY[machineId] + "_" + String.format("%03d", memIndex + 1);
		String fileName = null;
		
		Log.d("Files", "Path: " + path);
		File f = new File(path);
		File file[] = f.listFiles();
		Log.d("Files", "Size: "+ file.length);
		for (int i=0; i < file.length; i++)
		{
			if(file[i].getName().contains(compareName)) {
				Log.d("Files", "FileName:" + file[i].getName());
				fileName = file[i].getName();
				return fileName;
			}
		}
		
		return fileName;
	}
	
	public static void clearMemDataFile(String path, int machineId, int memIndex) {
		String compareName = MACHINE_NAME_ARRAY[machineId] + "_" + String.format("%03d", memIndex + 1);
		
		File f = new File(path);
		File file[] = f.listFiles();
		for (int i=0; i < file.length; i++)
		{
			if(file[i].getName().contains(compareName)) {
				file[i].delete();
			}
		}
	}
}
