package endexcase.scanmachine.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.adapter.MemoryAdapter;
import endexcase.scanmachine.adapter.item.MemoryItem;
import endexcase.scanmachine.database.DatabaseProxy;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.Utils;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import endexcase.scanmachine.widget.CustomToast;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Zack on 15/6/16.
 */
public class SaveMemoryFragment extends BaseFragment implements View.OnClickListener, IDialogValueListener {
	private static final AppLog smAppLog = new AppLog("Fragment",SaveMemoryFragment.class);
	
	private static final String TAG = SaveMemoryFragment.class.getSimpleName();
    private MemoryAdapter mAdapter;
    private GridView mGridView;
    List<MemoryItem> mList = new ArrayList<>();
    private Dialog mDialog, mExportDialog;
    
    private String mEditStr;
    
    private AppData mAppdata = null;
    private DatabaseProxy mDatabase;
    private ImageView mExportBtn;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_save_memory, container, false);
        
        mAppdata = AppData.getInstance();
        mDatabase = DatabaseProxy.getInstance(getActivity());
        
        init(view);
        return view;
    }

    private void init(View view) {
        super.setTitle(view, R.string.main_side_text_save_memo);
        mGridView = (GridView) view.findViewById(R.id.gridView_item_container);
        mList.clear();
        for(int i=0;i<50;i++){
        	int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (i/8);
			int bitNo = i%8;
			MemoryItem item = new MemoryItem(i + 1);
			
			item.setIsCheck(mAppdata.getBitData(byteAddr, bitNo));
			mList.add(item);
        }
        mAdapter = new MemoryAdapter(getActivity(), mList, getGridItemHeight(view));
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(mItemClickListener);
        
        mExportBtn = (ImageView) view.findViewById(R.id.btn_header_io);
        mExportBtn.setEnabled(true);
        mExportBtn.setVisibility(View.VISIBLE);
        mExportBtn.setOnClickListener(this);
        mExportBtn.setImageResource(R.drawable.btn_export_selector);
        
        mExportDialog = CustomDialogBuilder.getInstance()
        		.setContext(getActivity())
        		.setInflateLayout(R.layout.custom_dialog_warning)
    			.setCustomMessage(getResources().getString(R.string.dialog_warning_message_export))
    			.setOnClickListener(mExportListener)
    			.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void onResume() {
        super.onResume();

    }


    @Override
    public void onClick(View v) {
    	switch(v.getId()){
    	case R.id.btn_header_io:
    		//Export Data
    		if(Utils.isUsbExist()){
    			mExportDialog.show();
    		}else{
    			CustomToast.getCustomToast(mActivity).show(R.string.toast_message_usb_not_found);
    		}
    		break;
    	}
    }
    
    public void updateUI(int idx) {
    	MemoryItem item = mList.get(idx);
    	item.setIsCheck(true);
    	mList.set(idx, item);
    	mAdapter.notifyDataSetChanged();
    }
    
    AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        	if(mDialog!=null && mDialog.isShowing())
        		return;
        	final String oldFileName = Consts.findMemDataFileName(getActivity().getFilesDir().toString(), mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_],  position);
        	
        	mDialog = CustomDialogBuilder.getInstance()
        	.setContext(getActivity())
        	.setInflateLayout(R.layout.custom_dialog_save_memory)
        	.setMemoryId(position+1)
        	.setValueRetListener(SaveMemoryFragment.this)
        	.build();
        	
        	final EditText editText = (EditText)mDialog.findViewById(R.id.etInputFileName);
        	
        	if(oldFileName != null) {
        		String[] separated = oldFileName.split("_|\\.");
        		if(separated.length >= 3) {
        			mEditStr = separated[2];
        		}
        		else {
        			mEditStr = null;
        		}
        		editText.setText(mEditStr);
        	}
        	
        	Button confirmBtn = (Button)mDialog.findViewById(R.id.btConfirm);
        	
        	confirmBtn.setOnClickListener(new OnClickListener() {
        		@Override
        		public void onClick(View v) {
        			File oldFile = new File(getActivity().getFilesDir().toString() + "/" + oldFileName);
        			oldFile.delete();
        			
        			int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (position/8);
        			int bitNo = position%8;
        			
        			mAppdata.setBitData(byteAddr, bitNo, true);
        			mEditStr = editText.getText().toString();
        			
        			mDatabase.updateMachineMemData(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_],
        					mAppdata.mAddr29, mAppdata.mAddr30, mAppdata.mAddr31, mAppdata.mAddr32, mAppdata.mAddr33,
        					mAppdata.mAddr34, mAppdata.mAddr35);
        			mDatabase.saveMemoryData(mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_], position, mEditStr);
        			updateUI(position);
        			mDialog.dismiss();
        		}
        	});
        	mDialog.show();
        	
        }
    };
    


	@Override
	public void onReturnValue(String ret) {
	}
	
	private OnClickListener mExportListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mExportDialog.dismiss();
			Utils.copyHoleMemoToUsb(mActivity, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]);
		}
	}; 

}

	
