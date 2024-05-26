package otterop.datastructure;

import otterop.lang.OOPIterator;

class LinkedListIterator<T> implements OOPIterator<T> {

    private LinkedListNode<T> current;

    LinkedListIterator(LinkedList<T> linkedList) {
        this.current = linkedList.first();
    }
    
    public boolean hasNext() {
        return this.current != null;
    }

    public T next() {
        LinkedListNode<T> ret = this.current;
        this.current = this.current.next();
        return ret.value();
    }
}
