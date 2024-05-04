package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;

import com.kl.visionsdkdemo.MeasureActivity;
import com.kl.visionsdkdemo.R;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IRawBpDataCallback;
import com.mintti.visionsdk.ble.callback.IBpResultListener;
import com.kl.visionsdkdemo.databinding.FragmentBpBinding;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by leopold on 2021/3/8
 * Description:
 */
public class BpFragment extends BaseMeasureFragment<FragmentBpBinding> implements IBleWriteResponse,
        IBpResultListener,Handler.Callback, IRawBpDataCallback {
    private static final int MSG_BP_RESULT = 1;
    private static final int MSG_BP_LEAK = 2;
    private static final int MSG_BP_ERROR = 3;
    private static final int MSG_BP_ADD_DATA = 4;
    private static final int MSG_BP_DE_DATA = 5;
    private static final int MSG_DURATION = 6;
    private static final String SYSTOLIC = "systolic";
    private static final String DIASTOLIC = "diastolic";
    private static final String HR = "hr";
    private static final String SAMPLE_TIME = "sample_time";
    private static final String DE_DATA_SIZE = "de_data_size";
    private static final String SAMPLE_RATE = "sample_rate";
    private final Handler handler = new Handler(Looper.getMainLooper(),this);
    private File bpDeTxtFile;
    private BufferedWriter deFileWriter;
    private File bpAddTxtFile;
    private BufferedWriter addFileWriter;
    private BufferedWriter rawFileWriter;
    private int measureDuration = 0;
    private boolean isTest = false;
    private int standardSystolic = 120;
    private int standardDiastolic = 80;
    private int standardHeartRate = 80;
    private final ArrayList<Short> bpDeDataList = new ArrayList<>(); //放压数据
    private final ArrayList<Short> bpAddDataList = new ArrayList<>(); //加压数据
    private MessageDialog messageDialog;

    private ArrayList<String> bpMeasurements = new ArrayList<>();


    public static BpFragment newInstance() {

        Bundle args = new Bundle();

        BpFragment fragment = new BpFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected FragmentBpBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBpBinding.inflate(inflater,container,false);
    }

    @Override
    protected void initView(View rootView) {
        BleManager.getInstance().setBpResultListener(this);
        BleManager.getInstance().setRawBpDataCallback(this);
        getBinding().groupPicker.setVisibility(View.GONE);
        getBinding().btMeasureBp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btMeasureBp.getText().equals(getString(R.string.start_measure))){

                    //开始测量
                    resetValue();
//                    HandleVisionData.isBPTestMode = false;
                    bpDeTxtFile = createFile("bpDeTxt.txt");
                    bpAddTxtFile = createFile("bpAddTxt.txt");
                    deFileWriter = createBufferWriter(bpDeTxtFile);
                    addFileWriter = createBufferWriter(bpAddTxtFile);
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    getBinding().btMeasureBp.setText(getString(R.string.end_measure));
                    handler.sendEmptyMessageDelayed(MSG_DURATION,1000);


                }else {
                    //停止测量
                    closeFile();
                    handler.removeMessages(MSG_DURATION);
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    getBinding().btMeasureBp.setText(getString(R.string.start_measure));
                    saveData();

                }
            }
        });
        getBinding().btTestBp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btTestBp.getText().equals(getString(R.string.test_blood_pressure))){
                    resetValue();
                    getBinding().btTestBp.setText(R.string.stop);
//                    HandleVisionData.isBPTestMode = true;
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    handler.sendEmptyMessageDelayed(MSG_DURATION,1000);
                }else {
                    handler.removeMessages(MSG_DURATION);
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    getBinding().btTestBp.setText(getString(R.string.test_blood_pressure));
                }

            }
        });
        getBinding().switchTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isTest = isChecked;
                if (isTest){
                    getBinding().groupPicker.setVisibility(View.VISIBLE);
                }else {
                    getBinding().groupPicker.setVisibility(View.GONE);
                }
            }
        });

        getBinding().switchTest.setChecked(isTest);
        initPicker();


    }

    private void saveData() {



    }

    private void initPicker() {
        NumberPicker systolicPicker = getBinding().pickerSystolic;
        String[] sysValue = new String[23];
        for (int i = 3; i <=25 ; i++) {
            sysValue[i-3] = i*10+"";
        }
        systolicPicker.setMaxValue(22);
        systolicPicker.setMinValue(0);
        systolicPicker.setValue(9);
        systolicPicker.setDisplayedValues(sysValue);
        systolicPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
//                Log.e("BpFragment", "systolic: "+sysValue[newVal]);
                standardSystolic = Integer.parseInt(sysValue[newVal]);
            }
        });
        String[] diasValue = new String[14];
        for (int i = 3; i <=16 ; i++) {
            diasValue[i-3] = i*10+"";
        }

        NumberPicker diastolicPicker = getBinding().pickerDiastolic;
        diastolicPicker.setMaxValue(13);
        diastolicPicker.setMinValue(0);
        diastolicPicker.setValue(5);
        diastolicPicker.setDisplayedValues(diasValue);
        diastolicPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
//                Log.e("BpFragment", "onValueChange: "+sysValue[newVal]);
                standardDiastolic = Integer.parseInt(diasValue[newVal]);
            }
        });
        String[] hrValue = new String[]{"80","94"};
        NumberPicker hrPicker = getBinding().pickerHr;
        hrPicker.setMaxValue(1);
        hrPicker.setMinValue(0);
        hrPicker.setValue(0);
        hrPicker.setDisplayedValues(hrValue);
        hrPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                Log.e("BpFragment", "onValueChange: "+sysValue[newVal]);
                standardHeartRate = Integer.parseInt(hrValue[newVal]);
            }
        });

    }



    @Override
    public void onWriteSuccess() {
        Log.e("BpFragment", "onWriteSuccess: BP" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BpFragment", "onWriteFailed: BP" );
    }




    private void resetValue(){
        getBinding().tvSystolicValue.setText("-- / mmHg");
        getBinding().tvDiastolicValue.setText("-- / mmHg");
        getBinding().tvHrValue.setText("-- / BPM");
        getBinding().tvBpAdd.setText("0");
        getBinding().tvBpDe.setText("0");
        getBinding().tvSampleTime.setText( "0");
        getBinding().tvDeDataSize.setText("0");
        getBinding().tvSampleRate.setVisibility(View.GONE);
//        getBinding().tvErrorRangeSys.setText("0");
//        getBinding().tvErrorRangeDias.setText("0");
//        getBinding().tvErrorRangeHr.setText("0");
        bpDeDataList.clear();
        bpAddDataList.clear();
        measureDuration = 0;
        getBinding().tvDuration.setText(measureDuration+" s");
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (getBinding() == null){
            return false;
        }
        if (msg.what == MSG_BP_RESULT) {
            Bundle bundle = msg.getData();
            int hr = bundle.getInt(HR);
            int systolic = bundle.getInt(SYSTOLIC);
            int diastolic = bundle.getInt(DIASTOLIC);
            getBinding().tvSystolicValue.setText(systolic + " / mmHg");
            getBinding().tvDiastolicValue.setText(diastolic + " / mmHg");
            getBinding().tvHrValue.setText(hr + " / BPM");
            getBinding().tvDeDataSize.setText(bpDeDataList.size()+"");
//            getBinding().tvSampleRate.setVisibility(View.VISIBLE);
            bpMeasurements.add("bp");
            bpMeasurements.add(String.valueOf(systolic));
            bpMeasurements.add(String.valueOf(diastolic));
            bpMeasurements.add(String.valueOf(hr));
            ((MeasureActivity) requireActivity()).allDataArrayList[0]=bpMeasurements;
            bpDeDataList.clear();
            getBinding().btMeasureBp.setText(getString(R.string.start_measure));
            return true;
        }else if (msg.what == MSG_BP_LEAK){
            showErrorDialog(getString(R.string.leak));
//            MessageDialog.show(getString(R.string.hint),getString(R.string.leak),getString(R.string.ok));
            getBinding().btMeasureBp.setText(getString(R.string.start_measure));
            return true;
        }else if (msg.what == MSG_BP_ERROR){
            showErrorDialog(getString(R.string.measure_err));
//            MessageDialog.show(getString(R.string.hint),getString(R.string.measure_err),getString(R.string.ok));
            getBinding().btMeasureBp.setText(getString(R.string.start_measure));
            return true;
        }else if (msg.what == MSG_BP_ADD_DATA){
            getBinding().tvBpAdd.setText(String.valueOf(msg.arg1));
            return true;
        }else if (msg.what == MSG_BP_DE_DATA){
            getBinding().tvBpDe.setText(String.valueOf(msg.arg1));
            return true;
        }else if (msg.what == MSG_DURATION){
            measureDuration ++ ;
            getBinding().tvDuration.setText(measureDuration + " s");
            handler.sendEmptyMessageDelayed(MSG_DURATION,1000);
        }
        return false;

    }

    private void showErrorDialog(String msg){
        if (messageDialog == null){
            messageDialog = MessageDialog.build();
        }
//        else {
//            if (messageDialog.isShow()) {
//                messageDialog.dismiss();
//            }
//        }
        messageDialog.setTitle(getString(R.string.hint));
        messageDialog.setMessage(msg);
        messageDialog.setOkButton(R.string.ok);
        messageDialog.show();
    }

    @Override
    public void onBpResult(int systolic, int diastolic, int heartRate) {
        closeFile();
        handler.removeMessages(MSG_DURATION);
        Message msg = handler.obtainMessage(MSG_BP_RESULT);
        Bundle bundle = new Bundle();
        bundle.putInt(SYSTOLIC,systolic);
        bundle.putInt(DIASTOLIC,diastolic);
        bundle.putInt(HR,heartRate);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    @Override
    public void onLeadError() {
        Log.e("BpFragment", "onLeadError: " );
        closeFile();
        handler.removeMessages(MSG_DURATION);
        handler.sendEmptyMessage(MSG_BP_LEAK);
    }

    @Override
    public void onBpError() {
        closeFile();
        handler.removeMessages(MSG_DURATION);
        handler.sendEmptyMessage(MSG_BP_ERROR);

    }



    @Override
    public void onPressurizationData(short pressurizationData) {
        bpAddDataList.add(pressurizationData);
        Message message = handler.obtainMessage(MSG_BP_ADD_DATA);
        message.arg1 = pressurizationData;
        handler.sendMessage(message);
        try {
            if (addFileWriter != null){
                addFileWriter.write(String.valueOf(pressurizationData));
                addFileWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDecompressionData(short decompressionData) {
        bpDeDataList.add(decompressionData);
        Message message = handler.obtainMessage(MSG_BP_DE_DATA);
        message.arg1 = decompressionData;
        handler.sendMessage(message);
        try {
            if (deFileWriter != null){
                deFileWriter.write(String.valueOf(decompressionData));
                deFileWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPressure(short pressure) {
//        Log.e("BP", "onPressure: "+pressure );
    }

    @Override
    protected void closeFile() {
        super.closeFile();
        try {
            if (deFileWriter != null){
                deFileWriter.close();
                deFileWriter = null;
            }
            if (addFileWriter != null){
                addFileWriter.close();
                addFileWriter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (getBinding().btMeasureBp.getText().equals(getString(R.string.end_measure))){
            getBinding().btMeasureBp.performClick();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setBpResultListener(this);
        BleManager.getInstance().setRawBpDataCallback(this);
    }
}
