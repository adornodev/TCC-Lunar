using Lunar.SharedLibrary.Utils;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using Newtonsoft.Json;
using System;

namespace Lunar.SharedLibrary.Models
{
    [BsonIgnoreExtraElements]
    public class MobileRecordObject
    {
        public ObjectId     _id             { get; set; }
        public DateTime     AcquireDate     { get; set; }
        public double       Accelerometer_X { get; set; }
        public double       Accelerometer_Y { get; set; }
        public double       Accelerometer_Z { get; set; }
        public double       Latitude        { get; set; }
        public double       Longitude       { get; set; }
        public long         Timestamp       { get; set; }
        public int          Tilt            { get; set; }

        public int          Output          { get; set; }

        public string       OutputId        { get; set; }

        public MobileRecordObject()
        {
            this.AcquireDate     = DateTime.UtcNow;
            this.Accelerometer_X = Double.MinValue;
            this.Accelerometer_Y = Double.MinValue;
            this.Accelerometer_Z = Double.MinValue;
            this.Latitude        = Double.MinValue;
            this.Longitude       = Double.MinValue;
            this.Output          = int.MinValue;
            this.Tilt            = int.MinValue;
        }

        public string ExtractOutputIdFromInt(int output)
        {
            switch (output)
            {
                case 0:
                    return "NotClassified";
                case 1:
                    return "Pothole";
                case 2:
                    return "SpeedBump";
            }

            return "Unknown";
        }

        public string AsJSONString()
        {
            return JsonConvert.SerializeObject(this);
        }
    }
}
