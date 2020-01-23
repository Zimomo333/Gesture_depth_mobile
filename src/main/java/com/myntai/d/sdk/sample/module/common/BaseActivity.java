package com.myntai.d.sdk.sample.module.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.myntai.d.sdk.sample.R;

public class BaseActivity extends AppCompatActivity {

    protected static final String KEY_TITLE = "title";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getStringExtra(KEY_TITLE);
        setTitle(title != null ? title : "MYNTEYE-D SDK Demo");
    }

    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showToast(final int messageID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, getString(messageID), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showDialog(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(BaseActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });
    }

    public void hideshowProcessDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.hide();
                    mProgressDialog = null;
                }
            }
        });
    }

    public void showProcessDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null)
                    mProgressDialog =new ProgressDialog(BaseActivity.this);
                mProgressDialog.setMessage(message);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }
        });
    }

    public void showProcessDialog(final int stringId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null)
                    mProgressDialog =new ProgressDialog(BaseActivity.this);

                mProgressDialog.setMessage(getString(stringId));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }
        });
    }
}
