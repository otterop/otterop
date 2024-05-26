import { OOPIterator } from '@otterop/lang/OOPIterator';
import { List } from './List';

/** @internal */
export class ListIterator<T> implements OOPIterator<T> {

    #list : List<T>;

    #index : number;

    /** @internal */
    constructor(list : List<T>) {
        this.#list = list;
        this.#index = 0;
    }

    public hasNext() : boolean {
        return this.#index < this.#list.size();
    }

    public next() : T {
        let ret : T = this.#list.get(this.#index);
        this.#index++;
        return ret;
    }
}

