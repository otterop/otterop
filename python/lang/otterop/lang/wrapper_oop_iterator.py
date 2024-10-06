class WrapperOOPIterator:

    def __init__(self, it, convert):
        self._it = it
        self._convert = convert
        self._next = None
        self._has_next = None

    def __next__(self):
        return self.next()

    def has_next(self):
        if self._has_next is None:
            try:
                self._next = self._it.__next__()
                if self._convert is not None:
                    self._next = self._convert(self._next)
                self._has_next = True
            except StopIteration as e:
                self._has_next = False
        return self._has_next

    def next(self):
        if self._has_next is None:
            self.has_next()
        if self._has_next is False:
            raise StopIteration()
        self._has_next = None
        return self._next

