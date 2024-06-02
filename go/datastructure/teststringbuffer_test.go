package datastructure

import (
    "testing"
    lang "github.com/otterop/otterop/go/lang"
    test "github.com/otterop/otterop/go/test"
)


func testStringBufferNew() *TestStringBuffer {
    this := new(TestStringBuffer)
    this.TestBase = test.TestBaseNew()
    return this
}

type TestStringBuffer struct {
    *test.TestBase
}




func (this *TestStringBuffer) empty()  {
    var sb *StringBuffer = StringBufferNew()
    var s *lang.String = sb.OOPString()
    this.AssertTrue(s.CompareTo(lang.StringWrap(lang.StringLiteral(""))) == 0, lang.StringWrap(lang.StringLiteral("Should be an empty string")))
}

func (this *TestStringBuffer) addMoreStrings()  {
    var sb *StringBuffer = StringBufferNew()
    sb.Add(lang.StringWrap(lang.StringLiteral("a")))
    var s *lang.String = sb.OOPString()
    this.AssertTrue(s.CompareTo(lang.StringWrap(lang.StringLiteral("a"))) == 0, lang.StringWrap(lang.StringLiteral("Should be equals to 'a'")))
    sb.Add(lang.StringWrap(lang.StringLiteral(",b")))
    s = sb.OOPString()
    this.AssertTrue(s.CompareTo(lang.StringWrap(lang.StringLiteral("a,b"))) == 0, lang.StringWrap(lang.StringLiteral("Should be equals to 'a,b'")))
}

func TestTestStringBuffer(t *testing.T) {
    t.Run("empty", func(t *testing.T) {
        test := testStringBufferNew()
        test.SetGoTestingT(t)
        test.empty()
    })
    t.Run("addMoreStrings", func(t *testing.T) {
        test := testStringBufferNew()
        test.SetGoTestingT(t)
        test.addMoreStrings()
    })
}
