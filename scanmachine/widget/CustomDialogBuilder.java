package endexcase.scanmachine.widget;

import com.endex.ce60.R;

import endexcase.scanmachine.AppData;
import endexcase.scanmachine.Interface.IDialogValueListener;
import endexcase.scanmachine.adapter.item.MemoryItem;
import endexcase.scanmachine.task.AutoAdjustmentProcessRunnable;
import endexcase.scanmachine.task.VersionInfoRunnable;
import endexcase.scanmachine.util.Consts;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomDialogBuilder {

	private int mLayoutId, mMemoryId;
	private Context mContext;
	private OnClickListener mPositiveOnClickListener;
	private IDialogValueListener mValueListener;
	private MemoryItem memoItem;
	private EditText saveDialogEdittext;
	private OnItemClickListener mItemClickListener;
	private String mCustomMessage;
	private boolean cancelable=true;
	private Runnable mAutoRunnable, mVersionInfoRunnable;
	
	private Handler mHandler = new Handler();

	private CustomDialogBuilder() {
	}

	private static volatile CustomDialogBuilder mInstance = null;

	public final static CustomDialogBuilder getInstance() {
		if (mInstance == null) {
			synchronized (CustomDialogBuilder.class) {
				if (mInstance == null) {
					CustomDialogBuilder.mInstance = new CustomDialogBuilder();
				}
			}
		}
		return mInstance;
	}

	public CustomDialogBuilder setContext(Context context) {
		mContext = context;
		return this;
	}

	public CustomDialogBuilder setInflateLayout(int layoutId) {
		mLayoutId = layoutId;
		return this;
	}

	public CustomDialogBuilder setMemoryId(int memoryId) {
		mMemoryId = memoryId;
		return this;
	}

	public CustomDialogBuilder setMemoryItem(MemoryItem item) {
		memoItem = item;
		return this;
	}

	public CustomDialogBuilder setOnClickListener(OnClickListener listener) {
		mPositiveOnClickListener = listener;
		return this;
	}
	
	public CustomDialogBuilder setValueRetListener(IDialogValueListener valueListener){
		mValueListener = valueListener;
		return this;
	}
	
	public CustomDialogBuilder setOnItemClickListener(OnItemClickListener listener) {
		mItemClickListener = listener;
		return this;
	}
	
	public CustomDialogBuilder setCustomMessage(String message) {
		this.mCustomMessage = message;
		return this;
	}
	
	public CustomDialogBuilder setCancelable(boolean cancelable) {
		this.cancelable = cancelable;
		return this;
	}
	
	public Dialog build() {
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		if (mPositiveOnClickListener == null) {
			mPositiveOnClickListener = new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			};
		}
		
		OnClickListener mSaveMemoPostiveListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				String curRet = saveDialogEdittext.getText().toString().trim();
				if(!curRet.isEmpty()){
					mValueListener.onReturnValue(curRet);
				}
				dialog.dismiss();
			}
		};
		
		OnClickListener negativeListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		};

		switch (mLayoutId) {
		case R.layout.custom_dialog_warning:
			//setContext, setInflateLayout, setCustomMessage , setOnClickListener
			dialog.setContentView(mLayoutId);
			TextView tvMessage = (TextView) dialog.findViewById(R.id.tvMessage);
			tvMessage.setText(mCustomMessage);
			Button btConfirm_msg = (Button) dialog.findViewById(R.id.btConfirm);
			btConfirm_msg.setOnClickListener(mPositiveOnClickListener);
			Button btCancel_msg = (Button) dialog.findViewById(R.id.btCancel);
			btCancel_msg.setOnClickListener(negativeListener);
			break;
		case R.layout.custom_dialog_save_memory:
			//setContext, setInflateLayout, setValueRetListener
			dialog.setContentView(mLayoutId);
			TextView tvContent_save = (TextView) dialog.findViewById(R.id.tvContent);
			String input = String.format(mContext.getResources().getString(
							R.string.save_memo_dialog_content_save_to),mMemoryId);
			tvContent_save.setText(input);
			saveDialogEdittext = (EditText) dialog.findViewById(R.id.etInputFileName);
			Button btConfirm_save = (Button) dialog.findViewById(R.id.btConfirm);
			btConfirm_save.setOnClickListener(mSaveMemoPostiveListener);
			Button btCancel_save = (Button) dialog.findViewById(R.id.btCancel);
			btCancel_save.setOnClickListener(negativeListener);
			break;
		case R.layout.custom_dialog_load_memory:
			//setInflateLayout, setOnClickListener
			dialog.setContentView(mLayoutId);
			TextView tvContent_load = (TextView) dialog.findViewById(R.id.tvContent);
			tvContent_load.setText(memoItem.getItemFileName());
			Button btConfirm_load = (Button) dialog.findViewById(R.id.btConfirm);
			btConfirm_load.setOnClickListener(mPositiveOnClickListener);
			Button btCancel_load = (Button) dialog.findViewById(R.id.btCancel);
			btCancel_load.setOnClickListener(negativeListener);
			break;

		case R.layout.custom_dialog_machine_type:
			//setContext, setInflateLayout, setOnItemClickListener
			dialog.setContentView(mLayoutId);
			ListView lvMachineType = (ListView) dialog.findViewById(R.id.lv_dialog_machine_type);
			ArrayAdapter<String> listAdapter;
			String[] machineArr = mContext.getResources().getStringArray(R.array.machine_type_arr);
			listAdapter = new ArrayAdapter<String>(mContext, R.layout.custom_dialog_machine_type_item, machineArr);
			lvMachineType.setAdapter(listAdapter);
			lvMachineType.setOnItemClickListener(mItemClickListener);
			break;
		case R.layout.custom_dialog_software_version:
			//setContext, setInflateLayout, setValueRetListener
			final AppData mAppdata = AppData.getInstance(); 
			dialog.setContentView(mLayoutId);
			String verName = "";
			try {
				verName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			
			TextView tvContentApp = (TextView) dialog.findViewById(R.id.tv_dialog_content_app);
			tvContentApp.setText(verName);
			mVersionInfoRunnable = new VersionInfoRunnable(mHandler, mAppdata, dialog);
			mHandler.post(mVersionInfoRunnable);
			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mValueListener.onReturnValue("cancel");
					mHandler.removeCallbacks(mVersionInfoRunnable);
				}
			});
			break;
		case R.layout.custom_dialog_password:
			//setInflateLayout, setValueRetListener
			dialog.setContentView(mLayoutId);
			final EditText etPwdInput = (EditText) dialog.findViewById(R.id.etInputPwd);
			OnClickListener pwdConfirmListener = new OnClickListener(){

				@Override
				public void onClick(View v) {
					String val = etPwdInput.getText().toString();
					if(val.equals(Consts.PASSWORD)){
						mValueListener.onReturnValue("PWD");
						dialog.dismiss();
					}
				}
			};
			Button btConfirm_pwd = (Button) dialog.findViewById(R.id.btConfirm);
			btConfirm_pwd.setOnClickListener(pwdConfirmListener);
			Button btCancel_pwd = (Button) dialog.findViewById(R.id.btCancel);
			btCancel_pwd.setOnClickListener(negativeListener);
			break;
		case R.layout.custom_dialog_loading:
			//setContext, setInflateLayout
			final Dialog loadingDialog = new Dialog(mContext,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
			loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
			loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			loadingDialog.setContentView(mLayoutId);
			return loadingDialog;
		case R.layout.custom_dialog_auto_detect_process:
			//setContext, setInflateLayout | optional: setValueRetListener
			final AppData appdata = AppData.getInstance(); 
			dialog.setContentView(mLayoutId);
			RelativeLayout rl_auto_process_dialog = (RelativeLayout)dialog.findViewById(R.id.rl_auto_process_dialog);
			rl_auto_process_dialog.setOnTouchListener(new OnTouchListener() {
				
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(mValueListener!=null)mValueListener.onReturnValue("onDialogTouch");
					return false;
				}
			});
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			mAutoRunnable = new AutoAdjustmentProcessRunnable(mHandler, appdata, dialog);
			mHandler.post(mAutoRunnable);
			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if(mValueListener!=null)mValueListener.onReturnValue("cancel");
					mHandler.removeCallbacks(mAutoRunnable);
				}
			});
			break;
		}

		return dialog;
	}
}
