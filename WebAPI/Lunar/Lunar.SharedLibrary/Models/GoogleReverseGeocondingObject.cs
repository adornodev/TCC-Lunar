using MongoDB.Bson.Serialization.Attributes;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Lunar.SharedLibrary.Models
{
    [BsonIgnoreExtraElements]
    public class GoogleReverseGeocondingObject
    {

        public List<Result> results         { get; set; }
        public string       status          { get; set; }

        [BsonIgnoreExtraElements]
        public class Result
        {
            public string formatted_address { get; set; }
        }

    }
}
