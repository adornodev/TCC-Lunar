package finalcourseassignment.ufrj.android_lunar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.LOCATION_SERVICE;

public class TrackGPS implements LocationListener {

    Context mContext = null;

    Location location;
    double   latitude;
    double   longitude;

    boolean isGPSEnable        = false;
    boolean isNetworkEnable    = false;

    // Tempo mínimo para atualização 500ms
    private static final long MIN_TIME_BW_UPDATES = (long) (1000 * 0.5);

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

    @SuppressLint("MissingPermission")
    public Location getLocation()
    {

        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
        isGPSEnable        = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable    = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        try
        {
            // Verifica se internet móvel e GPS estão com algum problema de conexão
            if (!isGPSEnable && !isNetworkEnable) {
                Toast.makeText(mContext,"GPS e Internet não estão funcionando!",Toast.LENGTH_SHORT).show();
                return null;
                //mTimer.cancel();
                //mTimer.purge();
            }
            else
            {
                // Checa se é possível capturar as coordenadas através do GPS
                if (isGPSEnable)
                {
                    // Trace Message
                    Log.i("PDG_LUNAR", "Capturando pelo GPS");

                    location = null;

                    // Request para atualizar a localização
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            0, (LocationListener) mContext);



                    if (locationManager != null)
                    {
                        // Captura a última localização reconhecida
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        // Extrai as novas coordenadas
                        if (location != null)
                        {
                            // Trace Message
                            Log.i("PDG_LUNAR","[GPS] Latitude -> " + location.getLatitude()+ " , Longitude -> " + location.getLongitude());

                            latitude  = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                else {
                    // Is it network available?
                    if (isNetworkEnable)
                    {
                        // Trace Message
                        Log.i("PDG_LUNAR", "Capturando pela Internet Móvel");

                        // Faz o request da atualização de localização
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                0, (LocationListener) mContext);

                        if (locationManager != null)
                        {
                            // Captura a última localização reconhecida
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            // Extrai as novas coordenadas
                            if (location != null)
                            {
                                latitude  = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        return location;
    }

    public double getLongitude()
    {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public double getLatitude()
    {
        if (location != null)
            latitude = location.getLatitude();

        return latitude;
    }

    public void stopUseGPS()
    {
        if (locationManager != null)
            locationManager.removeUpdates(TrackGPS.this);
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