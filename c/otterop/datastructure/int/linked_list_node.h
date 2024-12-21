#ifndef __otterop_datastructure_LinkedListNode_int
#define __otterop_datastructure_LinkedListNode_int
#include <otterop/datastructure/int/linked_list.h>

typedef struct otterop_datastructure_LinkedList_s otterop_datastructure_LinkedList_t;

typedef struct otterop_datastructure_LinkedListNode_s otterop_datastructure_LinkedListNode_t;




otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_new(void *value);

otterop_datastructure_LinkedList_t *otterop_datastructure_LinkedListNode_list(otterop_datastructure_LinkedListNode_t *self);


void otterop_datastructure_LinkedListNode_set_list(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedList_t *list);


otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_prev(otterop_datastructure_LinkedListNode_t *self);


void otterop_datastructure_LinkedListNode_set_prev(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedListNode_t *node);


otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedListNode_next(otterop_datastructure_LinkedListNode_t *self);


void otterop_datastructure_LinkedListNode_set_next(otterop_datastructure_LinkedListNode_t *self, otterop_datastructure_LinkedListNode_t *node);


void *otterop_datastructure_LinkedListNode_value(otterop_datastructure_LinkedListNode_t *self);

#endif
