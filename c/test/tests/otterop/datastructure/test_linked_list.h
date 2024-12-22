#ifndef __otterop_datastructure_TestLinkedList
#define __otterop_datastructure_TestLinkedList
#include <otterop/lang/array.h>
#include <otterop/lang/string.h>
#include <otterop/test/test_base.h>

typedef struct otterop_lang_Array_s otterop_lang_Array_t;
typedef struct otterop_lang_String_s otterop_lang_String_t;
typedef struct otterop_test_TestBase_s otterop_test_TestBase_t;

typedef struct otterop_datastructure_TestLinkedList_s otterop_datastructure_TestLinkedList_t;




void otterop_datastructure_TestLinkedList_add(otterop_datastructure_TestLinkedList_t *self);


void otterop_datastructure_TestLinkedList_assert_true(otterop_datastructure_TestLinkedList_t *self, unsigned char arg0, otterop_lang_String_t *arg1);

otterop_datastructure_TestLinkedList_t* otterop_datastructure_TestLinkedList_new();
#endif
