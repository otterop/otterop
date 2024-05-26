package datastructure



type LinkedListNode[T any] struct {
    list *LinkedList[T]
    prev *LinkedListNode[T]
    next *LinkedListNode[T]
    value T
}




func LinkedListNodeNew[T any](value T) *LinkedListNode[T] {
    this := new(LinkedListNode[T])
    this.prev = nil
    this.next = nil
    this.value = value
    return this
}

func (this *LinkedListNode[T]) List() *LinkedList[T] {
    return this.list
}

func (this *LinkedListNode[T]) setList(list *LinkedList[T])  {
    this.list = list
}

func (this *LinkedListNode[T]) Prev() *LinkedListNode[T] {
    return this.prev
}

func (this *LinkedListNode[T]) setPrev(node *LinkedListNode[T])  {
    this.prev = node
}

func (this *LinkedListNode[T]) Next() *LinkedListNode[T] {
    return this.next
}

func (this *LinkedListNode[T]) setNext(node *LinkedListNode[T])  {
    this.next = node
}

func (this *LinkedListNode[T]) Value() T {
    return this.value
}