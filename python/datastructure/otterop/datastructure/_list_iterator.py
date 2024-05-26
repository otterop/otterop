
class ListIterator:
        
    def __init__(self, list):
        self._list = list
        self._index = 0

    def has_next(self):
        return self._index < self._list.size()

    def next(self):
        ret = self._list.get(self._index)
        self._index += 1
        return ret
