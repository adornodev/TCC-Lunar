using Newtonsoft.Json;
using System;


namespace Lunar.SharedLibrary.Models
{
    public class LunarObject
    {
        public string             Address               { get; set; }
        public MobileRecordObject MobileObject          { get; set; }


        public LunarObject()
        {
            this.Address   = String.Empty;
        }

        public string AsJSONString()
        {
            return JsonConvert.SerializeObject(this);
        }
    }

}
