package datastructure



type listIterator[T any] struct {
    list *List[T]
    index int
}




func listIteratorNew[T any](list *List[T]) *listIterator[T] {
    this := new(listIterator[T])
    this.list = list
    this.index = 0
    return this
}

func (this *listIterator[T]) HasNext() bool {
    return this.index < this.list.Size()
}

func (this *listIterator[T]) Next() T {
    var ret T = this.list.Get(this.index)
    this.index++
    return ret
}