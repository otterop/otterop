#include <otterop/test/test_base.h>
#include <gc.h>
#include "unity.h"

typedef struct otterop_test_TestBase_s {
} otterop_test_TestBase_t;




otterop_test_TestBase_t *otterop_test_TestBase_new() {
    otterop_test_TestBase_t *this = GC_malloc(sizeof(otterop_test_TestBase_t));
    return this;
}

void otterop_test_TestBase_assert_true(otterop_test_TestBase_t *this, int value, otterop_lang_String_t *message) {
    if (!value)
        TEST_FAIL_MESSAGE(otterop_lang_String_unwrap(message));
}
