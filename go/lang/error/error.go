package error

import (
    "github.com/otterop/otterop/go/lang/string"
)

type Error struct {
    code int
    message *string.String
}

func NewError(code int, message *string.String) *Error {
    this := new(Error)
    this.code = code
    this.message = message
    return this
}

func (this *Error) Code() int {
    return this.code
}

func (this *Error) Message() *string.String {
    return this.message
}