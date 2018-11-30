package com.example.francois.indoornav.location;

import android.support.annotation.IntDef;

import com.example.francois.indoornav.util.PointD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ILocationProvider {
    int SUCCESS = 1;
    int FAILED = 0;

    @IntDef({SUCCESS, FAILED})
    @Retention(RetentionPolicy.SOURCE)
    @interface LocationUpdated {
    }

    @LocationUpdated
    int updateLocation();
    PointD getLastLocation();
}
