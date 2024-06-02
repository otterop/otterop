from otterop.lang.string import String as _String
from otterop.datastructure.linked_list import LinkedList as _LinkedList

class StringBuffer:
    
    def __init__(self):
        self._strings = _LinkedList()

    def add(self, s):
        self._strings.add_last(s)

    def oop_string(self):
        strings = self._strings
        return _String.concat(strings)
