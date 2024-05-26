package datastructure

import (
    "testing"
    lang "github.com/otterop/otterop/go/lang"
    test "github.com/otterop/otterop/go/test"
)


func testLinkedListNew() *TestLinkedList {
    this := new(TestLinkedList)
    this.TestBase = test.TestBaseNew()
    return this
}

type TestLinkedList struct {
    *test.TestBase
}




func (this *TestLinkedList) add()  {
    var a *lang.String = lang.StringWrap(lang.StringLiteral("a"))
    var b *lang.String = lang.StringWrap(lang.StringLiteral("b"))
    var c *lang.String = lang.StringWrap(lang.StringLiteral("c"))
    var d *lang.String = lang.StringWrap(lang.StringLiteral("d"))
    var e *lang.String = lang.StringWrap(lang.StringLiteral("e"))
    var strings *lang.Array[*lang.String] = lang.ArrayNewArray(5, a)
    strings.Set(0, a)
    strings.Set(1, b)
    strings.Set(2, c)
    strings.Set(3, d)
    strings.Set(4, e)
    var expected *lang.Array[*lang.String] = lang.ArrayNewArray(5, a)
    expected.Set(0, e)
    expected.Set(1, d)
    expected.Set(2, a)
    expected.Set(3, b)
    expected.Set(4, c)
    var l *LinkedList[*lang.String] = LinkedListNew[*lang.String]()
    l.AddLast(a)
    l.AddLast(b)
    l.AddLast(c)
    l.AddFirst(d)
    l.AddFirst(e)
    this.AssertTrue(l.Size() == 5, lang.StringWrap(lang.StringLiteral("Size should be 5")))
    var i int = 0
    for it := (l).OOPIterator(); it.HasNext(); {
        s := it.Next()
        this.AssertTrue(s.CompareTo(expected.Get(i)) == 0, lang.StringWrap(lang.StringLiteral("Element mismatch")))
        i++
    }
}

func TestTestLinkedList(t *testing.T) {
    t.Run("add", func(t *testing.T) {
        test := testLinkedListNew()
        test.SetGoTestingT(t)
        test.add()
    })
}
