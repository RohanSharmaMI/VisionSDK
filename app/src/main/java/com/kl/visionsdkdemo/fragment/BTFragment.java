package com.kl.visionsdkdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kl.visionsdkdemo.BTCheckActivity;
import com.kl.visionsdkdemo.MeasureActivity;
import com.kl.visionsdkdemo.R;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.mintti.visionsdk.ble.callback.IBtResultListener;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;
import com.kl.visionsdkdemo.databinding.FragmentBtBinding;

import java.util.ArrayList;

/**
 * Created by leopold on 2021/3/9
 * Description:
 */
public class BTFragment extends BaseVBindingFragment<FragmentBtBinding>
        implements IBtResultListener, IBleWriteResponse, IRawBtDataCallback {
    private boolean isCheckModel=false;
    private final ArrayList<String> btArrayList = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    public static BTFragment newInstance() {
        Bundle args = new Bundle();
        BTFragment fragment = new BTFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected FragmentBtBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBtBinding.inflate(inflater,container,false);
    }

    @Override
    public void onResume() {
        super.onResume();
        BleManager.getInstance().setRawBtDataCallback(this);
        BleManager.getInstance().setBtResultListener(this);
        Log.d("BTFragment","onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setRawBtDataCallback(null);
        BleManager.getInstance().setBtResultListener(null);
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        getBinding().btMeasureBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCheckModel = false;
                if (getBinding().btMeasureBt.getText().equals(getString(R.string.start_measure))){
                    //开始测量
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BT, BTFragment.this);
                    getBinding().btMeasureBt.setText(getString(R.string.end_measure));

                }else {
                    //停止测量
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
                    getBinding().btMeasureBt.setText(getString(R.string.start_measure));

                }
            }
        });
        getBinding().btBtCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isCheckModel = true;
//                btGuideFragment.show(getChildFragmentManager(),"btGuide");
                startActivity(new Intent(requireActivity(), BTCheckActivity.class));
            }
        });
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BTFragment", "onWriteSuccess: BT" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BTFragment", "onWriteFailed: BT" );
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getBinding() != null) {
                    getBinding().tvBt.setText(0+" ℃");
                    getBinding().btMeasureBt.setText(getString(R.string.start_measure));
                }
            }
        });
    }

    @Override
    public void onBtResult(double temperature) {
        Log.e("BTFragment", "onBtResult: "+temperature );
//        requireActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                getBinding().tvBt.setText(temperature+" ℃");
//                getBinding().btMeasureBt.setText(getString(R.string.start_measure));
//            }
//        });
    }

    @Override
    public void onBtRawData(int temperature, int black, int environment,int voltage) {
        Log.e("BTFragment", "onBtRawData: "+temperature );
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getBinding() != null) {
                    getBinding().tvBt.setText(temperature/100.0+" ℃");
                    getBinding().tvBlack.setText(black/100.0+" ℃");
                    getBinding().tvEnvironment.setText(environment/100.0+" ℃");
                    getBinding().tvVoltage.setText(voltage*1.0f/10000+" mV");
                    btArrayList.add("bt");
                    btArrayList.add(String.valueOf(temperature));
                    btArrayList.add(String.valueOf(black));
                    btArrayList.add(String.valueOf(environment));
                    btArrayList.add(String.valueOf(voltage));
                    ((MeasureActivity) requireActivity()).allDataArrayList[2]=btArrayList;
                    Log.d("BTFragment", "run: "+voltage);
                    getBinding().btMeasureBt.setText(getString(R.string.start_measure));
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getBinding().btMeasureBt.getText().equals(getString(R.string.end_measure))) {
            BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
            getBinding().btMeasureBt.setText(getString(R.string.start_measure));
        }
        handler.removeCallbacksAndMessages(null);
    }
}
