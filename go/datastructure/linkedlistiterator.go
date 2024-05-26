package datastructure



type linkedListIterator[T any] struct {
    current *LinkedListNode[T]
}




func linkedListIteratorNew[T any](linkedList *LinkedList[T]) *linkedListIterator[T] {
    this := new(linkedListIterator[T])
    this.current = linkedList.First()
    return this
}

func (this *linkedListIterator[T]) HasNext() bool {
    return this.current != nil
}

func (this *linkedListIterator[T]) Next() T {
    var ret *LinkedListNode[T] = this.current
    this.current = this.current.Next()
    return ret.Value()
}