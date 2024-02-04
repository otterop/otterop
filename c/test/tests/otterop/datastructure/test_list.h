#ifndef __otterop_datastructure_TestList
#define __otterop_datastructure_TestList
#include <otterop/lang/array.h>
#include <otterop/lang/generic.h>
#include <otterop/lang/string.h>
#include <otterop/test/test_base.h>

typedef struct otterop_datastructure_TestList_s otterop_datastructure_TestList_t;




void otterop_datastructure_TestList_add(otterop_datastructure_TestList_t *this);


void otterop_datastructure_TestList_add_range(otterop_datastructure_TestList_t *this);


void otterop_datastructure_TestList_remove_index(otterop_datastructure_TestList_t *this);


void otterop_datastructure_TestList_remove_range(otterop_datastructure_TestList_t *this);


void otterop_datastructure_TestList_assert_true(otterop_datastructure_TestList_t *this, int arg0, otterop_lang_String_t *arg1);

otterop_datastructure_TestList_t* otterop_datastructure_TestList_new();
#endif
