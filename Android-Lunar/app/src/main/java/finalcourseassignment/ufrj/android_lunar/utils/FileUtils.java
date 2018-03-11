package finalcourseassignment.ufrj.android_lunar.utils;

import android.os.Environment;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils
{
    public static String delimiter = "_";

    public static boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;

        return false;
    }

    public static boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            return true;

        return false;
    }

    public static void saveData(File file, String data)
    {
        // Is there some problem with this data?
        if (data.contains(";;") == true)
            return;

        FileOutputStream fos = null;

        try
        {
            fos = new FileOutputStream(file, true);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // Sanity check
        if (fos == null)
        {
            Log.e("LUNAR", "FileOutputStream is null. File: " + file.getAbsolutePath());
            return;
        }

        try
        {
            fos.write(data.getBytes());
            fos.write("\n".getBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }


    public static String getDateTimeSystem()
    {

        DateTime      dt     = new DateTime(DateTimeZone.UTC);
        StringBuilder result = new StringBuilder();

        result.append(String.valueOf(dt.getYear()));
        result.append(delimiter);
        result.append(String.valueOf(dt.getMonthOfYear()));
        result.append(delimiter);
        result.append(String.valueOf(dt.getDayOfMonth()));
        result.append(delimiter);
        result.append(String.valueOf(dt.getHourOfDay()));
        result.append(delimiter);
        result.append(String.valueOf(dt.getMinuteOfHour()));
        result.append(delimiter);
        result.append(String.valueOf(dt.getSecondOfMinute()));

        return result.toString();
    }
}
