package br.com.pdglunar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class TrackGPS extends Service implements LocationListener {

    Context mContext = null;

    Location loc;
    double   latitude;
    double   longitude;

    boolean checkGPS        = false;
    boolean checkNetwork    = false;
    boolean canGetLocation  = false;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

    // Tempo mínimo para atualização - 500ms
    private static final long MIN_TIME_BW_UPDATES = 500 * 1 * 1;

    protected LocationManager locationManager;

    public TrackGPS(Context mContext) {
        this.mContext = mContext;

        if (ActivityCompat.checkSelfPermission(this.mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this.mContext,"Não tem permissão!",Toast.LENGTH_SHORT).show();
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            return;
        }

        // Captura a Localização atual
        getLocation();
    }
    public TrackGPS() {}

    private Location getLocation() {

        try
        {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // getting GPS status
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Verifica se internet móvel e GPS estão disponibilizados
            if (!checkGPS && !checkNetwork)
                Toast.makeText(mContext, "No Service Provider Available", Toast.LENGTH_SHORT).show();
            else
            {
                this.canGetLocation = true;

                // Checa a internet
                if (checkNetwork)
                {
                    try
                    {
                        // Trace Message
                        Log.i("PDG_LUNAR", "Capturando pela Internet Móvel");

                        // Faz o request da atualização de localização
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        // Info Message
                        Log.i("PDG_LUNAR", "Network");

                        if (locationManager != null)
                        {
                            // Captura a última localização conhecida
                            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            // Trace Message
                            Log.i("PDG_LUNAR", "Pegou coordenadas atualizadas");
                        }

                        // Extrai as novas coordenadas
                        if (loc != null)
                        {
                            latitude  = loc.getLatitude();
                            longitude = loc.getLongitude();
                        }
                    }
                    catch(SecurityException e){ }
                }
            }

            // Checa se é possível capturar as coordenadas através do GPS
            if (checkGPS)
            {
                if (loc == null)
                {
                    try
                    {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        // Trace Message
                        Log.i("PDG_LUNAR", "Capturando pelo GPS");

                        if (locationManager != null)
                        {
                            // Captura a última localização conhecida
                            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            // Extrai as novas coordenadas
                            if (loc != null)
                            {
                                latitude  = loc.getLatitude();
                                longitude = loc.getLongitude();
                            }
                        }
                    } catch (SecurityException e) { }
                }
            }

        } catch (Exception e) { e.printStackTrace(); }

        return loc;
    }

    public double getLongitude()
    {
        if (loc != null) {
            longitude = loc.getLongitude();
        }
        return longitude;
    }

    public double getLatitude()
    {
        if (loc != null)
            latitude = loc.getLatitude();

        return latitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }


    public void stopUseGPS()
    {
        if (locationManager != null)
            locationManager.removeUpdates(TrackGPS.this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude    = location.getLatitude();
        longitude   = location.getLongitude();

        // Info Message
        Log.i("PDG_LUNAR", "Localização Alterada .: Latitude: " + latitude + ", Longitude: " + longitude);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }

    @Override
    public void onProviderEnabled(String s) { }

    @Override
    public void onProviderDisabled(String s) { }

}