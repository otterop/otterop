
class Result:
        
    def __init__(self, res, err):
        self.res = res
        self.err = err

    def is_ok(self):
        return self.err != None

    def err(self):
        return self.err

    def unwrap(self):
        return self.res

    @staticmethod
    def of(res, err):
        return Result(res, err)
