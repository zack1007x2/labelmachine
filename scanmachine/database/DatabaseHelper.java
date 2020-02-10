package endexcase.scanmachine.database;

import endexcase.scanmachine.database.Table.MachineMemTable;
import endexcase.scanmachine.util.AppLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper  extends SQLiteOpenHelper{
	private static final AppLog smAppLog = new AppLog("database",DatabaseHelper.class);
	private static final String DATABASE_NAME = "ScanMachineDatabase";
	private static int DATABASE_VERSION = 1;
	
	private SQLiteDatabase mDatabase=null;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(MachineMemTable.SQL_CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(MachineMemTable.SQL_DELETE_TABLE_IF_EXIST);
		onCreate(db);
	}

	public boolean initMachineMemData() {
		int machineId=0;
		if(mDatabase==null)
			mDatabase = getWritableDatabase();
		
		for(machineId=0; machineId<23;machineId++) {
			ContentValues values = new ContentValues();
			values.put(MachineMemTable.COLUMN_MACHINE_ID, machineId);
			values.put(MachineMemTable.COLUMN_MEM_ADDR29, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR30, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR31, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR32, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR33, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR34, 0);
			values.put(MachineMemTable.COLUMN_MEM_ADDR35, 0);
			
			// Insert the new row, returning the primary key value of the new row
			long newRowId = mDatabase.insert(MachineMemTable.TABLE_NAME,null,values);
			smAppLog.d("insert with Id =  "+newRowId+" to table "+MachineMemTable.TABLE_NAME);
			if(newRowId<0)
				return false;
		}
		return true;
	}
	
	public boolean clearMachineMemData() {
		int machineId=0;
		
		if(mDatabase==null)
			mDatabase = getWritableDatabase();
		
		for(machineId=0; machineId<23; machineId++) {
			String selection = MachineMemTable.COLUMN_MACHINE_ID + "=" + machineId;
			
			Cursor cursor = mDatabase.query(MachineMemTable.TABLE_NAME, MachineMemTable.COLUMNS,selection, null, null, null, null);
			
			if(cursor.getCount() > 0) {
				ContentValues values = new ContentValues();
				values.put(MachineMemTable.COLUMN_MACHINE_ID, machineId);
				values.put(MachineMemTable.COLUMN_MEM_ADDR29, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR30, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR31, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR32, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR33, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR34, 0);
				values.put(MachineMemTable.COLUMN_MEM_ADDR35, 0);
				
				int count = mDatabase.update(MachineMemTable.TABLE_NAME, values, selection, null);
			}
			
		}
		
		return true;
	}
	
	public Cursor getMachineMemData(int machineId) {
		if(mDatabase==null)
			mDatabase = getWritableDatabase();
		
		
		String selection = MachineMemTable.COLUMN_MACHINE_ID + "=" + machineId;
		
		Cursor cursor = mDatabase.query(MachineMemTable.TABLE_NAME, MachineMemTable.COLUMNS,selection, null, null, null, null);
		smAppLog.d("cursor.count:"+ cursor.getCount());
		if(cursor.getCount()>0) {
			cursor.moveToFirst();
			
			return cursor;
		}
		return null;
	}
	
	public boolean updateMachineMemData(int machineId, int addr29, int addr30, int addr31, int addr32, int addr33, int addr34, int addr35) {
		if(mDatabase==null)
			mDatabase = getWritableDatabase();
		
		String selection = MachineMemTable.COLUMN_MACHINE_ID + "=" + machineId;
		
		Cursor cursor = mDatabase.query(MachineMemTable.TABLE_NAME, MachineMemTable.COLUMNS,selection, null, null, null, null);
		
		if(cursor.getCount() > 0) {
			ContentValues values = new ContentValues();
			values.put(MachineMemTable.COLUMN_MACHINE_ID, machineId);
			values.put(MachineMemTable.COLUMN_MEM_ADDR29, addr29);
			values.put(MachineMemTable.COLUMN_MEM_ADDR30, addr30);
			values.put(MachineMemTable.COLUMN_MEM_ADDR31, addr31);
			values.put(MachineMemTable.COLUMN_MEM_ADDR32, addr32);
			values.put(MachineMemTable.COLUMN_MEM_ADDR33, addr33);
			values.put(MachineMemTable.COLUMN_MEM_ADDR34, addr34);
			values.put(MachineMemTable.COLUMN_MEM_ADDR35, addr35);
			
			int count = mDatabase.update(MachineMemTable.TABLE_NAME, values, selection, null);
			
			return true;
		}
		
		return false;
	}
}
