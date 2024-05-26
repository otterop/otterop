package datastructure

import (
    lang "github.com/otterop/otterop/go/lang"
)


type List[T any] struct {
    array *lang.Array[T]
    capacity int
    size int
    tZero T
}




func ListNew[T any]() *List[T] {
    this := new(List[T])
    this.size = 0
    this.capacity = 4
    var genericT *lang.Generic[T] = lang.GenericNew[T]()
    this.tZero = genericT.Zero()
    this.array = lang.ArrayNewArray(this.capacity, this.tZero)
    return this
}

func (this *List[T]) ensureCapacity(capacity int)  {
    
    if this.capacity < capacity {
        this.capacity = this.capacity * 2
        var newArray *lang.Array[T] = lang.ArrayNewArray(this.capacity, this.tZero)
        lang.ArrayCopy(this.array, 0, newArray, 0, this.size)
        this.array = newArray
    }
}

func (this *List[T]) Add(element T)  {
    this.ensureCapacity(this.size + 1)
    this.array.Set(this.size, element)
    this.size++
}

func (this *List[T]) AddArray(src *lang.Array[T])  {
    this.ensureCapacity(this.size + src.Size())
    lang.ArrayCopy(src, 0, this.array, this.size, src.Size())
    this.size += src.Size()
}

func (this *List[T]) AddList(src *List[T])  {
    this.AddArray(src.array)
}

func (this *List[T]) checkIndexOutOfBounds(index int)  {
    
    if index < 0 || index > this.size {
        lang.PanicIndexOutOfBounds(lang.StringWrap(lang.StringLiteral("index is outside list bounds")))
    }
}

func (this *List[T]) Insert(index int, element T)  {
    this.checkIndexOutOfBounds(index)
    this.ensureCapacity(this.size + 1)
    
    if index < this.size {
        lang.ArrayCopy(this.array, index, this.array, index + 1, this.size - index)
    }
    this.array.Set(index, element)
    this.size++
}

func (this *List[T]) InsertArray(index int, src *lang.Array[T])  {
    this.checkIndexOutOfBounds(index)
    this.ensureCapacity(this.size + src.Size())
    
    if index < this.size {
        lang.ArrayCopy(this.array, index, this.array, index + src.Size(), this.size - index)
    }
    lang.ArrayCopy(src, 0, this.array, index, src.Size())
    this.size += src.Size()
}

func (this *List[T]) InsertList(index int, src *List[T])  {
    this.InsertArray(index, src.array)
}

func (this *List[T]) Get(index int) T {
    this.checkIndexOutOfBounds(index)
    return this.array.Get(index)
}

func (this *List[T]) RemoveIndex(index int) T {
    this.checkIndexOutOfBounds(index)
    var ret T = this.array.Get(index)
    
    if index + 1 < this.size {
        lang.ArrayCopy(this.array, index + 1, this.array, index, this.size - index - 1)
    }
    this.size--
    return ret
}

func (this *List[T]) RemoveRange(index int, count int) *List[T] {
    this.checkIndexOutOfBounds(index)
    
    if index + count > this.size {
        count = this.size - index
    }
    var ret *List[T] = ListNew[T]()
    var removed *lang.Array[T] = lang.ArrayNewArray(count, this.tZero)
    lang.ArrayCopy(this.array, index, removed, 0, count)
    ret.AddArray(removed)
    
    if index + count < this.size {
        lang.ArrayCopy(this.array, index + count, this.array, index, this.size - index - count)
    }
    this.size = this.size - count
    return ret
}

func (this *List[T]) Size() int {
    return this.size
}

func (this *List[T]) OOPIterator() lang.OOPIterator[T] {
    return listIteratorNew[T](this)
}