package panic

import (
    "github.com/otterop/otterop/go/lang/string"
)


func NewPanic() *Panic {
    this := new(Panic)
    return this
}

type Panic struct {
}




func IndexOutOfBounds(message *string.String)  {
    panic(message.Unwrap())
}
