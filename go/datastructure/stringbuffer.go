package datastructure

import (
    lang "github.com/otterop/otterop/go/lang"
)


type StringBuffer struct {
    strings *LinkedList[*lang.String]
}




func StringBufferNew() *StringBuffer {
    this := new(StringBuffer)
    this.strings = LinkedListNew[*lang.String]()
    return this
}

func (this *StringBuffer) Add(s *lang.String)  {
    this.strings.AddLast(s)
}

func (this *StringBuffer) OOPString() *lang.String {
    var strings lang.OOPIterable[*lang.String] = this.strings
    return lang.StringConcat(strings)
}