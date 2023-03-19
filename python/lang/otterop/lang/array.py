from otterop.lang.string import String

class Array:
    def __init__(self, list, start, end):
        self._wrapped = list
        self._start = start
        self._end = end

    def get(self, i):
        return self._wrapped[self._start + i]

    def set(self, i, value):
        self._wrapped[self._start + i] = value

    def size(self):
        return self._end - self._start

    def slice(self, start, end):
        new_start = self._start + start
        new_end = self._start + end
        if new_start < self._start or new_start > self._end or new_end < new_start \
           or new_end > self._end:
             raise IndexError("slice arguments out of bounds")
        return Array(self._wrapped, new_start, new_end)

    @staticmethod
    def wrap(list):
        return Array(list, 0, len(list))

    @staticmethod
    def wrap_string(list):
        list = [ String.wrap(s) for s in list ]
        return Array.wrap(list)