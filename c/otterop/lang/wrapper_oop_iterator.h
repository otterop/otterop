#ifndef __otterop_lang_WrapperOOPIterator
#define __otterop_lang_WrapperOOPIterator
#include <otterop/lang/oop_iterator.h>
#include <stdint.h>

typedef struct otterop_lang_WrapperOOPIterator_s otterop_lang_WrapperOOPIterator_t;

otterop_lang_OOPIterator_t*
otterop_lang_WrapperOOPIterator_wrap_array(void **array, int32_t array_cnt, void *(*wrap)(void *));

void **
otterop_lang_WrapperOOPIterator_unwrap_array(otterop_lang_OOPIterator_t *it, void *(*unwrap)(void *), int32_t *array_cnt);

otterop_lang_OOPIterator_t*
otterop_lang_WrapperOOPIterator_wrap(void *it_self, unsigned char (*it)(void *self, void **next), void *(*wrap)(void *));

void *otterop_lang_WrapperOOPIterator_unwrap(otterop_lang_OOPIterator_t *self, void *(*unwrap)(void *), unsigned char (**_ret_it)(void *self, void **next));

#endif
