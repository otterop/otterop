#include <otterop/datastructure/int/linked_list_node.h>
#include <gc.h>
#include <stdlib.h>

typedef struct otterop_datastructure_LinkedListNode_s otterop_datastructure_LinkedListNode_t;

typedef struct otterop_datastructure_LinkedListNode_s {
    otterop_datastructure_LinkedList_t *list;
    otterop_datastructure_LinkedListNode_t *prev;
    otterop_datastructure_LinkedListNode_t *next;
    void *value;
} otterop_datastructure_LinkedListNode_t;




otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_new(void *value) {
    otterop_datastructure_LinkedListNode_t *this = GC_malloc(sizeof(otterop_datastructure_LinkedListNode_t));
    this->prev = NULL;
    this->next = NULL;
    this->value = value;
    return this;
}

otterop_datastructure_LinkedList_t *otterop_datastructure_LinkedListNode_list(otterop_datastructure_LinkedListNode_t *this) {
    return this->list;
}

void otterop_datastructure_LinkedListNode_set_list(otterop_datastructure_LinkedListNode_t *this, otterop_datastructure_LinkedList_t *list) {
    this->list = list;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_prev(otterop_datastructure_LinkedListNode_t *this) {
    return this->prev;
}

void otterop_datastructure_LinkedListNode_set_prev(otterop_datastructure_LinkedListNode_t *this, otterop_datastructure_LinkedListNode_t *node) {
    this->prev = node;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_next(otterop_datastructure_LinkedListNode_t *this) {
    return this->next;
}

void otterop_datastructure_LinkedListNode_set_next(otterop_datastructure_LinkedListNode_t *this, otterop_datastructure_LinkedListNode_t *node) {
    this->next = node;
}

void *otterop_datastructure_LinkedListNode_value(otterop_datastructure_LinkedListNode_t *this) {
    return this->value;
}
