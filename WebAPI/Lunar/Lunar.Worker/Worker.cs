using Amazon;
using Amazon.SQS;
using Amazon.SQS.Model;
using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using WebUtilsLib;

namespace Lunar.Worker
{
    public class Worker
    {
        private static string               AWSAccessKey;
        private static string               AWSSecretKey;
        private static string               ToBeProcessedQueueName;
        private static string               ProcessedQueueName;
        private static SQSUtils             ToBeProcessedQueue;
        private static SQSUtils             ProcessedQueue;
        private static string               GoogleReverseGeocodingKey;
        private static string               GoogleReverseGeocodingUrlTemplate = "https://maps.googleapis.com/maps/api/geocode/json?latlng={0},{1}&key={2}&language=pt-BR";

        static void Main(string[] args)
        {
            // Load config
            Console.WriteLine("Loading config file");
            if (!InitAppConfigValues())
            {
                Console.Read();
                Environment.Exit(-101);
            }

            // Initialize AWS Services
            if (!InitAWSServices())
            {
                Console.WriteLine("Error to initialize AWS services.");
                Console.Read();
                Environment.Exit(-102);
            }

            string errorMessage;
            while (true)
            {
                // Get messages from SQS
                List<Message> messages = ToBeProcessedQueue.GetMessagesFromQueue(out errorMessage);

                if (messages == null || messages.Count == 0)
                {
                    Console.WriteLine("Do not have messages to be process!...");
                    Thread.Sleep(1000 * 60);
                }
                else
                {
                    int countProcessedMessages = 0;
                    foreach (Message message in messages)
                    {
                        // Go to process messages
                        LunarObject lunarObject = ProcessMessage(message);
                   
                        if (lunarObject != null)
                        {
                            // Send to ProcessedQueue
                            SendToProcessedQueue(lunarObject);

                            // Trace Message
                            if (countProcessedMessages % 10 == 0)
                                Console.WriteLine("Already processed {0} messages", (++countProcessedMessages));
                        }

                        
                        // Delete processed message
                        if(!ToBeProcessedQueue.DeleteMessage(message, out errorMessage))
                        {
                            Console.WriteLine("Error to delete message. Error Message:{0}", errorMessage);
                        }
                    }
                }
            }   
        }

        private static void SendToProcessedQueue(LunarObject obj)
        {
            string errorMessage = String.Empty;
            
            // Convert the object to a compressed string
            string jsonstr = Compression.Compress(obj.AsJSONString());

            // Add obj to queue
            if (!ProcessedQueue.EnqueueMessage(jsonstr, out errorMessage))
            {
                Console.WriteLine("Error to enqueue message. Error Message:{0}", errorMessage);
            }
        }

        private static bool InitAppConfigValues()
        {
            try
            {
                // AWS Keys
                AWSAccessKey            = Utils.LoadConfigurationSetting("AWSAccessKey", "");
                AWSSecretKey            = Utils.LoadConfigurationSetting("AWSSecretKey", "");

                // SQS                                    
                ToBeProcessedQueueName  = Utils.LoadConfigurationSetting("ToBeProcessedQueueName", "");
                ProcessedQueueName      = Utils.LoadConfigurationSetting("ProcessedQueueName", "");

                // Initialize SQS object
                ToBeProcessedQueue      = new SQSUtils(AWSAccessKey, AWSSecretKey, ToBeProcessedQueueName);
                ProcessedQueue          = new SQSUtils(AWSAccessKey, AWSSecretKey, ProcessedQueueName);

                // Reverse Geocoding Key
                GoogleReverseGeocodingKey = Utils.LoadConfigurationSetting("ReverseGeocodingApiKey", "");


            }
            catch (Exception ex)
            {
                Console.WriteLine("Error while parsing app config values. Error Message: {0}", ex.Message);
                return false;
            }

            return true;
        }

        private static bool InitAWSServices()
        {
            bool result = true;

            string tobeprocessederrormessage = String.Empty;
            string processederrormessage     = String.Empty;

            try
            {
                // Is there ToBeProcessed Queue?
                if (!ToBeProcessedQueue.OpenQueue(1, out tobeprocessederrormessage))
                    result = false;

                // Is there Processed Queue?
                if (!ProcessedQueue.OpenQueue(1, out processederrormessage))
                    result = false;
            }
            catch (Exception ex) { result = false; Console.WriteLine("ErrorMessage:{1} ... {2} \t {3}", tobeprocessederrormessage, processederrormessage, ex.Message); }

            return result;
        }

        private static LunarObject ProcessMessage(Message message)
        {
            LunarObject lunarObj = null;

            try
            {
                // Decompress and Deserialize message
                MobileRecordObject mobileObj = JsonConvert.DeserializeObject<MobileRecordObject>(message.Body);

                // Get OutputId
                mobileObj.OutputId = mobileObj.ExtractOutputIdFromInt(mobileObj.Output);

                if (mobileObj.Latitude != 0.0 && mobileObj.Longitude != 0.0 && mobileObj.Output != 0)
                    // Extract Address from Google Reverse Geocoding API
                    lunarObj = ExtractAddressFromGPSCoordinates(mobileObj);

                return lunarObj;

            }
            catch (Exception ex)
            {
                Console.WriteLine(String.Format("Error while treating message from {0}: {1}", ToBeProcessedQueueName, ex.Message));
                return lunarObj;
            }
        }

        private static LunarObject ExtractAddressFromGPSCoordinates (MobileRecordObject mobileObj)
        {
            // Sanity check
            if (mobileObj.Latitude == 0 || mobileObj.Longitude == 0)
                return null;

            // Build the url
            string finalUrl = String.Format(GoogleReverseGeocodingUrlTemplate, mobileObj.Latitude.ToString().Replace(",","."), mobileObj.Longitude.ToString().Replace(",", "."), GoogleReverseGeocodingKey);


            WebRequests client = new WebRequests();

            // GET Request
            string jsonstr = Get(ref client, finalUrl);

            // Checking if jsonstr is valid
            if (String.IsNullOrEmpty(jsonstr))
                return null;

            try
            {
                GoogleReverseGeocondingObject rcObj = JsonConvert.DeserializeObject<GoogleReverseGeocondingObject>(jsonstr);

                if (rcObj.status.ToUpper().Equals("OK") && rcObj.results != null && rcObj.results.Count >= 1)
                {
                    LunarObject lunarObject     = new LunarObject();
                    lunarObject.Address         = rcObj.results[0].formatted_address;
                    lunarObject.MobileObject    = mobileObj;

                    return lunarObject;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error to deserialize GoogleReverseGeocondingObject. Message: {0}", ex.Message);
            }

            return null;
        }


        public static string Get(ref WebRequests client, string url, int retries = 5)
        {
            string htmlResponse = String.Empty;
           
            do
            {
                try
                {
                    Uri uri = new Uri(url);
                    client.Host = uri.Host;

                    // Get html of the current category main page
                    htmlResponse = client.Get(url, true);
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex);
                }

                // Sanity check
                if (!String.IsNullOrWhiteSpace(htmlResponse) && client.StatusCode == HttpStatusCode.OK)
                {
                    break;
                }

                retries -= 1;

                Console.WriteLine(String.Format("Status Code not OK. Retries left: {0}", retries));

                Console.WriteLine("StatusCode = " + client.StatusCode + " Message = " + client.Error);

                Console.WriteLine("Html Response = " + htmlResponse);

                // Polite Sleeping
                Thread.Sleep(2000);

            } while (retries >= 0);

            return htmlResponse;
        }

    }
}
