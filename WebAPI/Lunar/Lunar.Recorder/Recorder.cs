using Amazon.SQS;
using Amazon.SQS.Model;
using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using MongoDB.Driver;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Threading;

namespace Lunar.Recorder
{
    public class Recorder
    {
        public  static FlexibleOptions                  ProgramOptions;
        private static SQSUtils                         ProcessedQueue;
        private static GetQueueUrlResponse              Response;
        private static MongoCollection                  Collection       { get; set; }
        private static string                           AWSAccessKey;
        private static string                           AWSSecretKey;
        private static string                           ProcessedQueueName;
        private static string                           MongoAddress;
        private static string                           MongoUser;
        private static string                           MongoPassword;
        private static string                           MongoDatabase;
        private static string                           MongoCollection;

        public static void Main(string[] args)
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

            // Initialize MongoDB
            if (InitMongoDb())
            {
                Console.WriteLine("Error to initialize MongoDb. Please, check appConfig values.");
                Environment.Exit(-103);

            }

            string errorMessage;

            while (true)
            {
                List<MobileRecordObject> mobileObjs = new List<MobileRecordObject>();

                // Get messages from SQS
                List<Message> messages = ProcessedQueue.GetMessagesFromQueue(out errorMessage);

                
                if (messages == null || messages.Count == 0)
                {
                    Console.WriteLine("Do not have messages to be save!...");
                    Thread.Sleep(1000 * 10);
                }
                else
                {
                    int countSentMessages = 0;
                    foreach (Message message in messages)
                    {
                        // Deserialize message
                        MobileRecordObject mobileObj = JsonConvert.DeserializeObject<MobileRecordObject>(Compression.Decompress(message.Body));

                        if (mobileObj != null)
                            mobileObjs.Add(mobileObj);

                        // Trace Message
                        if (countSentMessages % 10 == 0)
                        {
                            // Send register to database
                            SendObjectToMongoDb(mobileObjs);

                            Console.WriteLine("Already sent {0} messages", (++countSentMessages));
                        }

                        // Delete processed message
                        if (!ProcessedQueue.DeleteMessage(message, out errorMessage))
                        {
                            Console.WriteLine("Error to delete message. Error Message:{0}", errorMessage);
                        }
                    }

                    if (mobileObjs.Count != 0)
                        // Send register to database
                        SendObjectToMongoDb(mobileObjs);
                }
            }
        }

        private static bool InitMongoDb()
        {
            try
            {

                // Initialize database connection
                MongoDbContext.Configure(MongoUser, MongoPassword, MongoAddress, true, false, 600000, 600000);

                // Get database
                MongoDatabase mongoDatabase = MongoDbContext.GetDatabase(MongoDatabase);

                // Get collection
                Collection = mongoDatabase.GetCollection<MobileRecordObject>(MongoCollection);

                // Sanity Check
                if (Collection == null)
                    return false;
            }
            catch(Exception ex) { return false; }

            return true;
        }

        private static void SendObjectToMongoDb(List<MobileRecordObject> objs)
        {
            Collection.InsertBatch<MobileRecordObject>(objs);
        }


        private static bool ParseArguments(FlexibleOptions options)
        {
            try
            {
                // AWS Keys
                AWSAccessKey = options.Get("AWSAccessKey");
                AWSSecretKey = options.Get("AWSSecretKey");

                // SQS                                    
                ProcessedQueueName = options.Get("ProcessedQueueName");

                // Initialize SQS object
                ProcessedQueue = new SQSUtils(AWSAccessKey, AWSSecretKey, ProcessedQueueName);

                // MongoDB
                MongoAddress        = options.Get("MongoAddress");
                MongoUser           = options.Get("MongoUser");
                MongoPassword       = options.Get("MongoPassword");
                MongoDatabase       = options.Get("MongoDatabase");
                MongoCollection     = options.Get("MongoCollection");
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

            string processederrormessage     = String.Empty;

            try
            {
                // Is there Processed Queue?
                if (!ProcessedQueue.OpenQueue(1, out processederrormessage))
                    result = false;
            }
            catch (Exception ex) { result = false; Console.WriteLine("ErrorMessage: {1} \t {3}", processederrormessage, ex.Message); }

            return result;

        }
    }
}
