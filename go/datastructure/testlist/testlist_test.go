package testlist

import (
    "testing"
    array "github.com/otterop/otterop/go/lang/array"
    generic "github.com/otterop/otterop/go/lang/generic"
    string "github.com/otterop/otterop/go/lang/string"
    testbase "github.com/otterop/otterop/go/test/testbase"
    "github.com/otterop/otterop/go/datastructure/list"
)


func NewTestList() *TestList {
    this := new(TestList)
    this.TestBase = testbase.NewTestBase()
    return this
}

type TestList struct {
    *testbase.TestBase
}




func (this *TestList) Add()  {
    var l *list.List[*string.String] = list.NewList[*string.String]()
    l.Add(string.Wrap(string.Literal("a")))
    l.Add(string.Wrap(string.Literal("b")))
    l.Add(string.Wrap(string.Literal("c")))
    l.Add(string.Wrap(string.Literal("d")))
    l.Add(string.Wrap(string.Literal("e")))
    this.AssertTrue(l.Size() == 5, string.Wrap(string.Literal("Size should be 5")))
}

func (this *TestList) AddRange()  {
    var genericString *generic.Generic[*string.String] = generic.NewGeneric[*string.String]()
    var genericT *string.String = genericString.Zero()
    var l *list.List[*string.String] = list.NewList[*string.String]()
    var toAdd *array.Array[*string.String] = array.NewArray(5, genericT)
    toAdd.Set(0, string.Wrap(string.Literal("a")))
    toAdd.Set(1, string.Wrap(string.Literal("b")))
    toAdd.Set(2, string.Wrap(string.Literal("c")))
    toAdd.Set(3, string.Wrap(string.Literal("d")))
    toAdd.Set(4, string.Wrap(string.Literal("e")))
    l.AddArray(toAdd)
    this.AssertTrue(l.Size() == 5, string.Wrap(string.Literal("Size should be 5")))
}

func (this *TestList) RemoveIndex()  {
    var val *string.String
    var l *list.List[*string.String] = list.NewList[*string.String]()
    l.Add(string.Wrap(string.Literal("a")))
    l.Add(string.Wrap(string.Literal("b")))
    l.Add(string.Wrap(string.Literal("c")))
    l.Add(string.Wrap(string.Literal("d")))
    l.Add(string.Wrap(string.Literal("e")))
    l.RemoveIndex(1)
    l.RemoveIndex(1)
    this.AssertTrue(l.Size() == 3, string.Wrap(string.Literal("Size should be 3")))
    val = l.Get(0)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("a"))) == 0, string.Wrap(string.Literal("First element should be a")))
    val = l.Get(1)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("d"))) == 0, string.Wrap(string.Literal("Second element should be d")))
    val = l.Get(2)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("e"))) == 0, string.Wrap(string.Literal("Third element should be e")))
}

func (this *TestList) RemoveRange()  {
    var val *string.String
    var l *list.List[*string.String] = list.NewList[*string.String]()
    l.Add(string.Wrap(string.Literal("a")))
    l.Add(string.Wrap(string.Literal("b")))
    l.Add(string.Wrap(string.Literal("c")))
    l.Add(string.Wrap(string.Literal("d")))
    l.Add(string.Wrap(string.Literal("e")))
    l.RemoveRange(3, 2)
    this.AssertTrue(l.Size() == 3, string.Wrap(string.Literal("Size should be 3")))
    val = l.Get(0)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("a"))) == 0, string.Wrap(string.Literal("First element should be a")))
    val = l.Get(1)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("b"))) == 0, string.Wrap(string.Literal("Second element should be b")))
    val = l.Get(2)
    this.AssertTrue(val.CompareTo(string.Wrap(string.Literal("c"))) == 0, string.Wrap(string.Literal("Third element should be c")))
}

func Test(t *testing.T) {
    t.Run("Add", func(t *testing.T) {
        test := NewTestList()
        test.SetGoTestingT(t)
        test.Add()
    })
    t.Run("AddRange", func(t *testing.T) {
        test := NewTestList()
        test.SetGoTestingT(t)
        test.AddRange()
    })
    t.Run("RemoveIndex", func(t *testing.T) {
        test := NewTestList()
        test.SetGoTestingT(t)
        test.RemoveIndex()
    })
    t.Run("RemoveRange", func(t *testing.T) {
        test := NewTestList()
        test.SetGoTestingT(t)
        test.RemoveRange()
    })
}
