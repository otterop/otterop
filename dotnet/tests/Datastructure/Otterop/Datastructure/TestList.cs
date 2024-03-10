namespace Otterop.Datastructure
{
    public class TestList : Otterop.Test.TestBase
    {
        [Otterop.Test.Test]
        public void Add()
        {
            List<Otterop.Lang.String> l = new List<Otterop.Lang.String>();
            l.Add(Otterop.Lang.String.Wrap("a"));
            l.Add(Otterop.Lang.String.Wrap("b"));
            l.Add(Otterop.Lang.String.Wrap("c"));
            l.Add(Otterop.Lang.String.Wrap("d"));
            l.Add(Otterop.Lang.String.Wrap("e"));
            AssertTrue(l.Size() == 5, Otterop.Lang.String.Wrap("Size should be 5"));
        }

        [Otterop.Test.Test]
        public void AddRange()
        {
            Otterop.Lang.Generic<Otterop.Lang.String> genericT = new Otterop.Lang.Generic<Otterop.Lang.String>();
            Otterop.Lang.String genericTZero = genericT.Zero();
            List<Otterop.Lang.String> l = new List<Otterop.Lang.String>();
            Otterop.Lang.Array<Otterop.Lang.String> toAdd = Otterop.Lang.Array.NewArray(5, genericTZero);
            toAdd.Set(0, Otterop.Lang.String.Wrap("a"));
            toAdd.Set(1, Otterop.Lang.String.Wrap("b"));
            toAdd.Set(2, Otterop.Lang.String.Wrap("c"));
            toAdd.Set(3, Otterop.Lang.String.Wrap("d"));
            toAdd.Set(4, Otterop.Lang.String.Wrap("e"));
            l.AddArray(toAdd);
            AssertTrue(l.Size() == 5, Otterop.Lang.String.Wrap("Size should be 5"));
        }

        [Otterop.Test.Test]
        public void RemoveIndex()
        {
            Otterop.Lang.String val;
            List<Otterop.Lang.String> l = new List<Otterop.Lang.String>();
            l.Add(Otterop.Lang.String.Wrap("a"));
            l.Add(Otterop.Lang.String.Wrap("b"));
            l.Add(Otterop.Lang.String.Wrap("c"));
            l.Add(Otterop.Lang.String.Wrap("d"));
            l.Add(Otterop.Lang.String.Wrap("e"));
            l.RemoveIndex(1);
            l.RemoveIndex(1);
            AssertTrue(l.Size() == 3, Otterop.Lang.String.Wrap("Size should be 3"));
            val = l.Get(0);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("a")) == 0, Otterop.Lang.String.Wrap("First element should be a"));
            val = l.Get(1);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("d")) == 0, Otterop.Lang.String.Wrap("Second element should be d"));
            val = l.Get(2);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("e")) == 0, Otterop.Lang.String.Wrap("Third element should be e"));
        }

        [Otterop.Test.Test]
        public void RemoveRange()
        {
            Otterop.Lang.String val;
            List<Otterop.Lang.String> l = new List<Otterop.Lang.String>();
            l.Add(Otterop.Lang.String.Wrap("a"));
            l.Add(Otterop.Lang.String.Wrap("b"));
            l.Add(Otterop.Lang.String.Wrap("c"));
            l.Add(Otterop.Lang.String.Wrap("d"));
            l.Add(Otterop.Lang.String.Wrap("e"));
            l.RemoveRange(3, 2);
            AssertTrue(l.Size() == 3, Otterop.Lang.String.Wrap("Size should be 3"));
            val = l.Get(0);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("a")) == 0, Otterop.Lang.String.Wrap("First element should be a"));
            val = l.Get(1);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("b")) == 0, Otterop.Lang.String.Wrap("Second element should be b"));
            val = l.Get(2);
            AssertTrue(val.CompareTo(Otterop.Lang.String.Wrap("c")) == 0, Otterop.Lang.String.Wrap("Third element should be c"));
        }

    }

}
