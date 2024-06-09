import { OOPIterable } from '@otterop/lang/OOPIterable';
import { OOPIterator } from '@otterop/lang/OOPIterator';
import { Panic } from '@otterop/lang/Panic';
import { PureIterator } from '@otterop/lang/PureIterator';
import { String } from '@otterop/lang/String';
import { OOPObject } from '@otterop/lang/OOPObject';
import { LinkedListNode } from './LinkedListNode';
import { LinkedListIterator } from './LinkedListIterator';

export class LinkedList<T> implements OOPIterable<T> {

    #head : LinkedListNode<T>;

    #tail : LinkedListNode<T>;

    #size : number;

    public constructor() {
        this.#head = null;
        this.#tail = null;
        this.#size = 0;
    }

    public addBefore(node : LinkedListNode<T>, value : T) : LinkedListNode<T> {
        let newNode : LinkedListNode<T> = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeBefore(node, newNode);
        return newNode;
    }

    #nodeOfDifferentList() : void {
        Panic.invalidOperation(String.wrap("node of different list"));
    }

    #removeOnEmptyList() : void {
        Panic.invalidOperation(String.wrap("remove called on empty list"));
    }

    public addNodeBefore(node : LinkedListNode<T>, newNode : LinkedListNode<T>) : void {
        
        if (node.list() != newNode.list() || node.list() != this)
            this.#nodeOfDifferentList();
        
        let prevNode : LinkedListNode<T> = node.prev();
        
        if (prevNode == null)
            newNode.list().#head = newNode;
        else
            prevNode.setNext(newNode);
        
        newNode.setPrev(prevNode);
        newNode.setNext(node);
        node.setPrev(newNode);
        this.#size++;
    }

    public addAfter(node : LinkedListNode<T>, value : T) : LinkedListNode<T> {
        let newNode : LinkedListNode<T> = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeAfter(node, newNode);
        return newNode;
    }

    public addNodeAfter(node : LinkedListNode<T>, newNode : LinkedListNode<T>) : void {
        
        if (node.list() != newNode.list() || node.list() != this)
            this.#nodeOfDifferentList();
        
        let nextNode : LinkedListNode<T> = node.next();
        
        if (nextNode == null)
            newNode.list().#tail = newNode;
        else
            nextNode.setPrev(newNode);
        
        newNode.setNext(nextNode);
        newNode.setPrev(node);
        node.setNext(newNode);
        this.#size++;
    }

    public addFirst(value : T) : LinkedListNode<T> {
        let newNode : LinkedListNode<T> = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeFirst(newNode);
        return newNode;
    }

    public addNodeFirst(newNode : LinkedListNode<T>) : void {
        
        if (this.#head == null) {
            
            if (newNode.list() != this)
                this.#nodeOfDifferentList();
            
            this.#head = newNode;
            this.#tail = newNode;
            this.#size++;
        } else {
            this.addNodeBefore(this.#head, newNode);
        }
    }

    public addLast(value : T) : LinkedListNode<T> {
        let newNode : LinkedListNode<T> = new LinkedListNode<T>(value);
        newNode.setList(this);
        this.addNodeLast(newNode);
        return newNode;
    }

    public addNodeLast(newNode : LinkedListNode<T>) : void {
        
        if (this.#tail == null) {
            
            if (newNode.list() != this)
                this.#nodeOfDifferentList();
            
            this.#head = newNode;
            this.#tail = newNode;
            this.#size++;
        } else {
            this.addNodeAfter(this.#tail, newNode);
        }
    }

    public clear() : void {
        this.#head = null;
        this.#tail = null;
        this.#size = 0;
    }

    public removeFirst() : void {
        
        if (this.#head != null) {
            this.removeNode(this.#head);
        } else {
            this.#removeOnEmptyList();
        }
    }

    public removeLast() : void {
        
        if (this.#tail != null) {
            this.removeNode(this.#tail);
        } else {
            this.#removeOnEmptyList();
        }
    }

    public remove(value : T) : boolean {
        let curr : LinkedListNode<T> = this.#head;
        
        while (curr != null) {
            
            if (OOPObject.is(curr.value(), value)) {
                this.removeNode(curr);
                return true;
            }
            curr = curr.next();
        }
        return false;
    }

    public removeNode(node : LinkedListNode<T>) : void {
        
        if (node.list() != this)
            Panic.invalidOperation(String.wrap("node of different list"));
        
        let prev : LinkedListNode<T> = node.prev();
        let next : LinkedListNode<T> = node.next();
        
        if (prev != null) {
            prev.setNext(next);
        } else {
            node.list().#head = next;
        }
        
        if (next != null) {
            next.setPrev(prev);
        } else {
            node.list().#tail = prev;
        }
        node.setPrev(null);
        node.setNext(null);
        this.#size--;
    }

    public size() : number {
        return this.#size;
    }

    public first() : LinkedListNode<T> {
        return this.#head;
    }

    public last() : LinkedListNode<T> {
        return this.#tail;
    }

    public OOPIterator() : OOPIterator<T> {
        return new LinkedListIterator<T>(this);
    }

    [Symbol.iterator]() : Iterator<T> {
        return PureIterator.newIterator(this.OOPIterator());
    }
}

