class String:

    def __init__(self, _wrapped):
        self._wrapped = _wrapped

    @staticmethod
    def length():
        return len(self._wrapped)

    @staticmethod
    def wrap(_wrapped):
        return String(_wrapped)

    def __str__(self):
        return self._wrapped

    def compare_to(self, other):
        if not isinstance(other, String):
            return 1
        if self._wrapped < other._wrapped:
            return -1
        elif self._wrapped > other._wrapped:
            return 1
        return 0
