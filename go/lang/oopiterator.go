package lang


type OOPIterator[T any] interface {
    HasNext() bool;
    Next() T;
}

