import { LinkedList } from './LinkedList';

export class LinkedListNode<T> {

    #list : LinkedList<T>;

    #prev : LinkedListNode<T>;

    #next : LinkedListNode<T>;

    #value : T;

    public constructor(value : T) {
        this.#prev = null;
        this.#next = null;
        this.#value = value;
    }

    public list() : LinkedList<T> {
        return this.#list;
    }

    /** @internal */
    setList(list : LinkedList<T>) : void {
        this.#list = list;
    }

    public prev() : LinkedListNode<T> {
        return this.#prev;
    }

    /** @internal */
    setPrev(node : LinkedListNode<T>) : void {
        this.#prev = node;
    }

    public next() : LinkedListNode<T> {
        return this.#next;
    }

    /** @internal */
    setNext(node : LinkedListNode<T>) : void {
        this.#next = node;
    }

    public value() : T {
        return this.#value;
    }
}

