using Lunar.SharedLibrary.Models;
using System;

namespace Lunar.SharedLibrary.Utils
{
    public class EnumsHelper
    {
        public static int OutputToInt(Enums.Output output)
        {
            switch (output)
            {
                case Enums.Output.NotClassified:
                    return 0;
                case Enums.Output.Pothole:
                    return 1;
                case Enums.Output.SpeedBump:
                    return 2;
            }
            return int.MaxValue;
        }
    }
}
