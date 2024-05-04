package com.kl.visionsdkdemo.model;

import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


@androidx.room.Dao
public interface Dao {

    @Insert
    void insert(MeasurementModel measurementModel);

    @Update
    void update(MeasurementModel measurementModel);

    @Query("SELECT * FROM measurementTable")
    List<MeasurementModel> getDetails();


}
