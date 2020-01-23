package com.myntai.d.sdk.sample.module.measure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.myntai.d.sdk.MYNTCamera;
import com.myntai.d.sdk.bean.FrameData;
import com.myntai.d.sdk.bean.RectifyLogData;
import com.myntai.d.sdk.sample.R;
import com.myntai.d.sdk.sample.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.myntai.d.sdk.MYNTCamera.CAMERA_TYPE_D1200;

public class CameraTools {

    private static final String TAG = "CameraTools";

    private MYNTCamera mCamera;
    private CameraActivity mActivity;

    private String mPlyPath;
    private int testX = 400, testY = 300;

    public CameraTools(CameraActivity activity, MYNTCamera camera) {
        this.mCamera = camera;
        this.mActivity = activity;
    }

    /**
     * 保存数据
     *
     * */
    public void saveDepthData(final int width, final int height) {
        try {
            mActivity.showProcessDialog(R.string.camera_progress_save_depth);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String path = Environment.getExternalStorageDirectory().getPath() + "/mynteye/";

                    FrameData frames[] = mCamera.getSyncFrameData();
                    if (frames == null) {
                        mActivity.hideshowProcessDialog();
                        mActivity.showDialog("Error", "syncFrame 为 null");
                        return;
                    }

                    FrameData colorFrame = frames[0];
                    FrameData depthFrame = frames[1];

                    Bitmap colorBitmap;
                    if (mCamera.getCameraType() == CAMERA_TYPE_D1200)
                        colorBitmap = colorFrame.convert2Bitmap(colorFrame.bytes, width, height);
                    else
                        colorBitmap = colorFrame.convert2Bitmap(colorFrame.rgbaBytes(), width, height);

                    if (colorBitmap != null) {
                        Log.e(TAG, "storage path:" + path);
                        FileUtils.writeBitmapToFile(colorBitmap, path, "color.jpg");
                        colorBitmap.recycle();
                    } else {
                        Log.e(TAG, "colorBitmap is null");
                    }

                    Log.e(TAG, "colorFrame.time = " + colorFrame.timeMillis);
                    Log.e(TAG, "depthFrame.time = " + depthFrame.timeMillis);

                    final byte distances[] = new byte[depthFrame.bytes.length * 4];
                    Log.e(TAG, "depthFrame.bytes.length = " + depthFrame.bytes.length);
                    for (int i = 0; i < colorFrame.width * colorFrame.height; i++) {
                        byte distance[] = intToByteArray(depthFrame.getDistanceValue(i));
                        distances[i * 4] = distance[0];
                        distances[i * 4 + 1] = distance[1];
                        distances[i * 4 + 2] = distance[2];
                        distances[i * 4 + 3] = distance[3];
                    }
                    final int index = testY * 640 + testX;
                    mActivity.showToast("save -> (" + testX + ", " + testY + ") distance: " + depthFrame.getDistanceValue(index));
                    FileUtils.writeBytesToFile(distances, path, "depth.data");

                    RectifyLogData rectifyLogData = mCamera.getRectifyLogData();
                    if (rectifyLogData != null) {
                        FileUtils.writeTxtToFile(rectifyLogData.toString(), path, "rectifyLogData.txt");
                    }

                    mActivity.hideshowProcessDialog();
                    mActivity.showToast("Save Success!!");
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取数据
     *
     * */
    public void readDepthData() {
        final String path = Environment.getExternalStorageDirectory().getPath() + "/mynteye/depth.data";

        byte bytes[] = FileUtils.getBytes(path);

        if (bytes == null) {
            mActivity.showToast("Please save DepthData");
            return;
        }

        int index = testY * 640 + testX;
        byte distanceBytes[] = new byte[]{
                bytes[index * 4],
                bytes[index * 4 + 1],
                bytes[index * 4 + 2],
                bytes[index * 4 + 3],
        };
        mActivity.showToast("read -> (" + testX + ", " + testY + ") distance: " + byteArrayToInt(distanceBytes));
    }

    static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        //由高位到低位
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }

    static int byteArrayToInt(byte[] bytes) {
        int value= 0;
        //由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift= (4 - 1 - i) * 8;
            value +=(bytes[i] & 0x000000FF) << shift;//往高位游
        }
        return value;
    }

    private void savePointCloud(final boolean hasColor) {

        try {
            mActivity.showProcessDialog(R.string.camera_progress_save_ply);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //保存点云
                    final String path = Environment.getExternalStorageDirectory().getPath() + "/mynteye/" + "pointCloud/";
                    FrameData frames[] = mCamera.getSyncFrameData();
                    if (frames == null) {
                        mActivity.hideshowProcessDialog();
                        mActivity.showDialog("Error", "syncFrame 为 null");
                        return;
                    }
                    FrameData colorFrame = frames[0];
                    FrameData depthFrame = frames[1];

                    File dir = new File(path);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String filename = getDateTimeString() + ".ply";
                    mPlyPath = path + filename;
                    // 默认保存1m 范围内的点云
                    mCamera.savePointCloud(colorFrame, depthFrame, mPlyPath, hasColor, 1000);

                    mActivity.hideshowProcessDialog();
                    mActivity.showToast(R.string.camera_tool_save_ply_success);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存点云
     *
     * */
    public void savePLY() {
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.camera_tool_contain_color)
                .setPositiveButton(R.string.camera_tool_cancel, null)
                .setNeutralButton(R.string.camera_tool_contain_color_false, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        savePointCloud(false);
                    }
                })
                .setNegativeButton(R.string.camera_tool_contain_color_true, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        savePointCloud(true);
                    }
                })
                .show();
    }

    private static String getDateTimeString() {
        SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

}
