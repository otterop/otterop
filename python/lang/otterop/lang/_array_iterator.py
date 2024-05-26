
class ArrayIterator:
        
    def __init__(self, array):
        self._array = array
        self._i = 0

    def has_next(self):
        return self._i < self._array.size()

    def next(self):
        ret = self._array.get(self._i)
        self._i += 1
        return ret
