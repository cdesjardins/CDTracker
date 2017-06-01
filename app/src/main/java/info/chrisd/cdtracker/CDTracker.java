package info.chrisd.cdtracker;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
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
                logNewTrackPoint(l);
            }
        }
    };

    private void logNewTrackPoint(Location l) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(l.getTime());
        mLogView.append(format.format(date) + " " + l.getLongitude() + " " + l.getLatitude() + "\n");
    }

    private ArrayList<Uri> getAllTracks() {
        ArrayList<Uri> ret = new ArrayList<Uri>();
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + CDTrackerService.SAVE_DIR);
        for (final File fileEntry : dir.listFiles()) {
            if (fileEntry.isFile() == true) {
                ret.add(Uri.fromFile(fileEntry));
            }
        }
        return ret;
    }
    private void sendEmail() {
        ArrayList<Uri> tracks = getAllTracks();
        if (tracks.isEmpty() == true) {
            Toast.makeText(CDTracker.this, "There are no tracks to send", Toast.LENGTH_SHORT).show();
        } else {
            Intent sendIntent;

            sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"tracks@chrisd.info"});
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Tracks");
            sendIntent.setType("text/plain");
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, tracks);
            startActivity(Intent.createChooser(sendIntent, "Send Mail"));
        }
    }

    public void onEmailClick(View v) {
        sendEmail();
    }

    public void onClearClick(View v) {

        new AlertDialog.Builder(CDTracker.this)
                .setTitle("Delete files")
                .setMessage("Do you really want to delete all tracks?")
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        stopTrack();
                        File sdCard = Environment.getExternalStorageDirectory();
                        File dir = new File (sdCard.getAbsolutePath() + CDTrackerService.SAVE_DIR);
                        for (final File fileEntry : dir.listFiles()) {
                            if (fileEntry.delete() == false) {
                                Toast.makeText(CDTracker.this, "Unable to delete file: " + fileEntry.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        closeApp();
                    }
                })
                .create()
                .show();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((CDTrackerService.CDTrackerBinder)service).getService();
            if (mBoundService != null) {
                for (Location l : mBoundService.getRecentTrackPoints()) {
                    logNewTrackPoint(l);
                }
            }
            Log.i(TAG, "service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
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
        closeApp();
    }

    private void closeApp() {
        Intent stopIntent = new Intent(CDTracker.this, CDTrackerService.class);
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

