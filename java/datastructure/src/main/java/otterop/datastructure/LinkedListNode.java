package otterop.datastructure;

public class LinkedListNode<T> {
        private LinkedList<T> list;
        private LinkedListNode<T> prev;
        private LinkedListNode<T> next;
        private T value;

        public LinkedListNode(T value) {
            this.prev = null;
            this.next = null;
            this.value = value;
        }

        public LinkedList<T> list() {
            return this.list;
        }

        void setList(LinkedList<T> list) {
            this.list = list;
        }

        public LinkedListNode<T> prev() {
            return this.prev;
        }

        void setPrev(LinkedListNode<T> node) {
            this.prev = node;
        }

        public LinkedListNode<T> next() {
            return this.next;
        }

        void setNext(LinkedListNode<T> node) {
            this.next = node;
        }

        public T value() {
            return this.value;
        }
}
