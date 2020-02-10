package endexcase.scanmachine.task;

import com.endex.ce60.R;

import android.app.Dialog;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import endexcase.scanmachine.AppData;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.Utils;

public class VersionInfoRunnable implements Runnable{
	private String TAG = VersionInfoRunnable.class.getSimpleName();
	private Dialog mDialog;
	private Handler mHandler;
	private AppData mAppdata;
	
	public VersionInfoRunnable(Handler handler, AppData appData, Dialog dialog){
		mHandler = handler;
		mAppdata = appData;
		mDialog = dialog;
	}
	@Override
	public void run() {
		Log.d(TAG, "refresh version Info");
		//read data again after 2 sec
		TextView tvContentGateway = (TextView) mDialog.findViewById(R.id.tv_dialog_content_gateway);
		TextView tvContentMaster = (TextView) mDialog.findViewById(R.id.tv_dialog_content_master);
		TextView tvContentSlave1 = (TextView) mDialog.findViewById(R.id.tv_dialog_content_slave1);
		TextView tvContentSlave2 = (TextView) mDialog.findViewById(R.id.tv_dialog_content_slave2);
		
		String strGatewayVer = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_GatewaySoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_GatewaySoftwareDate_]);
		String strMasterVer = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MasterSoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MasterSoftwareDate_]);
		String strSlave1Ver = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave1SoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave1SoftwareDate_]);
		String strSlave2Ver = Utils.TransYearDate(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave2SoftwareYear_], mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_Slave2SoftwareDate_]);
		
		tvContentGateway.setText(strGatewayVer);
		tvContentMaster.setText(strMasterVer);
		tvContentSlave1.setText(strSlave1Ver);
		tvContentSlave2.setText(strSlave2Ver);
		mHandler.postDelayed(this, 2000);
	}

}
