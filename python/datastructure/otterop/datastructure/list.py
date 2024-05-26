from otterop.lang.array import Array as _Array
from otterop.lang.generic import Generic as _Generic
from otterop.lang.panic import Panic as _Panic
from otterop.lang.pure_iterator import PureIterator as _PureIterator
from otterop.lang.string import String as _String
from otterop.datastructure._list_iterator import ListIterator as _ListIterator

class List:
                
    def __init__(self):
        self._size = 0
        self._capacity = 4
        generic_t = _Generic()
        self._t_zero = generic_t.zero()
        self._array = _Array.new_array(self._capacity, self._t_zero)

    def _ensure_capacity(self, capacity):
        if self._capacity < capacity:
            self._capacity = self._capacity * 2
            new_array = _Array.new_array(self._capacity, self._t_zero)
            _Array.copy(self._array, 0, new_array, 0, self._size)
            self._array = new_array

    def add(self, element):
        self._ensure_capacity(self._size + 1)
        self._array.set(self._size, element)
        self._size += 1

    def add_array(self, src):
        self._ensure_capacity(self._size + src.size())
        _Array.copy(src, 0, self._array, self._size, src.size())
        self._size += src.size()

    def add_list(self, src):
        self.add_array(src._array)

    def _check_index_out_of_bounds(self, index):
        if index < 0 or index > self._size:
            _Panic.index_out_of_bounds(_String.wrap("index is outside list bounds"))

    def insert(self, index, element):
        self._check_index_out_of_bounds(index)
        self._ensure_capacity(self._size + 1)
        if index < self._size:
            _Array.copy(self._array, index, self._array, index + 1, self._size - index)
        self._array.set(index, element)
        self._size += 1

    def insert_array(self, index, src):
        self._check_index_out_of_bounds(index)
        self._ensure_capacity(self._size + src.size())
        if index < self._size:
            _Array.copy(self._array, index, self._array, index + src.size(), self._size - index)
        _Array.copy(src, 0, self._array, index, src.size())
        self._size += src.size()

    def insert_list(self, index, src):
        self.insert_array(index, src._array)

    def get(self, index):
        self._check_index_out_of_bounds(index)
        return self._array.get(index)

    def remove_index(self, index):
        self._check_index_out_of_bounds(index)
        ret = self._array.get(index)
        if index + 1 < self._size:
            _Array.copy(self._array, index + 1, self._array, index, self._size - index - 1)
        self._size -= 1
        return ret

    def remove_range(self, index, count):
        self._check_index_out_of_bounds(index)
        if index + count > self._size:
            count = self._size - index
        ret = List()
        removed = _Array.new_array(count, self._t_zero)
        _Array.copy(self._array, index, removed, 0, count)
        ret.add_array(removed)
        if index + count < self._size:
            _Array.copy(self._array, index + count, self._array, index, self._size - index - count)
        self._size = self._size - count
        return ret

    def size(self):
        return self._size

    def oop_iterator(self):
        return _ListIterator(self)

    def __iter__(self):
        return _PureIterator.new_iterator(self.oop_iterator())
