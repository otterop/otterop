#include <otterop/datastructure/int/linked_list_node.h>
#include <gc.h>
#include <stdlib.h>

typedef struct otterop_datastructure_LinkedListNode_s {
    otterop_datastructure_LinkedList_t *list;
    otterop_datastructure_LinkedListNode_t *prev;
    otterop_datastructure_LinkedListNode_t *next;
    void *value;
} otterop_datastructure_LinkedListNode_t;




otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_new(void *value) {
    otterop_datastructure_LinkedListNode_t *self = GC_malloc(sizeof(otterop_datastructure_LinkedListNode_t));
    self->prev = NULL;
    self->next = NULL;
    self->value = value;
    return self;
}

otterop_datastructure_LinkedList_t *otterop_datastructure_LinkedListNode_list(otterop_datastructure_LinkedListNode_t *self) {
    return self->list;
}

void otterop_datastructure_LinkedListNode_set_list(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedList_t *list) {
    self->list = list;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_prev(otterop_datastructure_LinkedListNode_t *self) {
    return self->prev;
}

void otterop_datastructure_LinkedListNode_set_prev(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedListNode_t *node) {
    self->prev = node;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_next(otterop_datastructure_LinkedListNode_t *self) {
    return self->next;
}

void otterop_datastructure_LinkedListNode_set_next(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedListNode_t *node) {
    self->next = node;
}

void *otterop_datastructure_LinkedListNode_value(otterop_datastructure_LinkedListNode_t *self) {
    return self->value;
}
