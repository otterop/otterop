
class Panic:

    @staticmethod
    def index_out_of_bounds(message):
        raise IndexError(message.unwrap())
