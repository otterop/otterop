#include <gc.h>
#include <otterop/lang/int/oop_iterable.h>

typedef struct otterop_lang_OOPIterable_s {
    void *implementation;
    otterop_lang_OOPIterator_t *(*oop_iterator)(void *);
} otterop_lang_OOPIterable_t;


otterop_lang_OOPIterable_t *otterop_lang_OOPIterable_new(void *implementation, otterop_lang_OOPIterator_t *(*oop_iterator)(void *)) {
    otterop_lang_OOPIterable_t *self = GC_malloc(sizeof(otterop_lang_OOPIterable_t));
    self->implementation = implementation;
    self->oop_iterator = oop_iterator;
    return self;
}


otterop_lang_OOPIterator_t *otterop_lang_OOPIterable_oop_iterator(otterop_lang_OOPIterable_t *self);


otterop_lang_OOPIterator_t *otterop_lang_OOPIterable_oop_iterator(otterop_lang_OOPIterable_t *self) {
    return self->oop_iterator(self->implementation);
}