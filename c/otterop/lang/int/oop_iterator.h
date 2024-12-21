#ifndef __otterop_lang_OOPIterator_int
#define __otterop_lang_OOPIterator_int

typedef struct otterop_lang_OOPIterator_s otterop_lang_OOPIterator_t;


otterop_lang_OOPIterator_t *otterop_lang_OOPIterator_new(void *implementation, int (*has_next)(void *), void *(*next)(void *));


int otterop_lang_OOPIterator_has_next(otterop_lang_OOPIterator_t *self);


void *otterop_lang_OOPIterator_next(otterop_lang_OOPIterator_t *self);
#endif
