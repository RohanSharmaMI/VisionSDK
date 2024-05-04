package com.kl.visionsdkdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.DialogLifecycleCallback;
import com.mintti.visionsdk.ble.BleDevice;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleConnectionListener;
import com.mintti.visionsdk.ble.callback.IBleScanCallback;
import com.kl.visionsdkdemo.adapter.BleDeviceAdapter;
import com.kl.visionsdkdemo.base.BaseVBindingActivity;
import com.kl.visionsdkdemo.databinding.ActivityMainBinding;
import com.kl.visionsdkdemo.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseVBindingActivity<ActivityMainBinding> implements IBleConnectionListener, Handler.Callback {
    private static final int MSG_SCAN_DELAY = 0x01;
    private static final int REQUEST_ENABLE_BT = 1;
    private final List<BleDevice> deviceList = new ArrayList<>();
    private BleDeviceAdapter deviceAdapter;
    private Handler mHandler;
    private boolean isScanning = false;

    public String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CAMERA};
    public List<String> permissionList = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.scan);
        }
        checkPermission();
        mHandler = new Handler(getMainLooper(),this);
        initView();
    }

    @Override
    public ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

//    private void requestPermission(){
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
//        }
//    }


    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT};
        }
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission);
                }
            }

            if (!permissionList.isEmpty()) {
                String[] requestPermission = permissionList.toArray(new String[permissionList
                        .size()]);
                ActivityCompat.requestPermissions(MainActivity.this, requestPermission, 1);
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                    } else {
                        Toast.makeText(MainActivity.this, permissions[i], Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        }
    }

    private void initView() {

        getBinding().recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getBinding().recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        deviceAdapter = new BleDeviceAdapter(deviceList);
        getBinding().recyclerView.setAdapter(deviceAdapter);
        deviceAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                BleManager.getInstance().stopScan();
                getBinding().swipeRefreshLayout.setRefreshing(false);
                if (deviceList.get(position).getName().contains("Mintti-Vision") || deviceList.get(position).getName().contains("HC")){
                    showProgressDialog(getString(R.string.connecting),false);
                    BleManager.getInstance().connect(deviceList.get(position));
                }else {

                    PopTip.show(getString(R.string.pleace_connect_device));

                }
            }


        });

        getBinding().btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (getBinding().btScan.getText().equals("Scan")){
                    if (!BleManager.getInstance().isBluetoothEnable()){
                        showToast(getString(R.string.please_turn_on_bluetooth));
                        return;

                    }
                    deviceList.clear();
                    deviceAdapter.notifyDataSetChanged();
                    getBinding().swipeRefreshLayout.setRefreshing(true);
                    startScan();
                    getBinding().btScan.setText("Stop scan");
                }else {
                    BleManager.getInstance().stopScan();
                    isScanning = false;
                    getBinding().swipeRefreshLayout.setRefreshing(false);
                    getBinding().btScan.setText("Scan");
                }
            }
        });

        getBinding().swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.e("MainActivity", "onRefresh: ");
                if (!isScanning){
                    startScan();
                    getBinding().btScan.setText("Stop scan");
                }

                //swipeRefreshLayout.setRefreshing(false);
            }
        });

    }
    private void startScan(){
        mHandler.sendEmptyMessageDelayed(MSG_SCAN_DELAY,10*1000);
        isScanning = true;
        BleManager.getInstance().startScan(new IBleScanCallback() {
            @Override
            public void onScanResult(BleDevice bleDevice) {

                if (TextUtils.isEmpty(bleDevice.getName()) || TextUtils.isEmpty(bleDevice.getMac())){
                    return;
                }
                boolean hasDevice = false;
                for (BleDevice device : deviceList) {
                    if (device.getMac().equals(bleDevice.getMac())){
                        hasDevice = true;
                        device.setRssi(bleDevice.getRssi());
                    }
                }
                if (!hasDevice){
                    deviceList.add(bleDevice);
                }
                deviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFailed(int errorCode) {
                isScanning = false;
                mHandler.removeMessages(MSG_SCAN_DELAY);
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        BleManager.getInstance().addConnectionListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BleManager.getInstance().removeConnectionListener(this);
    }

    @Override
    public void onConnectSuccess(String mac) {
        TipDialog.show(getString(R.string.connection_succeeded), TipDialog.TYPE.SUCCESS)
                .setDialogLifecycleCallback(new DialogLifecycleCallback<WaitDialog>() {
                    @Override
                    public void onDismiss(WaitDialog dialog) {
                        super.onDismiss(dialog);
                        startActivity(new Intent(MainActivity.this,MeasureActivity.class));
                    }
                });
    }

    @Override
    public void onConnectFailed(String mac) {
//        disProgressDialog();
//        showToast("连接失败");
        TipDialog.show(getString(R.string.connection_failed), TipDialog.TYPE.ERROR);
    }

    @Override
    public void onDisconnected(String mac, boolean isActiveDisconnect) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_measure,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_share){
            File dataFileDir = getExternalFilesDir("data");
            File[] files = dataFileDir.listFiles();
            showShareFileDialog(files);
            return true;
        }else if (item.getItemId() == R.id.menu_item_delete){
            File dataFileDir = getExternalFilesDir("data");
            File[] files = dataFileDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            showToast(getString(R.string.cleared_successfully));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_SCAN_DELAY){
            getBinding().swipeRefreshLayout.setRefreshing(false);
            BleManager.getInstance().stopScan();
            isScanning = false;
            getBinding().btScan.setText("Scan");
            return true;
        }
        return false;
    }
}