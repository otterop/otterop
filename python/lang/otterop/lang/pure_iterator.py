class PureIterator:
    def __init__(self, oop_iterator):
        self.oop_iterator = oop_iterator

    def __next__(self):
        if not self.oop_iterator.has_next():
            raise StopIteration
        return self.oop_iterator.next();

    @staticmethod
    def new_iterator(oop_iterator):
        return PureIterator(oop_iterator)
