#ifndef __otterop_lang_OOPIterable
#define __otterop_lang_OOPIterable
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_OOPIterator_s otterop_lang_OOPIterator_t;

typedef struct otterop_lang_OOPIterable_s otterop_lang_OOPIterable_t;


otterop_lang_OOPIterable_t *otterop_lang_OOPIterable_new(void *implementation, otterop_lang_OOPIterator_t *(*oop_iterator)(void *));


otterop_lang_OOPIterator_t *otterop_lang_OOPIterable_oop_iterator(otterop_lang_OOPIterable_t *this);
#endif
