namespace Otterop.Datastructure
{
    public class StringBuffer
    {
        private LinkedList<Otterop.Lang.String> strings;
        public StringBuffer()
        {
            this.strings = new LinkedList<Otterop.Lang.String>();
        }

        public void Add(Otterop.Lang.String s)
        {
            this.strings.AddLast(s);
        }

        public Otterop.Lang.String OOPString()
        {
            Otterop.Lang.OOPIterable<Otterop.Lang.String> strings = this.strings;
            return Otterop.Lang.String.Concat(strings);
        }

    }

}
