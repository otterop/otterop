package array;

import String "github.com/otterop/otterop/go/lang/string"

type Array[T any] struct {
    _wrapped []T;
    start int
    end int
}

func newArray[T any](array []T, start int, end int) *Array[T] {
    ret := new(Array[T])
    ret._wrapped = array
    ret.start = start
    ret.end = end
    return ret
}

func (a *Array[T]) Size() int {
    return a.end - a.start
}

func (a *Array[T]) Get(i int) T {
    return a._wrapped[a.start + i]
}

func (a *Array[T]) Set(i int, val T) {
    a._wrapped[a.start + i] = val
}

func (a *Array[T]) Slice(start int, end int) *Array[T] {
    newStart := a.start + start
    newEnd := a.start + end
    if newStart < a.start || newStart > a.end || newEnd < newStart ||
       newEnd > a.end {
        panic("slice arguments out of bounds")
    }
    return newArray(a._wrapped, newStart, newEnd)
}

func Wrap[T any](array []T) *Array[T] {
    return newArray(array, 0, len(array))
}

func WrapString(arg []string) *Array[*String.String] {
    ret := make([]*String.String, len(arg))
    for i, v := range arg {
        ret[i] = String.Wrap(v)
    }
    return newArray(ret, 0, len(ret))
}
