package finalcourseassignment.ufrj.android_lunar.utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

public class CompressionUtils
{
    public static String Compress(byte[] input)
    {
        ByteArrayOutputStream stream              = new ByteArrayOutputStream();
        Deflater             compresser           = new Deflater(Deflater.BEST_COMPRESSION, true);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream, compresser);
        try
        {
            deflaterOutputStream.write(input);
            deflaterOutputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return new String(stream.toByteArray());
    }

    public static String Decompress(byte[] input)
    {
        ByteArrayOutputStream   stream                = new ByteArrayOutputStream();
        Inflater                decompresser         = new Inflater(true);
        InflaterOutputStream    inflaterOutputStream = new InflaterOutputStream(stream, decompresser);

        try
        {
            inflaterOutputStream.write(input);
            inflaterOutputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return new String(stream.toByteArray());
    }
}
