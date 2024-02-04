
class Result:
        
    def __init__(self, res, err):
        self._res = res
        self._err = err

    def err(self):
        return self._err

    def unwrap(self):
        return self._res

    @staticmethod
    def of(res, err):
        return Result(res, err)
