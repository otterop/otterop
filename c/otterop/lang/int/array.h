#ifndef __otterop_lang_Array_int
#define __otterop_lang_Array_int
#include <stddef.h>
#include <stdlib.h>
#include <gc.h>
#include <otterop/lang/string.h>
#include <otterop/lang/int/oop_iterable.h>
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_Array_s otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_new_array(int size, void *clazz);

otterop_lang_Array_t *otterop_lang_Array_wrap(void *wrapped, int wrapped_cnt);

otterop_lang_Array_t *otterop_lang_Array_wrap_string(char **wrapped, int wrapped_cnt);

void otterop_lang_Array_copy(otterop_lang_Array_t *src, int src_pos,
                             otterop_lang_Array_t *dst, int dst_pos, int size);

void *otterop_lang_Array_get(otterop_lang_Array_t *self, int i);

void otterop_lang_Array_set(otterop_lang_Array_t *self, int i, void* value);

otterop_lang_Array_t *otterop_lang_Array_slice(otterop_lang_Array_t *self, int start, int end);

int otterop_lang_Array_size(otterop_lang_Array_t *self);

otterop_lang_OOPIterator_t *otterop_lang_Array_oop_iterator(otterop_lang_Array_t *self);

otterop_lang_OOPIterable_t
*otterop_lang_Array__to_otterop_lang_OOPIterable(otterop_lang_Array_t *self);

#endif
