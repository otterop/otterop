package testbase

import (
    string "github.com/otterop/otterop/go/lang/string"
    "testing"
)

type TestBase struct {
    t *testing.T
}




func NewTestBase() *TestBase {
    this := new(TestBase)
    return this
}

func (this *TestBase) SetGoTestingT(t *testing.T) {
    this.t = t
}

func (this *TestBase) AssertTrue(value bool, message *string.String)  {
    if !value {
        this.t.Error(message.Unwrap())
    }
}
