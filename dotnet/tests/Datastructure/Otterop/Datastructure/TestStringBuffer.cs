namespace Otterop.Datastructure
{
    public class TestStringBuffer : Otterop.Test.TestBase
    {
        [Otterop.Test.Test]
        public void Empty()
        {
            StringBuffer sb = new StringBuffer();
            Otterop.Lang.String s = sb.OOPString();
            AssertTrue(s.CompareTo(Otterop.Lang.String.Wrap("")) == 0, Otterop.Lang.String.Wrap("Should be an empty string"));
        }

        [Otterop.Test.Test]
        public void AddMoreStrings()
        {
            StringBuffer sb = new StringBuffer();
            sb.Add(Otterop.Lang.String.Wrap("a"));
            Otterop.Lang.String s = sb.OOPString();
            AssertTrue(s.CompareTo(Otterop.Lang.String.Wrap("a")) == 0, Otterop.Lang.String.Wrap("Should be equals to 'a'"));
            sb.Add(Otterop.Lang.String.Wrap(",b"));
            s = sb.OOPString();
            AssertTrue(s.CompareTo(Otterop.Lang.String.Wrap("a,b")) == 0, Otterop.Lang.String.Wrap("Should be equals to 'a,b'"));
        }

    }

}
