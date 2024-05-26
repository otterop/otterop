#include <otterop/datastructure/int/list_iterator.h>
#include <gc.h>

typedef struct otterop_datastructure_ListIterator_s otterop_datastructure_ListIterator_t;

typedef struct otterop_datastructure_ListIterator_s {
    otterop_datastructure_List_t *list;
    int index;
} otterop_datastructure_ListIterator_t;




otterop_datastructure_ListIterator_t *otterop_datastructure_ListIterator_new(otterop_datastructure_List_t *list) {
    otterop_datastructure_ListIterator_t *this = GC_malloc(sizeof(otterop_datastructure_ListIterator_t));
    this->list = list;
    this->index = 0;
    return this;
}

int otterop_datastructure_ListIterator_has_next(otterop_datastructure_ListIterator_t *this) {
    return this->index < otterop_datastructure_List_size(this->list);
}

void *otterop_datastructure_ListIterator_next(otterop_datastructure_ListIterator_t *this) {
    void *ret = otterop_datastructure_List_get(this->list, this->index);
    this->index++;
    return ret;
}

otterop_lang_OOPIterator_t
*otterop_datastructure_ListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_ListIterator_t *this) {
    return otterop_lang_OOPIterator_new(this,
        (int (*)(void *)) otterop_datastructure_ListIterator_has_next,
        (void * (*)(void *)) otterop_datastructure_ListIterator_next);
}


