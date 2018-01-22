using Lunar.SharedLibrary.Models;
using Lunar.SharedLibrary.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace Lunar.Api.Controllers
{
    [RoutePrefix("api/v1/public")]
    public class AddressController : ApiController
    {

        private MongoDbUtils<MobileRecordObject> _MongoMROObj;
  
        [HttpGet]
        [Route("mobilerecods/{output}")]
        public HttpResponseMessage GetMobileRecordsById(int output)
        {
            // Inicializa as componentes do banco
            InitializeMongoDb();

            try
            {
                List<MobileRecordObject> list =  _MongoMROObj.GetRecords(_MongoMROObj, "Output", output.ToString());

                MobileRecordObject record = new MobileRecordObject();
                _MongoMROObj.collection.InsertOne(record);
    
            }
            catch (Exception ex)
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Failed connection with Database");
            }

            // SE DEU CERTO, RESGATA CERTOS VALORES
            return Request.CreateResponse(HttpStatusCode.OK, "VALORES AQUI");
        }

        

        private void InitializeMongoDb()
        {
            _MongoMROObj = new MongoDbUtils<MobileRecordObject>("lunar", "lunar", "localhost:27017", "LunarDb");

            // Open the Connection
            _MongoMROObj.GetCollection("TestRecord");
        }
    }
}