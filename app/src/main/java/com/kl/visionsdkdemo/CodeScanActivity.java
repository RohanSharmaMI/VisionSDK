package com.kl.visionsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.kl.visionsdkdemo.fragment.DeviceInfoFragment;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zbar.ZBarView;

/**
 * Created by gaoyingjie on 2020/6/10
 * Description:
 */
public class CodeScanActivity extends AppCompatActivity implements QRCodeView.Delegate{

    private static final int CAMERA_REQUEST_CODE = 0;
    private ZBarView zBarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_scan);
//        initPermission();
        zBarView = findViewById(R.id.zbarview);
        zBarView.setDelegate(this);
        zBarView.changeToScanBarcodeStyle(); // 切换成扫描条码样式

    }

    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {// 有相机权限
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA },
                    CAMERA_REQUEST_CODE);// 无权限，申请相机权限，然后利用回调函数判断是否申请成功
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        initPermission();
//        startCamera();
    }

    private void startCamera() {
        zBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
//        mZBarView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT); // 打开前置摄像头开始预览，但是并未开始识别
        zBarView.startSpotAndShowRect(); // 显示扫描框，并开始识别
    }

    @Override
    protected void onStop() {
        super.onStop();
        zBarView.stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        zBarView.onDestroy();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
       Intent intent = new Intent();
       intent.putExtra(DeviceInfoFragment.KEY_CODE_128,result);
       CodeScanActivity.this.setResult(RESULT_OK,intent);
       finish();

    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 0:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    startCamera();
                }else {
                    Toast.makeText(this, "无权限无法扫码", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }
}

