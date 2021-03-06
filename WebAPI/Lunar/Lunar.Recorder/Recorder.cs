﻿using Amazon.SQS;
using Amazon.SQS.Model;
using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using MongoDB.Driver;
using MongoDB.Driver.Builders;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Threading;

namespace Lunar.Recorder
{
   public class Recorder
    {
        private static int                              CaptureInterval;
        private static SQSUtils                         ProcessedQueue;
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
            Console.WriteLine("Loading config file...");
            if (!InitAppConfigValues())
            {
                Console.Read();
                Environment.Exit(-101);
            }

            // Initialize AWS Services
            if (!InitAWSServices())
            {
                Console.WriteLine("Error to initialize AWS services!");
                Console.Read();
                Environment.Exit(-102);
            }

            // Initialize MongoDB
            if (!InitMongoDb())
            {
                Console.WriteLine("Error to initialize MongoDb! Please, you must check appConfig values.");
                Console.Read();
                Environment.Exit(-103);

            }

            string errorMessage;

            int countSentMessages = 0;

            while (true)
            {
                List<MobileRecordObject> mobileObjs = new List<MobileRecordObject>();

                // Get messages from SQS
                List<Message> messages = ProcessedQueue.GetMessagesFromQueue(out errorMessage);

                // Sanity check
                if (messages == null || messages.Count == 0)
                {
                    Console.WriteLine("Do not have messages to be stored...");
                    Thread.Sleep(1000 * CaptureInterval);
                }
                else
                {
                    foreach (Message message in messages)
                    {
                        // Deserialize and Decompress message
                        MobileRecordObject obj = JsonConvert.DeserializeObject<MobileRecordObject>(Compression.Decompress(message.Body));

                        if (obj != null)
                        {
                            // Info Message
                            Console.WriteLine(String.Format(">> Reading message with:  X -> {0}\tY -> {1}\tZ -> {2}\t Tilt -> {3}\t Output -> {4}", obj.Accelerometer_X, obj.Accelerometer_Y, obj.Accelerometer_Z, (obj.Tilt != int.MinValue) ? obj.Tilt.ToString() : "--", obj.Output));

                            mobileObjs.Add(obj);
                        }

                        // Trace Message
                        if (countSentMessages % 5 == 0)
                        {
                            // Send register to database
                            SendObjectToMongoDb(mobileObjs);

                            mobileObjs.Clear();
                            Console.WriteLine("Already sent {0} messages", (++countSentMessages));
                        }

                        // Delete processed message
                        if (!ProcessedQueue.DeleteMessage(message, out errorMessage))
                        {
                            Console.WriteLine("Error to delete message. Error Message:{0}", errorMessage);
                        }
                    }

                    if (mobileObjs.Count != 0)
                    {
                        Console.WriteLine("Already sent {0} messages", countSentMessages);

                        // Send register to database
                        SendObjectToMongoDb(mobileObjs);
                    }
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
            foreach(MobileRecordObject mro in objs)
            {
                // Before inserting, we need to check if obj was already inserted and update it if this is the case
                IMongoQuery        query          = Query.And(Query.EQ("Latitude", mro.Latitude), Query.EQ("Longitude", mro.Longitude), Query.GTE("AcquireDate", mro.AcquireDate.AddMinutes(-1)), Query.LTE("AcquireDate", mro.AcquireDate.AddMinutes(1)));
                MobileRecordObject mroOnMongo     = Collection.FindOneAs<MobileRecordObject>(query);

                if (mroOnMongo != null)
                {
                    mro._id = mroOnMongo._id;
                    Collection.Save(mro);
                }
                else
                {
                    Collection.Insert(mro);
                }
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
                ProcessedQueueName      = Utils.LoadConfigurationSetting("ProcessedQueueName", "");

                // Initialize SQS object
                ProcessedQueue          = new SQSUtils(AWSAccessKey, AWSSecretKey, ProcessedQueueName);

                // MongoDB
                MongoAddress            = Utils.LoadConfigurationSetting("MongoAddress", "");
                MongoUser               = Utils.LoadConfigurationSetting("MongoUser", "");
                MongoPassword           = Utils.LoadConfigurationSetting("MongoPassword", "");
                MongoDatabase           = Utils.LoadConfigurationSetting("MongoDatabase", "");
                MongoCollection         = Utils.LoadConfigurationSetting("MongoCollection", "");

                // Configuration Fields
                CaptureInterval         = Int32.Parse(Utils.LoadConfigurationSetting("CaptureInterval", "30"));


            }
            catch (Exception ex)
            {
                Console.WriteLine("Error while parsing app config values! Error Message: {0}", ex.Message);
                return false;
            }

            return true;
        }

        private static bool InitAWSServices()
        {
            bool result = true;

            string processederrormessage = String.Empty;

            try
            {
                // Is there Processed Queue?
                if (!ProcessedQueue.OpenQueue(1, out processederrormessage))
                    result = false;
            }
            catch (Exception ex) { result = false; Console.WriteLine("Error Message: {0}\t {1}", processederrormessage, ex.Message); }

            return result;

        }
    }
}
