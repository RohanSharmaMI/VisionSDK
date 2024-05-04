package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.kl.visionsdkdemo.R;

import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.linktop.constant.TestPaper;
import com.mintti.visionsdk.ble.DeviceType;
import com.mintti.visionsdk.ble.bean.BgEvent;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBgResultListener;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.mintti.visionsdk.ble.callback.IRawBgDataCallback;
import com.kl.visionsdkdemo.databinding.FragmentBgBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by leopold on 2021/3/25
 * Description:
 */
public class BGFragment extends BaseVBindingFragment<FragmentBgBinding> implements
        IBgResultListener, Handler.Callback, IBleWriteResponse, IRawBgDataCallback {

    private static final int MSG_ADJUST_FAILED = 2;
    private static final int MSG_WAIT_INSERT = 3;
    private static final int MSG_WAIT_DRIP = 4;
    private static final int MSG_DRIP_BLOOD = 5;
    private static final int MSG_BG_MEASURE_OVER = 6;
    private static final int MSG_PAPER_USED = 7;
    private static final int MSG_PAPER_OUT = 8;
    private static final int MSG_ADJUST_TIMEOUT = 9;
    private static final String BUNDLE_BG_RESULT = "bg_result";
    private static final String BUNDLE_GLUCOSE_SUM = "glucose_sum";
    private static final String BUNDLE_BG_COUNT = "bg_count";
    private final Handler handler = new Handler(Looper.getMainLooper(),this);
    protected String[] mTestPaperCodes;
    protected String mManufacturer;

    private int adjustCount = 0;

    public static BGFragment newInstance() {
        Bundle args = new Bundle();
        BGFragment fragment = new BGFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected FragmentBgBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBgBinding.inflate(inflater,container,false);
    }


    protected void initView(View rootView) {
        BleManager.getInstance().setBgResultListener(this);
        BleManager.getInstance().setRawBgDataCallback(this);
        if (BleManager.getInstance().getDeviceType() == DeviceType.TYPE_VISION) {
            initTestPaper();
        }else {
            getBinding().llSelectPaperCodeContainer.setVisibility(View.GONE);
        }

        getBinding().btMeasureBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btMeasureBg.getText().equals(getString(R.string.start_measure))){
                    resetValue();
                    //先校准   校准成功再写入指令开始血糖采集
                    adjustCount = 0;
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BG, BGFragment.this);
                    getBinding().btMeasureBg.setEnabled(false);
                    handler.sendEmptyMessageDelayed(MSG_ADJUST_TIMEOUT,1000*10);

                }else {
                    getBinding().btMeasureBg.setText(getString(R.string.start_measure));
                    resetValue();
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BG, BGFragment.this);
                }
            }
        });

    }

    private void initTestPaper() {
        String[] manufacturers = BleManager.getInstance().getTestPaperManufacturer();
        ArrayAdapter<String> adapterManufacturer = new ArrayAdapter<>(getContext()
                , android.R.layout.simple_spinner_dropdown_item
                , getManufacturerList(manufacturers));
        getBinding().spinTestPaperManufacturer.setAdapter(adapterManufacturer);
        getBinding().spinTestPaperManufacturer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mManufacturer = manufacturers[position];
                mTestPaperCodes = BleManager.getInstance().getTestPaperCodesByManufacturer(mManufacturer);
                ArrayAdapter<String> adapterTestPaper = new ArrayAdapter<>(getContext()
                        , android.R.layout.simple_spinner_dropdown_item
                        , Arrays.asList(mTestPaperCodes));
                getBinding().spinTestPaperCode.setAdapter(adapterTestPaper);
                if (TestPaper.Manufacturer.YI_CHENG.equals(mManufacturer)) {
                    //Default value select TestPaperCode.C20.
                    getBinding().spinTestPaperCode.setSelection(TestPaper.Code.indexOf(mTestPaperCodes
                            , TestPaper.Code.C20));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getBinding().spinTestPaperCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BleManager.getInstance().setTestPaper(mManufacturer,mTestPaperCodes[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        getBinding().spinTestPaperManufacturer.setSelection(TestPaper.Manufacturer.indexOf(TestPaper.Manufacturer.YI_CHENG));
    }

    private List<String> getManufacturerList(String[] array) {
        List<String> list = new ArrayList<>();
        for (String name : array) {
            switch (name) {
                case TestPaper.Manufacturer.HMD:
                    list.add(getString(R.string.manufacturer_hmd));
                    break;
                case TestPaper.Manufacturer.BENE_CHECK:
                    list.add(getString(R.string.manufacturer_bene_check));
                    break;
                case TestPaper.Manufacturer.YI_CHENG:
                    list.add(getString(R.string.manufacturer_yi_cheng));
                    break;
            }
        }
        return list;
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BGFragment", "onWriteSuccess: " );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BGFragment", "onWriteFailed: " );
    }





    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (getBinding() == null) {
            return false;
        }
        getBinding().btMeasureBg.setEnabled(true);
        getBinding().btMeasureBg.setText(getString(R.string.end_measure));
        switch (msg.what){
            case MSG_ADJUST_FAILED:
                getBinding().tvBgResult.setText(R.string.calibration_failed);
                getBinding().btMeasureBg.setText(getString(R.string.start_measure));
                showToast(getString(R.string.calibration_failed));
                return true;
            case MSG_WAIT_INSERT:
                getBinding().tvBgStatus.setText(R.string.waot_insert_strip);
                getBinding().btMeasureBg.setText(getString(R.string.start_measure));
                return true;
            case MSG_WAIT_DRIP:
                getBinding().tvBgStatus.setText(R.string.waiting_blood);
                return true;
            case MSG_DRIP_BLOOD:
                getBinding().tvBgStatus.setText(R.string.wait_calculation);
                return true;
            case MSG_BG_MEASURE_OVER:
                Bundle bundle = msg.getData();
                getBinding().tvBgStatus.setText(R.string.blood_sugar_test_over);
                getBinding().tvBgResult.setText(getString(R.string.blood_sugar_result)+(bundle.getDouble(BUNDLE_BG_RESULT)) +"mmol/L");
                getBinding().btMeasureBg.setText(getString(R.string.start_measure));

                return true;
            case MSG_PAPER_USED:
                getBinding().tvBgStatus.setText(R.string.test_strip_used);
                return true;
            case MSG_PAPER_OUT:
                getBinding().tvBgStatus.setText(R.string.test_strip_is_pulled);
                getBinding().btMeasureBg.setText(getString(R.string.start_measure));
                return true;
            case MSG_ADJUST_TIMEOUT:
                TipDialog.show("校准超时", WaitDialog.TYPE.WARNING);
                getBinding().btMeasureBg.setText(getString(R.string.start_measure));
                return true;
        }

        return false;
    }

    private void resetValue(){
        getBinding().tvBgResult.setText(getString(R.string.blood_sugar_result_));
        getBinding().tvGlucoseSum.setText("Glucose sum: --");
        getBinding().tvBgCount.setText("Bg count: --");
        getBinding().tvBgStatus.setText(getString(R.string.waiting_for_calibration));
    }


    @Override
    public void onBgEvent(BgEvent bgEvent) {
        handler.removeMessages(MSG_ADJUST_TIMEOUT);
        switch (bgEvent){
            case BG_EVENT_CALIBRATION_FAILED:
                handler.sendEmptyMessage(MSG_ADJUST_FAILED);
                break;
            case BG_EVENT_WAIT_PAGER_INSERT:
                //等待插入试纸
                handler.sendEmptyMessage(MSG_WAIT_INSERT);
                break;
            case BG_EVENT_WAIT_DRIP_BLOOD:
                //等待滴入血液
                handler.sendEmptyMessage(MSG_WAIT_DRIP);
                break;
            case BG_EVENT_BLOOD_SAMPLE_DETECTING:
                //已采集到血液
                handler.sendEmptyMessage(MSG_DRIP_BLOOD);
                break;
            case BG_EVENT_MEASURE_END:

                break;
            case BG_EVENT_PAPER_USED:
                handler.sendEmptyMessage(MSG_PAPER_USED);
                break;
//            case BG_EVENT_PAPER_PULL_OUT:
//                handler.sendEmptyMessage(MSG_PAPER_OUT);
//                break;

        }
    }

    @Override
    public void onBgResult(double bg) {
        //已获取血糖结果
        Message message = handler.obtainMessage(MSG_BG_MEASURE_OVER);
        Bundle bundle = new Bundle();
        bundle.putDouble(BUNDLE_BG_RESULT,bg);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    @Override
    public void onBgRawData(int glucoseSum, int bgCount, double result) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getBinding() != null) {
                    getBinding().tvGlucoseSum.setText("Glucose sum: "+glucoseSum);
                    getBinding().tvBgCount.setText("Bg count: "+bgCount);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getBinding().btMeasureBg.getText().equals(getString(R.string.end_measure))) {
            getBinding().btMeasureBg.performClick();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setBgResultListener(null);
        BleManager.getInstance().setRawBgDataCallback(null);
        handler.removeCallbacksAndMessages(null);
    }
}
