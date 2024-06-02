from otterop.lang.string import String as _String
from otterop.test.test_base import TestBase as _TestBase
from otterop.datastructure.string_buffer import StringBuffer as _StringBuffer

class TestStringBuffer(_TestBase):

    def empty(self):
        sb = _StringBuffer()
        s = sb.oop_string()
        self.assert_true(s.compare_to(_String.wrap("")) == 0, _String.wrap("Should be an empty string"))

    def add_more_strings(self):
        sb = _StringBuffer()
        sb.add(_String.wrap("a"))
        s = sb.oop_string()
        self.assert_true(s.compare_to(_String.wrap("a")) == 0, _String.wrap("Should be equals to 'a'"))
        sb.add(_String.wrap(",b"))
        s = sb.oop_string()
        self.assert_true(s.compare_to(_String.wrap("a,b")) == 0, _String.wrap("Should be equals to 'a,b'"))

def test_empty():
    TestStringBuffer().empty()

def test_add_more_strings():
    TestStringBuffer().add_more_strings()
