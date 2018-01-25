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
        public  static  FlexibleOptions     ProgramOptions;
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
            if (!ParseArguments(ProgramOptions))
            {
                Console.WriteLine("Error parsing arguments! Aborting...");
                Environment.Exit(-101);
            }

            // Initialize AWS Services
            if (!InitAWSServices())
            {
                Console.WriteLine("Error to initialize AWS services.");
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
                    Thread.Sleep(1000 * 10);
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

        private static bool ParseArguments(FlexibleOptions options)
        {
            try
            {
                // AWS Keys
                AWSAccessKey = options.Get("AWSAccessKey");
                AWSSecretKey = options.Get("AWSSecretKey");

                // SQS                                    
                ToBeProcessedQueueName = options.Get("ToBeProcessedQueueName");
                ProcessedQueueName     = options.Get("ProcessedQueueName");

                // Initialize SQS object
                ToBeProcessedQueue  = new SQSUtils(AWSAccessKey, AWSSecretKey, ToBeProcessedQueueName);
                ProcessedQueue      = new SQSUtils(AWSAccessKey, AWSSecretKey, ProcessedQueueName);

            }
            catch (Exception ex)
            {
                Console.WriteLine("Error while parsing arguments. Error Message: {0}", ex.Message);
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
            MobileRecordObject mobileObj = new MobileRecordObject();

            try
            {
                // Decompress and Deserialize message
                mobileObj = JsonConvert.DeserializeObject<MobileRecordObject>(Compression.Decompress(message.Body));

                // Extract Addres from XXX API
                ExtractAddressFromGPSCoordinates(ref mobileObj);

            }
            catch (Exception ex)
            {
                Console.WriteLine(String.Format("Error while treating message from {0}: {1}", ToBeProcessedQueueName, ex.Message));
                return null;
            }

            return mobileObj;
        }

        private static void ExtractAddressFromGPSCoordinates (ref MobileRecordObject mobileObj)
        {

        }

    }
}
