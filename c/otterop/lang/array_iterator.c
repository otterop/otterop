#include <otterop/lang/int/array_iterator.h>
#include <gc.h>

typedef struct otterop_lang_ArrayIterator_s otterop_lang_ArrayIterator_t;

typedef struct otterop_lang_ArrayIterator_s {
    otterop_lang_Array_t *array;
    int i;
} otterop_lang_ArrayIterator_t;




otterop_lang_ArrayIterator_t *otterop_lang_ArrayIterator_new(otterop_lang_Array_t *array) {
    otterop_lang_ArrayIterator_t *this = GC_malloc(sizeof(otterop_lang_ArrayIterator_t));
    this->array = array;
    this->i = 0;
    return this;
}

int otterop_lang_ArrayIterator_has_next(otterop_lang_ArrayIterator_t *this) {
    return this->i < otterop_lang_Array_size(this->array);
}

void *otterop_lang_ArrayIterator_next(otterop_lang_ArrayIterator_t *this) {
    void *ret = otterop_lang_Array_get(this->array, this->i);
    this->i++;
    return ret;
}

otterop_lang_OOPIterator_t
*otterop_lang_ArrayIterator__to_otterop_lang_OOPIterator(otterop_lang_ArrayIterator_t *this) {
    return otterop_lang_OOPIterator_new(this,
        (int (*)(void *)) otterop_lang_ArrayIterator_has_next,
        (void * (*)(void *)) otterop_lang_ArrayIterator_next);
}


