package br.com.pdglunar.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class GPSService extends Service implements LocationListener {

    boolean isGPSEnable     = false;
    boolean isNetworkEnable = false;

    double  latitude, longitude;

    // Variáveis relacionadas ao intervalos de notificação e atualização da localização
    long              notify_interval = 1000;
    private final int LOCATION_UPDATE = 1000;

    LocationManager locationManager;
    Location location;

    private Handler mHandler    = new Handler();
    private Timer mTimer      = null;

    Intent intent;

    public static String str_receiver = "service.receiver";
    public static boolean stopService = false;

    public GPSService() { }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 5, notify_interval);
        intent = new Intent(str_receiver);
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    private void getlocation()
    {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable     = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // Sanity Check
        if (stopService)
        {
            if (locationManager != null)
                locationManager.removeUpdates(GPSService.this);

            return;
        }

        // Trace Message
        Log.i("PDG_LUNAR","GPSService.getLocation()");

        // Verifica se o GPS e a Internet estão com problema de conexão
        if (!isGPSEnable && !isNetworkEnable) {
            Toast.makeText(this,"GPS e Internet não estão funcionando!",Toast.LENGTH_SHORT).show();
            mTimer.cancel();
            mTimer.purge();
        }
        else {

            // Is it network available?
            if (isNetworkEnable)
            {
                location = null;

                // Checa se há permissão para acessar o GPS do dispositivo
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    return;

                // Request para atualizar a localização
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE, 0, this);

                if (locationManager!=null)
                {
                    // Captura a última localização reconhecida
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    if (location!=null){

                        // Trace Message
                        Log.i("PDG_LUNAR","[Network] Latitude -> " + location.getLatitude()+ " , Longitude -> " + location.getLongitude());

                        latitude    = location.getLatitude();
                        longitude   = location.getLongitude();

                        // Atualiza a localização
                        update(location);
                    }
                }
            }

            // Verifica se o GPS está habilitado. A precisão é superior.
            if (isGPSEnable){

                location = null;

                // Request para atualizar a localização
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,LOCATION_UPDATE,0,this);

                if (locationManager!=null)
                {
                    // Captura a última localização reconhecida
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (location!=null)
                    {
                        // Trace Message
                        Log.i("PDG_LUNAR","[GPS] Latitude -> " + location.getLatitude()+ " , Longitude -> " + location.getLongitude());

                        latitude    = location.getLatitude();
                        longitude   = location.getLongitude();

                        // Atualiza a localização
                        update(location);
                    }
                }
            }
        }
    }

    private class TimerTaskToGetLocation extends TimerTask
    {
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getlocation();
                }
            });

        }
    }

    private void update(Location location)
    {
        // Insere os dados na Intent para serem capturados posteriormente pelo aplicativo
        intent.putExtra("latitude",location.getLatitude()+"");
        intent.putExtra("longitude",location.getLongitude()+"");

        // Envia a intent ao Broadcast
        sendBroadcast(intent);
    }
}


