using Amazon;
using Amazon.SQS;
using Amazon.SQS.Model;
using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Lunar.Worker
{
    public class Worker
    {
        private static GetQueueUrlResponse  Response;
        private static string               AWSAccessKey;
        private static string               AWSSecretKey;
        private static string               ToBeProcessedQueueName;
        private static string               ProcessedQueueName;
        private static SQSUtils             ToBeProcessedQueue;
        private static SQSUtils             ProcessedQueue;

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
                        MobileRecordObject mobileObj = ProcessMessage(message);
                   
                        if (mobileObj != null)
                        {
                            // Send to ProcessedQueue
                            SendToProcessedQueue(mobileObj);

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

        private static void SendToProcessedQueue(MobileRecordObject obj)
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

        private static MobileRecordObject ProcessMessage(Message message)
        {

            try
            {
                // Decompress and Deserialize message
                MobileRecordObject mobileObj = JsonConvert.DeserializeObject<MobileRecordObject>(message.Body);

                // Get OutputId
                mobileObj.OutputId = mobileObj.ExtractOutputIdFromInt(mobileObj.Output);

                // Extract Addres from XXX API
                ExtractAddressFromGPSCoordinates(ref mobileObj);

                return mobileObj;

            }
            catch (Exception ex)
            {
                Console.WriteLine(String.Format("Error while treating message from {0}: {1}", ToBeProcessedQueueName, ex.Message));
                return null;
            }
        }

        private static void ExtractAddressFromGPSCoordinates (ref MobileRecordObject mobileObj)
        {

        }

    }
}
