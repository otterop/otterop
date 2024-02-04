package list

import (
    array "github.com/otterop/otterop/go/lang/array"
    generic "github.com/otterop/otterop/go/lang/generic"
    panic "github.com/otterop/otterop/go/lang/panic"
    string "github.com/otterop/otterop/go/lang/string"
)


type List[T any] struct {
    array *array.Array[T]
    capacity int
    size int
    tZero T
}




func NewList[T any]() *List[T] {
    this := new(List[T])
    this.size = 0
    this.capacity = 4
    var genericT *generic.Generic[T] = generic.NewGeneric[T]()
    this.tZero = genericT.Zero()
    this.array = array.NewArray(this.capacity, this.tZero)
    return this
}

func (this *List[T]) ensureCapacity(capacity int)  {
    
    if this.capacity < capacity {
        this.capacity = this.capacity * 2
        var newArray *array.Array[T] = array.NewArray(this.capacity, this.tZero)
        array.Copy(this.array, 0, newArray, 0, this.size)
        this.array = newArray
    }
}

func (this *List[T]) Add(element T)  {
    this.ensureCapacity(this.size + 1)
    var arr *array.Array[T] = this.array
    arr.Set(this.size, element)
    this.size++
}

func (this *List[T]) AddArray(src *array.Array[T])  {
    this.ensureCapacity(this.size + src.Size())
    array.Copy(src, 0, this.array, this.size, src.Size())
    this.size += src.Size()
}

func (this *List[T]) AddList(src *List[T])  {
    this.AddArray(src.array)
}

func (this *List[T]) checkIndexOutOfBounds(index int)  {
    
    if index < 0 || index > this.size {
        panic.IndexOutOfBounds(string.Wrap(string.Literal("index is outside list bounds")))
    }
}

func (this *List[T]) Insert(index int, element T)  {
    this.checkIndexOutOfBounds(index)
    this.ensureCapacity(this.size + 1)
    
    if index < this.size {
        array.Copy(this.array, index, this.array, index + 1, this.size - index)
    }
    var arr *array.Array[T] = this.array
    arr.Set(index, element)
    this.size++
}

func (this *List[T]) InsertArray(index int, src *array.Array[T])  {
    this.checkIndexOutOfBounds(index)
    this.ensureCapacity(this.size + src.Size())
    
    if index < this.size {
        array.Copy(this.array, index, this.array, index + src.Size(), this.size - index)
    }
    array.Copy(src, 0, this.array, index, src.Size())
    this.size += src.Size()
}

func (this *List[T]) InsertList(index int, src *List[T])  {
    this.InsertArray(index, src.array)
}

func (this *List[T]) Get(index int) T {
    this.checkIndexOutOfBounds(index)
    var arr *array.Array[T] = this.array
    return arr.Get(index)
}

func (this *List[T]) RemoveIndex(index int) T {
    this.checkIndexOutOfBounds(index)
    var arr *array.Array[T] = this.array
    var ret T = arr.Get(index)
    
    if index + 1 < this.size {
        array.Copy(this.array, index + 1, this.array, index, this.size - index - 1)
    }
    this.size--
    return ret
}

func (this *List[T]) RemoveRange(index int, count int) *List[T] {
    this.checkIndexOutOfBounds(index)
    
    if index + count > this.size {
        count = this.size - index
    }
    var ret *List[T] = NewList[T]()
    var removed *array.Array[T] = array.NewArray(count, this.tZero)
    array.Copy(this.array, index, removed, 0, count)
    ret.AddArray(removed)
    
    if index + count < this.size {
        array.Copy(this.array, index + count, this.array, index, this.size - index - count)
    }
    this.size = this.size - count
    return ret
}

func (this *List[T]) Size() int {
    return this.size
}