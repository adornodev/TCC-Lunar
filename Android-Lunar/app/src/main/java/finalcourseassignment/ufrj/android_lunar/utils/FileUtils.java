package finalcourseassignment.ufrj.android_lunar.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by USUARIO on 08-11-17.
 */

public class FileUtils
{
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

    public static void SaveData(File file, String data)
    {
        //Se falhou em pegar algum dado de um sensor, temos que impedir que seja salvo
        if (data.contains(";;") == true) //|| data.contains("0.0"))
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


    public static String getDateTimeSystem() {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(new Date());

        String result = calendar.get(Calendar.YEAR) + "_" +
                (calendar.get(Calendar.MONTH) + 1) + "_" +
                calendar.get(Calendar.DAY_OF_MONTH) + "_" +
                calendar.get(Calendar.HOUR) + "_" +
                calendar.get(Calendar.MINUTE) + "_" +
                calendar.get(Calendar.SECOND);

        return result;
    }
}
