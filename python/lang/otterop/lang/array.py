from otterop.lang.string import String

class Array:
    def __init__(self, list):
        self._wrapped = list

    def get(self, i):
        return self._wrapped[i]

    def set(self, i, value):
        self._wrapped[i] = value

    def size(self):
        return len(self._wrapped)

    @staticmethod
    def wrap(list):
        return Array(list)

    @staticmethod
    def wrap_string(list):
        list = [ String.wrap(s) for s in list ]
        return Array.wrap(list)