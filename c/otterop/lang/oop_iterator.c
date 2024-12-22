#include <gc.h>
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_OOPIterator_s {
    void *implementation;
    unsigned char (*has_next)(void *);
    void *(*next)(void *);
} otterop_lang_OOPIterator_t;


otterop_lang_OOPIterator_t *otterop_lang_OOPIterator_new(void *implementation, unsigned char (*has_next)(void *), void *(*next)(void *)) {
    otterop_lang_OOPIterator_t *self = GC_malloc(sizeof(otterop_lang_OOPIterator_t));
    self->implementation = implementation;
    self->has_next = has_next;
    self->next = next;
    return self;
}


unsigned char otterop_lang_OOPIterator_has_next(otterop_lang_OOPIterator_t *self);


void *otterop_lang_OOPIterator_next(otterop_lang_OOPIterator_t *self);


unsigned char otterop_lang_OOPIterator_has_next(otterop_lang_OOPIterator_t *self) {
    return self->has_next(self->implementation);
}

void *otterop_lang_OOPIterator_next(otterop_lang_OOPIterator_t *self) {
    return self->next(self->implementation);
}