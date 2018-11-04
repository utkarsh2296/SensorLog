package h.utkarshdubey.a3_mt17063_sensorlog;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class A3_MT17063_MainActivity extends AppCompatActivity implements SensorEventListener{

    private TextView acc;
    private TextView gyro;
    int bufferSize;
    private TextView loc;
    private TextView network;
    private TextView wifi;
    private TextView micro;
    private Sensor myAcc;
    private Sensor myGyro;
    private Thread thred;
    private double dblvl;
    private int readSensors;
    private boolean Tflag;
    private Toolbar export;
    private boolean started = false;
    private SensorManager sm;
    private TelephonyManager telephonyManager;
    private WifiManager wifiManager;
    AudioRecord audio;
    static final int REQUEST_LOCATION = 1;
    static final int REQUEST_AUDIO = 2;
    static final int REQUEST_STORAGE= 3;
    LocationManager locationManager;
    LocationListener locationListener;
    private LinearLayout start;
    private LinearLayout stop;
    private A3_MT17063_DatabaseHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get all the variables from xml
        acc = findViewById(R.id.accelerometer);
        gyro = findViewById(R.id.gyro);
        loc = findViewById(R.id.gps);
        network = findViewById(R.id.network);
        wifi = findViewById(R.id.wifi);
        micro = findViewById(R.id.mic);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        // register sensors and services
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        myAcc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        myGyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        helper = new A3_MT17063_DatabaseHelper(this);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //check if user has given permission or not
        requestPermissions();

        // perform everything only when user has given all permission
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (started == false) {
                    started = true;
                    readSensors();
                }
            }
        });
        // stop showing the readings
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                started = false;
                acc.setText("");
                gyro.setText("");
                loc.setText("");
                network.setText("");
                wifi.setText("");
                micro.setText("");
                doUnregister();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        // Exporting the database value to CSV
        switch (item.getItemId()) {
            case R.id.export:
                String exportDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                String hasValue = "false";
                try {
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir + File.separator + "SensorLog.csv"));
                    SQLiteDatabase db = helper.getReadableDatabase();
                    Cursor curCSV = db.rawQuery("SELECT * FROM " + A3_MT17063_DatabaseHelper.table_name, null);
                    csvWrite.writeNext(curCSV.getColumnNames());
                    Log.d("ExportDir",exportDir);
                    //Toast.makeText(this, exportDir, Toast.LENGTH_SHORT).show();
                    while (curCSV.moveToNext()) {
                        hasValue = "true";
                        String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3)};
                        csvWrite.writeNext(arrStr);
                    }
                    csvWrite.close();
                    curCSV.close();
                } catch (Exception sqlEx) {
                    Log.e("Exporting Error", sqlEx.getMessage(), sqlEx);
                }
                Log.d("has Value",hasValue);
                Toast.makeText(this, "Exporting data to CSV " + hasValue, Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    private void readSensors() {
        // function that will fetch value from sensors
        if(started) {
            sm.registerListener(this, myAcc, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener(this, myGyro, SensorManager.SENSOR_DELAY_NORMAL);
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            // Define a listener that responds to location updates
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    String lat = String.valueOf(location.getLatitude());
                    String longtd = String.valueOf(location.getLongitude());
                    loc.setText("Lat: " + lat + " Long: " + longtd);
                    new PushinDataBase().execute("Location", "Lat: " + lat + " Long: " + longtd);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List cellinfo = telephonyManager.getAllCellInfo();
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> wifiList = wifiManager.getScanResults();
            showCellInfo(cellinfo);
            showWifiInfo(wifiList);
            Tflag = true;
            readMicro();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(started)doUnregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        readSensors();
    }

    private void doUnregister() {
        // unregister every sensors
        sm.unregisterListener(this);
        Tflag = false;
        micro.setText("");
        locationManager.removeUpdates(locationListener);
    }

    private void readMicrobuffer() {
        try {
            short[] buffer = new short[bufferSize];

            int bufferReadResult = 1;

            if (audio != null) {

                // Sense the voice...
                bufferReadResult = audio.read(buffer, 0, bufferSize);
                double sumLevel = 0;
                for (int i = 0; i < bufferReadResult; i++) {
                    sumLevel += buffer[i];
                }
                dblvl = Math.abs((sumLevel / bufferReadResult));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readMicro() {
        int sampleRate = 8000;
        try {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (audio != null) {
                audio.startRecording();
                thred = new Thread(new Runnable() {
                    public void run() {
                        while (thred != null && !thred.isInterrupted()) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                            readMicrobuffer();
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    new PushinDataBase().execute("MicroPhone",String.valueOf(dblvl));
                                    micro.setText("Audio" + "dB:" + dblvl);
                                    if (Tflag == false) {
                                        try {
                                            if (audio != null) {
                                                audio.stop();
                                                audio.release();
                                                audio = null;
                                            }
                                        } catch (Exception e) {
                                            Log.d("exception in tflag", e.getMessage());

                                        }
                                        micro.setText("");
                                        return;
                                    }
                                }
                            });
                        }
                    }
                });
                thred.start();
            }
        } catch (Exception e) {
            Log.d("exception in micro", e.getMessage());
        }

    }

    private void showWifiInfo(List<ScanResult> wifiList) {
        String accessPoints = "";
        Log.d("WIfi","reading values");
        for (int i = 0; i < wifiList.size(); ++i) {
            accessPoints += wifiList.get(i).SSID + "\n";
        }
        wifi.setText(accessPoints);
        new PushinDataBase().execute("WIFI Access Points",String.valueOf(dblvl));
    }


    private void showCellInfo(List cellinfo) {
        String details = "";
        for (int i = 0; i < cellinfo.size(); ++i) {
            try {
                CellInfo info = (CellInfo) cellinfo.get(i);
                if (info instanceof CellInfoGsm) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                    details += "GSM" + identityGsm.getCid() + ",";
                } else if (info instanceof CellInfoLte) {
                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    details += "LTE" + identityLte.getCi() + ",";
                }
            } catch (Exception ex) {

            }
        }
        new PushinDataBase().execute("CellPhone Id's",details);
        //for end
        network.setText(details);
    }
    public static String reduce(double d)
    {
        return String.format("%.3f",d);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double Xcor = event.values[0];
            double Ycor = event.values[1];
            double Zcor = event.values[2];
            String mag=String.valueOf(Xcor)+","+String.valueOf(Ycor)+","+String.valueOf(Zcor);
            acc.setText("X: "+reduce(Xcor)+", "+"Y: "+reduce(Ycor)+", "+"Z: "+reduce(Zcor));
            new PushinDataBase().execute("Accelerometer Magnitude",mag);
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            double Xcor = event.values[0];
            double Ycor = event.values[1];
            double Zcor = event.values[2];
            String mag=String.valueOf(Xcor)+","+String.valueOf(Ycor)+","+String.valueOf(Zcor);
            gyro.setText("X: "+reduce(Xcor)+", "+"Y: "+reduce(Ycor)+", "+"Z: "+reduce(Zcor));
            new PushinDataBase().execute("Gyroscope Magnitude",String.valueOf(mag));
        }
        if (readSensors % 1000 == 0 && started == true) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List cellinfo = telephonyManager.getAllCellInfo();
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> wifiList = wifiManager.getScanResults();
            showCellInfo(cellinfo);
            showWifiInfo(wifiList);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            }
            else
                requestPermissions();
        }
        if (requestCode == REQUEST_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            }
            else
                requestPermissions();
        }
        if (requestCode == REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                readSensors();
            }
            else
                requestPermissions();
        }
    }

    private class PushinDataBase extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            SQLiteDatabase dbwrite= helper.getWritableDatabase();
            dbwrite.beginTransaction();
            try
            {
                Date time = Calendar.getInstance().getTime();
                ContentValues values=new ContentValues();
                values.put(A3_MT17063_DatabaseHelper.time,String.valueOf(time));
                values.put(A3_MT17063_DatabaseHelper.name,strings[0]);
                values.put(A3_MT17063_DatabaseHelper.val,strings[1]);
                dbwrite.insertOrThrow(A3_MT17063_DatabaseHelper.table_name,null,values);
                dbwrite.setTransactionSuccessful();
            }
            catch (Exception e)
            {
                Log.d("SQLExcption", e.getMessage());
            }
            finally {
                dbwrite.endTransaction();
            }
            return null;
        }
    }
}