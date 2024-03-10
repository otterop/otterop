package datastructure

import (
    "testing"
    lang "github.com/otterop/otterop/go/lang"
    test "github.com/otterop/otterop/go/test"
)


func testListNew() *TestList {
    this := new(TestList)
    this.TestBase = test.TestBaseNew()
    return this
}

type TestList struct {
    *test.TestBase
}




func (this *TestList) add()  {
    var l *List[*lang.String] = ListNew[*lang.String]()
    l.Add(lang.StringWrap(lang.StringLiteral("a")))
    l.Add(lang.StringWrap(lang.StringLiteral("b")))
    l.Add(lang.StringWrap(lang.StringLiteral("c")))
    l.Add(lang.StringWrap(lang.StringLiteral("d")))
    l.Add(lang.StringWrap(lang.StringLiteral("e")))
    this.AssertTrue(l.Size() == 5, lang.StringWrap(lang.StringLiteral("Size should be 5")))
}

func (this *TestList) addRange()  {
    var genericT *lang.Generic[*lang.String] = lang.GenericNew[*lang.String]()
    var genericTZero *lang.String = genericT.Zero()
    var l *List[*lang.String] = ListNew[*lang.String]()
    var toAdd *lang.Array[*lang.String] = lang.ArrayNewArray(5, genericTZero)
    toAdd.Set(0, lang.StringWrap(lang.StringLiteral("a")))
    toAdd.Set(1, lang.StringWrap(lang.StringLiteral("b")))
    toAdd.Set(2, lang.StringWrap(lang.StringLiteral("c")))
    toAdd.Set(3, lang.StringWrap(lang.StringLiteral("d")))
    toAdd.Set(4, lang.StringWrap(lang.StringLiteral("e")))
    l.AddArray(toAdd)
    this.AssertTrue(l.Size() == 5, lang.StringWrap(lang.StringLiteral("Size should be 5")))
}

func (this *TestList) removeIndex()  {
    var val *lang.String
    var l *List[*lang.String] = ListNew[*lang.String]()
    l.Add(lang.StringWrap(lang.StringLiteral("a")))
    l.Add(lang.StringWrap(lang.StringLiteral("b")))
    l.Add(lang.StringWrap(lang.StringLiteral("c")))
    l.Add(lang.StringWrap(lang.StringLiteral("d")))
    l.Add(lang.StringWrap(lang.StringLiteral("e")))
    l.RemoveIndex(1)
    l.RemoveIndex(1)
    this.AssertTrue(l.Size() == 3, lang.StringWrap(lang.StringLiteral("Size should be 3")))
    val = l.Get(0)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("a"))) == 0, lang.StringWrap(lang.StringLiteral("First element should be a")))
    val = l.Get(1)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("d"))) == 0, lang.StringWrap(lang.StringLiteral("Second element should be d")))
    val = l.Get(2)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("e"))) == 0, lang.StringWrap(lang.StringLiteral("Third element should be e")))
}

func (this *TestList) removeRange()  {
    var val *lang.String
    var l *List[*lang.String] = ListNew[*lang.String]()
    l.Add(lang.StringWrap(lang.StringLiteral("a")))
    l.Add(lang.StringWrap(lang.StringLiteral("b")))
    l.Add(lang.StringWrap(lang.StringLiteral("c")))
    l.Add(lang.StringWrap(lang.StringLiteral("d")))
    l.Add(lang.StringWrap(lang.StringLiteral("e")))
    l.RemoveRange(3, 2)
    this.AssertTrue(l.Size() == 3, lang.StringWrap(lang.StringLiteral("Size should be 3")))
    val = l.Get(0)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("a"))) == 0, lang.StringWrap(lang.StringLiteral("First element should be a")))
    val = l.Get(1)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("b"))) == 0, lang.StringWrap(lang.StringLiteral("Second element should be b")))
    val = l.Get(2)
    this.AssertTrue(val.CompareTo(lang.StringWrap(lang.StringLiteral("c"))) == 0, lang.StringWrap(lang.StringLiteral("Third element should be c")))
}

func TestTestList(t *testing.T) {
    t.Run("add", func(t *testing.T) {
        test := testListNew()
        test.SetGoTestingT(t)
        test.add()
    })
    t.Run("addRange", func(t *testing.T) {
        test := testListNew()
        test.SetGoTestingT(t)
        test.addRange()
    })
    t.Run("removeIndex", func(t *testing.T) {
        test := testListNew()
        test.SetGoTestingT(t)
        test.removeIndex()
    })
    t.Run("removeRange", func(t *testing.T) {
        test := testListNew()
        test.SetGoTestingT(t)
        test.removeRange()
    })
}
