package array;

import String "github.com/otterop/otterop/go/lang/string"

type Array[T any] struct {
    _wrapped []T
}

func NewArray[T any](array []T) *Array[T] {
    ret := new(Array[T])
    ret._wrapped = array
    return ret
}

func (a *Array[T]) Size() int {
    return len(a._wrapped)
}

func (a *Array[T]) Get(i int) T {
    return a._wrapped[i]
}

func (a *Array[T]) Set(i int, val T) {
    a._wrapped[i] = val
}

func Wrap[T any](array []T) *Array[T] {
    return NewArray(array)
}

func WrapString(arg []string) *Array[*String.String] {
    ret := make([]*String.String, len(arg))
    for i, v := range arg {
        ret[i] = String.Wrap(v)
    }
    return Wrap(ret)
}
