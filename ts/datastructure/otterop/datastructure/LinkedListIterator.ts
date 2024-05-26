import { OOPIterator } from '@otterop/lang/OOPIterator';
import { LinkedListNode } from './LinkedListNode';
import { LinkedList } from './LinkedList';

/** @internal */
export class LinkedListIterator<T> implements OOPIterator<T> {

    #current : LinkedListNode<T>;

    /** @internal */
    constructor(linkedList : LinkedList<T>) {
        this.#current = linkedList.first();
    }

    public hasNext() : boolean {
        return this.#current != null;
    }

    public next() : T {
        let ret : LinkedListNode<T> = this.#current;
        this.#current = this.#current.next();
        return ret.value();
    }
}

