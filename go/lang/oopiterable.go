package lang


type OOPIterable[T any] interface {
    OOPIterator() OOPIterator[T];
}

