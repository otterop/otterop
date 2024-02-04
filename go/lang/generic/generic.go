package generic



func NewGeneric[T any]() *Generic[T] {
    this := new(Generic[T])
    return this
}

type Generic[T any] struct {
}




func (this *Generic[T]) Zero() T {
    return *new(T)
}
