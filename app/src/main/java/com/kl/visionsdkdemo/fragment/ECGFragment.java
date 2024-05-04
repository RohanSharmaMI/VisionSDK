package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kl.vision_ecg.ISmctAlgoCallback;
import com.kl.vision_ecg.SmctConstant;
import com.kl.visionsdkdemo.MeasureActivity;
import com.kl.visionsdkdemo.R;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IEcgResultListener;
import com.kl.visionsdkdemo.base.utils.TimeUtils;
import com.kl.visionsdkdemo.databinding.FragmentEcgBinding;
import com.mintti.visionsdk.common.ArrayUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ECGFragment extends BaseMeasureFragment<FragmentEcgBinding> implements IBleWriteResponse, IEcgResultListener, ISmctAlgoCallback {

    private File ecgFile;
    private FileOutputStream fosEcg = null;
    private BufferedOutputStream dataOutputStream = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String avgHrStr = "";
    private String rrMaxStr = "";
    private String rrMinStr = "";
    private String brStr = "";
    private String hrvStr = "";
    private ArrayList<String> ecgArrayList = new ArrayList<>();
    private boolean isEmpty = true;
    public static ECGFragment newInstance() {
        Bundle args = new Bundle();
        ECGFragment fragment = new ECGFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentEcgBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentEcgBinding.inflate(inflater,container,false);
    }



    @Override
    protected void initView(View rootView) {
        getBinding().gainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0){
                    getBinding().ecgView.setGain(1);
                }else if (position == 1){
                    getBinding().ecgView.setGain(2);
                }else {
                    getBinding().ecgView.setGain(5);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        float gain = getBinding().ecgView.getGain();
        if (gain == 1f){
            getBinding().gainSpinner.setSelection(0);
        }else if (gain ==2f){
            getBinding().gainSpinner.setSelection(1);
        }else {
            getBinding().gainSpinner.setSelection(2);
        }
        if (BleManager.getInstance().isConnected()) {
            getBinding().ecgView.setSampleRate(BleManager.getInstance().getSampleRate());
        }

//        if (EcgType.IS_LOW_COST){
//            getBinding().ecgView.setSampleRate(512);
//        }else {
//            getBinding().ecgView.setSampleRate(200);
//        }
        getBinding().btMeasureEcg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btMeasureEcg.getText().equals(getString(R.string.start_measure))){

                    //开始测量
                    startMeasure();

                }else {
                    //停止测量
                    stopMeasure();

                }
            }
        });
    }

    private void stopMeasure() {
        BleManager.getInstance().stopMeasure(MeasureType.TYPE_ECG, ECGFragment.this);

        ((MeasureActivity) requireActivity()).allDataArrayList[4]=ecgArrayList;
        getBinding().tvEcgDuration.setText("00:00");
        getBinding().btMeasureEcg.setText(getString(R.string.start_measure));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getBinding().ecgView.clearDatas();
                if(isEmpty) {
                    ecgArrayList.add("ecg");
                    ecgArrayList.add(avgHrStr);
                    ecgArrayList.add(brStr);
                    ecgArrayList.add(rrMaxStr);
                    ecgArrayList.add(rrMinStr);
                    ecgArrayList.add(hrvStr);
                }
                else{
                    ecgArrayList.set(0,"ecg");
                    ecgArrayList.set(1,avgHrStr);
                    ecgArrayList.set(2,brStr);
                    ecgArrayList.set(3,rrMaxStr);
                    ecgArrayList.set(4,rrMinStr);
                    ecgArrayList.set(5,hrvStr);
                }
            }
        },500);

    }

    private void startMeasure() {
        BleManager.getInstance().setEcgResultListener(null);
        BleManager.getInstance().setEcgResultListener(this);
        ecgFile = createFile("ecg_handle.ecg");
        fosEcg = createFos(ecgFile);
        dataOutputStream = new BufferedOutputStream(fosEcg);
        resetResult();
        BleManager.getInstance().startMeasure(MeasureType.TYPE_ECG, ECGFragment.this);
        getBinding().btMeasureEcg.setText(getString(R.string.end_measure));
    }


    @Override
    public void onWriteSuccess() {
        Log.e("ECGFragment", "onWriteSuccess: ECG" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("ECGFragment", "onWriteFailed: ECG" );
    }


    @Override
    public void algoData(int key, int value) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (key) {
                    case SmctConstant.KEY_ALGO_HEART_RATE:// 心率
//                        Log.e("ECGFragment", " 心率 = "+value );
                        getBinding().tvAvgHrValue.setText(value+" bpm");
                        avgHrStr = String.valueOf(value);
                        break;
                    case SmctConstant.KEY_ALGO_ARRHYTHMIA:// 心律失常
                        Log.e("ECGFragment", " 心律失常 = "+value );
                        break;
                    case SmctConstant.KEY_ALGO_RESP_RATE:
//                        Log.e("ECGFragment", " 呼吸率 = "+value );
                        getBinding().tvRespValue.setText(value + "bpm");
                        brStr = String.valueOf(value);
                        break;

                }
            }
        });
    }

    @Override
    public void algoData(int i, int i1, int i2) {

    }
    private void resetResult(){
        getBinding().tvRrMaxValue.setText(  "-- ms");
        getBinding().tvRrMinValue.setText( "-- ms");
        getBinding().tvHrvValue.setText("-- ms");
        getBinding().tvAvgHrValue.setText("-- ms");
        getBinding().tvRespValue.setText("-- ms");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setEcgResultListener(null);

    }



    @Override
    public void onDrawWave(int wave) {
        if (getBinding() != null) {
            getBinding().ecgView.addPoint(wave);
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.write(ArrayUtils.int2ByteArray(wave));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onHeartRate(int heartRate) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getBinding() ==null){
                    return;
                }
                getBinding().tvAvgHrValue.setText(heartRate+"BPM");
            }
        });
    }

    @Override
    public void onRespiratoryRate(int respiratoryRate) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getBinding() ==null){
                    return;
                }
                getBinding().tvRespValue.setText(respiratoryRate+"BPM");
            }
        });
    }

    @Override
    public void onEcgResult(int rrMax, int rrMin, int hrv) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getBinding() ==null){
                    return;
                }
                rrMaxStr = String.valueOf(rrMax);
                rrMinStr = String.valueOf(rrMin);
                hrvStr = String.valueOf(hrv);
                getBinding().tvRrMaxValue.setText(rrMax+"ms");
                getBinding().tvRrMinValue.setText(rrMin+"ms");
                getBinding().tvHrvValue.setText(hrv+"ms");
            }
        });
    }

    @Override
    public void onEcgDuration(int duration,boolean isEnd) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getBinding() ==null){
                    return;
                }
                getBinding().tvEcgDuration.setText(TimeUtils.formatDuration(duration));
            }
        });
    }

    @Override
    protected void closeFile() {
        try {
            if (fosEcg != null){
                fosEcg.close();
                fosEcg = null;
            }
            if (dataOutputStream != null){
                dataOutputStream.close();
                dataOutputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getBinding().btMeasureEcg.getText().equals(getString(R.string.end_measure))) {
            getBinding().btMeasureEcg.performClick();
        }
        getBinding().ecgView.clearDatas();
        handler.removeCallbacksAndMessages(null);
    }

}
