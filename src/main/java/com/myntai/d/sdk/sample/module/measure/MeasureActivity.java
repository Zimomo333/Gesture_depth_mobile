package com.myntai.d.sdk.sample.module.measure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.myntai.d.sdk.MYNTCamera;
import com.myntai.d.sdk.USBMonitor;
import com.myntai.d.sdk.sample.R;
import com.myntai.d.sdk.sample.adapter.ListAdapter;
import com.myntai.d.sdk.sample.module.common.BaseActivity;

import java.util.ArrayList;

import static com.myntai.d.sdk.MYNTCamera.CAMERA_TYPE_D1000;

public class MeasureActivity extends BaseActivity {
    private static String TAG = "MeasureActivity";
    private static ArrayList<MYNTCamera> mCameras = new ArrayList<>();

    public static MYNTCamera getCameraWithSn(String sn) {
        for (MYNTCamera camera: mCameras) {
            if (camera.getSerialNumber().equals(sn)) {
                return camera;
            }
        }
        return null;
    }

    private Button mOpenCameraButton;
    private ListView mListView;
    private RadioButton mBit8RadioButton;
    private RadioButton mBit11RadioButton;
    private RadioButton mBit14RadioButton;
    private RadioButton mPixel480RadioButton;
    private RadioButton mPixel720RadioButton;
    private CheckBox mPreviewLRCheckBox;
    private CheckBox mAECheckBox;
    private CheckBox mAWBCheckBox;
    private CheckBox mColorFPSBox;
    private CheckBox mDepthFPSBox;
    private CheckBox mColorPreviewBox;
    private CheckBox mDepthPreviewBox;
    private Spinner mCameraSourceSpinner;
    private Spinner mCameraFrameSpinner;

    private USBMonitor mUSBMonitor;
    private ListAdapter mAdapter;

    private Handler mHandler = new Handler();

    private USBMonitor.IUSBMonitorListener mUsbMonitorListener = new USBMonitor.IUSBMonitorListener() {

        private boolean isExist(MYNTCamera camera) {
            for (MYNTCamera _camera: mCameras) {
                if (_camera.getSerialNumber().equals(camera.getSerialNumber())) {
                    return true;
                }
            }
            return false;
        }

        private void addCamera(MYNTCamera camera) {
            if (!isExist(camera))
                mCameras.add(camera);
            mAdapter.notifyDataSetChanged();
        }

        private void removeCamera(MYNTCamera camera) {
            if (isExist(camera)) {
                mCameras.remove(camera);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void didAttach(MYNTCamera camera) {
            addCamera(camera);
            Toast.makeText(MeasureActivity.this, "didAttach Camera" + camera.getName(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void didDettach(final MYNTCamera camera) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (camera != null) {
                            camera.destroy();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            removeCamera(camera);
            Toast.makeText(MeasureActivity.this, "didDettach Camera " + camera.getName(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void didConnectedCamera(MYNTCamera camera) {

        }

        @Override
        public void didDisconnectedCamera(MYNTCamera camera) {

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_measure);

        initUI();

        mUSBMonitor = new USBMonitor(this, mUsbMonitorListener);
        mUSBMonitor.register();
    }

    private void initUI() {
        mListView = findViewById(R.id.listView);
        mOpenCameraButton = findViewById(R.id.openCameraButton);
        mBit8RadioButton = findViewById(R.id.bit8RadioButton);
        mBit11RadioButton = findViewById(R.id.bit11RadioButton);
        mBit14RadioButton = findViewById(R.id.bit14RadioButton);
        mPixel480RadioButton = findViewById(R.id.pixel480RadioButton);
        mPixel720RadioButton = findViewById(R.id.pixel720RadioButton);
        mPreviewLRCheckBox = findViewById(R.id.previewLRCheckBox);
        mAECheckBox = findViewById(R.id.aeCheckBox);
        mAWBCheckBox = findViewById(R.id.awbCheckBox);
        mColorFPSBox = findViewById(R.id.color_fps_checkbox);
        mDepthFPSBox = findViewById(R.id.depth_fps_checkBox);
        mColorPreviewBox = findViewById(R.id.colorPreviewCheckBox);
        mDepthPreviewBox = findViewById(R.id.depthPreviewCheckBox);

        mCameraSourceSpinner = findViewById(R.id.cameraSourceSpinner);
        mCameraSourceSpinner.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, CameraActivity.CAMERA_SOURCE_ITEMS));

        mCameraFrameSpinner = findViewById(R.id.cameraFrameSpinner);
        mCameraFrameSpinner.setAdapter(new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, CameraActivity.CAMERA_FRAME_ITEMS));
        mCameraFrameSpinner.setSelection(2);

        mAdapter = new ListAdapter(this, mCameras);
        mListView.setAdapter(mAdapter);

        mOpenCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameras.isEmpty()) {
                    Toast.makeText(MeasureActivity.this, "Please input the camera!", Toast.LENGTH_LONG).show();
                    return;
                }
                startCameraActivity(mCameras.get(0));
            }
        });
    }

    private void cancelDeviceFunction(int deviceType, Boolean isUsb3) {
        if (deviceType == CAMERA_TYPE_D1000) {

            if (isUsb3) {
                View btn8 = findViewById(R.id.bit8RadioButton);
                btn8.setVisibility(View.INVISIBLE);
            } else {
//                View btn11 = findViewById(R.id.bit11RadioButton);
//                btn11.setVisibility(View.INVISIBLE);
//                View btn14 = findViewById(R.id.bit14RadioButton);
//                btn14.setVisibility(View.INVISIBLE);
            }
        } else {
            View btn8 = findViewById(R.id.bit8RadioButton);
            btn8.setVisibility(View.VISIBLE);
            View btn11 = findViewById(R.id.bit11RadioButton);
            btn11.setVisibility(View.VISIBLE);
            View btn14 = findViewById(R.id.bit14RadioButton);
            btn14.setVisibility(View.VISIBLE);
        }
    }

    private void startCameraActivity(MYNTCamera camera) {
        short bit = MYNTCamera.DEPTH_DATA_8_BITS;
        int height = 480;

        if (mBit8RadioButton.isChecked())
            bit = MYNTCamera.DEPTH_DATA_8_BITS;
        if (mBit11RadioButton.isChecked())
            bit = MYNTCamera.DEPTH_DATA_11_BITS;
        if (mBit14RadioButton.isChecked())
            bit = MYNTCamera.DEPTH_DATA_14_BITS;

        if (mPixel480RadioButton.isChecked())
            height = 480;
        if (mPixel720RadioButton.isChecked())
            height = 720;

        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.KEY_DEPTHTYPE, bit);
        intent.putExtra(CameraActivity.KEY_PREVIEW_HEIGHT, height);
        intent.putExtra(CameraActivity.KEY_PREVIEW_LR, mPreviewLRCheckBox.isChecked());
        intent.putExtra(CameraActivity.KEY_AUTOAWB, mAWBCheckBox.isChecked());
        intent.putExtra(CameraActivity.KEY_AUTOAE, mAECheckBox.isChecked());
        intent.putExtra(CameraActivity.KEY_SN, camera.getSerialNumber());
        intent.putExtra(CameraActivity.KEY_CAMERATYPE, camera.getCameraType());
        intent.putExtra(CameraActivity.KEY_ENABLE_COLOR_FPS, mColorFPSBox.isChecked());
        intent.putExtra(CameraActivity.KEY_ENABLE_DEPTH_FPS, mDepthFPSBox.isChecked());
        intent.putExtra(CameraActivity.KEY_ENABLE_COLOR_PREVIEW, mColorPreviewBox.isChecked());
        intent.putExtra(CameraActivity.KEY_ENABLE_DEPTH_PREVIEW, mDepthPreviewBox.isChecked());
        intent.putExtra(CameraActivity.KEY_CAMERA_SOURCE, mCameraSourceSpinner.getSelectedItemPosition());
        intent.putExtra(CameraActivity.KEY_CAMERA_FRAME, mCameraFrameSpinner.getSelectedItemPosition());

        startActivity(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清空相机
        mCameras.clear();

        if (mUSBMonitor != null)
            mUSBMonitor.destroy();
        if (mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
    }
}
