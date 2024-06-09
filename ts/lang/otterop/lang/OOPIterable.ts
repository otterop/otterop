import { OOPIterator } from './OOPIterator';

export interface OOPIterable<T> extends Iterable<T> {
    OOPIterator() : OOPIterator<T>;
}

