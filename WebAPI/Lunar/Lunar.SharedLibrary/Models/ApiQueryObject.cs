using MongoDB.Bson.Serialization.Attributes;
using Newtonsoft.Json;
using System;

namespace Lunar.SharedLibrary.Models
{
    [BsonIgnoreExtraElements]
    public class ApiQueryObject
    {
        private int _NumberOfDays;
        private int _Limit;

        [JsonProperty(PropertyName = "output")]
        public int      Output          { get; set; }

        [JsonProperty(PropertyName = "latitude")]
        public double   Latitude        { get; set; }

        [JsonProperty(PropertyName = "longitude")]
        public double   Longitude       { get; set; }

        [JsonProperty(PropertyName = "city")]
        public string   City            { get; set; }

        [JsonProperty(PropertyName = "state")]
        public string   State           { get; set; }

        [JsonProperty(PropertyName = "numberofdays")]
        public int      NumberOfDays    { get { return _NumberOfDays; } set {
                                                                                if (value > 30)
                                                                                {
                                                                                    _NumberOfDays = 30;
                                                                                }
                                                                                else if (value > 0 && value <= 30)
                                                                                    _NumberOfDays = value;
                                                                            }}

        [JsonProperty(PropertyName = "limit")]
        public int      Limit           { get { return _Limit; }        set {
                                                                                if (value > 1000)
                                                                                {
                                                                                    _Limit = 100;
                                                                                }
                                                                                else if (value > 0 && value <= 100)
                                                                                    _Limit = value;
                                                                            }}

        public ApiQueryObject()
        {
            this.Latitude       = Double.MinValue;
            this.Longitude      = Double.MinValue;
            this.Output         = int.MinValue;
            this.City           = String.Empty;
            this.State          = String.Empty;
            this._NumberOfDays  = 30;
            this._Limit         = 100;
        }
    }
}
