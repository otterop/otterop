#ifndef __otterop_test_TestBase
#define __otterop_test_TestBase
#include <otterop/lang/string.h>

typedef struct otterop_test_TestBase_s otterop_test_TestBase_t;




otterop_test_TestBase_t *otterop_test_TestBase_new();

void otterop_test_TestBase_assert_true(otterop_test_TestBase_t *this, int value, otterop_lang_String_t *message);

#endif
