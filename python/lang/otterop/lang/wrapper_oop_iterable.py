from otterop.lang.wrapper_oop_iterator import WrapperOOPIterator


class WrapperOOPIterable:

    def __init__(self, iterable, wrap):
        self._iterable = iterable
        self._wrap = wrap or (lambda x: x)

    def oop_iterator(self):
        return WrapperOOPIterator(self._iterable.__iter__(), self._wrap)

    def __iter__(self):
        return WrapperOOPIterator(self._iterable.__iter__(), self._wrap)

    @staticmethod
    def wrap(iterable, wrap):
        if wrap is None and isinstance(iterable, WrapperOOPIterable):
            return iterable
        return WrapperOOPIterable(iterable, wrap)

    @staticmethod
    def unwrap(oop_iterable, unwrap):
        if unwrap is None:
            return oop_iterable
        return WrapperOOPIterable(oop_iterable, unwrap)
