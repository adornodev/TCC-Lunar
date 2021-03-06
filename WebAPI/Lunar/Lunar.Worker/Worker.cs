﻿using Amazon;
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
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using WebUtilsLib;

namespace Lunar.Worker
{
    public class Worker
    {
        private static int                  CaptureInterval;
        private static bool                 ShouldUseMachineLearningAlgorithm;

        private static string               AWSAccessKey;
        private static string               AWSSecretKey;
        private static string               ToBeProcessedQueueName;
        private static string               ProcessedQueueName;
        private static string               ToBeTestedQueueName;
    
        private static SQSUtils             ToBeProcessedQueue;
        private static SQSUtils             ProcessedQueue;
        private static SQSUtils             ToBeTestedQueue;

        private static string               GoogleReverseGeocodingKey;
        private static string               GoogleReverseGeocodingUrlTemplate = "https://maps.googleapis.com/maps/api/geocode/json?latlng={0},{1}&key={2}&language=pt-BR";
        
        private static Regex                CityStateRegex  = new Regex(@",\s\d{1,}\s-\s([^,]*),\s([^-]*)\s-\s\D{2}", RegexOptions.Compiled);
        private static Regex                CityStateRegex2 = new Regex(@",\s([^-]*)\s-\s(\D{2}),\s", RegexOptions.Compiled);
        
        static void Main(string[] args)
        {
            // Load config
            Console.WriteLine(">> Loading config file...");
            if (!InitAppConfigValues())
            {
                Console.Read();
                Environment.Exit(-101);
            }

            // Initialize AWS Services
            if (!InitAWSServices())
            {
                Console.WriteLine(">> Error to initialize AWS services!");
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
                    Console.WriteLine(">> Do not have messages to be process...");
                    Thread.Sleep(1000 * CaptureInterval);
                }
                else
                {
                    int countProcessedMessages = 0;

                    // Iterate over messages
                    foreach (Message message in messages)
                    {
                        // Process message
                        MobileRecordObject obj = ProcessMessage(message);
                   
                        if (obj != null)
                        {
                            countProcessedMessages++;


                            if (ShouldUseMachineLearningAlgorithm)
                            {
                                // Send to LUNAR_ToBeTested Queue
                                Send2ToBeTestedQueue(obj);
                            }
                            else
                            {
                                // Send to LUNAR_Processed Queue
                                SendToProcessedQueue(obj);
                            }
                                
                            // Trace Message
                            if (countProcessedMessages % 10 == 0)
                                Console.WriteLine(">> Already processed {0} messages", countProcessedMessages);
                        }

                        
                        // Delete processed message
                        if(!ToBeProcessedQueue.DeleteMessage(message, out errorMessage))
                        {
                            Console.WriteLine(">> Error to delete message. Error Message: {0}", errorMessage);
                        }
                    }
                }
            }   
        }

        private static void Send2ToBeTestedQueue(MobileRecordObject obj)
        {
            string errorMessage = String.Empty;

            string jsonstr = obj.AsJSONString();

            // Add obj to queue
            if (!ToBeTestedQueue.EnqueueMessage(jsonstr, out errorMessage))
            {
                Console.WriteLine(">> Error to enqueue message on ToBeTestedQueue Queue. Error Message:{0}", errorMessage);
            }
        }

        private static void SendToProcessedQueue(MobileRecordObject obj)
        {
            string errorMessage = String.Empty;
            
            // Convert the object to a compressed string
            string jsonstr = Compression.Compress(obj.AsJSONString());

            // Add obj to queue
            if (!ProcessedQueue.EnqueueMessage(jsonstr, out errorMessage))
            {
                Console.WriteLine(">> Error to enqueue message on ProcessedQueue Queue. Error Message:{0}", errorMessage);
            }
        }

        private static bool InitAppConfigValues()
        {
            try
            {
                // AWS Keys
                AWSAccessKey                = Utils.LoadConfigurationSetting("AWSAccessKey", "");
                AWSSecretKey                = Utils.LoadConfigurationSetting("AWSSecretKey", "");

                // SQS                                        
                ToBeProcessedQueueName      = Utils.LoadConfigurationSetting("ToBeProcessedQueueName", "");
                ProcessedQueueName          = Utils.LoadConfigurationSetting("ProcessedQueueName", "");
                ToBeTestedQueueName         = Utils.LoadConfigurationSetting("ToBeTestedQueueName", "");

                // Initialize SQS objects  
                ToBeProcessedQueue          = new SQSUtils(AWSAccessKey, AWSSecretKey, ToBeProcessedQueueName);
                ToBeTestedQueue             = new SQSUtils(AWSAccessKey, AWSSecretKey, ToBeTestedQueueName);
                ProcessedQueue              = new SQSUtils(AWSAccessKey, AWSSecretKey, ProcessedQueueName);

                // Reverse Geocoding Key
                GoogleReverseGeocodingKey   = Utils.LoadConfigurationSetting("ReverseGeocodingApiKey", "");

                // Configuration Fields
                CaptureInterval                      = Int32.Parse(Utils.LoadConfigurationSetting("CaptureInterval", "30"));
                ShouldUseMachineLearningAlgorithm    = Boolean.Parse(Utils.LoadConfigurationSetting("ShouldUseMachineLearningAlgorithm", "true"));

            }
            catch (Exception ex)
            {
                Console.WriteLine(">> Error while parsing app config values. Error Message: {0}", ex.Message);
                return false;
            }

            return true;
        }

        private static bool InitAWSServices()
        {
            bool result = true;

            string errorMessage = String.Empty;

            try
            {
                // Try to open the LUNAR_ToBeProcessed Queue
                if (!ToBeProcessedQueue.OpenQueue(10, out errorMessage))
                    result = false;

                // Try to open the LUNAR_Processed Queue
                if (!ProcessedQueue.OpenQueue(1, out errorMessage))
                    result = false;

                // Try to open the LUNAR_ToBeTested Queue
                if (!ProcessedQueue.OpenQueue(1, out errorMessage))
                    result = false;

            }
            catch (Exception ex)
            {
                result = false;
                Console.WriteLine(">> Error Messages: {0}\t{1}", errorMessage, ex.Message);
            }

            return result;
        }

        private static MobileRecordObject ProcessMessage(Message message)
        {

            try
            {
                // Deserialize message
                MobileRecordObject mobileObj = JsonConvert.DeserializeObject<MobileRecordObject>(message.Body);

                // Info Message
                Console.WriteLine(String.Format(">> Processing message with:  X -> {0}\tY -> {1}\tZ -> {2}\t Tilt -> {3}\t Output -> {4}", mobileObj.Accelerometer_X, mobileObj.Accelerometer_Y, mobileObj.Accelerometer_Z, (mobileObj.Tilt != int.MinValue) ? mobileObj.Tilt.ToString() : "--", mobileObj.Output));

                // Is it a valid object?
                if (ValidMobileObject(mobileObj))
                {
                    if (mobileObj.Output != 0)
                    {
                        // Extract Address from Google Reverse Geocoding API
                        ExtractAddressFromGPSCoordinates(ref mobileObj);
                    }
                }

                return mobileObj;

            }
            catch (Exception ex)
            {
                Console.WriteLine(String.Format(">> Error while treating message from {0}: {1}", ToBeProcessedQueueName, ex.Message));
                return null;
            }
        }

        private static bool ValidMobileObject(MobileRecordObject mobileObj)
        {
            bool result = true;

            // GPS Filter
            if (mobileObj.Latitude == 0.0 || mobileObj.Longitude == 0.0)
                result = false;

            // OutputId Filter
            if (mobileObj.OutputId.ToUpper().Equals("UNKNOWN"))
                result = false;

            // Tilt Filter
            if (mobileObj.Tilt != int.MinValue && Math.Abs(mobileObj.Tilt) < Constants.LIMIT_TILT_PHONE)
                result = false;

            return result;
        }

        private static void ExtractAddressFromGPSCoordinates (ref MobileRecordObject mobileObj)
        {
            // Sanity check
            if (mobileObj.Latitude == 0 || mobileObj.Longitude == 0)
                return;

            // Build the url
            string finalUrl = String.Format(GoogleReverseGeocodingUrlTemplate, mobileObj.Latitude.ToString().Replace(",","."), mobileObj.Longitude.ToString().Replace(",", "."), GoogleReverseGeocodingKey);


            WebRequests client = new WebRequests();

            // GET Request
            string jsonstr = Get(ref client, finalUrl);

            // Check if jsonstr is valid
            if (String.IsNullOrEmpty(jsonstr))
                return;

            try
            {
                GoogleReverseGeocondingObject rcObj = JsonConvert.DeserializeObject<GoogleReverseGeocondingObject>(jsonstr);

                // Is it a valid response?
                if (rcObj.status.ToUpper().Equals("OK") && rcObj.results != null && rcObj.results.Count >= 1)
                {
                    mobileObj.Address = rcObj.results[0].formatted_address;

                    Match match = CityStateRegex.Match(mobileObj.Address);

                    if (!match.Success)
                        match = CityStateRegex2.Match(mobileObj.Address);

                    if (match.Success && match.Groups.Count == 3)
                    {
                        mobileObj.City  = match.Groups[1].Value.Trim().ToLower();
                        mobileObj.State = match.Groups[2].Value.Trim().ToLower();
                    }
                }

            }
            catch (Exception ex)
            {
                Console.WriteLine("Error to deserialize/parser GoogleReverseGeocondingObject. Message: {0}", ex.Message);
            }
        }


        public static string Get(ref WebRequests client, string url, int retries = 3)
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

                Console.WriteLine(String.Format("Status Code not OK. Retries left: {0}. Url: {1}", retries, url));

                Console.WriteLine("StatusCode = " + client.StatusCode + " Message = " + client.Error);

                Console.WriteLine("Html Response = " + htmlResponse);

                // Polite Sleeping
                Thread.Sleep(2000);

            } while (retries >= 0);

            return htmlResponse;
        }

    }
}
