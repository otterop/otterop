import { OOPIterator } from './OOPIterator';

export interface OOPIterable<T> {
    OOPIterator() : OOPIterator<T>;
}

