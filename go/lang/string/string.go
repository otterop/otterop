package string

import "strings"

type String struct {
    wrapped string
}

func newString(wrapped string) *String {
    ret := new(String)
    ret.wrapped = wrapped
    return ret
}

func (this *String) Length() int {
    return len(this.wrapped)
}

func (this *String) CompareTo(other interface{}) int {
    otherString, ok := other.(*String)
    if !ok {
        return 1
    }
    return strings.Compare(this.wrapped, otherString.wrapped)
}

func (this *String) String() string {
    return this.wrapped
}

func Wrap(wrapped string) *String {
    return newString(wrapped)
}
