package finalcourseassignment.ufrj.android_lunar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import finalcourseassignment.ufrj.android_lunar.utils.FileUtils;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;


public class MainActivity extends AppCompatActivity implements SensorEventListener
{
    final int TIME_TO_SAVE_DATA = 30;
    final int LIMIT_TILT        = 30;

    Button btnActivate;
    Button btnPothole;
    Button btnSpeedBump;

    TextView tvAccelerometerX;
    TextView tvAccelerometerY;
    TextView tvAccelerometerZ;

    EditText et_filename;

    CheckBox cbCSV;
    CheckBox cbSQS;

    SensorManager sensorAccelerometerManager;
    Handler handler;

    List<TextView> listTv;

    // Path to save data
    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TCC-Lunar/";

    String filename = "";
    File generalFile, potholeFile, speedBumpFile;

    long curTime;
    long lastUpdate = 0;

    double latitude, longitude;

    boolean isEnableActivateButton;
    boolean isFirstWrite                = true;
    boolean isFirstPotholeWrite         = true;
    boolean isFirstSpeedBumpWrite       = true;
    boolean isEnableSQSMode             = false;

    Chronometer timer;

    boolean gps_permission, storage_permission;

    private static final int    REQUEST_GPS_PERMISSIONS     = 100;
    private static final int    REQUEST_STORAGE_PERMISSIONS = 101;
    private static final String DELIMITER                   = ";";

    OnLocationUpdatedListener locationListener;
    Runnable locationRunnable;

    // AWS Fields
    AmazonSQSClient sqsClient;
    AWSCredentials awsCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region Layout Variables Declaration
        tvAccelerometerX = (TextView) findViewById(R.id.tv_accelerometer_X);
        tvAccelerometerY = (TextView) findViewById(R.id.tv_accelerometer_Y);
        tvAccelerometerZ = (TextView) findViewById(R.id.tv_accelerometer_Z);

        cbCSV            = (CheckBox) findViewById(R.id.cb_csv);
        cbSQS            = (CheckBox) findViewById(R.id.cb_sqs);
        et_filename      = (EditText) findViewById(R.id.et_filename);
        btnActivate      = (Button)   findViewById(R.id.btn_activate);
        btnPothole       = (Button)   findViewById(R.id.btn_pothole);
        btnSpeedBump     = (Button)   findViewById(R.id.btn_speedbump);

        timer            = (Chronometer) findViewById(R.id.chronometer);
        //endregion

        // Chronometer formatting
        timer.setTextColor(Color.RED);
        timer.setFormat(">> %s <<");

        isEnableActivateButton      = false;
        sensorAccelerometerManager  = (SensorManager) getSystemService(SENSOR_SERVICE);
        handler  = new Handler();
        listTv   = new ArrayList<>();

        if (isEnableSQSMode)
            // Initialize AWS Fields
            InitializeAWSFields();


        // Checks if the SDCard is available for writing
        if (FileUtils.isExternalStorageWritable())
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TCC-Lunar/";

        // Checks if directory exist
        generalFile = new File(path);
        if (!generalFile.isDirectory())
            // Create Directory
            generalFile.mkdir();

        //region Pothole button Listener
        btnPothole.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Checks if the capture process is inactive
                if (!isEnableActivateButton)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Checks the selected output format
                if (isEnableSQSMode)
                {
                    // Format data
                    String jsonString = formatToJsonString((SystemClock.elapsedRealtime() - timer.getBase()), 1);

                    // Send data to SQS
                    new SendMessageAsyncTask().execute(new Message(sqsClient, new ArrayList<>(Arrays.asList(jsonString))));
                }
                else
                {
                    // Save data on csv file
                    String data = formatEventLineToCSV((SystemClock.elapsedRealtime() - timer.getBase()),1);
                    FileUtils.saveData(potholeFile, data);
                }

                // Info Message
                Toast.makeText(MainActivity.this, getString(R.string.msg_pothole_added), Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region SpeedBump Button Listener
        btnSpeedBump.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Check if the capture process is inactive
                if (!isEnableActivateButton)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Checks the selected output format
                if (isEnableSQSMode)
                {
                    // Format data
                    String jsonString = formatToJsonString((SystemClock.elapsedRealtime() - timer.getBase()), 2);

                    // Send data to SQS
                    new SendMessageAsyncTask().execute(new Message(sqsClient, new ArrayList<>(Arrays.asList(jsonString))));
                }
                else
                {
                    String data = formatEventLineToCSV((SystemClock.elapsedRealtime() - timer.getBase()),2);
                    FileUtils.saveData(speedBumpFile, data);
                }


                // Info Message
                Toast.makeText(MainActivity.this, getString(R.string.msg_speedbump_added), Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Activate Button Listener
        btnActivate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                // Has the user chosen the output format?
                if (!cbCSV.isChecked() && !cbSQS.isChecked())
                {
                    Toast.makeText(MainActivity.this,getString(R.string.msg_must_choose_checkbox), Toast.LENGTH_SHORT).show();
                    return;
                }
                // Checks if the capture process is inactive
                if (!isEnableActivateButton)
                {
                    // Request permissions
                    getPermissions();

                    // Checks if the application has the required permissions
                    if (!gps_permission || !storage_permission)
                    {
                        Toast.makeText(MainActivity.this, getString(R.string.msg_must_accept_permissions), Toast.LENGTH_SHORT).show();
                        try
                        {
                            Thread.sleep(2000);
                        }catch(InterruptedException e) { }

                        MainActivity.this.finish();
                    }

                    // Start Location
                    startLocation();

                    // Capture and format the output file name
                    if (et_filename.getText().toString().trim().equals(""))
                        filename = "Lunar" + "_" + FileUtils.getDateTimeSystem();
                    else
                        filename = et_filename.getText().toString().trim() + "_" + FileUtils.getDateTimeSystem();

                    // Create output file
                    generalFile = new File(path + filename + ".txt");

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
                    // Stop Location
                    stopLocation();

                    btnActivate.setText(getString(R.string.btn_activate));
                    btnActivate.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
                    btnActivate.setTextColor((ResourcesCompat.getColor(getResources(), R.color.white, null)));

                    // Remove the Accelerometer Sensor Register
                    sensorAccelerometerManager.unregisterListener(MainActivity.this);

                    // Stop the chronometer
                    timer.stop();
                    timer.setBase(SystemClock.elapsedRealtime());
                }

                // Change the status of the Activate button
                isEnableActivateButton = !isEnableActivateButton;

                // Setting Boolean Variables
                isFirstWrite                  = true;
                isFirstPotholeWrite           = true;
                isFirstSpeedBumpWrite         = true;
            }
        });
        //endregion

        // Keep the screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                SmartLocation.with(MainActivity.this).location().start(locationListener);
                Log.i("LUNAR",
                        String.format("Latitude %.6f, Longitude %.6f",
                                latitude,
                                longitude
                        )
                );
            }
        };

        locationListener = new OnLocationUpdatedListener()
        {
            @Override
            public void onLocationUpdated(Location location)
            {
                latitude  = location.getLatitude();
                longitude = location.getLongitude();
                handler.postDelayed(locationRunnable,500);
            }
        };

        // Get last location
        getLastLocation();
    }

    private void InitializeAWSFields()
    {
        awsCredentials = new AWSCredentials()
        {
            @Override
            public String getAWSAccessKeyId() {
                return getString(R.string.aws_access_key);
            }

            @Override
            public String getAWSSecretKey() {
                return getString(R.string.aws_secret_key);
            }
        };

        sqsClient = new AmazonSQSClient(awsCredentials);
    }



    public void onCheckboxClick(View view)
    {
        boolean checked = ((CheckBox) view).isChecked();

        switch(view.getId())
        {
            case R.id.cb_csv:
                if (checked)
                {
                    sqsClient = null;

                    isEnableSQSMode = false;
                    cbSQS.setChecked(false);
                }

                break;

            case R.id.cb_sqs:
                if (checked)
                {
                    isEnableSQSMode = true;
                    cbCSV.setChecked(false);

                    // Initialize AWS Fields
                    InitializeAWSFields();
                }
                else
                {
                    sqsClient       = null;
                    isEnableSQSMode = false;
                }

                break;
        }
    }

    //region Task Classes
    private class SendMessageAsyncTask extends AsyncTask<Message, Void, Integer>{

        @Override
        protected Integer doInBackground(Message... messageObj) {

            // Get messages from list
            List<String> messages = messageObj[0].messagesList;

            // Get queue url
            String queueUrl = messageObj[0].client.getQueueUrl(getString(R.string.queuename)).getQueueUrl();

            int msgCounter = 0;

            // Iterate over all messages
            for (Iterator<String> message = messages.iterator(); message.hasNext();)
            {
                messageObj[0].client.sendMessage(new SendMessageRequest(queueUrl, message.next()));
                msgCounter++;
            }
            return msgCounter;
        }

        @Override
        protected void onPostExecute(Integer numberOfMessages){
            Log.d("PDG_LUNAR", "Processed Message: " + numberOfMessages);
        }
    }

    private static class Message {
        AmazonSQSClient client;
        List<String>    messagesList;

        Message(AmazonSQSClient client, List<String> messagesList)
        {
            this.client       = client;
            this.messagesList = messagesList;
        }
    }
    //endregion

    private void getPermissions()
    {
        // Check if location permission wasn't granted
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

        // Check if storage permission wasn't granted
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

    @Override
    protected void onResume()
    {
        super.onResume();

        if (isEnableSQSMode)
            // Initialize AWS
            InitializeAWSFields();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        sensorAccelerometerManager.unregisterListener(this);
        SmartLocation.with(MainActivity.this).location().stop();
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            getSensorData(event);
    }

    private void activateSensorAcecelerometer()
    {
        sensorAccelerometerManager.registerListener(this,sensorAccelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void getSensorData(SensorEvent event)
    {

        tvAccelerometerX.setText("X: " + event.values[0]);
        tvAccelerometerY.setText("Y: " + event.values[1]);
        tvAccelerometerZ.setText("Z: " + event.values[2]);

        // Add textViews into listTv to extract accelerometer's value after that
        listTv.clear();
        listTv.add(tvAccelerometerX);
        listTv.add(tvAccelerometerY);
        listTv.add(tvAccelerometerZ);

        // Get current Time
        curTime = System.currentTimeMillis();

        // Make sure you've waited the time to save the data
        if ((curTime - lastUpdate) > TIME_TO_SAVE_DATA)
        {
            int tilt = getTilt(event.values);

            // Is the mobile device on an acceptable slope?
            if (Math.abs(tilt) > LIMIT_TILT)
            {

                if (isEnableSQSMode)
                {
                    // Format data
                    String jsonString = formatToJsonString((SystemClock.elapsedRealtime() - timer.getBase()), 0, tilt);

                    // Send data to SQS
                    new SendMessageAsyncTask().execute(new Message(sqsClient, new ArrayList<>(Arrays.asList(jsonString))));

                }
                else
                {
                    FileUtils.saveData(generalFile, formatLineToCSV(listTv, tilt, (SystemClock.elapsedRealtime() - timer.getBase())));
                }
            }
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
    }

    public String formatLineToCSV(List<TextView> listTv, int tilt, long timestamp)
    {
        String data;
        StringBuilder line = new StringBuilder();

        // First written to file?
        // Add Header
        if (isFirstWrite)
        {
            line.append("accelerometer_X" + DELIMITER +
                        "accelerometer_Y" + DELIMITER +
                        "accelerometer_Z" + DELIMITER +
                        "latitude"        + DELIMITER +
                        "longitude"       + DELIMITER +
                        "tilt"            + DELIMITER +
                        "timestamp"       + DELIMITER +
                        "datetime\n");

            isFirstWrite = false;
        }

        // Iterate over all TextViews
        for (TextView tV : listTv)
        {
            data = tV.getText().toString().substring(2).trim();

            // Check if the number is in scientific formatting. If so, it will have to be converted
            if (data.contains("E") == true)
                data = new BigDecimal(data).toString();

            line.append(data);
            line.append(DELIMITER);
        }

        line.append(String.valueOf(latitude));
        line.append(DELIMITER);
        line.append(String.valueOf(longitude));
        line.append(DELIMITER);
        line.append(String.valueOf(tilt));
        line.append(DELIMITER);
        line.append(timestamp);
        line.append(DELIMITER);
        line.append(new DateTime(DateTimeZone.UTC).toString());

        return line.toString();
    }


    public String formatToJsonString(long time, int id)
    {
        //id -> 0 No Event
        //id -> 1 Pothole
        //id -> 2 SpeedBump

        listTv.clear();
        listTv.add(tvAccelerometerX);
        listTv.add(tvAccelerometerY);
        listTv.add(tvAccelerometerZ);

        JSONObject   jsonobj        = new JSONObject();
        List<String> accelerometers = new ArrayList<>();

        float[] values = new float[3];
        try
        {
            int counter = 0;
            // Iterate over all TextViews (accelerometers)
            for (TextView tV : listTv)
            {
                String data = tV.getText().toString().substring(2).trim();

                // Check if the number is in scientific formatting. If so, it will have to be converted
                if (data.contains("E") == true)
                    data = new BigDecimal(data).toString();

                values[counter] = Float.parseFloat(data);
                accelerometers.add(data);

                counter++;
            }

            // Add into json object if i have all values
            if (accelerometers.size() == 3)
            {
                int tilt = getTilt(values);

                // Is the mobile device on an acceptable slope?
                if (Math.abs(tilt) < LIMIT_TILT)
                {
                    jsonobj.put("Accelerometer_X", accelerometers.get(0));
                    jsonobj.put("Accelerometer_Y", accelerometers.get(1));
                    jsonobj.put("Accelerometer_Z", accelerometers.get(2));
                    jsonobj.put("Latitude"       , latitude);
                    jsonobj.put("Longitude"      , longitude);
                    jsonobj.put("Timestamp"      , time);
                    jsonobj.put("Tilt"           , tilt);
                    jsonobj.put("AcquireDate"    , new DateTime(DateTimeZone.UTC));
                    jsonobj.put("Output"         , id);
                }
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
            Log.e("LUNAR","Error to build json object. Message: " + e.getMessage());
        }

        return jsonobj.toString();

    }

    public String formatToJsonString(long time, int id, int tilt)
    {
        //id -> 0 No Event
        //id -> 1 Pothole
        //id -> 2 SpeedBump

        listTv.clear();
        listTv.add(tvAccelerometerX);
        listTv.add(tvAccelerometerY);
        listTv.add(tvAccelerometerZ);

        JSONObject   jsonobj        = new JSONObject();
        List<String> accelerometers = new ArrayList<>();

        try
        {
            // Iterate over all TextViews (accelerometers)
            for (TextView tV : listTv)
            {
                String data = tV.getText().toString().substring(2).trim();

                // Check if the number is in scientific formatting. If so, it will have to be converted
                if (data.contains("E") == true)
                    data = new BigDecimal(data).toString();

                accelerometers.add(data);
            }

            // Add into json object if i have all values
            if (accelerometers.size() == 3)
            {
                jsonobj.put("Accelerometer_X", accelerometers.get(0));
                jsonobj.put("Accelerometer_Y", accelerometers.get(1));
                jsonobj.put("Accelerometer_Z", accelerometers.get(2));
                jsonobj.put("Latitude"       , latitude);
                jsonobj.put("Longitude"      , longitude);
                jsonobj.put("Timestamp"      , time);
                jsonobj.put("Tilt"           , tilt);
                jsonobj.put("AcquireDate"    , new DateTime(DateTimeZone.UTC));
                jsonobj.put("Output"         , id);
            }

        }
        catch (JSONException e)
        {
            e.printStackTrace();
            Log.e("LUNAR","Error to build json object. Message: " + e.getMessage());
        }

        return jsonobj.toString();

    }

    public String formatEventLineToCSV(long timer, int id)
    {
        //id -> 1 Pothole
        //id -> 2 SpeedBump

        StringBuilder line = new StringBuilder();

        if (id == 1 && isFirstPotholeWrite)
        {
            line.append("latitude;longitude;timestamp;datetime\n");
            isFirstPotholeWrite = false;
        }

        if (id == 2 && isFirstSpeedBumpWrite)
        {
            line.append("latitude;longitude;timestamp;datetime\n");
            isFirstSpeedBumpWrite = false;
        }

        line.append(String.valueOf(latitude));
        line.append(DELIMITER);
        line.append(String.valueOf(longitude));
        line.append(DELIMITER);
        line.append(timer);
        line.append(DELIMITER);
        line.append(new DateTime(DateTimeZone.UTC).toString());

        return line.toString();
    }

    private void getLastLocation()
    {
        Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        if (lastLocation != null)
        {
            latitude  = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();

            Log.i("LUNAR",
                    String.format("[From Cache] Latitude %.6f, Longitude %.6f",
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude()
                    )
            );
        }
    }
    private void startLocation()
    {
        SmartLocation.with(this).location().start(locationListener);
    }

    private void stopLocation()
    {
        SmartLocation.with(this).location().stop();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}

