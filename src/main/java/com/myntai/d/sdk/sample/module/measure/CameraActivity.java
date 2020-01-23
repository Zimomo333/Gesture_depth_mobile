package com.myntai.d.sdk.sample.module.measure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.myntai.d.sdk.MYNTCamera;
import com.myntai.d.sdk.bean.FrameData;
import com.myntai.d.sdk.bean.ImuData;
import com.myntai.d.sdk.sample.Classifier;
import com.myntai.d.sdk.sample.DisplayResult;
import com.myntai.d.sdk.sample.R;
import com.myntai.d.sdk.sample.module.common.BaseActivity;
import com.myntai.d.sdk.sample.widget.UVCCameraTextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraActivity extends BaseActivity {
    //加载OpenCV,必须先加载
    static{
        if(!OpenCVLoader.initDebug())
        {
            Log.d("OpenCV", "init failed");
        }
    }

    static String TAG = "CameraActivity";
    static String KEY_DEPTHTYPE = "depth_type";
    static String KEY_SN = "camera_sn";
    static String KEY_PREVIEW_HEIGHT = "preview_height";
    static String KEY_PREVIEW_LR = "preview_lr";
    static String KEY_SERIALNUMBER = "serial_number";
    static String KEY_CAMERATYPE = "camera_type";
    static String KEY_AUTOAE = "camera_auto_ae";
    static String KEY_AUTOAWB = "camera_auto_awb";
    static String KEY_ENABLE_COLOR_FPS = "camera_color_fps";
    static String KEY_ENABLE_DEPTH_FPS = "camera_depth_fps";
    static String KEY_ENABLE_COLOR_PREVIEW = "camera_color_preview";
    static String KEY_ENABLE_DEPTH_PREVIEW = "camera_depth_preview";
    static String KEY_CAMERA_SOURCE = "camera_source";
    static String KEY_CAMERA_FRAME = "camera_frame";

    static int REQUEST_EXTERNAL_STORAGE = 1;
    static String []PERMISSIONS_STORAGE = new String[] {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    static String[] CAMERA_SOURCE_ITEMS = {"FRAME", "IMU", "FRAME & IMU"};
    static MYNTCamera.Source[] CAMERA_SOURCES = {MYNTCamera.Source.VIDEO, MYNTCamera.Source.MOTION, MYNTCamera.Source.ALL};

    static String[] CAMERA_FRAME_ITEMS = {"COLOR", "DEPTH", "COLOR & DEPTH"};
    static MYNTCamera.Frame[] CAMERA_FRAMES = {MYNTCamera.Frame.COLOR, MYNTCamera.Frame.DEPTH, MYNTCamera.Frame.ALL};

    private Surface mColorSurface;
    private Surface mDepthSurface;

    private Point mSpotPoint = new Point(0, 0);
    private Size mDisplaySize = new Size(0, 0);
    private Size mPreviewSize = new Size(640, 480);
    private short mDepthType = MYNTCamera.DEPTH_DATA_11_BITS;
    private long mOpenCameraTime = 0L;
    private boolean isPreviewLR;
    private boolean isOpenAE;
    private boolean isOpenAWB;
    private boolean isColorFps;
    private boolean isDepthFps;
    private boolean isColorPreview;
    private boolean isDepthPreview;
    private final int spotWidth = 80;
    private final int spotHeight = 80;
    private Boolean isSaved = false;
    private MYNTCamera.Source mSource;
    private MYNTCamera.Frame mFrame;
    private FrameData mColorFramedData;
    private UVCCameraTextureView mColorTextureView;
    private UVCCameraTextureView mDepthTextureView;
    private RelativeLayout mDepthLayout;
    private RelativeLayout mTextureSuperLayout;

    private TextView mInfoTextView;
    private TextView mAccTextView;
    private TextView mGyroTextView;
    private TextView mMeasureTextView;
    private ImageView mMeasureSpotImageView;

    private Button mSwitchIRButton;

    private Handler mHandler = new Handler();

    private CameraTools mCameraTools;

    MYNTCamera camera;

    private Classifier classifier;//识别类
    private static final String MODEL_FILE = "file:///android_asset/digital_gesture.pb"; //模型存放路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_measure_camera);
        initUI();
        if (!initCamera()) {
            Log.e(TAG, "initCamera error");
            return;
        }

        checkPermission();
    }

    private void initUI() {
        mMeasureTextView = findViewById(R.id.measureTextView);
        mMeasureSpotImageView = findViewById(R.id.measureSpotImageView);
        mInfoTextView = findViewById(R.id.infoTextView);
        mAccTextView = findViewById(R.id.accTextView);
        mGyroTextView = findViewById(R.id.gyroTextView);
        mSwitchIRButton = findViewById(R.id.switchIRButton);

        mColorTextureView = findViewById(R.id.colorTextureView);
        mDepthTextureView = findViewById(R.id.depthTextureView);
        mDepthLayout = findViewById(R.id.depthLayout);
        mTextureSuperLayout = findViewById(R.id.textureView_super_layout);
        mSwitchIRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIRDialog();
            }
        });
    }

    private boolean initCamera() {

        String sn = getIntent().getStringExtra(KEY_SN);
        if (sn != null) {
            camera = MeasureActivity.getCameraWithSn(sn);
        }
        if (camera == null) {
            showDialog("Error", "Camera is null");
            finish();
            return false;
        }
        mCameraTools = new CameraTools(this, camera);
        // 读取参数
        isPreviewLR = getIntent().getBooleanExtra(KEY_PREVIEW_LR, false);
        isOpenAE = getIntent().getBooleanExtra(KEY_AUTOAE, false);
        isOpenAWB = getIntent().getBooleanExtra(KEY_AUTOAWB, false);
        mDepthType = getIntent().getShortExtra(KEY_DEPTHTYPE,  MYNTCamera.DEPTH_DATA_8_BITS);
        isColorFps = getIntent().getBooleanExtra(KEY_ENABLE_COLOR_FPS, false);
        isDepthFps = getIntent().getBooleanExtra(KEY_ENABLE_DEPTH_FPS, false);
        isColorPreview = getIntent().getBooleanExtra(KEY_ENABLE_COLOR_PREVIEW, true);
        isDepthPreview = getIntent().getBooleanExtra(KEY_ENABLE_DEPTH_PREVIEW, true);
        mSource = CAMERA_SOURCES[getIntent().getIntExtra(KEY_CAMERA_SOURCE, 0)];
        mFrame = CAMERA_FRAMES[getIntent().getIntExtra(KEY_CAMERA_FRAME, 0)];

        int previewHeight = getIntent().getIntExtra(KEY_PREVIEW_HEIGHT, 480);
        int cameraType = getIntent().getIntExtra(KEY_CAMERATYPE,  MYNTCamera.CAMERA_TYPE_D1000);

        if (previewHeight == 480) {
            mPreviewSize = new Size(640, 480);
        } else {
            mPreviewSize = new Size(1280, 720);
        }

        mColorTextureView.setAspectRatio((double)mPreviewSize.getWidth() / mPreviewSize.getHeight());
        mDepthTextureView.setAspectRatio((double)mPreviewSize.getWidth() / mPreviewSize.getHeight());
        mDepthTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    didClickDepthView(event);
                }
                return true;
            }
        });

        camera.setCameraListener(new MYNTCamera.ICameraListener() {
            @Override
            public void didConnectedCamera(MYNTCamera camera) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startMeasure();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void didDisconnectedCamera(MYNTCamera camera) {
            }

        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mOpenCameraTime = System.currentTimeMillis();
                Boolean result = camera.connect();
                if (!result) {
                    showDialog("Error", getString(R.string.camera_open_fail_reopen));
                }
            }
        }, 100);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Size layoutSize = new Size(mDepthLayout.getWidth(), mDepthLayout.getHeight());
                int rateWidth = layoutSize.getWidth();

                if ((float)layoutSize.getWidth() / layoutSize.getHeight() > (float)mPreviewSize.getWidth() / mPreviewSize.getHeight()) {
                    rateWidth = mPreviewSize.getWidth() * layoutSize.getHeight() / mPreviewSize.getHeight();
                }
                mDisplaySize = new Size(rateWidth, mPreviewSize.getHeight() * rateWidth / mPreviewSize.getWidth());
                mSpotPoint = new Point(mDisplaySize.getWidth() / 2, mDisplaySize.getHeight() / 2);
                View spot = mMeasureSpotImageView;
                spot.layout(mSpotPoint.x - spotWidth / 2, mSpotPoint.y - spotHeight / 2, mSpotPoint.x + spotWidth /2, mSpotPoint.y + spotHeight / 2);
                Log.e(TAG, "displaySize = " + mDisplaySize + ", layoutSize = " + layoutSize + ", rateWidth = " + rateWidth);
            }
        }, 500);

        return true;
    }

    public void savePly(View view) {
        mCameraTools.savePLY();
    }

    //缩放图片,使用openCV，缩放方法采用area interpolation法
    private Bitmap scaleImage(Bitmap bitmap, int width, int height)
    {

        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap, src);
        //new Size(width, height)
        Imgproc.resize(src, dst, new org.opencv.core.Size(width,height),0,0,Imgproc.INTER_AREA);
        Bitmap bitmap1 = Bitmap.createBitmap(dst.cols(),dst.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(dst, bitmap1);
        return bitmap1;
    }

    public void saveDepth(View view) {
        TextureView textureView = findViewById(R.id.depthTextureView);
        Bitmap bitmap = textureView.getBitmap();
        //缩放得到用于显示的图片 128*128
        Bitmap displayBitmap = scaleImage(bitmap,128,128);
        //缩放得到用于预测的图片 64*64
        Bitmap bitmapForPredit = scaleImage(bitmap,64,64);

        //加载模型
        classifier = new Classifier(getAssets(),MODEL_FILE);
        ArrayList<String> result = classifier.predict(bitmapForPredit);
        //传递参数
        Bundle bundle = new Bundle();
        bundle.putParcelable("image",displayBitmap);
        bundle.putStringArrayList("recognize_result",result);
        Intent intent = new Intent(CameraActivity.this, DisplayResult.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void readDepth(View view) {
        mCameraTools.readDepthData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");

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

        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    private void checkPermission() {
        try {
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startMeasure() throws Exception {
        Log.e(TAG, "startMeasure");
        if (camera == null)
            return;

        /*
         * D-1000
         *  USB2.0 使用8bit
         *  USB3.0 使用11bit
         *
         * D-1200 (Mobile版)
         *  只支持USB2.0，可用8bit & 11bit
         *
         * */

        //打开设备
        int result = camera.open();

        if (result < 0) {
            showDialog("Error", String.format(getString(R.string.camera_open_fail), result));
            return;
        }

        Log.e(TAG, "isUSB3 ->" + camera.getIsUSB3());
        mColorSurface = new Surface(mColorTextureView.getSurfaceTexture());
        mDepthSurface = new Surface(mDepthTextureView.getSurfaceTexture());

        if (isColorPreview)
            camera.setPreviewDisplay(mColorSurface, MYNTCamera.Frame.COLOR);
        if (isDepthPreview)
            camera.setPreviewDisplay(mDepthSurface, MYNTCamera.Frame.DEPTH);

        if (isOpenAE)
            camera.setEnableAE();
        else
            camera.setDisableAE();

        if (isOpenAWB)
            camera.setEnableAWB();
        else
            camera.setDisableAWB();

        camera.setEnableFrameFPS(isColorFps, MYNTCamera.CAMERA_COLOR);
        camera.setEnableFrameFPS(isDepthFps, MYNTCamera.CAMERA_DEPTH);
        camera.setPlyMode(0);
        camera.setMaxFrameFPS(MYNTCamera.FPS.FPS30);
        camera.setPreviewSize(mPreviewSize.getWidth() , mPreviewSize.getHeight());

        if (isPreviewLR)
            camera.setColorMode(MYNTCamera.ColorFrame.ALL);
        else
            camera.setColorMode(MYNTCamera.ColorFrame.LEFT);

        camera.setDepthType(mDepthType);
        camera.setFrameCallback(new MYNTCamera.IFrameCallback() {
            @Override
            public void onFrame(final FrameData frameData) {
                didUpdateFrame(frameData);
            }
        });
        camera.setImuCallback(new MYNTCamera.IIMUCallback() {
            @Override
            public void onIMU(ImuData imuData) {
                didUpdateIMU(imuData);
            }
        });

        result = camera.start(mSource, mFrame);
        updateInfoLabel(result == MYNTCamera.STATE_CODE_SUCCESS);

        if (result == MYNTCamera.STATE_CODE_SUCCESS) {
        } else if (result == MYNTCamera.STATE_CODE_CAMERA_CLOSED)
            showDialog("Error", getString(R.string.camera_open_fail_camera_closed));
        else if (result == MYNTCamera.STATE_CODE_D1000_CONFIG_U2_ERROR)
            showDialog("Error", getString(R.string.camera_open_fail_u2_error));
        else if (result == MYNTCamera.STATE_CODE_D1000_CONFIG_U3_ERROR)
            showDialog("Error", getString(R.string.camera_open_fail_u3_error));
        else
            showDialog("Error", String.format(getString(R.string.camera_open_fail_unknown_error), result));
    }

    private void didUpdateFrame(final FrameData frameData) {
        if (frameData.flag == FrameData.DEPTH) {
            Log.e(TAG, "didUpdateFrame depth");
            long startTime = System.currentTimeMillis();
            frameData.getDistanceInts();
            long endTime = System.currentTimeMillis();
//            Log.e(TAG, "getDistanceInts time:" + (endTime - startTime));
            updateDistanceLabel();

        } else if (frameData.flag == FrameData.COLOR) {
            Log.e(TAG, "didUpdateFrame color");
            mColorFramedData = frameData;
            updateDistanceLabel();
        }
    }

    private void didUpdateIMU(final ImuData data) {
        if (data.flag == ImuData.ACCELEROMETER) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAccTextView.setText(String.format("acc: x -> %.2f, y -> %.2f, z -> %.2f, timestamp -> %d, temperature -> %.2f",
                            data.value[0],
                            data.value[1],
                            data.value[2],
                            data.timestamp,
                            data.temperature));
                }
            });
        }
        if (data.flag == ImuData.GYROSCOPE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGyroTextView.setText(String.format("gyro: x -> %.2f, y -> %.2f, z -> %.2f, timestamp -> %d, temperature -> %.2f",
                            data.value[0],
                            data.value[1],
                            data.value[2],
                            data.timestamp,
                            data.temperature)
                    );
                }
            });
        }
    }

    private void didClickDepthView(MotionEvent event) {
        Log.e(TAG, "---------");
        // 转换坐标
        View v = mMeasureSpotImageView;
        Point center = new Point((int)event.getX(), (int)event.getY());
        int left = 0;
        int right = 0;
        left = center.x > mDisplaySize.getWidth() ? mDisplaySize.getWidth() - spotWidth / 2 : center.x - spotWidth / 2;
        right = center.x  > mDisplaySize.getWidth() ? mDisplaySize.getWidth() : center.x + spotWidth / 2;
        v.layout(left  ,
                center.y - spotHeight / 2,
                right,
                center.y + spotHeight / 2);

        Log.e(TAG, "-------didClickDepthView spot -- $center");
        mSpotPoint = center;
    }

    private void updateInfoLabel(boolean isOK) {
        final StringBuilder info = new StringBuilder();
        String depthType = "";
        if (camera.getDepthType() == MYNTCamera.DEPTH_DATA_8_BITS)
            depthType = "8bit";
        else if(camera.getDepthType() == MYNTCamera.DEPTH_DATA_11_BITS)
            depthType = "11bit";
        else
            depthType = "14bit";
        info.append("device: " + (camera.getCameraType() == MYNTCamera.CAMERA_TYPE_D1000 ? "D-1000" : "Stark"));
        info.append("\nusb3.0: " + camera.getIsUSB3());
        info.append("\ndepthType: " + depthType);
        info.append("\nsize: " + camera.getPreviewWidth()+ "x" + camera.getPreviewHeight());
        info.append("\nIR: " + camera.isIRSupported());
        info.append("\nIMU: " + camera.isIMUSupported());
//        info.append("\nAE: " + camera.getAEStatusEnabled());
//        info.append("\nAWB: " + camera.getAWBStatusEnabled());
        if (!isOK) {
            info.append("\n ⚠️⚠️⚠️⚠️⚠️ config is not support ⚠️⚠️⚠️⚠️⚠️");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mInfoTextView.setText(info.toString());
            }
        });
    }

    private void updateDistanceLabel() {
        try {
            final int spotX = mSpotPoint.x;
            final int spotY = mSpotPoint.y;
            float rate = (float)mPreviewSize.getHeight() / (float)mDisplaySize.getHeight();
            final int realX = (int)((float)spotX * rate);
            final int realY = (int)((float)spotY * rate);

            final int index = realY * mPreviewSize.getWidth() + realX;

            FrameData depthData = camera.getDepthFrameData();

            final int distance = depthData == null ? -1 : depthData.getDistanceValue(realX, realY);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (camera != null)
                        mMeasureTextView.setText("distance: " + distance + "mm\n" +
                                "x: " + realX +", y: " + realY +", index: "+ index+"\n" +
                                "color fps: [uvc: " + String.format(" % 2.2f", camera.getUVCFPS(MYNTCamera.Frame.COLOR)) + ", " +
                                "preview: " + String.format(" % 2.2f", camera.getPreviewFPS(MYNTCamera.Frame.COLOR)) + "]\n" +
                                "depth fps: [uvc: " + String.format(" % 2.2f", camera.getUVCFPS(MYNTCamera.Frame.DEPTH)) + ", " +
                                "preview: " + String.format(" % 2.2f", camera.getPreviewFPS(MYNTCamera.Frame.DEPTH)) + "]");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showIRDialog() {
        if (camera == null) {
            showToast("Camera 未初始化！！");
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("IR Value");

        LinearLayout linear = new LinearLayout(this);

        linear.setOrientation(LinearLayout.VERTICAL) ;

        final TextView text = new TextView(this);
        StringBuilder irStr = new StringBuilder();
        irStr.append(getString(R.string.camera_ir_value));
        irStr.append(camera.getIRCurrentValue());
        text.setText(irStr);
        text.setPadding(10, 10, 10, 10);

        final SeekBar seek = new SeekBar(this);
        seek.setMax(camera.getIRMaxValue());
        seek.setProgress(camera.getIRCurrentValue());

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                StringBuilder irStr = new StringBuilder();
                irStr.append(getString(R.string.camera_ir_value));
                irStr.append(progress);
                text.setText(irStr);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        linear.addView(seek);
        linear.addView(text);

        alert.setView(linear);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                camera.setIRCurrentValue(seek.getProgress());
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alert.show();
    }

}
