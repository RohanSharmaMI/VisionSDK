package com.kl.visionsdkdemo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measurementTable")
public class MeasurementModel {

    @PrimaryKey(autoGenerate = true)
    int id;

    public String getSystolicBP() {
        return systolicBP;
    }

    public void setSystolicBP(String systolicBP) {
        this.systolicBP = systolicBP;
    }

    public String getDiastolicBP() {
        return diastolicBP;
    }

    public void setDiastolicBP(String diastolicBP) {
        this.diastolicBP = diastolicBP;
    }

    public String getHeartRateBP() {
        return heartRateBP;
    }

    public void setHeartRateBP(String heartRateBP) {
        this.heartRateBP = heartRateBP;
    }

    public String getBloodSugar() {
        return bloodSugar;
    }

    public void setBloodSugar(String bloodSugar) {
        this.bloodSugar = bloodSugar;
    }

    public String getGlucoseSum() {
        return GlucoseSum;
    }

    public void setGlucoseSum(String glucoseSum) {
        GlucoseSum = glucoseSum;
    }

    public String getBgCount() {
        return bgCount;
    }

    public void setBgCount(String bgCount) {
        this.bgCount = bgCount;
    }

    public String getSpo2() {
        return spo2;
    }

    public void setSpo2(String spo2) {
        this.spo2 = spo2;
    }

    public String getHearRateSpo2() {
        return hearRateSpo2;
    }

    public void setHearRateSpo2(String hearRateSpo2) {
        this.hearRateSpo2 = hearRateSpo2;
    }

    private String systolicBP;
    private String diastolicBP;
    private String heartRateBP;
    private String bloodSugar;
    private String GlucoseSum;
    private String bgCount;
    private String bodyTemperature;
    private String bold;
    private String environment;
    private String voltage;
    private String spo2;
    private String hearRateSpo2;

    public MeasurementModel(String bodyTemperature, String bold, String environment, String voltage){

        this.bodyTemperature = bodyTemperature;
        this.bold = bold;
        this.environment = environment;
        this.voltage = voltage;

    }

    public String getBodyTemperature() {
        return bodyTemperature;
    }

    public void setBodyTemperature(String bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public String getBold() {
        return bold;
    }

    public void setBold(String bold) {
        this.bold = bold;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getVoltage() {
        return voltage;
    }

    public void setVoltage(String voltage) {
        this.voltage = voltage;
    }
}
