from otterop.lang.array import Array as _Array
from otterop.lang.string import String as _String
from otterop.test.test_base import TestBase as _TestBase
from otterop.datastructure.linked_list import LinkedList as _LinkedList

class TestLinkedList(_TestBase):

    def add(self):
        a = _String.wrap("a")
        b = _String.wrap("b")
        c = _String.wrap("c")
        d = _String.wrap("d")
        e = _String.wrap("e")
        strings = _Array.new_array(5, a)
        strings.set(0, a)
        strings.set(1, b)
        strings.set(2, c)
        strings.set(3, d)
        strings.set(4, e)
        expected = _Array.new_array(5, a)
        expected.set(0, e)
        expected.set(1, d)
        expected.set(2, a)
        expected.set(3, b)
        expected.set(4, c)
        l = _LinkedList()
        l.add_last(a)
        l.add_last(b)
        l.add_last(c)
        l.add_first(d)
        l.add_first(e)
        self.assert_true(l.size() == 5, _String.wrap("Size should be 5"))
        i = 0
        for s in l:
            self.assert_true(s.compare_to(expected.get(i)) == 0, _String.wrap("Element mismatch"))
            i += 1


def test_add():
    TestLinkedList().add()
