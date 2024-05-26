
class LinkedListIterator:
    
    def __init__(self, linked_list):
        self._current = linked_list.first()

    def has_next(self):
        return self._current != None

    def next(self):
        ret = self._current
        self._current = self._current.next()
        return ret.value()
