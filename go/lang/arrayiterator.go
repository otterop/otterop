package lang



type arrayIterator[T any] struct {
    array *Array[T]
    i int
}




func arrayIteratorNew[T any](array *Array[T]) *arrayIterator[T] {
    this := new(arrayIterator[T])
    this.array = array
    this.i = 0
    return this
}

func (this *arrayIterator[T]) HasNext() bool {
    return this.i < this.array.Size()
}

func (this *arrayIterator[T]) Next() T {
    var ret T = this.array.Get(this.i)
    this.i++
    return ret
}