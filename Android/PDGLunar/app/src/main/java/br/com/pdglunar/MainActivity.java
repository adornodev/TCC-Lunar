package br.com.pdglunar;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.pdglunar.service.GPSService;
import br.com.pdglunar.utils.FileUtils;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    final int TIME_TO_SAVE_DATA = 30;

    Button btnActivate;
    Button btnPothole;
    Button btnSpeedBump;

    SensorManager sensorAccelerometerManager;

    double   longitude;
    double   latitude;

    TextView tvAccelerometerX;
    TextView tvAccelerometerY;
    TextView tvAccelerometerZ;

    EditText et_filename;

    List<TextView> listTv;

    // Path to save data
    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TCC-Lunar/";

    String filename = "";

    File generalFile, potholeFile, speedBumpFile;

    long curTime;
    long lastUpdate = 0;

    boolean isEnableActivateButton;
    boolean isFirstWrite;
    boolean isFirstHoleWrite;
    boolean isFirstSpeedBumpWrite;
    boolean isFirstClickPotholeButton   = true;
    boolean isFirstClickSpeedBumpButton = true;


    Chronometer timer;

    boolean gps_permission, storage_permission;
    boolean isBroadcastRegistred = false;

    private static final int REQUEST_GPS_PERMISSIONS     = 100;
    private static final int REQUEST_STORAGE_PERMISSIONS = 101;

    SharedPreferences        mPref;
    SharedPreferences.Editor medit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region Layout Variables Declaration
        tvAccelerometerX = (TextView) findViewById(R.id.tv_accelerometer_X);
        tvAccelerometerY = (TextView) findViewById(R.id.tv_accelerometer_Y);
        tvAccelerometerZ = (TextView) findViewById(R.id.tv_accelerometer_Z);

        et_filename      = (EditText) findViewById(R.id.et_filename);
        btnActivate      = (Button)   findViewById(R.id.btn_activate);
        btnPothole       = (Button)   findViewById(R.id.btn_pothole);
        btnSpeedBump     = (Button)   findViewById(R.id.btn_speedbump);

        timer            = (Chronometer) findViewById(R.id.chronometer);
        //endregion

        // Chronometer formatting
        timer.setTextColor(Color.RED);
        timer.setFormat(">> %s <<");


        mPref            = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        medit            = mPref.edit();

        isEnableActivateButton      = false;
        sensorAccelerometerManager  = (SensorManager) getSystemService(SENSOR_SERVICE);

        listTv = new ArrayList<>();

        // Checks if the SDCard is available for writing
        if (FileUtils.isExternalStorageWritable()) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TCC-Lunar/";
        }

        // Checks if directory exist
        generalFile = new File(path);
        if (!generalFile.isDirectory())
            // Create Directory
            generalFile.mkdir();


        // Request permission
        getPermission();

        //region Pothole button Listener
        btnPothole.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Checks if the capture process is inactive
                if (!isEnableActivateButton)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formatting data
                String data = formatSpeedBumpHoleLineToSaveTxtFile((SystemClock.elapsedRealtime() - timer.getBase()),0);

                // Saving data
                FileUtils.SaveData(potholeFile,data);

                // Check if it is the first click
                // This is checked to insert or not the header in the output file
                if (isFirstClickPotholeButton)
                    isFirstClickPotholeButton = false;

                // Info Message
                Toast.makeText(MainActivity.this,"Buraco adicionado",Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region SpeedBump Button Listener
        btnSpeedBump.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Checks if the capture process is inactive
                if (!isEnableActivateButton)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formatting data
                String data = formatSpeedBumpHoleLineToSaveTxtFile((SystemClock.elapsedRealtime() - timer.getBase()),1);

                // Saving data
                FileUtils.SaveData(speedBumpFile, data);

                // Check if it is the first click
                // This is checked to insert or not the header in the output file
                if (isFirstClickSpeedBumpButton)
                    isFirstClickSpeedBumpButton = false;

                // Info Message
                Toast.makeText(MainActivity.this,"Quebra mola adicionado",Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Activate Button Listener
        btnActivate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Checks if the application has the required permissions
                if (!gps_permission || !storage_permission)
                {
                    Toast.makeText(MainActivity.this,"The LUNAR application can not run without having the permissions granted", Toast.LENGTH_LONG).show();
                    try
                    {
                        Thread.sleep(3000);
                    }catch(InterruptedException e) { }

                    MainActivity.this.finish();
                    //System.exit(0);
                }

                // Register the background service
                if (!isBroadcastRegistred)
                {
                    registerReceiver(broadcastReceiver, new IntentFilter(GPSService.str_receiver));
                    isBroadcastRegistred = true;
                }

                // Checks if the capture process is inactive
                if (!isEnableActivateButton) {

                    // Checks if GPS is allowed to use
                    if (gps_permission) {

                        if (mPref.getString("service", "").matches(""))
                        {
                            medit.putString("service", "service").commit();

                            Intent intent = new Intent(getApplicationContext(), GPSService.class);
                            startService(intent);

                        }
                        else
                            Toast.makeText(getApplicationContext(), "Service is running...", Toast.LENGTH_SHORT).show();
                    }

                    else
                    {
                        Toast.makeText(getApplicationContext(), "Please, active the GPS!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Capture and format the output file name
                    if (et_filename.getText().toString().trim().equals(""))
                        filename = "Lunar" + "_" + FileUtils.getDateTimeSystem();
                    else
                        filename = et_filename.getText().toString().trim() + "_" + FileUtils.getDateTimeSystem();

                    // Create output file
                    generalFile = new File(path + filename + ".txt");

                    // Setting Boolean Variables
                    isFirstWrite                  = true;
                    isFirstHoleWrite              = true;
                    isFirstSpeedBumpWrite         = true;
                    isFirstClickPotholeButton     = true;
                    isFirstClickSpeedBumpButton   = true;

                    // Reset chronometer
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.start();

                    // Formats the Activate button
                    btnActivate.setText("PARAR");
                    btnActivate.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                    btnActivate.setTextColor((ResourcesCompat.getColor(getResources(), R.color.black, null)));

                    // Create SpeedBump and Pothole output files
                    potholeFile   = new File(path + "Pothole"     + "_" + filename + ".txt");
                    speedBumpFile = new File(path + "SpeedBump"   + "_" + filename + ".txt");

                    // Activates the Accelerometer Sensor
                    activateSensorAcecelerometer();
                }

                // If the user interrupts the data capture
                else
                {
                    btnActivate.setText(getString(R.string.btn_activate));
                    btnActivate.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
                    btnActivate.setTextColor((ResourcesCompat.getColor(getResources(), R.color.white, null)));

                    // Remove the Accelerometer Sensor Register
                    sensorAccelerometerManager.unregisterListener(MainActivity.this);

                    // Remove the broadcast register
                    if (isBroadcastRegistred)
                    {
                        unregisterReceiver(broadcastReceiver);
                        isBroadcastRegistred = false;
                    }

                    if (medit != null) {
                        medit.clear();
                        medit.commit();
                    }

                    // Setting Boolean Variables
                    isFirstWrite                  = true;
                    isFirstHoleWrite              = true;
                    isFirstSpeedBumpWrite         = true;

                    // Stop the chronometer
                    timer.stop();
                    timer.setBase(SystemClock.elapsedRealtime());
                }

                // Changes the status of the Activate button
                isEnableActivateButton = !isEnableActivateButton;
            }
        });
        //endregion

    }

    private void getPermission() {

        // Check if location permission was granted
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            // Check if the user has already given DENY permission previously
            if  ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)))
            {
                // TODO Explain why this permission is important
            }
            else
            {
                // Request permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GPS_PERMISSIONS);
            }
        }
        else
        {
            gps_permission = true;
        }

        // Check if storage permission was granted
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
        {

            // Check if the user has already given DENY permission previously
            if  ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)))
            {
                // TODO Explain why this permission is important
            }
            else
            {
                // Request permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSIONS);
            }

        }
        else
        {
            storage_permission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case REQUEST_GPS_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    gps_permission = true;
                else
                    Toast.makeText(getApplicationContext(), getString(R.string.text_accept_permission), Toast.LENGTH_LONG).show();
                break;

            case REQUEST_STORAGE_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    storage_permission = true;
                else
                    Toast.makeText(getApplicationContext(), getString(R.string.text_accept_permission), Toast.LENGTH_LONG).show();
                break;
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            latitude    = Double.valueOf(intent.getStringExtra("latitude"));
            longitude   = Double.valueOf(intent.getStringExtra("longitude"));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorAccelerometerManager.unregisterListener(this);

        if (medit != null) {
            medit.clear();
            medit.commit();
        }

        if (isBroadcastRegistred)
        {
            stopService(new Intent(this, GPSService.class));
            unregisterReceiver(broadcastReceiver);
            isBroadcastRegistred = false;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        GPSService.stopService = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getSensorData(event);
        }
    }

    private void activateSensorAcecelerometer() {
        sensorAccelerometerManager.registerListener(this,sensorAccelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void getSensorData(SensorEvent event) {

        tvAccelerometerX.setText("X: " + event.values[0]);
        tvAccelerometerY.setText("Y: " + event.values[1]);
        tvAccelerometerZ.setText("Z: " + event.values[2]);

        listTv.clear();
        listTv.add(tvAccelerometerX);
        listTv.add(tvAccelerometerY);
        listTv.add(tvAccelerometerZ);

        // Get Tilt
        int tilt = getTilt(event.values);

        //Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        //int rotation = display.getRotation();
        //https://developer.android.com/reference/android/view/Display.html#getRotation()

        // Get current Time
        curTime = System.currentTimeMillis();

        // Check if you have already spent the time needed to save the data
        if ((curTime - lastUpdate) > TIME_TO_SAVE_DATA)
        {
            FileUtils.SaveData(generalFile, formatLineToSaveTxtFile(listTv, tilt, (SystemClock.elapsedRealtime() - timer.getBase())));
            lastUpdate = curTime;
        }
    }

    private int getTilt(float[] values)
    {
        double norm_values = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);

        // Normalize the accelerometer vector
        values[2] = (float) (values[2]/norm_values);

        int tilt  = (int) Math.round(Math.toDegrees(Math.acos(values[2])));

        return tilt;
        /*
        if (inclination < 25 || inclination > 155)
        {
            // device is flat
            Log.i("PDG_LUNAR", "[FLAT] Inclination: " + inclination);
        }
        else
        {
            // device is not flat
            Log.i("PDG_LUNAR", "[NOT FLAT] Inclination: " + inclination);
        }
        */
    }

    public String formatLineToSaveTxtFile(List<TextView> listTv, int tilt, long time) {
        String data;
        StringBuilder line = new StringBuilder();

        // First written to file?
        // Add Header
        if (isFirstWrite) {
            line.append("accelerometer_X;accelerometer_Y;accelerometer_Z;latitude;longitude;tilt;timestamp\n");
            isFirstWrite = false;
        }

        // Iterate over all TextViews
        for (TextView tV : listTv) {
            data = tV.getText().toString().substring(2).trim();

            // Check if the number is in scientific formatting. If so, it will have to be converted
            if (data.contains("E") == true)
                data = new BigDecimal(data).toString();

            line.append(data);
            line.append(";");
        }

        line.append(String.valueOf(latitude));
        line.append(";");
        line.append(String.valueOf(longitude));
        line.append(";");
        line.append(String.valueOf(tilt));
        line.append(";");
        line.append(time);

        return line.toString();
    }

    public String formatSpeedBumpHoleLineToSaveTxtFile(long timer, int id) {

        //id -> 0 Pothole
        //id -> 1 SpeedBump
        StringBuilder line = new StringBuilder();

        if (id == 0 && isFirstHoleWrite)
        {
            line.append("latitude;longitude;timestamp\n");
            isFirstHoleWrite = false;
        }

        if (id == 1 && isFirstSpeedBumpWrite)
        {
            line.append("latitude;longitude;timestamp\n");
            isFirstSpeedBumpWrite = false;
        }

        line.append(String.valueOf(latitude));
        line.append(";");
        line.append(String.valueOf(longitude));
        line.append(";");
        line.append(timer);

        return line.toString();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

