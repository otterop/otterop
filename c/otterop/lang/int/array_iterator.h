#ifndef __otterop_lang_ArrayIterator_int
#define __otterop_lang_ArrayIterator_int
#include <otterop/lang/int/array.h>
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_Array_s otterop_lang_Array_t;
typedef struct otterop_lang_OOPIterator_s otterop_lang_OOPIterator_t;

typedef struct otterop_lang_ArrayIterator_s otterop_lang_ArrayIterator_t;




otterop_lang_ArrayIterator_t *otterop_lang_ArrayIterator_new(otterop_lang_Array_t *array);

int otterop_lang_ArrayIterator_has_next(otterop_lang_ArrayIterator_t *this);


void *otterop_lang_ArrayIterator_next(otterop_lang_ArrayIterator_t *this);


otterop_lang_OOPIterator_t
*otterop_lang_ArrayIterator__to_otterop_lang_OOPIterator(otterop_lang_ArrayIterator_t *this);


#endif
