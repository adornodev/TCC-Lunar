using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using MongoDB.Driver;
using MongoDB.Driver.Builders;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace Lunar.Api.Controllers
{
    [RoutePrefix("api/v1/public/lunar")]
    public class MobileController : ApiController
    {
        private static MongoCollection  Collection { get; set; }
        private static string           MongoAddress;
        private static string           MongoUser;
        private static string           MongoPassword;
        private static string           MongoDatabase;
        private static string           MongoCollection;

        [HttpGet]
        [Route("")]
        public HttpResponseMessage GetMobileRecordsById([FromUri] ApiQueryObject queryObject)
        {

            // Initialize AppConfig files
            if (!InitAppConfigValues())
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Failed parsing private configuration values");
            }

            // Initialize mongo properties
            if (!InitMongoDb())
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Failed connection with Database");
            }

            List<string> results = new List<string>();
            try
            {
                bool success;

                // Build query from GET parameters
                IMongoQuery query = BuildQuery(queryObject, out success);

                if (!success || query == null)
                    throw new Exception();

                MongoCursor<LunarObject> cursor = Collection.FindAs<LunarObject>(query).SetLimit(queryObject.Limit);

                // Iterate over all records on collection
                foreach (LunarObject rec in cursor)
                {
                    results.Add(JsonConvert.SerializeObject(rec));
                }
            }
            catch (Exception ex)
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Fail to get records from Database. Check the documentation to see how the parameter works. Message: " + ex.Message);
            }

            return Request.CreateResponse(HttpStatusCode.OK, results);
        }

        private IMongoQuery BuildQuery(ApiQueryObject queryObject, out bool success)
        {
            success           = false;
            IMongoQuery query = null;

            // "numberOfDay" as parameter
            if (queryObject.Output == int.MinValue && queryObject.Latitude == Double.MinValue && queryObject.Longitude == Double.MinValue)
            {
                success = true;
                query   = Query.GTE("AcquireDate", DateTime.UtcNow.AddDays(-1 * queryObject.NumberOfDays));
            }
            
            // "output" as parameter
            else if (queryObject.Output != int.MinValue && queryObject.Latitude == Double.MinValue && queryObject.Longitude == Double.MinValue)
            {
                if (queryObject.Output >= 0 && queryObject.Output <= 2)
                {
                    success = true;
                    query   = Query.And(Query.EQ("Output", queryObject.Output), Query.GTE("AcquireDate", DateTime.UtcNow.AddDays(-1 * queryObject.NumberOfDays)));
                }
            }

            // "latitude" and "longitude" as parameters
            else if (queryObject.Output == int.MinValue && queryObject.Latitude != Double.MinValue && queryObject.Longitude != Double.MinValue)
            {
                success = true;
                query   = Query.And(Query.EQ("Latitude", queryObject.Latitude), Query.EQ("Longitude", queryObject.Longitude), Query.GTE("AcquireDate", DateTime.UtcNow.AddDays(-1 * queryObject.NumberOfDays)));
            }

            // "output", "latitude" and "longitude" as parameters
            else if (queryObject.Output != int.MinValue && queryObject.Latitude != Double.MinValue && queryObject.Longitude != Double.MinValue)
            {
                if (queryObject.Output >= 0 && queryObject.Output <= 2)
                {
                    success = true;
                    query   = Query.And(Query.EQ("Output", queryObject.Output), Query.EQ("Latitude", queryObject.Latitude), Query.EQ("Longitude", queryObject.Longitude), Query.GTE("AcquireDate", DateTime.UtcNow.AddDays(-1 * queryObject.NumberOfDays)));
                }
            }

            return query;
        }

        private static bool InitAppConfigValues()
        {
            try
            {
                // Mongo Fields
                MongoUser       = Utils.LoadConfigurationSetting("MongoUser"        , "");
                MongoPassword   = Utils.LoadConfigurationSetting("MongoPassword"    , "");
                MongoAddress    = Utils.LoadConfigurationSetting("MongoAddress"     , "");
                MongoDatabase   = Utils.LoadConfigurationSetting("MongoDatabase"    , "");
                MongoCollection = Utils.LoadConfigurationSetting("MongoCollection"  , "");

            }
            catch (Exception ex) { return false; }

            return true;
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
            catch (Exception ex) { return false; }

            return true;
        }
    }
}