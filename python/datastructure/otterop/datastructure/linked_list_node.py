
class LinkedListNode:
                
    def __init__(self, value):
        self._value = value

    def list(self):
        return self._list

    def _set_list(self, list):
        self._list = list

    def prev(self):
        return self._prev

    def _set_prev(self, node):
        self._prev = node

    def next(self):
        return self._next

    def _set_next(self, node):
        self._next = node

    def value(self):
        return self._value
