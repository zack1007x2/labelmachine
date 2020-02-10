package endexcase.scanmachine.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import endexcase.scanmachine.AppData;
import endexcase.scanmachine.adapter.MemoryAdapter;
import endexcase.scanmachine.adapter.item.MemoryItem;
import endexcase.scanmachine.database.DatabaseProxy;
import endexcase.scanmachine.uart.EndexScanProtocols;
import endexcase.scanmachine.util.AppLog;
import endexcase.scanmachine.util.Consts;
import endexcase.scanmachine.util.Utils;
import endexcase.scanmachine.widget.CustomDialogBuilder;
import endexcase.scanmachine.widget.CustomToast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.endex.ce60.R;

/**
 */
public class LoadMemoryFragment extends BaseFragment implements OnClickListener{
	private static final AppLog smAppLog = new AppLog("Fragment",
			LoadMemoryFragment.class);

	private static final String TAG = LoadMemoryFragment.class.getSimpleName();
	private MemoryAdapter mAdapter;
	private GridView mGridView;
	List<MemoryItem> mList = new ArrayList<>();
	private Dialog mDialog, mImportDialog;

	private AppData mAppdata = null;
	private DatabaseProxy mDatabase;
	private ImageView mImportBtn;
	View view;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_load_memory, container,
				false);

		mAppdata = AppData.getInstance();
		mDatabase = DatabaseProxy.getInstance(getActivity());

		init(view);
		return view;
	}

	private void init(View view) {
		super.setTitle(view, R.string.main_side_text_load_memo);
		mGridView = (GridView) view.findViewById(R.id.gridView_item_container);

		mList.clear();
		for (int i = 0; i < 50; i++) {
			int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (i / 8);
			int bitNo = i % 8;
			MemoryItem item = new MemoryItem(i + 1);

			item.setIsCheck(mAppdata.getBitData(byteAddr, bitNo));
			mList.add(item);
		}
		mAdapter = new MemoryAdapter(getActivity(), mList,
				getGridItemHeight(view));
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(mItemClickListener);
		
		mImportBtn = (ImageView) view.findViewById(R.id.btn_header_io);
		mImportBtn.setEnabled(true);
		mImportBtn.setVisibility(View.VISIBLE);
		mImportBtn.setOnClickListener(this);
		mImportBtn.setImageResource(R.drawable.btn_import_selector);
		
		mImportDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_warning)
    			.setCustomMessage(getResources().getString(R.string.dialog_warning_message_import))
    			.setOnClickListener(mImportListener)
    			.build();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
    	case R.id.btn_header_io:
    		//Import Data
    		if(Utils.isUsbExist()){
    			mImportDialog.show();
    		}else{
    			CustomToast.getCustomToast(mActivity).show(R.string.toast_message_usb_not_found);
    		}
    		break;
    	}
	}

	AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				final int position, long id) {
			if(mDialog != null && mDialog.isShowing())
        		return;
			int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29
					+ (position / 8);
			int bitNo = position % 8;

			if (mAppdata.getBitData(byteAddr, bitNo)) {
				String fileName = Consts
						.findMemDataFileName(
								getActivity().getFilesDir().toString(),
								mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_],
								position);

				mDialog = CustomDialogBuilder.getInstance()
						.setContext(getActivity())
						.setInflateLayout(R.layout.custom_dialog_load_memory)
						.setMemoryId(position + 1)
						.setMemoryItem(mList.get(position))
						.build();

				TextView messageView = (TextView) mDialog
						.findViewById(R.id.tvContentTitle);
				messageView.setText(fileName);

				Button confirmBtn = (Button) mDialog
						.findViewById(R.id.btConfirm);
				confirmBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						
						mDatabase
								.readMemoryData(
										mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_],
										position);
						mDialog.dismiss();
						mActivity.reInitParameter();
						
						int currentLan = mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_LanguageMode_];
						setLanguage(currentLan);
						mActivity.setLanguageMode(currentLan);
					}

				});

				mDialog.show();
			}
		}
	};
	
	private OnClickListener mImportListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			mImportDialog.dismiss();
			Utils.copyHoleMemoToLocal(mActivity, mAppdata.mWordPara[EndexScanProtocols.WORD_COMMAND_MachineType_]);
			//刷新Adapter....
			mList.clear();
			for (int i = 0; i < 50; i++) {
				int byteAddr = EndexScanProtocols.BYTE_COMMAND_ADDR29 + (i / 8);
				int bitNo = i % 8;
				MemoryItem item = new MemoryItem(i + 1);

				item.setIsCheck(mAppdata.getBitData(byteAddr, bitNo));
				mList.add(item);
			}
			mAdapter.notifyDataSetChanged();
		}
	};

	private void setLanguage(int currentLan) {
		String languageToLoad = null;
		if(currentLan == AppData.LANGUAGE_ENGLISH)
			languageToLoad = "en";
		else
			languageToLoad = "zh";
		
		Locale locale = new Locale(languageToLoad); 
		Locale.setDefault(locale);
		android.content.res.Configuration config = new android.content.res.Configuration();
		config.locale = locale;
		getActivity().getBaseContext().getResources().updateConfiguration(config, getActivity().getBaseContext().getResources().getDisplayMetrics());
		
		super.setTitle(view, R.string.main_side_text_load_memo);
		
		mImportDialog = CustomDialogBuilder.getInstance()
				.setContext(getActivity())
				.setInflateLayout(R.layout.custom_dialog_warning)
    			.setCustomMessage(getResources().getString(R.string.dialog_warning_message_import))
    			.setOnClickListener(mImportListener)
    			.build();
		mDialog = null;
		
	}

}
