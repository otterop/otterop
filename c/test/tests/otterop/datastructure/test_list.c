#include "unity.h"
#include "unity_fixture.h"
#include <otterop/datastructure/test_list.h>
#include <otterop/datastructure/int/list.h>
#include <gc.h>

typedef struct otterop_datastructure_TestList_s {
    otterop_test_TestBase_t *_super;
} otterop_datastructure_TestList_t;




void otterop_datastructure_TestList_add(otterop_datastructure_TestList_t *self) {
    otterop_datastructure_List_t *l = otterop_datastructure_List_new();
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("a"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("b"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("c"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("d"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("e"));
    otterop_datastructure_TestList_assert_true(self, otterop_datastructure_List_size(l) == 5, otterop_lang_String_wrap("Size should be 5"));
}

void otterop_datastructure_TestList_add_range(otterop_datastructure_TestList_t *self) {
    otterop_lang_Generic_t *generic_t = otterop_lang_Generic_new();
    otterop_lang_String_t *generic_t_zero = otterop_lang_Generic_zero(generic_t);
    otterop_datastructure_List_t *l = otterop_datastructure_List_new();
    otterop_lang_Array_t *to_add = otterop_lang_Array_new_array(5, generic_t_zero);
    otterop_lang_Array_set(to_add, 0, otterop_lang_String_wrap("a"));
    otterop_lang_Array_set(to_add, 1, otterop_lang_String_wrap("b"));
    otterop_lang_Array_set(to_add, 2, otterop_lang_String_wrap("c"));
    otterop_lang_Array_set(to_add, 3, otterop_lang_String_wrap("d"));
    otterop_lang_Array_set(to_add, 4, otterop_lang_String_wrap("e"));
    otterop_datastructure_List_add_array(l, to_add);
    otterop_datastructure_TestList_assert_true(self, otterop_datastructure_List_size(l) == 5, otterop_lang_String_wrap("Size should be 5"));
}

void otterop_datastructure_TestList_remove_index(otterop_datastructure_TestList_t *self) {
    otterop_lang_String_t *val;
    otterop_datastructure_List_t *l = otterop_datastructure_List_new();
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("a"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("b"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("c"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("d"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("e"));
    otterop_datastructure_List_remove_index(l, 1);
    otterop_datastructure_List_remove_index(l, 1);
    otterop_datastructure_TestList_assert_true(self, otterop_datastructure_List_size(l) == 3, otterop_lang_String_wrap("Size should be 3"));
    val = otterop_datastructure_List_get(l, 0);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("a")) == 0, otterop_lang_String_wrap("First element should be a"));
    val = otterop_datastructure_List_get(l, 1);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("d")) == 0, otterop_lang_String_wrap("Second element should be d"));
    val = otterop_datastructure_List_get(l, 2);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("e")) == 0, otterop_lang_String_wrap("Third element should be e"));
}

void otterop_datastructure_TestList_remove_range(otterop_datastructure_TestList_t *self) {
    otterop_lang_String_t *val;
    otterop_datastructure_List_t *l = otterop_datastructure_List_new();
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("a"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("b"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("c"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("d"));
    otterop_datastructure_List_add(l, otterop_lang_String_wrap("e"));
    otterop_datastructure_List_remove_range(l, 3, 2);
    otterop_datastructure_TestList_assert_true(self, otterop_datastructure_List_size(l) == 3, otterop_lang_String_wrap("Size should be 3"));
    val = otterop_datastructure_List_get(l, 0);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("a")) == 0, otterop_lang_String_wrap("First element should be a"));
    val = otterop_datastructure_List_get(l, 1);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("b")) == 0, otterop_lang_String_wrap("Second element should be b"));
    val = otterop_datastructure_List_get(l, 2);
    otterop_datastructure_TestList_assert_true(self, otterop_lang_String_compare_to(val, otterop_lang_String_wrap("c")) == 0, otterop_lang_String_wrap("Third element should be c"));
}

void otterop_datastructure_TestList_assert_true(otterop_datastructure_TestList_t *self, int arg0, otterop_lang_String_t *arg1) {
    return  otterop_test_TestBase_assert_true(self->_super, arg0, arg1);
}

otterop_datastructure_TestList_t* otterop_datastructure_TestList_new() {
    otterop_datastructure_TestList_t *self = GC_malloc(sizeof(*self));
    self->_super = otterop_test_TestBase_new();
    return self;
}

TEST_GROUP(otterop_datastructure_TestList);

TEST_SETUP(otterop_datastructure_TestList) {}

TEST_TEAR_DOWN(otterop_datastructure_TestList) {}

TEST(otterop_datastructure_TestList, otterop_datastructure_TestList_add) {
    otterop_datastructure_TestList_add(otterop_datastructure_TestList_new());
}

TEST(otterop_datastructure_TestList, otterop_datastructure_TestList_add_range) {
    otterop_datastructure_TestList_add_range(otterop_datastructure_TestList_new());
}

TEST(otterop_datastructure_TestList, otterop_datastructure_TestList_remove_index) {
    otterop_datastructure_TestList_remove_index(otterop_datastructure_TestList_new());
}

TEST(otterop_datastructure_TestList, otterop_datastructure_TestList_remove_range) {
    otterop_datastructure_TestList_remove_range(otterop_datastructure_TestList_new());
}

TEST_GROUP_RUNNER(otterop_datastructure_TestList) {
    RUN_TEST_CASE(otterop_datastructure_TestList, otterop_datastructure_TestList_add);
    RUN_TEST_CASE(otterop_datastructure_TestList, otterop_datastructure_TestList_add_range);
    RUN_TEST_CASE(otterop_datastructure_TestList, otterop_datastructure_TestList_remove_index);
    RUN_TEST_CASE(otterop_datastructure_TestList, otterop_datastructure_TestList_remove_range);
}
