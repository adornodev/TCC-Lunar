using Newtonsoft.Json.Serialization;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web.Http;

namespace Lunar.Api
{
    public static class WebApiConfig
    {
        public static void Register(HttpConfiguration config)
        {
            // Serviços e configuração da API da Web
            var formatters      = GlobalConfiguration.Configuration.Formatters;
            var jsonFormatter   = formatters.JsonFormatter;
            var settings        = jsonFormatter.SerializerSettings;

            jsonFormatter.SerializerSettings.PreserveReferencesHandling = Newtonsoft.Json.PreserveReferencesHandling.None;
            config.Formatters.Remove(config.Formatters.XmlFormatter);
            settings.Formatting       = Newtonsoft.Json.Formatting.Indented;
            //settings.ContractResolver = new CamelCasePropertyNamesContractResolver();
             
            // Rotas da API da Web
            config.MapHttpAttributeRoutes();

            config.Routes.MapHttpRoute(
                name: "DefaultApi",
                routeTemplate: "api/{controller}/{id}",
                defaults: new { id = RouteParameter.Optional }
            );
        }
    }
}
