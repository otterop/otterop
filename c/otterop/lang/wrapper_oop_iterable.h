#ifndef __otterop_lang_WrapperOOPIterable
#define __otterop_lang_WrapperOOPIterable
#include <otterop/lang/oop_iterable.h>
#include <stdint.h>

typedef struct otterop_lang_WrapperOOPIterable_s otterop_lang_WrapperOOPIterable_t;

otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap_array(void **array, int32_t array_cnt,  void *(*wrap)(void *));

void **
otterop_lang_WrapperOOPIterable_unwrap_array(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), int32_t *array_cnt);


otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap(void *it_self, unsigned char (*it)(void *self, void **next),  void *(*wrap)(void *));

void *otterop_lang_WrapperOOPIterable_unwrap(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), unsigned char (**_ret_it)(void *self, void **next));

#endif
