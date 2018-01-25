using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;

namespace Lunar.SharedLibrary.Utils
{
    public class Compression
    {
        /// <summary>
        /// The method compresses the value string using the GZipStream. The function returns
        /// the compressed representation of value. The first four bytes of the result
        /// contain the size of the compressed string
        /// </returns>
        public static string Compress(string value)
        {
            // Valid input?
            byte[] gzBuffer;
            if (string.IsNullOrWhiteSpace(value))
            {
                gzBuffer = Enumerable.Repeat<byte>(0, 4).ToArray<byte>();
                return Convert.ToBase64String(gzBuffer);
            }

            // Copy value to compressed stream
            byte[] buffer = Encoding.UTF8.GetBytes(value);
            MemoryStream ms = new MemoryStream();
            using (GZipStream zip = new GZipStream(ms, CompressionMode.Compress, true))
                zip.Write(buffer, 0, buffer.Length);
            ms.Position = 0;

            // Copy compressed string to result
            byte[] compressed = new byte[ms.Length];
            ms.Read(compressed, 0, compressed.Length);
            gzBuffer = new byte[compressed.Length + 4];
            System.Buffer.BlockCopy(compressed, 0, gzBuffer, 4, compressed.Length);
            System.Buffer.BlockCopy(BitConverter.GetBytes(buffer.Length), 0, gzBuffer, 0, 4);
            return Convert.ToBase64String(gzBuffer);
        }

        /// <summary>
        /// The method decompress the "compressedvalue" string. The first four bytes of the
        /// compressed string must contains its size.
        /// </summary>
        /// <param name="compressedvalue">Compressed string</param>
        /// <returns>
        /// The decompressed representation of "compressedvalue"
        /// </returns>
        public static string Decompress(string compressedvalue)
        {
            // Empty compressedvalue?
            if (compressedvalue.Length == 4)
                return string.Empty;

            // Decompress
            byte[] gzBuffer = Convert.FromBase64String(compressedvalue);
            using (MemoryStream ms = new MemoryStream())
            {
                int msgLength = BitConverter.ToInt32(gzBuffer, 0);
                ms.Write(gzBuffer, 4, gzBuffer.Length - 4);
                byte[] buffer = new byte[msgLength];
                ms.Position = 0;
                using (GZipStream zip = new GZipStream(ms, CompressionMode.Decompress))
                {
                    zip.Read(buffer, 0, buffer.Length);
                }
                return Encoding.UTF8.GetString(buffer);
            }
        }
    }
}