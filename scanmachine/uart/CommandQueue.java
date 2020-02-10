package endexcase.scanmachine.uart;

import java.util.ArrayList;

import endexcase.scanmachine.util.AppLog;

import android.support.annotation.Nullable;

public class CommandQueue {
	private static final AppLog smAppLog = new AppLog("uart",CommandQueue.class);
	private final ArrayList<byte[]> mInternalQueue = new ArrayList<byte[]>();
	private final int MAX_QUEUE_CAPACITY;
	
	public CommandQueue(int maxCapacity){
		MAX_QUEUE_CAPACITY = maxCapacity;
	}
	
	public synchronized boolean insertCommand(byte[] commandBytes){
		if(mInternalQueue.size() >= MAX_QUEUE_CAPACITY){
			// 容量爆滿，清除Queue，並顯示Log
			clean();
			smAppLog.e("Queue MAX has reached,clean the queue");
			return false;
		}
		
		mInternalQueue.add(commandBytes);
		return true;
	}
	
	@Nullable
	public synchronized byte[] getCommand(){
		if(mInternalQueue.isEmpty())
			return null;
		return mInternalQueue.remove(0);
	}
	
	public synchronized void clean(){	
		mInternalQueue.clear();
	}
	
	public synchronized int getCount(){
		return mInternalQueue.size();
	}
}
