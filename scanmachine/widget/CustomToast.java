package endexcase.scanmachine.widget;

import com.endex.ce60.R;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {

	private static TextView mToastmsg;
	private static View mLayout;
	private static Toast mToast = null;
	private static Context mContext = null;

	public CustomToast() {
	}

	private static CustomToast mCustomToast;

	public static CustomToast getCustomToast(Context con) {
		mContext = con;
		mCustomToast = new CustomToast();
		LayoutInflater inflater = LayoutInflater.from(mContext);
		mLayout = inflater.inflate(R.layout.custom_toast, null);
		mToastmsg = (TextView) mLayout.findViewById(R.id.tv_toast_message);
		return mCustomToast;
	}

	public void show(int resId) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = new Toast(mContext);
		mToast.setGravity(Gravity.CENTER, 0, 0);
		mToast.setDuration(Toast.LENGTH_SHORT);
		mToast.setView(mLayout);
		mToastmsg.setText(resId);
		mToast.show();
	}

	public void show(String str) {
		if (mToast != null) {
			mToast.cancel();
		}
		mToast = new Toast(mContext);
		mToast.setGravity(Gravity.CENTER, 0, 0);
		mToast.setDuration(Toast.LENGTH_SHORT);
		mToast.setView(mLayout);
		mToastmsg.setText(str);
		mToast.show();
	}

	public void setDuration(int duration) {
		mToast.setDuration(duration);
	}

}