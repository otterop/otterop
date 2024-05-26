import { OOPIterator } from './OOPIterator';
import { Array } from './Array';

/** @internal */
export class ArrayIterator<T> implements OOPIterator<T> {

    #array : Array<T>;

    #i : number;

    /** @internal */
    constructor(array : Array<T>) {
        this.#array = array;
        this.#i = 0;
    }

    public hasNext() : boolean {
        return this.#i < this.#array.size();
    }

    public next() : T {
        let ret : T = this.#array.get(this.#i);
        this.#i++;
        return ret;
    }
}

