using Lunar.SharedLibrary.Models;
using MongoDB.Bson;
using MongoDB.Driver;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Lunar.SharedLibrary.Utils
{
    public class MongoDbUtils<T>
    {
        private static string _connectionString;
        private static string _dbName;

        public IMongoCollection<T> collection;

        public MongoDbUtils(string user, string password, string address, string database, int connectionTimeoutMilliseconds = 60000, int socketTimeoutMilliseconds = 90000)
        {
            _dbName = database;
            _connectionString = BuildConnectionString(user, password, true, address, connectionTimeoutMilliseconds, socketTimeoutMilliseconds);
        }

        /// <summary>
        /// Builds the connection string for MongoDB.
        /// </summary>
        /// <param name="login">The login.</param>
        /// <param name="password">The password.</param>
        /// <param name="safeMode">The safe mode. True to receive write confirmation from mongo.</param>
        /// <param name="addresses">List of addresses. Format: host1[:port1][,host2[:port2],...[,hostN[:portN]]]</param>
        /// <param name="connectionTimeoutMilliseconds">The time to attempt a connection before timing out.</param>
        /// <param name="socketTimeoutMilliseconds">The time to attempt a send or receive on a socket before the attempt times out.</param>
        private string BuildConnectionString(string login, string password, bool safeMode, string addresses, int connectionTimeoutMilliseconds, int socketTimeoutMilliseconds)
        {
            var cb = new MongoUrlBuilder("mongodb://" + addresses.Replace("mongodb://", ""));

            if (!login.Equals(String.Empty))
                cb.Username = login;
            if (!password.Equals(String.Empty))
                cb.Password = password;

            cb.ConnectionMode = ConnectionMode.Automatic;
            cb.W = safeMode ? WriteConcern.W1.W : WriteConcern.Unacknowledged.W;

            if (connectionTimeoutMilliseconds < 15000)
                connectionTimeoutMilliseconds = 15000;

            if (socketTimeoutMilliseconds < 15000)
                socketTimeoutMilliseconds = 15000;

            cb.ConnectTimeout = TimeSpan.FromMilliseconds(connectionTimeoutMilliseconds);
            cb.SocketTimeout = TimeSpan.FromMilliseconds(socketTimeoutMilliseconds);
            cb.MaxConnectionIdleTime = TimeSpan.FromSeconds(30);

            // Generate final connection string
            return cb.ToString();
        }

        public bool CreateCollection(string collectioName)
        {
            // Sanit Check
            if (String.IsNullOrWhiteSpace(_dbName) || collection == null)
                return false;

            try
            {
                collection.Indexes.CreateOne(new BsonDocument("AcquireDate", -1), new CreateIndexOptions { Background = true });
            }
            catch { return false; }

            return true;
        }

        public async Task<bool> CollectionExistsAsync(string collectionName)
        {
            var filter = new BsonDocument("name", collectionName);

            // Filter by collection name
            var collections = await GetDatabase(null).ListCollectionsAsync(new ListCollectionsOptions { Filter = filter });

            GetCollection(collectionName);

            // Check for existence
            return await collections.AnyAsync();
        }

        public void GetCollection(string collectionName)
        {
            // Open Connection
            IMongoClient client = OpenConnection();

            // Get Database
            IMongoDatabase database = GetDatabase(client);

            // Get Collection
            IMongoCollection<T> collection = database.GetCollection<T>(collectionName);

            this.collection = collection;
        }

        private static IMongoClient OpenConnection()
        {
            // Create mongo client
            IMongoClient client = new MongoClient(_connectionString);

            return client;
        }

        public static IMongoDatabase GetDatabase(IMongoClient client)
        {
            if (client == null)
            {
                // Open Connection
                client = OpenConnection();
            }

            // GetDatabase
            IMongoDatabase database = client.GetDatabase(_dbName);

            return database;
        }

        public async Task<List<MobileRecordObject>> GetRecords(MongoDbUtils<MobileRecordObject> Mongo, string field, string value = "", int orderby = -1)
        {
            List<MobileRecordObject> listRecords = new List<MobileRecordObject>();

            // Sanity check
            if (Mongo.collection == null || String.IsNullOrWhiteSpace(field))
                return null;

            Mongo.collection.Find(rec => rec.Output == Int32.Parse(value)).SortByDescending<>
            //if (orderby == -1 && field.ToLower().Equals("output"))
            //    listRecords = Mongo.collection.Find<MobileRecordObject>(Builders<MobileRecordObject>.Filter.Eq(x => x.Output, Int32.Parse(value))).SortByDescending("_id");
            //else
            //    listRecords = Mongo.collection.Find<MobileRecordObject>(Builders<MobileRecordObject>.Filter.Eq(x => x.Output, Int32.Parse(value)));


            int count = 1;

            await Mongo.collection.Find(x => x.Output == 0)
                .Skip(1)
                .ForEachAsync(
                    reg =>
                    {
                        Console.WriteLine($"S/N:" + reg._id);
                        count++;
                    });

            return listRecords;
        }
    }
}
