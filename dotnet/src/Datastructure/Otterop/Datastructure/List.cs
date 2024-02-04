namespace Otterop.Datastructure
{
    public class List<T>
    {
        private Otterop.Lang.Array<T> array;
        private int capacity;
        private int size;
        private T tZero;
        public List()
        {
            this.size = 0;
            this.capacity = 4;
            Otterop.Lang.Generic<T> genericT = new Otterop.Lang.Generic<T>();
            this.tZero = genericT.Zero();
            this.array = Otterop.Lang.Array.NewArray(this.capacity, this.tZero);
        }

        void EnsureCapacity(int capacity)
        {
            if (this.capacity < capacity)
            {
                this.capacity = this.capacity * 2;
                Otterop.Lang.Array<T> newArray = Otterop.Lang.Array.NewArray(this.capacity, this.tZero);
                Otterop.Lang.Array.Copy(this.array, 0, newArray, 0, this.size);
                this.array = newArray;
            }

        }

        public void Add(T element)
        {
            EnsureCapacity(this.size + 1);
            Otterop.Lang.Array<T> arr = this.array;
            arr.Set(this.size, element);
            this.size++;
        }

        public void AddArray(Otterop.Lang.Array<T> src)
        {
            EnsureCapacity(this.size + src.Size());
            Otterop.Lang.Array.Copy(src, 0, this.array, this.size, src.Size());
            this.size += src.Size();
        }

        public void AddList(List<T> src)
        {
            AddArray(src.array);
        }

        void CheckIndexOutOfBounds(int index)
        {
            if (index < 0 || index > this.size)
            {
                Otterop.Lang.Panic.IndexOutOfBounds(Otterop.Lang.String.Wrap("index is outside list bounds"));
            }

        }

        public void Insert(int index, T element)
        {
            CheckIndexOutOfBounds(index);
            EnsureCapacity(this.size + 1);
            if (index < this.size)
            {
                Otterop.Lang.Array.Copy(this.array, index, this.array, index + 1, this.size - index);
            }

            Otterop.Lang.Array<T> arr = this.array;
            arr.Set(index, element);
            this.size++;
        }

        public void InsertArray(int index, Otterop.Lang.Array<T> src)
        {
            CheckIndexOutOfBounds(index);
            EnsureCapacity(this.size + src.Size());
            if (index < this.size)
            {
                Otterop.Lang.Array.Copy(this.array, index, this.array, index + src.Size(), this.size - index);
            }

            Otterop.Lang.Array.Copy(src, 0, this.array, index, src.Size());
            this.size += src.Size();
        }

        public void InsertList(int index, List<T> src)
        {
            InsertArray(index, src.array);
        }

        public T Get(int index)
        {
            CheckIndexOutOfBounds(index);
            Otterop.Lang.Array<T> arr = this.array;
            return arr.Get(index);
        }

        public T RemoveIndex(int index)
        {
            CheckIndexOutOfBounds(index);
            Otterop.Lang.Array<T> arr = this.array;
            T ret = arr.Get(index);
            if (index + 1 < this.size)
            {
                Otterop.Lang.Array.Copy(this.array, index + 1, this.array, index, this.size - index - 1);
            }

            this.size--;
            return ret;
        }

        public List<T> RemoveRange(int index, int count)
        {
            CheckIndexOutOfBounds(index);
            if (index + count > this.size)
            {
                count = this.size - index;
            }

            List<T> ret = new List<T>();
            Otterop.Lang.Array<T> removed = Otterop.Lang.Array.NewArray(count, this.tZero);
            Otterop.Lang.Array.Copy(this.array, index, removed, 0, count);
            ret.AddArray(removed);
            if (index + count < this.size)
            {
                Otterop.Lang.Array.Copy(this.array, index + count, this.array, index, this.size - index - count);
            }

            this.size = this.size - count;
            return ret;
        }

        public int Size()
        {
            return this.size;
        }

    }

}
