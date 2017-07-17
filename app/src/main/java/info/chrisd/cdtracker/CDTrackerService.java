package info.chrisd.cdtracker;

import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CDTrackerService extends Service {
    private static final String TAG = "CDTrackerService";
    public static final String STARTFOREGROUND_ACTION = "info.chrisd.cdtracker.startforeground";
    public static final String LOCATION_ACTION = "info.chrisd.cdtracker.location_update";
    public static final String SAVE_DIR = "/CDTracker";
    private boolean mTracking = true;
    private boolean mStarted = false;
    PendingIntent mLocationPendingIntent = null;
    private ArrayList<CDLocation> mLocations = new ArrayList<CDLocation>();

    private class CDLocation {
        public CDLocation(Location l) {
            mLoc = l;
        }

        public String toString() {
            String ret = new String();
            ret = "            <trkpt lat=\"" + mLoc.getLatitude() + "\" lon=\"" + mLoc.getLongitude() + "\">";
            if (mLoc.hasAltitude() == true) {
                ret += "<ele>" + mLoc.getAltitude() + "</ele>";
            }
            ret += "<time>" + timeString() + "</time>";
            ret += "</trkpt>\n";
            return ret;
        }

        public String timeString() {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date(mLoc.getTime());
            String ret = new String();
            ret = dateFormat.format(date) + "T" + timeFormat.format(date) + "Z";
            return ret;
        }

        public Location getLoc() {
            return mLoc;
        }

        private Location mLoc;
    }

    private void xmlHeader(FileOutputStream fs) throws IOException {
        String xml = "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n" +
                "<gpx version=\"1.1\" creator=\"blog.chrisd.info\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/gpx/1/1/gpx.xsd\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:gpx10=\"http://www.topografix.com/GPX/1/0\" xmlns:ogt10=\"http://gpstracker.android.sogeti.nl/GPX/1/0\">\n" +
                "    <metadata>\n" +
                "        <time>" + mLocations.get(0).timeString() + "</time>\n" +
                "    </metadata>\n" +
                "    <trk>\n" +
                "    <name>Track 1</name>\n" +
                "        <trkseg>\n";
        fs.write(xml.getBytes());
    }

    private void xmlPoints(FileOutputStream fs) throws IOException {
        for (CDLocation loc : mLocations) {
            fs.write(loc.toString().getBytes());
        }
    }

    private void xmlFooter(FileOutputStream fs) throws IOException {
        String xml = "        </trkseg>\n" +
                "    </trk>\n" +
                "</gpx>";
        fs.write(xml.getBytes());
    }
    private void saveFile() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + SAVE_DIR);
        if (dir.isDirectory() || dir.mkdir() == true) {
            String fn = "/cdtrack-" + mLocations.get(0).timeString() + ".gpx";
            fn = fn.replace(":", "");
            File f = new File(dir.getAbsolutePath() + fn);
            try {
                FileOutputStream fs = new FileOutputStream(f);
                xmlHeader(fs);
                xmlPoints(fs);
                xmlFooter(fs);
                Toast.makeText(CDTrackerService.this, "Wrote file" + f.toString(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(CDTrackerService.this, "Unable to create file: " + f.toString(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(CDTrackerService.this, "Unable to create dir: " + dir.toString(), Toast.LENGTH_SHORT).show();
        }
        mLocations.clear();
    }

    private BroadcastReceiver mLocationRx = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(LOCATION_ACTION)) {
                Location l = (Location) intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);

                if (mTracking == true) {
                    synchronized (mLocations) {
                        mLocations.add(new CDLocation(l));
                        if (mLocations.size() > 1000) {
                            saveFile();
                        }
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
                stopTrack(true);
                Toast.makeText(CDTrackerService.this, "CDTracker shutting down", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public CDTrackerService() {
    }

    @Override
    public void onCreate () {
        super.onCreate();
        Log.i(TAG, "onCreate");
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LOCATION_ACTION);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        registerReceiver(mLocationRx, intentFilter);
        mLocationPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(LOCATION_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        crit.setAltitudeRequired(true);
        crit.setBearingAccuracy(Criteria.NO_REQUIREMENT);
        crit.setBearingRequired(false);
        crit.setCostAllowed(false);
        crit.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        crit.setPowerRequirement(Criteria.NO_REQUIREMENT);
        crit.setSpeedAccuracy(Criteria.NO_REQUIREMENT);
        crit.setSpeedRequired(false);
        crit.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
        try {
            locationManager.requestLocationUpdates(5000, 10, crit, mLocationPendingIntent);
        } catch (SecurityException e) {
            Toast.makeText(CDTrackerService.this, "Unable to request location updates", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (mLocationPendingIntent != null) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                locationManager.removeUpdates(mLocationPendingIntent);
            }
            unregisterReceiver(mLocationRx);
        }
        mStarted = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + intent + " " + flags + " " + startId);
        if (intent != null) {
            if ((intent.getAction().equals(STARTFOREGROUND_ACTION)) && (mStarted == false)) {
                Log.i(TAG, "Starting foreground service");
                mStarted = true;
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_pin);
                Intent notificationIntent = new Intent(this, CDTracker.class);
                notificationIntent.setAction("info.chrisd.cdtracker.main");
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                Notification notification = new NotificationCompat.Builder(this)
                        .setContentTitle("CDTracker")
                        .setTicker("CDTracker")
                        .setContentText("CDTracker running")
                        .setSmallIcon(R.drawable.ic_pin)
                        .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .build();
                startForeground(543, notification);
            }
        }
        return START_STICKY;
    }

    public class CDTrackerBinder extends Binder {
        CDTrackerService getService() {
            return CDTrackerService.this;
        }
    }

    public void startTrack() {
        mTracking = true;
    }
    public void stopTrack(boolean save) {
        synchronized (mLocations) {
            mTracking = false;
            if (save) {
                if (mLocations.isEmpty() == false) {
                    saveFile();
                } else {
                    Toast.makeText(CDTrackerService.this, "No locations to save", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public ArrayList<Location> getRecentTrackPoints() {
        List<CDLocation> tail = mLocations.subList(Math.max(mLocations.size() - 10, 0), mLocations.size());
        ArrayList<Location> ret = new ArrayList<Location>();
        for (CDLocation l : tail) {
            ret.add(l.getLoc());
        }
        return ret;
    }

    private final IBinder mBinder = new CDTrackerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind " + intent);
        return mBinder;
    }
}
