package info.chrisd.cdtracker;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class CDTracker extends AppCompatActivity {
    private static final String TAG = "CDTracker";
    private TextView mLogView;
    private TextView mTitle;
    private CheckBox mTracking;
    private CDTrackerService mBoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cdtracker);
        mLogView = (TextView)findViewById(R.id.textView);
        mTitle = (TextView)findViewById(R.id.textViewTitle);
        mTracking = (CheckBox)findViewById(R.id.checkBox);
        mLogView.setMovementMethod(new ScrollingMovementMethod());
        Intent startIntent = new Intent(CDTracker.this, CDTrackerService.class);
        startIntent.setAction(CDTrackerService.STARTFOREGROUND_ACTION);
        startService(startIntent);
        bindService(new Intent(CDTracker.this, CDTrackerService.class), mConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(mLocationRx, new IntentFilter(CDTrackerService.LOCATION_ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        unregisterReceiver(mLocationRx);
    }
    private BroadcastReceiver mLocationRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CDTrackerService.LOCATION_ACTION)) {
                Location l = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(l.getTime());
                mLogView.append(format.format(date) + " " + l.getLongitude() + " " + l.getLatitude() + "\n");
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((CDTrackerService.CDTrackerBinder)service).getService();
            Log.i(TAG, "service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Log.i(TAG, "service disconnected");
        }
    };

    private void startTrack() {
        if (mBoundService != null) {
            mBoundService.startTrack();
        }
    }

    private void stopTrack() {
        if (mBoundService != null) {
            mBoundService.stopTrack();
        }
    }

    public void onExitClick(View v) {
        stopTrack();
        Intent stopIntent = new Intent(CDTracker.this, CDTrackerService.class);
        //stopIntent.setAction(CDTrackerService.STOPFOREGROUND_ACTION);
        stopService(stopIntent);
        finish();
    }

    public void onTrackingClick(View v) {
        if (mTracking.isChecked() == true) {
            startTrack();
        } else {
            stopTrack();
        }
    }
}

