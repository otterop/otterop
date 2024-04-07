from otterop.lang.array import Array as _Array
from otterop.lang.generic import Generic as _Generic
from otterop.lang.string import String as _String
from otterop.test.test_base import TestBase as _TestBase
from otterop.datastructure.list import List as _List

class TestList(_TestBase):

    def add(self):
        l = _List()
        l.add(_String.wrap("a"))
        l.add(_String.wrap("b"))
        l.add(_String.wrap("c"))
        l.add(_String.wrap("d"))
        l.add(_String.wrap("e"))
        self.assert_true(l.size() == 5, _String.wrap("Size should be 5"))

    def add_range(self):
        generic_t = _Generic()
        generic_t_zero = generic_t.zero()
        l = _List()
        to_add = _Array.new_array(5, generic_t_zero)
        to_add.set(0, _String.wrap("a"))
        to_add.set(1, _String.wrap("b"))
        to_add.set(2, _String.wrap("c"))
        to_add.set(3, _String.wrap("d"))
        to_add.set(4, _String.wrap("e"))
        l.add_array(to_add)
        self.assert_true(l.size() == 5, _String.wrap("Size should be 5"))

    def remove_index(self):
        
        l = _List()
        l.add(_String.wrap("a"))
        l.add(_String.wrap("b"))
        l.add(_String.wrap("c"))
        l.add(_String.wrap("d"))
        l.add(_String.wrap("e"))
        l.remove_index(1)
        l.remove_index(1)
        self.assert_true(l.size() == 3, _String.wrap("Size should be 3"))
        val = l.get(0)
        self.assert_true(val.compare_to(_String.wrap("a")) == 0, _String.wrap("First element should be a"))
        val = l.get(1)
        self.assert_true(val.compare_to(_String.wrap("d")) == 0, _String.wrap("Second element should be d"))
        val = l.get(2)
        self.assert_true(val.compare_to(_String.wrap("e")) == 0, _String.wrap("Third element should be e"))

    def remove_range(self):
        
        l = _List()
        l.add(_String.wrap("a"))
        l.add(_String.wrap("b"))
        l.add(_String.wrap("c"))
        l.add(_String.wrap("d"))
        l.add(_String.wrap("e"))
        l.remove_range(3, 2)
        self.assert_true(l.size() == 3, _String.wrap("Size should be 3"))
        val = l.get(0)
        self.assert_true(val.compare_to(_String.wrap("a")) == 0, _String.wrap("First element should be a"))
        val = l.get(1)
        self.assert_true(val.compare_to(_String.wrap("b")) == 0, _String.wrap("Second element should be b"))
        val = l.get(2)
        self.assert_true(val.compare_to(_String.wrap("c")) == 0, _String.wrap("Third element should be c"))

def test_add():
    TestList().add()

def test_add_range():
    TestList().add_range()

def test_remove_index():
    TestList().remove_index()

def test_remove_range():
    TestList().remove_range()
