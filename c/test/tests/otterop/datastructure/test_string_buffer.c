#include "unity.h"
#include "unity_fixture.h"
#include <otterop/datastructure/test_string_buffer.h>
#include <otterop/datastructure/int/string_buffer.h>
#include <gc.h>

typedef struct otterop_datastructure_TestStringBuffer_s {
    otterop_test_TestBase_t *_super;
} otterop_datastructure_TestStringBuffer_t;




void otterop_datastructure_TestStringBuffer_empty(otterop_datastructure_TestStringBuffer_t *this) {
    otterop_datastructure_StringBuffer_t *sb = otterop_datastructure_StringBuffer_new();
    otterop_lang_String_t *s = otterop_datastructure_StringBuffer_oop_string(sb);
    otterop_datastructure_TestStringBuffer_assert_true(this, otterop_lang_String_compare_to(s, otterop_lang_String_wrap("")) == 0, otterop_lang_String_wrap("Should be an empty string"));
}

void otterop_datastructure_TestStringBuffer_add_more_strings(otterop_datastructure_TestStringBuffer_t *this) {
    otterop_datastructure_StringBuffer_t *sb = otterop_datastructure_StringBuffer_new();
    otterop_datastructure_StringBuffer_add(sb, otterop_lang_String_wrap("a"));
    otterop_lang_String_t *s = otterop_datastructure_StringBuffer_oop_string(sb);
    otterop_datastructure_TestStringBuffer_assert_true(this, otterop_lang_String_compare_to(s, otterop_lang_String_wrap("a")) == 0, otterop_lang_String_wrap("Should be equals to 'a'"));
    otterop_datastructure_StringBuffer_add(sb, otterop_lang_String_wrap(",b"));
    s = otterop_datastructure_StringBuffer_oop_string(sb);
    otterop_datastructure_TestStringBuffer_assert_true(this, otterop_lang_String_compare_to(s, otterop_lang_String_wrap("a,b")) == 0, otterop_lang_String_wrap("Should be equals to 'a,b'"));
}

void otterop_datastructure_TestStringBuffer_assert_true(otterop_datastructure_TestStringBuffer_t *this, int arg0, otterop_lang_String_t *arg1) {
    return  otterop_test_TestBase_assert_true(this->_super, arg0, arg1);
}

otterop_datastructure_TestStringBuffer_t* otterop_datastructure_TestStringBuffer_new() {
    otterop_datastructure_TestStringBuffer_t *this = GC_malloc(sizeof(*this));
    this->_super = otterop_test_TestBase_new();
    return this;
}

TEST_GROUP(otterop_datastructure_TestStringBuffer);

TEST_SETUP(otterop_datastructure_TestStringBuffer) {}

TEST_TEAR_DOWN(otterop_datastructure_TestStringBuffer) {}

TEST(otterop_datastructure_TestStringBuffer, otterop_datastructure_TestStringBuffer_empty) {
    otterop_datastructure_TestStringBuffer_empty(otterop_datastructure_TestStringBuffer_new());
}

TEST(otterop_datastructure_TestStringBuffer, otterop_datastructure_TestStringBuffer_add_more_strings) {
    otterop_datastructure_TestStringBuffer_add_more_strings(otterop_datastructure_TestStringBuffer_new());
}

TEST_GROUP_RUNNER(otterop_datastructure_TestStringBuffer) {
    RUN_TEST_CASE(otterop_datastructure_TestStringBuffer, otterop_datastructure_TestStringBuffer_empty);
    RUN_TEST_CASE(otterop_datastructure_TestStringBuffer, otterop_datastructure_TestStringBuffer_add_more_strings);
}
