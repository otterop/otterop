#include <otterop/lang/int/array_iterator.h>
#include <gc.h>

typedef struct otterop_lang_ArrayIterator_s {
    otterop_lang_Array_t *array;
    int i;
} otterop_lang_ArrayIterator_t;




otterop_lang_ArrayIterator_t *otterop_lang_ArrayIterator_new(otterop_lang_Array_t *array) {
    otterop_lang_ArrayIterator_t *self = GC_malloc(sizeof(otterop_lang_ArrayIterator_t));
    self->array = array;
    self->i = 0;
    return self;
}

int otterop_lang_ArrayIterator_has_next(otterop_lang_ArrayIterator_t *self) {
    return self->i < otterop_lang_Array_size(self->array);
}

void *otterop_lang_ArrayIterator_next(otterop_lang_ArrayIterator_t *self) {
    void *ret = otterop_lang_Array_get(self->array, self->i);
    self->i++;
    return ret;
}

otterop_lang_OOPIterator_t
*otterop_lang_ArrayIterator__to_otterop_lang_OOPIterator(otterop_lang_ArrayIterator_t *self) {
    return otterop_lang_OOPIterator_new(self,
        (int (*)(void *)) otterop_lang_ArrayIterator_has_next,
        (void * (*)(void *)) otterop_lang_ArrayIterator_next);
}


