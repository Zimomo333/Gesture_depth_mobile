package com.myntai.d.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.myntai.d.sdk.MYNTCamera;
import com.myntai.d.sdk.sample.module.common.BaseActivity;
import com.myntai.d.sdk.sample.module.measure.MeasureActivity;

import java.util.ArrayList;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private TextView mSDKInfoTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSDKInfoTextView = findViewById(R.id.sdkinfo_textview);
        findViewById(R.id.start_button).setOnClickListener(this);

        // 打印SDK信息
        mSDKInfoTextView.setText(" " +
                "\n-------- SDK INFO --------" +
                "\nSDK Version: " + MYNTCamera.getSDKVersion() +
                "\nSDK Build Time: " + MYNTCamera.getSDKBuildTime() +
                "\n--------------------------" +
                "\n" +
                "\n-------- SAMPLE INFO --------" +
                "\nSAMPLE Version: " + BuildConfig.VERSION_NAME +
                "\n-----------------------------" +
                "\n\n\n" +
                getString(R.string.device_support_list));
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MeasureActivity.class);
        intent.putExtra(KEY_TITLE, "Sample");
        startActivity(intent);
    }

    //    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        MLog.e(TAG, "onItemSelected: " + position);
//        Class<?> cls = mSampleTypes[position].getCls();
//        if (cls == null) {
//            Toast.makeText(this, "开发中...", Toast.LENGTH_LONG).show();
//            return;
//        }
//        Intent intent = new Intent(this, cls);
//        intent.putExtra(KEY_TITLE, mSampleTypes[position].getName());
//        startActivity(intent);
//    }

}
