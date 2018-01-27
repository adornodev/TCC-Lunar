using Lunar.SharedLibrary.Models;
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
        private static MongoCollection Collection { get; set; }

        [HttpGet]
        [Route("mobilerecods/{output}")]
        public HttpResponseMessage GetMobileRecordsById(int output)
        {
            // Initialize mongo properties
            if (!InitMongoDb())
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Failed connection with Database");
            }

            List<string> results = new List<string>();
            try
            {
                MongoCursor<MobileRecordObject> cursor = Collection.FindAs<MobileRecordObject>(Query.EQ("Output", output));

                // Iterate over all records on collection
                foreach (MobileRecordObject rec in cursor)
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


        private static bool InitMongoDb()
        {
            try
            {
                // Initialize database connection
                MongoDbContext.Configure("lunar", "lunar", "localhost:27017", true, false, 600000, 600000);

                // Get database
                MongoDatabase mongoDatabase = MongoDbContext.GetDatabase("LunarDb");

                // Get collection
                Collection = mongoDatabase.GetCollection<MobileRecordObject>("TestRecord");

                // Sanity Check
                if (Collection == null)
                    return false;
            }
            catch (Exception ex) { return false; }

            return true;
        }
    }
}