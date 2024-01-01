from otterop.lang.string import String

class Error:
        
    def __init__(self, code, message):
        self.code = code
        self.message = message

    def code(self):
        return self.code

    def message(self):
        return self.message
