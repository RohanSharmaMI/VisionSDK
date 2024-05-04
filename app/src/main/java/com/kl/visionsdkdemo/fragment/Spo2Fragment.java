package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.kl.visionsdkdemo.MeasureActivity;
import com.kl.visionsdkdemo.R;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IRawSpo2DataCallback;
import com.mintti.visionsdk.ble.callback.ISpo2ResultListener;
import com.kl.visionsdkdemo.databinding.FragmentBoBinding;
import com.kl.visionsdkdemo.view.PPGDrawWave;
import com.mintti.visionsdk.common.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class Spo2Fragment extends BaseMeasureFragment<FragmentBoBinding>
        implements IBleWriteResponse, ISpo2ResultListener,Handler.Callback , IRawSpo2DataCallback {
    private static final int MSG_SPO = 0;
    private static final int MSG_HR = 1;

    private PPGDrawWave oxWave;
    private Handler mHandler;
    private boolean isMeasureEnd;

    private File boGlowFile;
    private File boInfraredFile;
    private FileOutputStream fosBoGlow;
    private FileOutputStream fosBoInfrared;
    private int mPrevHr = 0;

    private ArrayList<String> spo2ArrayList = new ArrayList<>();
    private boolean isEmpty = true;


    public static Spo2Fragment newInstance() {

        Bundle args = new Bundle();
        Spo2Fragment fragment = new Spo2Fragment();
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    protected FragmentBoBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBoBinding.inflate(inflater,container,false);
    }

    @Override
    protected void initView(View view) {
        BleManager.getInstance().setSpo2ResultListener(this);
        BleManager.getInstance().setRawSpo2DataCallback(this);
        oxWave = new PPGDrawWave();
        getBinding().boWaveView.setDrawWave(oxWave);
        mHandler = new Handler(Looper.getMainLooper(),this);
        getBinding().btMeasureBo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btMeasureBo.getText().equals(getString(R.string.start_measure))){
                    reset();
                    createSpo2File();
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_SPO2, Spo2Fragment.this);
                    getBinding().btMeasureBo.setText(getString(R.string.end_measure));
                }else {
                    //停止测量
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_SPO2, Spo2Fragment.this);
                    getBinding().btMeasureBo.setText(getString(R.string.start_measure));
                    closeFile();
//                    waveView.pause();
                    isMeasureEnd = true;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            oxWave.clear();
                        }
                    },500);


                }
            }
        });
    }



    @Override
    public void onWriteSuccess() {
        Log.e("BOFragment", "onWriteSuccess: BO" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BOFragment", "onWriteFailed: BO" );
    }


    @Override
    public void onResume() {
        super.onResume();
        oxWave.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        oxWave.clear();
        mHandler.removeCallbacksAndMessages(null);
        if (getBinding().btMeasureBo.getText().equals(getString(R.string.end_measure))) {
            getBinding().btMeasureBo.performClick();

        }
    }


    private void reset(){
        getBinding().tvSpo2.setText("-- %");
        getBinding().tvHr.setText("000 bpm");
        getBinding().boWaveView.reply();
        isMeasureEnd = false;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setSpo2ResultListener(null);
        BleManager.getInstance().setRawSpo2DataCallback(null);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case MSG_SPO:
                Bundle bundle = msg.getData();
                double spo = bundle.getDouble("spo");

                if (spo>0 && spo < 100){
                    getBinding().tvSpo2.setText(spo+" %");
                }
                int heartRate = bundle.getInt("hr");
//                    if(Math.abs(mPrevHr - heartRate) < 5)
//                    {
//                        if (heartRate>30 && heartRate<200){
//                            getBinding().tvHr.setText(heartRate+" bpm");
//                        }
//                    }
                if (heartRate>30 && heartRate<200){
                    getBinding().tvHr.setText(heartRate+" bpm");
                }
                mPrevHr = heartRate;
                if(isEmpty)
                {
                    spo2ArrayList.add("spo2");
                    spo2ArrayList.add(String.valueOf(spo));
                    spo2ArrayList.add(String.valueOf(heartRate));
                    isEmpty = false;
                }
                else {
                    spo2ArrayList.set(0,"spo2");
                    spo2ArrayList.set(1,String.valueOf(spo));
                    spo2ArrayList.set(2,String.valueOf(heartRate));
                }

                ((MeasureActivity) requireActivity()).allDataArrayList[3]=spo2ArrayList;

                return true;
            case MSG_HR:
                return true;
            default:return false;
        }
    }

    @Override
    public void onWaveData(int waveData) {
        if (!isMeasureEnd) {
            oxWave.addData(waveData);
        }
    }

    @Override
    public void onSpo2ResultData(int heartRate, double spo2) {
        Message message = mHandler.obtainMessage();
        message.what = MSG_SPO;
        Bundle bundle = new Bundle();
        bundle.putDouble("spo",spo2);
        bundle.putInt("hr",heartRate);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    public void onSpo2End() {
        LogUtils.e("Spo2Fragment", "onSpo2End");
        //停止测量
//        BleManager.getInstance().stopMeasure(MeasureType.TYPE_SPO2, Spo2Fragment.this);
        closeFile();
//                    waveView.pause();
        isMeasureEnd = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(getBinding() != null){
                    getBinding().btMeasureBo.setText(getString(R.string.start_measure));
                    oxWave.clear();
                }


            }
        },500);
    }

    @Override
    public void onSpo2RawData(byte[] redDatum, byte[] irDatum) {
        try {
            if (fosBoGlow != null){
                fosBoGlow.write(redDatum);
            }
            if (fosBoInfrared != null){
                fosBoInfrared.write(irDatum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSpo2File(){
        try {
            boGlowFile = createFile("red.bin");
            fosBoGlow = new FileOutputStream(boGlowFile);
            boInfraredFile = createFile("ir.bin");
            fosBoInfrared = new FileOutputStream(boInfraredFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void closeFile(){
        try {
            if (fosBoGlow !=null){
                fosBoGlow.close();
                fosBoGlow = null;
            }
            if (fosBoInfrared != null){
                fosBoInfrared.close();
                fosBoInfrared = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
