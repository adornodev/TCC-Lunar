using Amazon;
using Amazon.SQS;
using Amazon.SQS.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Lunar.SharedLibrary.Utils
{
    public class SQSUtils
    {

        ///////////////////////////////////////////////////////////////////////
        //                           Fields                                  //
        ///////////////////////////////////////////////////////////////////////

        public AmazonSQSClient          queue                   { get; set; }   // AWS simple queue service reference
        public GetQueueUrlResponse      queueurl                { get; set; }   // AWS queue url
        public ReceiveMessageRequest    rcvMessageRequest       { get; set; }   // AWS receive message request
        public ReceiveMessageResponse   rcvMessageResponse      { get; set; }   // AWS receive message response
        public DeleteMessageRequest     delMessageRequest       { get; set; }   // AWS delete message request
        public DeleteMessageResponse    delMessageResponse      { get; set; }   // AWS delete message response
        public RegionEndpoint           Region                  { get; set; }   // AWS region
        public string                   QueueName               { get; set; }

        public const int amazonsqsmaxmsgsize = 256 * 1024;                      // AMAZON queue max message size

        private string AWSAccessKey;
        private string AWSSecretKey;

        public bool EnqueueMessages(List<string> messages, AmazonSQSClient client, out string errorMessage, int numberOfMessages = 10)
        {
            // We enqueue more than one message at a time, so we reduce the costs
            List<SendMessageBatchRequestEntry> tempMessages = new List<SendMessageBatchRequestEntry>();

            errorMessage = String.Empty;

            try
            {
                foreach (string message in messages)
                {
                    tempMessages.Add(new SendMessageBatchRequestEntry { MessageBody = message });

                    if (tempMessages.Count == numberOfMessages)
                    {
                        client.SendMessageBatch(client.GetQueueUrl(QueueName).QueueUrl, tempMessages);
                        tempMessages.Clear();
                    }
                }

                if (tempMessages.Count > 0)
                {
                    client.SendMessageBatch(client.GetQueueUrl(QueueName).QueueUrl, tempMessages);
                    tempMessages.Clear();
                }
            }
            catch (Exception ex)
            {
                errorMessage = ex.Message;
                return false;
            }

            return true;
        }

        public List<Message> GetMessagesFromQueue(out string errormessage)
        {
            errormessage = String.Empty;

            List<Message> result = new List<Message>();

            try
            {
                ReceiveMessageResponse  receiveMessageResponse  = queue.ReceiveMessage(rcvMessageRequest);

                result = receiveMessageResponse.Messages;

            }
            catch (Exception ex)
            {
                errormessage = String.Format("Message:{0} \t StackTrace: {1} ", ex.Message, ex.StackTrace);
                return null;
            }

            return result;
        }
   
        public bool OpenQueue(int maxnumberofmessages, out string errormessage)
        {

            bool success = false;
            errormessage = String.Empty;

            if (!string.IsNullOrWhiteSpace(QueueName))
            {
                queue = new AmazonSQSClient(AWSAccessKey, AWSSecretKey, Region);
                try
                {
                    // Get queue url
                    GetQueueUrlRequest sqsRequest = new GetQueueUrlRequest();
                    sqsRequest.QueueName = QueueName;
                    queueurl = queue.GetQueueUrl(sqsRequest);

                    // Format receive messages request
                    rcvMessageRequest = new ReceiveMessageRequest();
                    rcvMessageRequest.QueueUrl = queueurl.QueueUrl;
                    rcvMessageRequest.MaxNumberOfMessages = maxnumberofmessages;

                    // Format the delete messages request
                    delMessageRequest = new DeleteMessageRequest();
                    delMessageRequest.QueueUrl = queueurl.QueueUrl;

                    success = true;
                }
                catch (Exception ex)
                {
                    success = false;
                    errormessage = String.Format("Message:{0} \t StackTrace: {1} ", ex.Message, ex.StackTrace);
                }
            }

            return success;
        }

        public bool DeleteMessage(Message message, out string errormessage)
        {
            errormessage = String.Empty;
            bool result = false;
            try
            {
                delMessageRequest.ReceiptHandle = message.ReceiptHandle;
                delMessageResponse = queue.DeleteMessage(delMessageRequest);
                result = delMessageResponse.HttpStatusCode == System.Net.HttpStatusCode.OK;
            }
            catch (Exception ex)
            {
                errormessage = String.Format("Message:{0} \t StackTrace: {1} ", ex.Message, ex.StackTrace);
            }

            return result;
        }


        /// <summary>
        /// Inserts a message in the queue
        /// </summary>
        public bool EnqueueMessage(string msgbody, out string errormessage)
        {
            errormessage = String.Empty;

            bool result = false;

            try
            {
                SendMessageRequest sendMessageRequest = new SendMessageRequest();
                sendMessageRequest.QueueUrl     = queueurl.QueueUrl;
                sendMessageRequest.MessageBody  = msgbody;

                queue.SendMessage(sendMessageRequest);

                result = true;
            }
            catch (Exception ex)
            {
                errormessage = String.Format("Message:{0} \t StackTrace: {1} ", ex.Message, ex.StackTrace);
            }

            return result;
        }

        public bool EnqueueMessage(string msgbody, out string errormessage, int maxretries = 3)
        {
            // Insert domain info into queue
            bool result = false;
            int retrycount = maxretries;


            while (true)
            {
                errormessage = String.Empty;
                
                // Try the insertion
                if (EnqueueMessage(msgbody, out errormessage))
                {
                    result = true;
                    break;
                }

                // Retry
                retrycount--;
                if (retrycount <= 0)
                    break;

                Thread.Sleep(2000);
            }

            // Return
            return result;
        }


        #region Constructors
        public SQSUtils() { }

        public SQSUtils(string AWSAccessKey, string AWSSecretKey, string queueName)
        {
            this.AWSAccessKey = AWSAccessKey;
            this.AWSSecretKey = AWSSecretKey;
            this.QueueName    = queueName;
            this.Region       = RegionEndpoint.USEast1;
        }

        public SQSUtils(string AWSAccessKey, string AWSSecretKey, string queueName, RegionEndpoint Region)
        {
            this.AWSAccessKey = AWSAccessKey;
            this.AWSSecretKey = AWSSecretKey;
            this.QueueName    = queueName;
            this.Region       = Region;
        }

        #endregion
    }
}