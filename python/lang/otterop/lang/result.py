class Result:

    def __init__(self, _res, _err):
        self._res = _res
        self._err = _err

    def is_ok(self):
        return self._err != None

    def err(self):
        return self._err

    def unwrap(self):
        return self._res

    @staticmethod
    def of(res, err):
        return Result(res, err)
