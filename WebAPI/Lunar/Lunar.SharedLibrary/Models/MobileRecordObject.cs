using Lunar.SharedLibrary.Utils;
using MongoDB.Bson;
using System;

namespace Lunar.SharedLibrary.Models
{
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

        public int             Output       { get; private set; }
        public Enums.Output    OutputId     { get { return _OutPut; } set { _OutPut = value; Output = EnumsHelper.OutputToInt(value); } }

        // Private attribute to convert Enum to Int
        private Enums.Output    _OutPut;


        public MobileRecordObject()
        {
            this.AcquireDate     = DateTime.UtcNow;
            this.Accelerometer_X = Double.MinValue;
            this.Accelerometer_Y = Double.MinValue;
            this.Accelerometer_Z = Double.MinValue;
            this.Tilt            = int.MinValue;
            this.Output          = -1;
            this.OutputId        = Enums.Output.NotClassified;

        }
    }
}
