package com.kl.visionsdkdemo.model;
import androidx.room.RoomDatabase;

@androidx.room.Database(entities = MeasurementModel.class,version = 1)
public abstract class MainDatabase extends RoomDatabase {

    public abstract Dao dao();

}
