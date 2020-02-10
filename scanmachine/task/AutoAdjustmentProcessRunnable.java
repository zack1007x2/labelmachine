package endexcase.scanmachine.task;

import java.util.ArrayList;
import java.util.HashMap;

import com.endex.ce60.R;

import android.app.Dialog;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;
import endexcase.scanmachine.AppData;
import endexcase.scanmachine.uart.EndexScanProtocols;

public class AutoAdjustmentProcessRunnable implements Runnable{
	private String TAG = AutoAdjustmentProcessRunnable.class.getSimpleName();
	private Dialog mDialog;
	private Handler mHandler;
	private AppData mAppdata;
	
	private HashMap<Integer, int[]> mMsgStrMap = new HashMap<>();
	TextView tvMessage;
	TableRow optionalRow1, optionalRow2, optionalRow3;
	TextView optionalRow1_title, optionalRow1_value, optionalRow1_unit;
	TextView optionalRow2_title, optionalRow2_value, optionalRow2_unit;
	TextView optionalRow3_title, optionalRow3_value, optionalRow3_unit;
	ArrayList<TableRow> rowArr = new ArrayList<TableRow>();
	public AutoAdjustmentProcessRunnable(Handler handler, AppData appData, Dialog dialog){
		mHandler = handler;
		mAppdata = appData;
		mDialog = dialog;
		mMsgStrMap.clear();
		mMsgStrMap.put(49, new int[]{R.string.dialog_auto_process_message_type_49});
		mMsgStrMap.put(50, new int[]{R.string.dialog_auto_process_message_type_50, R.string.dialog_auto_process_title_container});
		mMsgStrMap.put(51, new int[]{R.string.dialog_auto_process_message_type_51, R.string.dialog_auto_process_title_container});
		mMsgStrMap.put(52, new int[]{R.string.dialog_auto_process_message_type_52, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_setvalue});
		mMsgStrMap.put(53, new int[]{R.string.dialog_auto_process_message_type_53, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_setvalue});
		mMsgStrMap.put(54, new int[]{R.string.dialog_auto_process_message_type_54, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_setvalue});
		mMsgStrMap.put(55, new int[]{R.string.dialog_auto_process_message_type_55, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_setvalue});
		mMsgStrMap.put(56, new int[]{R.string.dialog_auto_process_message_type_56, R.string.dialog_auto_process_title_length, R.string.dialog_auto_process_title_gap});
		mMsgStrMap.put(57, new int[]{R.string.dialog_auto_process_message_type_57, R.string.dialog_auto_process_title_length, R.string.dialog_auto_process_title_gap});
		mMsgStrMap.put(58, new int[]{R.string.dialog_auto_process_message_type_58, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_length, R.string.dialog_auto_process_title_gap});
		mMsgStrMap.put(59, new int[]{R.string.dialog_auto_process_message_type_59, R.string.dialog_auto_process_title_readvalue, R.string.dialog_auto_process_title_length, R.string.dialog_auto_process_title_gap});
		mMsgStrMap.put(60, new int[]{R.string.dialog_auto_process_message_type_60});
		mMsgStrMap.put(61, new int[]{R.string.dialog_auto_process_message_type_61});
		mMsgStrMap.put(62, new int[]{R.string.dialog_auto_process_message_type_62});
		mMsgStrMap.put(63, new int[]{R.string.dialog_auto_process_message_type_63});
		
		tvMessage = (TextView) mDialog.findViewById(R.id.tvMessage);
		optionalRow1 = (TableRow)mDialog.findViewById(R.id.optionalRow1);
		optionalRow2 = (TableRow)mDialog.findViewById(R.id.optionalRow2);
		optionalRow3 = (TableRow)mDialog.findViewById(R.id.optionalRow3);
		rowArr.add(optionalRow1);
		rowArr.add(optionalRow2);
		rowArr.add(optionalRow3);
	}
	@Override
	public void run() {
		int curSysPage = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_SysPage_];
		int ResId[] = mMsgStrMap.get(curSysPage);
		Log.d(TAG, "curSysPage:"+curSysPage);
		try{
			switch(ResId.length){
			case 1:
				optionalRow1.setVisibility(View.GONE);
				optionalRow2.setVisibility(View.GONE);
				optionalRow3.setVisibility(View.GONE);
				break;
			case 2:
				optionalRow1.setVisibility(View.VISIBLE);
				optionalRow2.setVisibility(View.GONE);
				optionalRow3.setVisibility(View.GONE);
				break;
			case 3:
				optionalRow1.setVisibility(View.VISIBLE);
				optionalRow2.setVisibility(View.VISIBLE);
				optionalRow3.setVisibility(View.GONE);
				break;
			case 4:
				optionalRow1.setVisibility(View.VISIBLE);
				optionalRow2.setVisibility(View.VISIBLE);
				optionalRow3.setVisibility(View.VISIBLE);
				break;
			}
			
			//handle message and unit visibility
			for(int i=0;i<ResId.length-1;i++){
				if(ResId.length>1){
					((TextView)rowArr.get(i).getChildAt(0)).setText(ResId[i+1]);
					switch(ResId[0]){
					case R.string.dialog_auto_process_message_type_50:
					case R.string.dialog_auto_process_message_type_51:
						((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_BotDiameter_]/10.0));
						break;
					case R.string.dialog_auto_process_message_type_52:
					case R.string.dialog_auto_process_message_type_54:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtL_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperSetL_]));
						break;
					case R.string.dialog_auto_process_message_type_53:
					case R.string.dialog_auto_process_message_type_55:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtR_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperSetR_]));
						break;
					case R.string.dialog_auto_process_message_type_56:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthL_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabGapL_]));
						break;
					case R.string.dialog_auto_process_message_type_57:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthR_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabGapR_]));
						break;
					case R.string.dialog_auto_process_message_type_58:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtL_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthL_]));
						else if(i==2)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabGapL_]));
						break;
					case R.string.dialog_auto_process_message_type_59:
						if(i==0)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_PaperThoughtR_]));
						else if(i==1)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabLengthR_]));
						else if(i==2)
							((TextView)rowArr.get(i).getChildAt(1)).setText(String.valueOf(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LabGapR_]));
						break;
							
					}
					
					if(ResId[i+1]==R.string.dialog_auto_process_title_readvalue||ResId[i+1]==R.string.dialog_auto_process_title_setvalue){
						((TextView)rowArr.get(i).getChildAt(2)).setVisibility(View.GONE);
					}else{
						((TextView)rowArr.get(i).getChildAt(2)).setVisibility(View.VISIBLE);
					}
				}
			}
			tvMessage.setText(ResId[0]);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			mHandler.postDelayed(this, 200);
		}
		
	}

}
