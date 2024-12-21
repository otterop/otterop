#ifndef __otterop_lang_WrapperOOPIterable
#define __otterop_lang_WrapperOOPIterable
#include <otterop/lang/oop_iterable.h>

typedef struct otterop_lang_WrapperOOPIterable_s otterop_lang_WrapperOOPIterable_t;

otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap_array(void **array, int array_cnt,  void *(*wrap)(void *));

void **
otterop_lang_WrapperOOPIterable_unwrap_array(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), int *array_cnt);


otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap(void *it_self, int (*it)(void *self, void **next),  void *(*wrap)(void *));

void *otterop_lang_WrapperOOPIterable_unwrap(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), int (**_ret_it)(void *self, void **next));

#endif
