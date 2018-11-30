package com.example.francois.indoornav.location;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import com.example.francois.indoornav.R;
import com.example.francois.indoornav.util.PointD;

import java.lang.ref.WeakReference;

public class LocationProviderAsyncTask extends AsyncTask<Void, Double, Void> {

    private ILocationProvider mLocationProvider;
    private WeakReference<TextView> itTextView;
    private WeakReference<TextView> resultTextView;
    private WeakReference<Context> mContext;
    public LocationProviderAsyncTask(Context context, ILocationProvider locationProvider,
                                     TextView itTextView, TextView resultTextView) {
        super();
        mLocationProvider = locationProvider;
        this.itTextView = new WeakReference<>(itTextView);
        this.resultTextView = new WeakReference<>(resultTextView);
        this.mContext = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        double it = 0;
        PointD coordinates = new PointD();
        while (!isCancelled()) {
            try {
                //Thread.sleep(500);
                mLocationProvider.updateLocation();
                coordinates.set(mLocationProvider.getLastLocation());
            } catch (Exception e) {
                Log.v("Error:", e.toString());
            }
            publishProgress(++it, coordinates.x, coordinates.y);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        super.onProgressUpdate(values);
        TextView it = itTextView.get();
        TextView res = resultTextView.get();
        Context context = mContext.get();
        it.setText(context.getString(R.string.it, values[0].intValue()));
        res.setText(context.getString(R.string.coor,values[1], values[2]));
    }
}
