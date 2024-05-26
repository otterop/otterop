namespace Otterop.Datastructure
{
    public class LinkedList<T> : Otterop.Lang.OOPIterable<T>, IEnumerable<T>
    {
        private LinkedListNode<T> head;
        private LinkedListNode<T> tail;
        private int size;
        public LinkedList()
        {
            this.head = null;
            this.tail = null;
            this.size = 0;
        }

        public LinkedListNode<T> AddBefore(LinkedListNode<T> node, T value)
        {
            LinkedListNode<T> newNode = new LinkedListNode<T>(value);
            newNode.SetList(this);
            AddNodeBefore(node, newNode);
            return newNode;
        }

        private void NodeOfDifferentList()
        {
            Otterop.Lang.Panic.InvalidOperation(Otterop.Lang.String.Wrap("node of different list"));
        }

        private void RemoveOnEmptyList()
        {
            Otterop.Lang.Panic.InvalidOperation(Otterop.Lang.String.Wrap("remove called on empty list"));
        }

        public void AddNodeBefore(LinkedListNode<T> node, LinkedListNode<T> newNode)
        {
            if (node.List() != newNode.List() || node.List() != this)
                NodeOfDifferentList();

            LinkedListNode<T> prevNode = node.Prev();
            if (prevNode == null)
                newNode.List().head = newNode;
            else
                prevNode.SetNext(newNode);

            newNode.SetPrev(prevNode);
            newNode.SetNext(node);
            node.SetPrev(newNode);
            this.size++;
        }

        public LinkedListNode<T> AddAfter(LinkedListNode<T> node, T value)
        {
            LinkedListNode<T> newNode = new LinkedListNode<T>(value);
            newNode.SetList(this);
            AddNodeAfter(node, newNode);
            return newNode;
        }

        public void AddNodeAfter(LinkedListNode<T> node, LinkedListNode<T> newNode)
        {
            if (node.List() != newNode.List() || node.List() != this)
                NodeOfDifferentList();

            LinkedListNode<T> nextNode = node.Next();
            if (nextNode == null)
                newNode.List().tail = newNode;
            else
                nextNode.SetPrev(newNode);

            newNode.SetNext(nextNode);
            newNode.SetPrev(node);
            node.SetNext(newNode);
            this.size++;
        }

        public LinkedListNode<T> AddFirst(T value)
        {
            LinkedListNode<T> newNode = new LinkedListNode<T>(value);
            newNode.SetList(this);
            AddNodeFirst(newNode);
            return newNode;
        }

        public void AddNodeFirst(LinkedListNode<T> newNode)
        {
            if (this.head == null)
            {
                if (newNode.List() != this)
                    NodeOfDifferentList();

                this.head = newNode;
                this.tail = newNode;
                this.size++;
            }
            else
            {
                AddNodeBefore(this.head, newNode);
            }

        }

        public LinkedListNode<T> AddLast(T value)
        {
            LinkedListNode<T> newNode = new LinkedListNode<T>(value);
            newNode.SetList(this);
            AddNodeLast(newNode);
            return newNode;
        }

        public void AddNodeLast(LinkedListNode<T> newNode)
        {
            if (this.tail == null)
            {
                if (newNode.List() != this)
                    NodeOfDifferentList();

                this.head = newNode;
                this.tail = newNode;
                this.size++;
            }
            else
            {
                AddNodeAfter(this.tail, newNode);
            }

        }

        public void Clear()
        {
            this.head = null;
            this.tail = null;
            this.size = 0;
        }

        public void RemoveFirst()
        {
            if (this.head != null)
            {
                RemoveNode(this.head);
            }
            else
            {
                RemoveOnEmptyList();
            }

        }

        public void RemoveLast()
        {
            if (this.tail != null)
            {
                RemoveNode(this.tail);
            }
            else
            {
                RemoveOnEmptyList();
            }

        }

        public bool Remove(T value)
        {
            LinkedListNode<T> curr = this.head;
            while (curr != null)
            {
                if (Otterop.Lang.OOPObject.Is(curr.Value(), value))
                {
                    RemoveNode(curr);
                    return true;
                }

                curr = curr.Next();
            }

            return false;
        }

        public void RemoveNode(LinkedListNode<T> node)
        {
            if (node.List() != this)
                Otterop.Lang.Panic.InvalidOperation(Otterop.Lang.String.Wrap("node of different list"));

            LinkedListNode<T> prev = node.Prev();
            LinkedListNode<T> next = node.Next();
            if (prev != null)
            {
                prev.SetNext(next);
            }
            else
            {
                node.List().head = next;
            }

            if (next != null)
            {
                next.SetPrev(prev);
            }
            else
            {
                node.List().tail = prev;
            }

            node.SetPrev(null);
            node.SetNext(null);
            this.size--;
        }

        public int Size()
        {
            return this.size;
        }

        public LinkedListNode<T> First()
        {
            return this.head;
        }

        public LinkedListNode<T> Last()
        {
            return this.tail;
        }

        public Otterop.Lang.OOPIterator<T> OOPIterator()
        {
            return new LinkedListIterator<T>(this);
        }

        public IEnumerator<T> GetEnumerator()
        {
            return Otterop.Lang.PureIterator.NewIterator(OOPIterator());
        }

        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }
    }

}
