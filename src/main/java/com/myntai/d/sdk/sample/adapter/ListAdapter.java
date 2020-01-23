package com.myntai.d.sdk.sample.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.myntai.d.sdk.MYNTCamera;
import com.myntai.d.sdk.sample.R;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<MYNTCamera> mCameras;

    public ListAdapter(Context context, ArrayList<MYNTCamera> cameras) {
        this.mContext = context;
        this.mCameras = cameras;
    }

    @Override
    public int getCount() {
        return mCameras.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return mCameras.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemHolder holder;
        View view;
        if (convertView == null) {
            view = View.inflate(mContext, R.layout.list_item, null);
            holder = new ItemHolder(view);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ItemHolder)view.getTag();
        }
        MYNTCamera camera = mCameras.get(position);
        holder.nameTextView.setText(camera.getName());
        holder.descTextView.setText("sn: " + camera.getSerialNumber());
        return view;
    }

    class ItemHolder {

        TextView nameTextView;
        TextView descTextView;

        ItemHolder(View view) {
            nameTextView = view.findViewById(R.id.nameTextView);
            descTextView = view.findViewById(R.id.descTextView);
        }

    }

}
