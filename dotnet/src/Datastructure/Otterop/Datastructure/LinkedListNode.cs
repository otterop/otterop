namespace Otterop.Datastructure
{
    public class LinkedListNode<T>
    {
        private LinkedList<T> list;
        private LinkedListNode<T> prev;
        private LinkedListNode<T> next;
        private T value;
        public LinkedListNode(T value)
        {
            this.prev = null;
            this.next = null;
            this.value = value;
        }

        public LinkedList<T> List()
        {
            return this.list;
        }

        internal void SetList(LinkedList<T> list)
        {
            this.list = list;
        }

        public LinkedListNode<T> Prev()
        {
            return this.prev;
        }

        internal void SetPrev(LinkedListNode<T> node)
        {
            this.prev = node;
        }

        public LinkedListNode<T> Next()
        {
            return this.next;
        }

        internal void SetNext(LinkedListNode<T> node)
        {
            this.next = node;
        }

        public T Value()
        {
            return this.value;
        }

    }

}
