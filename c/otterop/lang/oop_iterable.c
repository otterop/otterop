#include <gc.h>
#include <otterop/lang/int/oop_iterable.h>

typedef struct otterop_lang_OOPIterable_s otterop_lang_OOPIterable_t;

typedef struct otterop_lang_OOPIterable_s {
    void *implementation;
    otterop_lang_OOPIterator_t *(*oop_iterator)(void *);
} otterop_lang_OOPIterable_t;


otterop_lang_OOPIterable_t *otterop_lang_OOPIterable_new(void *implementation, otterop_lang_OOPIterator_t *(*oop_iterator)(void *)) {
    otterop_lang_OOPIterable_t *this = GC_malloc(sizeof(otterop_lang_OOPIterable_t));
    this->implementation = implementation;
    this->oop_iterator = oop_iterator;
    return this;
}


otterop_lang_OOPIterator_t *otterop_lang_OOPIterable_oop_iterator(otterop_lang_OOPIterable_t *this);


otterop_lang_OOPIterator_t *otterop_lang_OOPIterable_oop_iterator(otterop_lang_OOPIterable_t *this) {
    return this->oop_iterator(this->implementation);
}