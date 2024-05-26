#include <otterop/datastructure/int/linked_list_node.h>
#include <otterop/datastructure/int/linked_list_iterator.h>
#include <gc.h>
#include <stdlib.h>

typedef struct otterop_datastructure_LinkedListNode_s otterop_datastructure_LinkedListNode_t;
typedef struct otterop_datastructure_LinkedListIterator_s otterop_datastructure_LinkedListIterator_t;

typedef struct otterop_datastructure_LinkedListIterator_s {
    otterop_datastructure_LinkedListNode_t *current;
} otterop_datastructure_LinkedListIterator_t;




otterop_datastructure_LinkedListIterator_t *otterop_datastructure_LinkedListIterator_new(otterop_datastructure_LinkedList_t *linked_list) {
    otterop_datastructure_LinkedListIterator_t *this = GC_malloc(sizeof(otterop_datastructure_LinkedListIterator_t));
    this->current = otterop_datastructure_LinkedList_first(linked_list);
    return this;
}

int otterop_datastructure_LinkedListIterator_has_next(otterop_datastructure_LinkedListIterator_t *this) {
    return this->current != NULL;
}

void *otterop_datastructure_LinkedListIterator_next(otterop_datastructure_LinkedListIterator_t *this) {
    otterop_datastructure_LinkedListNode_t *ret = this->current;
    this->current = otterop_datastructure_LinkedListNode_next(this->current);
    return otterop_datastructure_LinkedListNode_value(ret);
}

otterop_lang_OOPIterator_t
*otterop_datastructure_LinkedListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_LinkedListIterator_t *this) {
    return otterop_lang_OOPIterator_new(this,
        (int (*)(void *)) otterop_datastructure_LinkedListIterator_has_next,
        (void * (*)(void *)) otterop_datastructure_LinkedListIterator_next);
}


