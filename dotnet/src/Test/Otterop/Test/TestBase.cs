using Xunit;

namespace Otterop.Test
{
    public class TestBase
    {
        public void AssertTrue(bool value, Otterop.Lang.String message)
        {
            Assert.True(value, message!.ToString());
        }
    }

}
