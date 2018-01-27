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
    [RoutePrefix("api/v1/public")]
    public class MobileController : ApiController
    {
        private static MongoCollection  Collection { get; set; }
        private static string           MongoAddress;
        private static string           MongoUser;
        private static string           MongoPassword;
        private static string           MongoDatabase;
        private static string           MongoCollection;

        [HttpGet]
        [Route("mobilerecods/{output}")]
        public HttpResponseMessage GetMobileRecordsById(int output)
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
                MongoCursor<LunarObject> cursor = Collection.FindAs<LunarObject>(Query.EQ("Output", output));

                // Iterate over all records on collection
                foreach (LunarObject rec in cursor)
                {
                    results.Add(JsonConvert.SerializeObject(rec));
                }
            }
            catch (Exception ex)
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Fail to get records from Database");
            }

            return Request.CreateResponse(HttpStatusCode.OK, results);
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