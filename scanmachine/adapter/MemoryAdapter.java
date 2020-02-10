package endexcase.scanmachine.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import endexcase.scanmachine.adapter.item.MemoryItem;

import java.util.ArrayList;
import java.util.List;

import com.endex.ce60.R;

/**
 * Created by Zack on 2016/1/30.
 */
public class MemoryAdapter extends BaseAdapter {

    List<MemoryItem> mList = new ArrayList<>();
    Context mContext;
    private LayoutInflater inflater;
    ViewGroup.LayoutParams mLayoutParams;
    int mItemHeight;


    public MemoryAdapter(Context context, List<MemoryItem> list, int itemHeight) {
        mList = list;
        mContext = context;
        mItemHeight = itemHeight;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mList.get(position).getItemId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null)
            inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.grid_memory_item, null);

        convertView.setMinimumHeight(mItemHeight);
        MemoryItem item = mList.get(position);
        TextView tvItemId = (TextView) convertView.findViewById(R.id.tv_item_id);
        if(item.isCheck()){
            convertView.setBackground(mContext.getResources().getDrawable(R.drawable.data_on));
            tvItemId.setTextColor(mContext.getResources().getColor(R.color.color_text_memory_on));
        }else{
        	convertView.setBackground(mContext.getResources().getDrawable(R.drawable.data_off));
            tvItemId.setTextColor(mContext.getResources().getColor(R.color.color_text_memory_off));
        }
        
        final MemoryItem m = mList.get(position);
        tvItemId.setText(String.valueOf(m.getItemId()));
        return convertView;
    }
}
