namespace Otterop.Datastructure
{
    class LinkedListIterator<T> : Otterop.Lang.OOPIterator<T>
    {
        private LinkedListNode<T> current;
        internal LinkedListIterator(LinkedList<T> linkedList)
        {
            this.current = linkedList.First();
        }

        public bool HasNext()
        {
            return this.current != null;
        }

        public T Next()
        {
            LinkedListNode<T> ret = this.current;
            this.current = this.current.Next();
            return ret.Value();
        }

    }

}
