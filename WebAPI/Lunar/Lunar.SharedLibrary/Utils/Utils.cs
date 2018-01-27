using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Lunar.SharedLibrary.Utils
{
    public static class Utils
    {
        public static string LoadConfigurationSetting(string keyname, string defaultvalue)
        {
            string result = defaultvalue;
            try
            {
                result = ConfigurationManager.AppSettings[keyname];
            }
            catch
            {
                result = defaultvalue;
            }
            if (result == null)
                result = defaultvalue;
            return result;
        }

        public static int CountOccurences(string substring, string source)
        {
            int px = 0;
            int count = 0;
            while ((px = source.IndexOf(substring, px, StringComparison.OrdinalIgnoreCase)) != -1)
            {
                px += substring.Length;
                count++;
            }

            return count;
        }

        public static bool IsValidJson(string strInput)
        {
            strInput = strInput.Trim();
            if ((strInput.StartsWith("{") && strInput.EndsWith("}")) || //For object
                (strInput.StartsWith("[") && strInput.EndsWith("]")))   //For array
            {

                JToken obj = JToken.Parse(strInput);
                return true;
            }
            else
                return false;
        }
    }
}
