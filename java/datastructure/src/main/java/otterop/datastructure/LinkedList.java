package otterop.datastructure;

import otterop.lang.OOPIterable;
import otterop.lang.OOPIterator;
import otterop.lang.Panic;
import otterop.lang.PureIterator;
import otterop.lang.String;

import java.util.Iterator;

import static otterop.lang.OOPObject.is;

public class LinkedList<T> implements OOPIterable<T> {

    private LinkedListNode<T> head;
    private LinkedListNode<T> tail;
    private int size;

    public LinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public LinkedListNode<T> addBefore(LinkedListNode<T> node, T value) {
        LinkedListNode<T> newNode = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeBefore(node, newNode);
        return newNode;
    }

    private void nodeOfDifferentList() {
        Panic.invalidOperation(String.wrap("node of different list"));
    }

    private void removeOnEmptyList() {
        Panic.invalidOperation(String.wrap("remove called on empty list"));
    }

    public void addNodeBefore(LinkedListNode<T> node, LinkedListNode<T> newNode) {
        if (node.list() != newNode.list() || node.list() != this)
            nodeOfDifferentList();
        LinkedListNode<T> prevNode = node.prev();
        if (prevNode == null)
            newNode.list().head = newNode;
        else
            prevNode.setNext(newNode);
        newNode.setPrev(prevNode);
        newNode.setNext(node);
        node.setPrev(newNode);
        this.size++;
    }

    public LinkedListNode<T> addAfter(LinkedListNode<T> node, T value) {
        LinkedListNode<T> newNode = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeAfter(node, newNode);
        return newNode;
    }

    public void addNodeAfter(LinkedListNode<T> node, LinkedListNode<T> newNode) {
        if (node.list() != newNode.list() || node.list() != this)
            nodeOfDifferentList();
        LinkedListNode<T> nextNode = node.next();
        if (nextNode == null)
            newNode.list().tail = newNode;
        else
            nextNode.setPrev(newNode);
        newNode.setNext(nextNode);
        newNode.setPrev(node);
        node.setNext(newNode);
        this.size++;
    }
    public LinkedListNode<T> addFirst(T value) {
        LinkedListNode<T> newNode = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeFirst(newNode);
        return newNode;
    }

    public void addNodeFirst(LinkedListNode<T> newNode) {
        if (this.head == null) {
            if (newNode.list() != this)
                nodeOfDifferentList();
            this.head = newNode;
            this.tail = newNode;
            this.size++;
        } else {
            this.addNodeBefore(this.head, newNode);
        }
    }

    public LinkedListNode<T> addLast(T value) {
        LinkedListNode<T> newNode = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeLast(newNode);
        return newNode;
    }

    public void addNodeLast(LinkedListNode<T> newNode) {
        if (this.tail == null) {
            if (newNode.list() != this)
                nodeOfDifferentList();
            this.head = newNode;
            this.tail = newNode;
            this.size++;
        } else {
            this.addNodeAfter(this.tail, newNode);
        }
    }

    public void clear() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public void removeFirst() {
        if (this.head != null) {
            this.removeNode(this.head);
        } else {
            removeOnEmptyList();
        }
    }

    public void removeLast() {
        if (this.tail != null) {
            this.removeNode(this.tail);
        } else {
            removeOnEmptyList();
        }
    }

    public boolean remove(T value) {
        LinkedListNode<T> curr = this.head;
        while (curr != null) {
            if (is(curr.value(), value)) {
                this.removeNode(curr);
                return true;
            }
            curr = curr.next();
        }
        return false;
    }

    public void removeNode(LinkedListNode<T> node) {
        if (node.list() != this)
            Panic.invalidOperation(String.wrap("node of different list"));
        LinkedListNode<T> prev = node.prev();
        LinkedListNode<T> next = node.next();
        if (prev != null) {
            prev.setNext(next);
        } else {
            node.list().head = next;
        }
        if (next != null) {
            next.setPrev(prev);
        } else {
            node.list().tail = prev;
        }
        node.setPrev(null);
        node.setNext(null);
        this.size--;
    }

    public int size() {
        return this.size;
    }

    public LinkedListNode<T> first() {
        return this.head;
    }

    public LinkedListNode<T> last() {
        return this.tail;
    }

    public OOPIterator<T> OOPIterator() {
        return new LinkedListIterator<T>(this);
    }

    public Iterator<T> iterator() {
        return PureIterator.newIterator(this.OOPIterator());
    }
}
