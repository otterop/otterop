from otterop.lang.string import String as _String

class Error:
        
    def __init__(self, code, message):
        self._code = code
        self._message = message

    def code(self):
        return self._code

    def message(self):
        return self._message
