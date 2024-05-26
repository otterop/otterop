namespace Otterop.Lang
{
    class ArrayIterator<T> : OOPIterator<T>
    {
        private Array<T> array;
        private int i;
        internal ArrayIterator(Array<T> array)
        {
            this.array = array;
            this.i = 0;
        }

        public bool HasNext()
        {
            return this.i < this.array.Size();
        }

        public T Next()
        {
            T ret = this.array.Get(this.i);
            this.i++;
            return ret;
        }

    }

}
