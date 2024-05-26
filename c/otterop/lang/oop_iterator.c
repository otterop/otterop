#include <gc.h>
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_OOPIterator_s otterop_lang_OOPIterator_t;

typedef struct otterop_lang_OOPIterator_s {
    void *implementation;
    int (*has_next)(void *);
    void *(*next)(void *);
} otterop_lang_OOPIterator_t;


otterop_lang_OOPIterator_t *otterop_lang_OOPIterator_new(void *implementation, int (*has_next)(void *), void *(*next)(void *)) {
    otterop_lang_OOPIterator_t *this = GC_malloc(sizeof(otterop_lang_OOPIterator_t));
    this->implementation = implementation;
    this->has_next = has_next;
    this->next = next;
    return this;
}


int otterop_lang_OOPIterator_has_next(otterop_lang_OOPIterator_t *this);


void *otterop_lang_OOPIterator_next(otterop_lang_OOPIterator_t *this);


int otterop_lang_OOPIterator_has_next(otterop_lang_OOPIterator_t *this) {
    return this->has_next(this->implementation);
}

void *otterop_lang_OOPIterator_next(otterop_lang_OOPIterator_t *this) {
    return this->next(this->implementation);
}