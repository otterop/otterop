namespace Otterop.Datastructure
{
    public class TestLinkedList : Otterop.Test.TestBase
    {
        [Otterop.Test.Test]
        public void Add()
        {
            Otterop.Lang.String a = Otterop.Lang.String.Wrap("a");
            Otterop.Lang.String b = Otterop.Lang.String.Wrap("b");
            Otterop.Lang.String c = Otterop.Lang.String.Wrap("c");
            Otterop.Lang.String d = Otterop.Lang.String.Wrap("d");
            Otterop.Lang.String e = Otterop.Lang.String.Wrap("e");
            Otterop.Lang.Array<Otterop.Lang.String> strings = Otterop.Lang.Array.NewArray(5, a);
            strings.Set(0, a);
            strings.Set(1, b);
            strings.Set(2, c);
            strings.Set(3, d);
            strings.Set(4, e);
            Otterop.Lang.Array<Otterop.Lang.String> expected = Otterop.Lang.Array.NewArray(5, a);
            expected.Set(0, e);
            expected.Set(1, d);
            expected.Set(2, a);
            expected.Set(3, b);
            expected.Set(4, c);
            LinkedList<Otterop.Lang.String> l = new LinkedList<Otterop.Lang.String>();
            l.AddLast(a);
            l.AddLast(b);
            l.AddLast(c);
            l.AddFirst(d);
            l.AddFirst(e);
            AssertTrue(l.Size() == 5, Otterop.Lang.String.Wrap("Size should be 5"));
            int i = 0;
            foreach (Otterop.Lang.String s in l)
            {
                AssertTrue(s.CompareTo(expected.Get(i)) == 0, Otterop.Lang.String.Wrap("Element mismatch"));
                i++;
            }

        }

    }

}
