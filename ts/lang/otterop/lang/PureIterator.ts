import { OOPIterator } from "./OOPIterator";

export class PureIterator<T> implements Iterator<T> {
    private oopIterator : OOPIterator<T>;

    private constructor(oopIterator: OOPIterator<T>) {
        this.oopIterator = oopIterator;
    }

    next() : IteratorResult<T, number | undefined> {
        if (!this.oopIterator.hasNext()) {
            return {
                done: true,
                value: undefined
            }
        } else {
            return {
                done: false,
                value: this.oopIterator.next()
            }
        }
    }

    public static newIterator<T>(oopIterator: OOPIterator<T>) : Iterator<T> {
        return new PureIterator<T>(oopIterator);
    }
}

