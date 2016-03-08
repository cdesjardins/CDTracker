package info.chrisd.cdtracker;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class CDTracker extends AppCompatActivity {
    private static final String TAG = "CDTracker";
    private static final String mIntentAction = "info.chrisd.LOCATION_UPDATE";
    private TextView mLogView;
    private TextView mTitle;
    private CheckBox mTracking;


    private BroadcastReceiver mLocationRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(mIntentAction)) {
                Location l = (Location)intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                mLogView.append("\ngot location update " + l);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cdtracker);
        mLogView = (TextView)findViewById(R.id.textView);
        mTitle = (TextView)findViewById(R.id.textViewTitle);
        mTracking = (CheckBox)findViewById(R.id.checkBox);
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);


        registerReceiver(mLocationRx, new IntentFilter(mIntentAction));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(mIntentAction), PendingIntent.FLAG_CANCEL_CURRENT);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        crit.setAltitudeRequired(false);
        crit.setBearingAccuracy(Criteria.NO_REQUIREMENT);
        crit.setBearingRequired(false);
        crit.setCostAllowed(false);
        crit.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        crit.setPowerRequirement(Criteria.NO_REQUIREMENT);
        crit.setSpeedAccuracy(Criteria.NO_REQUIREMENT);
        crit.setSpeedRequired(false);
        crit.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        locationManager.requestLocationUpdates(10, 0, crit, pendingIntent);
    }

    private void startTrack() {

    }

    private void stopTrack() {

    }

    public void onTrackingClick(View v) {
        if (mTracking.isChecked() == true) {
            startTrack();
        } else {
            stopTrack();
        }
    }
}

