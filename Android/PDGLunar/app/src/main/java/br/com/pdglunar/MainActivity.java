package br.com.pdglunar;

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
    Button btnPotHole;
    Button btnSpeedBump;

    SensorManager sensorAccelerometerManager;

    double   longitude;
    double   latitude;

    TextView tvAccelerometerX;
    TextView tvAccelerometerY;
    TextView tvAccelerometerZ;

    EditText eT_filename;

    List<TextView> listTv;

    // Caminho a serem salvos os dados
    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TCC-Lunar/";

    String filename = "";

    File file, fileBuraco, fileQuebraMola;

    long curTime;
    long lastUpdate = 0;

    boolean isBtnAtivarEnabled;
    boolean isFirstWrite;
    boolean isFirstHoleWrite;
    boolean isFirstSpeedBumpWrite;
    boolean isFirstClickHoleButton      = true;
    boolean isFirstClickSpeedBumpButton = true;


    Chronometer timer;

    boolean gps_permission;
    boolean isBroadcastRegistred = false;
    private static final int REQUEST_PERMISSIONS = 100;

    SharedPreferences mPref;
    SharedPreferences.Editor medit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region Declaração Variáveis do Layout
        tvAccelerometerX = (TextView) findViewById(R.id.tv_accelerometer_X);
        tvAccelerometerY = (TextView) findViewById(R.id.tv_accelerometer_Y);
        tvAccelerometerZ = (TextView) findViewById(R.id.tv_accelerometer_Z);

        eT_filename      = (EditText) findViewById(R.id.et_filename);
        btnActivate      = (Button)   findViewById(R.id.btn_activate);
        btnPotHole       = (Button)   findViewById(R.id.btn_pothole);
        btnSpeedBump     = (Button)   findViewById(R.id.btn_speedbump);

        timer            = (Chronometer) findViewById(R.id.chronometer);
        //endregion

        // Formatando o cronômetro
        timer.setTextColor(Color.RED);
        timer.setFormat(">> %s <<");


        mPref            = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        medit            = mPref.edit();

        isBtnAtivarEnabled          = false;
        sensorAccelerometerManager  = (SensorManager) getSystemService(SENSOR_SERVICE);

        listTv = new ArrayList<>();

        // Checa se SDCard está disponível para escrita
        if (FileUtils.isExternalStorageWritable()) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TCC-Lunar/";
        }

        // Checa se o diretório existe
        file = new File(path);
        if (!file.isDirectory())
            file.mkdir();


        // Solicita a permissão do Aplicativo ao usuário
        getPermission();

        //region Listener do botão Buraco
        btnPotHole.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Verifica se o processo de captura não está ativo
                if (!isBtnAtivarEnabled)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formata o dado
                String data = formatSpeedBumpHoleLineToSaveTxtFile((SystemClock.elapsedRealtime() - timer.getBase()),0);

                // Salva o dado
                FileUtils.SaveData(fileBuraco,data);

                // Verifica se é o primeiro click
                // Isto é checado para inserir ou não o header no arquivo de saída
                if (isFirstClickHoleButton)
                    isFirstClickHoleButton = false;

                // Info Message
                Toast.makeText(MainActivity.this,"Buraco adicionado",Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Listener do botão Quebra-Mola
        btnSpeedBump.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Verifica se o processo de captura não está ativo
                if (!isBtnAtivarEnabled)
                {
                    Toast.makeText(MainActivity.this,"Precisa iniciar a captura antes!",Toast.LENGTH_SHORT).show();
                    return;
                }

                // Formata o dado
                String data = formatSpeedBumpHoleLineToSaveTxtFile((SystemClock.elapsedRealtime() - timer.getBase()),1);

                // Salva o dado
                FileUtils.SaveData(fileQuebraMola, data);

                // Verifica se é o primeiro click
                // Isto é checado para inserir ou não o header no arquivo de saída
                if (isFirstClickSpeedBumpButton)
                    isFirstClickSpeedBumpButton = false;

                // Info Message
                Toast.makeText(MainActivity.this,"Quebra mola adicionado",Toast.LENGTH_SHORT).show();
            }
        });
        //endregion

        //region Listener do botão Ativar
        btnActivate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Registra o serviço caso não esteja
                if (!isBroadcastRegistred)
                {
                    registerReceiver(broadcastReceiver, new IntentFilter(GPSService.str_receiver));
                    isBroadcastRegistred = true;
                }

                // Verifica se o processo de captura não está ativo
                if (!isBtnAtivarEnabled) {

                    // Checa se o GPS possui permissão para utilizar
                    if (gps_permission) {

                        if (mPref.getString("service", "").matches(""))
                        {
                            medit.putString("service", "service").commit();

                            Intent intent = new Intent(getApplicationContext(), GPSService.class);
                            startService(intent);

                        }
                        else
                            Toast.makeText(getApplicationContext(), "Service já está rodando", Toast.LENGTH_SHORT).show();
                    }

                    else
                    {
                        Toast.makeText(getApplicationContext(), "Por favor, ative o GPS!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Captura e formata o nome do arquvivo de saída
                    if (eT_filename.getText().toString().trim().equals(""))
                        filename = "Lunar" + "_" + FileUtils.getDateTimeSystem();
                    else
                        filename = eT_filename.getText().toString().trim() + "_" + FileUtils.getDateTimeSystem();

                    // Cria o arquivo de saída
                    file = new File(path + filename + ".txt");

                    // Seta variáveis booleanas
                    isFirstWrite                  = true;
                    isFirstHoleWrite              = true;
                    isFirstSpeedBumpWrite         = true;
                    isFirstClickHoleButton        = true;
                    isFirstClickSpeedBumpButton   = true;

                    // Reinicia o cronômetro
                    timer.setBase(SystemClock.elapsedRealtime());
                    timer.start();

                    // Formatar o botão Ativar
                    btnActivate.setText("PARAR");
                    btnActivate.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
                    btnActivate.setTextColor((ResourcesCompat.getColor(getResources(), R.color.black, null)));

                    // Cria os arquivos de saída para Buraco e Quebra-Mola
                    fileBuraco      = new File(path + "Buraco"     + "_" + eT_filename.getText().toString().trim() + "_" + FileUtils.getDateTimeSystem() + ".txt");
                    fileQuebraMola  = new File(path + "QuebraMola" + "_" + eT_filename.getText().toString().trim() + "_" + FileUtils.getDateTimeSystem() + ".txt");

                    // Ativa o Sensor Acelerômetro
                    activateSensorAcecelerometer();
                }

                // Caso o usuário interrompa a captura dos dados
                else
                {
                    btnActivate.setText(getString(R.string.btn_activate));
                    btnActivate.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
                    btnActivate.setTextColor((ResourcesCompat.getColor(getResources(), R.color.white, null)));

                    // Remove o registro do sensor Acelerômetro
                    sensorAccelerometerManager.unregisterListener(MainActivity.this);

                    // Remove o registro do BroadCast
                    if (isBroadcastRegistred)
                    {
                        unregisterReceiver(broadcastReceiver);
                        isBroadcastRegistred = false;
                    }

                    if (medit != null) {
                        medit.clear();
                        medit.commit();
                    }

                    // Seta variáveis booleanas
                    isFirstWrite                  = true;
                    isFirstHoleWrite              = true;
                    isFirstSpeedBumpWrite         = true;

                    // Interrompe o cronômetro
                    timer.stop();
                    timer.setBase(SystemClock.elapsedRealtime());
                }

                // Altera o status do botão Ativar
                isBtnAtivarEnabled = !isBtnAtivarEnabled;
            }
        });
        //endregion

    }

    private void getPermission() {

        // Checa se a permissão do aplicativo foi atendida
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {

            if  ((ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)))
            { }
            else
            {
                // Solicita a permissão
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_PERMISSIONS);
            }

        }
        else
        {
            gps_permission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    gps_permission = true;
                }
                else {
                    Toast.makeText(getApplicationContext(), "Por favor, aceite a permissão!", Toast.LENGTH_LONG).show();
                }
            }
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
            unregisterReceiver(broadcastReceiver);
            isBroadcastRegistred = false;
        }
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

        curTime = System.currentTimeMillis();

        // Checa se já passou o tempo necessário para salvar os dados
        if ((curTime - lastUpdate) > TIME_TO_SAVE_DATA) {
            FileUtils.SaveData(file, formatLineToSaveTxtFile(listTv,(SystemClock.elapsedRealtime() - timer.getBase())));
            lastUpdate = curTime;
        }
    }

    public String formatLineToSaveTxtFile(List<TextView> listTv, long time) {
        String data;
        StringBuilder line = new StringBuilder();

        // Primeira escrita no arquivo?
        // Adiciona o Header
        if (isFirstWrite) {
            line.append("accelerometer_X;accelerometer_Y;accelerometer_Z;latitude;longitude;timestamp\n");
            isFirstWrite = false;
        }

        // Iteração em todos os textViews
        for (TextView tV : listTv) {
            data = tV.getText().toString().substring(2).trim();

            // Formata o numero caso esteja em notação científica
            if (data.contains("E") == true)
                data = new BigDecimal(data).toString();

            line.append(data);
            line.append(";");
        }

        line.append(String.valueOf(latitude));
        line.append(";");
        line.append(String.valueOf(longitude));
        line.append(";");
        line.append(time);

        return line.toString();
    }

    public String formatSpeedBumpHoleLineToSaveTxtFile(long timer, int id) {
        //id -> 0 buraco ,  id -> 1 quebra-mola
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

