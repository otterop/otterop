from otterop.lang.string import String

class TestBase:

    def assert_true(self, value, message):
        if not value:
            assert value, message.unwrap()
