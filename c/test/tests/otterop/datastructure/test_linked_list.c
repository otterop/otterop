#include "unity.h"
#include "unity_fixture.h"
#include <otterop/datastructure/test_linked_list.h>
#include <otterop/datastructure/int/linked_list.h>
#include <stdint.h>
#include <gc.h>

typedef struct otterop_datastructure_TestLinkedList_s {
    otterop_test_TestBase_t *_super;
} otterop_datastructure_TestLinkedList_t;




void otterop_datastructure_TestLinkedList_add(otterop_datastructure_TestLinkedList_t *self) {
    otterop_lang_String_t *a = otterop_lang_String_wrap("a");
    otterop_lang_String_t *b = otterop_lang_String_wrap("b");
    otterop_lang_String_t *c = otterop_lang_String_wrap("c");
    otterop_lang_String_t *d = otterop_lang_String_wrap("d");
    otterop_lang_String_t *e = otterop_lang_String_wrap("e");
    otterop_lang_Array_t *strings = otterop_lang_Array_new_array(5, a);
    otterop_lang_Array_set(strings, 0, a);
    otterop_lang_Array_set(strings, 1, b);
    otterop_lang_Array_set(strings, 2, c);
    otterop_lang_Array_set(strings, 3, d);
    otterop_lang_Array_set(strings, 4, e);
    otterop_lang_Array_t *expected = otterop_lang_Array_new_array(5, a);
    otterop_lang_Array_set(expected, 0, e);
    otterop_lang_Array_set(expected, 1, d);
    otterop_lang_Array_set(expected, 2, a);
    otterop_lang_Array_set(expected, 3, b);
    otterop_lang_Array_set(expected, 4, c);
    otterop_datastructure_LinkedList_t *l = otterop_datastructure_LinkedList_new();
    otterop_datastructure_LinkedList_add_last(l, a);
    otterop_datastructure_LinkedList_add_last(l, b);
    otterop_datastructure_LinkedList_add_last(l, c);
    otterop_datastructure_LinkedList_add_first(l, d);
    otterop_datastructure_LinkedList_add_first(l, e);
    otterop_datastructure_TestLinkedList_assert_true(self, otterop_datastructure_LinkedList_size(l) == 5, otterop_lang_String_wrap("Size should be 5"));
    int32_t i = 0;
    otterop_lang_OOPIterator_t *__it_l = otterop_datastructure_LinkedList_oop_iterator(l);
    for (; otterop_lang_OOPIterator_has_next(__it_l);) {
        otterop_lang_String_t *s = otterop_lang_OOPIterator_next(__it_l);
        otterop_datastructure_TestLinkedList_assert_true(self, otterop_lang_String_compare_to(s, otterop_lang_Array_get(expected, i)) == 0, otterop_lang_String_wrap("Element mismatch"));
        i++;
    }
}

void otterop_datastructure_TestLinkedList_assert_true(otterop_datastructure_TestLinkedList_t *self, unsigned char arg0, otterop_lang_String_t *arg1) {
    return  otterop_test_TestBase_assert_true(self->_super, arg0, arg1);
}

otterop_datastructure_TestLinkedList_t* otterop_datastructure_TestLinkedList_new() {
    otterop_datastructure_TestLinkedList_t *self = GC_malloc(sizeof(*self));
    self->_super = otterop_test_TestBase_new();
    return self;
}

TEST_GROUP(otterop_datastructure_TestLinkedList);

TEST_SETUP(otterop_datastructure_TestLinkedList) {}

TEST_TEAR_DOWN(otterop_datastructure_TestLinkedList) {}

TEST(otterop_datastructure_TestLinkedList, otterop_datastructure_TestLinkedList_add) {
    otterop_datastructure_TestLinkedList_add(otterop_datastructure_TestLinkedList_new());
}

TEST_GROUP_RUNNER(otterop_datastructure_TestLinkedList) {
    RUN_TEST_CASE(otterop_datastructure_TestLinkedList, otterop_datastructure_TestLinkedList_add);
}
