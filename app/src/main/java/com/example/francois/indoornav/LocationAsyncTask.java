package com.example.francois.indoornav;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.lang.ref.WeakReference;

class LocationAsyncTask extends AsyncTask<Void, Double, Void> {

    private Dwm1000Master dwm1000;
    private WeakReference<TextView> itTextView;
    private WeakReference<TextView> resultTextView;
    private WeakReference<Context> mContext;
    LocationAsyncTask(Context context, Dwm1000Master dwm1000Master, TextView itTextView, TextView resultTextView) {
        super();
        dwm1000 = dwm1000Master;
        this.itTextView = new WeakReference<>(itTextView);
        this.resultTextView = new WeakReference<>(resultTextView);
        this.mContext = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        double it = 0;
        double[] coordinates = new double[2];
        while (!isCancelled()) {
            try {
                //Thread.sleep(500);
                coordinates = dwm1000.updateCoordinates();
            } catch (Exception e) {
                Log.v("Error:", e.toString());
            }
            publishProgress(++it, coordinates[0], coordinates[1]);
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
