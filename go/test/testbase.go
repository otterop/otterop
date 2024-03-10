package test

import (
    lang "github.com/otterop/otterop/go/lang"
    "testing"
)

type TestBase struct {
    t *testing.T
}




func TestBaseNew() *TestBase {
    this := new(TestBase)
    return this
}

func (this *TestBase) SetGoTestingT(t *testing.T) {
    this.t = t
}

func (this *TestBase) AssertTrue(value bool, message *lang.String)  {
    if !value {
        this.t.Error(message.Unwrap())
    }
}
