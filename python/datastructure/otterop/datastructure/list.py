from otterop.lang.array import Array
from otterop.lang.generic import Generic
from otterop.lang.panic import Panic
from otterop.lang.string import String

class List:
                
    def __init__(self):
        self._size = 0
        self._capacity = 4
        generic_t = Generic()
        self._t_zero = generic_t.zero()
        self._array = Array.new_array(self._capacity, self._t_zero)

    def ensure_capacity(self, capacity):
        if self._capacity < capacity:
            self._capacity = self._capacity * 2
            new_array = Array.new_array(self._capacity, self._t_zero)
            Array.copy(self._array, 0, new_array, 0, self._size)
            self._array = new_array

    def add(self, element):
        self.ensure_capacity(self._size + 1)
        arr = self._array
        arr.set(self._size, element)
        self._size += 1

    def add_array(self, src):
        self.ensure_capacity(self._size + src.size())
        Array.copy(src, 0, self._array, self._size, src.size())
        self._size += src.size()

    def add_list(self, src):
        self.add_array(src.array)

    def check_index_out_of_bounds(self, index):
        if index < 0 or index > self._size:
            Panic.index_out_of_bounds(String.wrap("index is outside list bounds"))

    def insert(self, index, element):
        self.check_index_out_of_bounds(index)
        self.ensure_capacity(self._size + 1)
        if index < self._size:
            Array.copy(self._array, index, self._array, index + 1, self._size - index)
        arr = self._array
        arr.set(index, element)
        self._size += 1

    def insert_array(self, index, src):
        self.check_index_out_of_bounds(index)
        self.ensure_capacity(self._size + src.size())
        if index < self._size:
            Array.copy(self._array, index, self._array, index + src.size(), self._size - index)
        Array.copy(src, 0, self._array, index, src.size())
        self._size += src.size()

    def insert_list(self, index, src):
        self.insert_array(index, src.array)

    def get(self, index):
        self.check_index_out_of_bounds(index)
        arr = self._array
        return arr.get(index)

    def remove_index(self, index):
        self.check_index_out_of_bounds(index)
        arr = self._array
        ret = arr.get(index)
        if index + 1 < self._size:
            Array.copy(self._array, index + 1, self._array, index, self._size - index - 1)
        self._size -= 1
        return ret

    def remove_range(self, index, count):
        self.check_index_out_of_bounds(index)
        if index + count > self._size:
            count = self._size - index
        ret = List()
        removed = Array.new_array(count, self._t_zero)
        Array.copy(self._array, index, removed, 0, count)
        ret.add_array(removed)
        if index + count < self._size:
            Array.copy(self._array, index + count, self._array, index, self._size - index - count)
        self._size = self._size - count
        return ret

    def size(self):
        return self._size
