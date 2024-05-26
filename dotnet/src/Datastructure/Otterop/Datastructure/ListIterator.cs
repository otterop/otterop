namespace Otterop.Datastructure
{
    class ListIterator<T> : Otterop.Lang.OOPIterator<T>
    {
        private List<T> list;
        private int index;
        internal ListIterator(List<T> list)
        {
            this.list = list;
            this.index = 0;
        }

        public bool HasNext()
        {
            return this.index < this.list.Size();
        }

        public T Next()
        {
            T ret = this.list.Get(this.index);
            this.index++;
            return ret;
        }

    }

}
