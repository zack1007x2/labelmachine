package endexcase.scanmachine.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.endex.ce60.R;

import endexcase.scanmachine.Interface.IEditTextDoneListener;
import endexcase.scanmachine.database.SharedPrefConstants;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.Hex;

public class DebugFragment extends BaseFragment implements View.OnTouchListener, OnClickListener, IEditTextDoneListener{
	
	private EditText etAddr, etData, etCmdDelay;
	private Button btSent; 
	private RelativeLayout btClose;
	private TextView tvResult;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_debug,
				container, false);
		init(view);
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		etCmdDelay.setText(String.valueOf(PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(SharedPrefConstants.PREF_KEY_CMD_DELAY, 50)));
	}

	private void init(View view) {
		tvResult = (TextView)view.findViewById(R.id.tv_result);
		etAddr = (EditText)view.findViewById(R.id.et_addr);
		etData = (EditText)view.findViewById(R.id.et_data);
		etCmdDelay = (EditText)view.findViewById(R.id.et_delay);
		
		etAddr.setOnEditorActionListener(onEditorActionListener);
		etData.setOnEditorActionListener(onEditorActionListener);
		
		registerEdittextDoneListener(etCmdDelay, this);
		
		btSent = (Button)view.findViewById(R.id.btn_sent);
		btClose = (RelativeLayout)view.findViewById(R.id.btn_header_back);
		btSent.setOnClickListener(this);
		btClose.setOnTouchListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_sent:
			byte[] cmd = null;
			String addr = etAddr.getText().toString();
			String data = etData.getText().toString();
			if(!addr.isEmpty() && !data.isEmpty()){
				int CmdAddr = Integer.parseInt(addr,16);
				if(data.length()<=2){
					byte b = (byte) (Integer.parseInt(data,16) & 0xff);
					cmd = EndexScanProtocols.getByteRequestCommand(CmdAddr, b);
					tvResult.setText("ByteCmd " + Hex.hexBytesToString(cmd) + "  sent!");
				}else{
					cmd = EndexScanProtocols.getWordRequestCommand(CmdAddr, Integer.parseInt(data,16));
					tvResult.setText("WordCmd " + Hex.hexBytesToString(cmd) + "  sent!");
				}
				mActivity.insertCmd(cmd);
			}
			break;
		}
	}

	@Override
	public void onEditTextDone(int viewId, int parentId, String retValue) {
		switch (viewId) {
		case R.id.et_delay:
//			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
//			sharedPref.edit().putInt(SharedPrefConstants.PREF_KEY_CMD_DELAY, Integer.valueOf(retValue)).commit();
//			mActivity.setCmdDlay(Integer.valueOf(retValue));
//			etCmdDelay.setText(retValue);
			break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(v.getId()){
		case R.id.btn_header_back:
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				 v.setPressed(true);
				return true;
			}else if(event.getAction()==MotionEvent.ACTION_UP){
				mActivity.showFragment(R.id.btn_header_secret);
				 v.setPressed(false);
				return true;
			}
			break;
		}
		return false;
	}

}
