#include <stdint.h>
#include <otterop/datastructure/int/list_iterator.h>
#include <gc.h>

typedef struct otterop_datastructure_ListIterator_s {
    otterop_datastructure_List_t *list;
    int32_t index;
} otterop_datastructure_ListIterator_t;




otterop_datastructure_ListIterator_t *otterop_datastructure_ListIterator_new(otterop_datastructure_List_t *list) {
    otterop_datastructure_ListIterator_t *self = GC_malloc(sizeof(otterop_datastructure_ListIterator_t));
    self->list = list;
    self->index = 0;
    return self;
}

unsigned char otterop_datastructure_ListIterator_has_next(otterop_datastructure_ListIterator_t *self) {
    return self->index < otterop_datastructure_List_size(self->list);
}

void *otterop_datastructure_ListIterator_next(otterop_datastructure_ListIterator_t *self) {
    void *ret = otterop_datastructure_List_get(self->list, self->index);
    self->index++;
    return ret;
}

otterop_lang_OOPIterator_t
*otterop_datastructure_ListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_ListIterator_t *self) {
    return otterop_lang_OOPIterator_new(self,
        (unsigned char (*)(void *)) otterop_datastructure_ListIterator_has_next,
        (void * (*)(void *)) otterop_datastructure_ListIterator_next);
}


