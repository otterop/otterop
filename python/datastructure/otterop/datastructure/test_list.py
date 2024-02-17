from otterop.lang.array import Array
from otterop.lang.generic import Generic
from otterop.lang.string import String
from otterop.test.test_base import TestBase
from otterop.datastructure.list import List

class TestList(TestBase):

    def add(self):
        l = List()
        l.add(String.wrap("a"))
        l.add(String.wrap("b"))
        l.add(String.wrap("c"))
        l.add(String.wrap("d"))
        l.add(String.wrap("e"))
        self.assert_true(l.size() == 5, String.wrap("Size should be 5"))

    def add_range(self):
        generic_t = Generic().zero()
        l = List()
        to_add = Array.new_array(5, generic_t)
        to_add.set(0, String.wrap("a"))
        to_add.set(1, String.wrap("b"))
        to_add.set(2, String.wrap("c"))
        to_add.set(3, String.wrap("d"))
        to_add.set(4, String.wrap("e"))
        l.add_array(to_add)
        self.assert_true(l.size() == 5, String.wrap("Size should be 5"))

    def remove_index(self):
        
        l = List()
        l.add(String.wrap("a"))
        l.add(String.wrap("b"))
        l.add(String.wrap("c"))
        l.add(String.wrap("d"))
        l.add(String.wrap("e"))
        l.remove_index(1)
        l.remove_index(1)
        self.assert_true(l.size() == 3, String.wrap("Size should be 3"))
        val = l.get(0)
        self.assert_true(val.compare_to(String.wrap("a")) == 0, String.wrap("First element should be a"))
        val = l.get(1)
        self.assert_true(val.compare_to(String.wrap("d")) == 0, String.wrap("Second element should be d"))
        val = l.get(2)
        self.assert_true(val.compare_to(String.wrap("e")) == 0, String.wrap("Third element should be e"))

    def remove_range(self):
        
        l = List()
        l.add(String.wrap("a"))
        l.add(String.wrap("b"))
        l.add(String.wrap("c"))
        l.add(String.wrap("d"))
        l.add(String.wrap("e"))
        l.remove_range(3, 2)
        self.assert_true(l.size() == 3, String.wrap("Size should be 3"))
        val = l.get(0)
        self.assert_true(val.compare_to(String.wrap("a")) == 0, String.wrap("First element should be a"))
        val = l.get(1)
        self.assert_true(val.compare_to(String.wrap("b")) == 0, String.wrap("Second element should be b"))
        val = l.get(2)
        self.assert_true(val.compare_to(String.wrap("c")) == 0, String.wrap("Third element should be c"))

def test_add():
    TestList().add()

def test_add_range():
    TestList().add_range()

def test_remove_index():
    TestList().remove_index()

def test_remove_range():
    TestList().remove_range()
