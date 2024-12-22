#include <stdint.h>
#include <otterop/datastructure/int/linked_list.h>
#include <gc.h>
#include <stdlib.h>
#include <otterop/datastructure/int/linked_list_iterator.h>

typedef struct otterop_datastructure_LinkedList_s {
    otterop_datastructure_LinkedListNode_t *head;
    otterop_datastructure_LinkedListNode_t *tail;
    int32_t size;
} otterop_datastructure_LinkedList_t;




void otterop_datastructure_LinkedList_node_of_different_list(otterop_datastructure_LinkedList_t *self);


void otterop_datastructure_LinkedList_remove_on_empty_list(otterop_datastructure_LinkedList_t *self);


otterop_datastructure_LinkedList_t *otterop_datastructure_LinkedList_new() {
    otterop_datastructure_LinkedList_t *self = GC_malloc(sizeof(otterop_datastructure_LinkedList_t));
    self->head = NULL;
    self->tail = NULL;
    self->size = 0;
    return self;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_before(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *node, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, self);
    otterop_datastructure_LinkedList_add_node_before(self, node, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_node_of_different_list(otterop_datastructure_LinkedList_t *self) {
    otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("node of different list"));
}

void otterop_datastructure_LinkedList_remove_on_empty_list(otterop_datastructure_LinkedList_t *self) {
    otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("remove called on empty list"));
}

void otterop_datastructure_LinkedList_add_node_before(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *node, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != otterop_datastructure_LinkedListNode_list(new_node) || otterop_datastructure_LinkedListNode_list(node) != self) {
        otterop_datastructure_LinkedList_node_of_different_list(self);
    }
    
    otterop_datastructure_LinkedListNode_t *prev_node = otterop_datastructure_LinkedListNode_prev(node);
    
    if (prev_node == NULL) {
        otterop_datastructure_LinkedListNode_list(new_node)->head = new_node;
    }
     else {
        otterop_datastructure_LinkedListNode_set_next(prev_node, new_node);
    }
    
    otterop_datastructure_LinkedListNode_set_prev(new_node, prev_node);
    otterop_datastructure_LinkedListNode_set_next(new_node, node);
    otterop_datastructure_LinkedListNode_set_prev(node, new_node);
    self->size++;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_after(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *node, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, self);
    otterop_datastructure_LinkedList_add_node_after(self, node, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_after(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *node, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != otterop_datastructure_LinkedListNode_list(new_node) || otterop_datastructure_LinkedListNode_list(node) != self) {
        otterop_datastructure_LinkedList_node_of_different_list(self);
    }
    
    otterop_datastructure_LinkedListNode_t *next_node = otterop_datastructure_LinkedListNode_next(node);
    
    if (next_node == NULL) {
        otterop_datastructure_LinkedListNode_list(new_node)->tail = new_node;
    }
     else {
        otterop_datastructure_LinkedListNode_set_prev(next_node, new_node);
    }
    
    otterop_datastructure_LinkedListNode_set_next(new_node, next_node);
    otterop_datastructure_LinkedListNode_set_prev(new_node, node);
    otterop_datastructure_LinkedListNode_set_next(node, new_node);
    self->size++;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_first(otterop_datastructure_LinkedList_t *self, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, self);
    otterop_datastructure_LinkedList_add_node_first(self, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_first(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (self->head == NULL) {
        
        if (otterop_datastructure_LinkedListNode_list(new_node) != self) {
            otterop_datastructure_LinkedList_node_of_different_list(self);
        }
        
        self->head = new_node;
        self->tail = new_node;
        self->size++;
    } else {
        otterop_datastructure_LinkedList_add_node_before(self, self->head, new_node);
    }
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_last(otterop_datastructure_LinkedList_t *self, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, self);
    otterop_datastructure_LinkedList_add_node_last(self, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_last(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (self->tail == NULL) {
        
        if (otterop_datastructure_LinkedListNode_list(new_node) != self) {
            otterop_datastructure_LinkedList_node_of_different_list(self);
        }
        
        self->head = new_node;
        self->tail = new_node;
        self->size++;
    } else {
        otterop_datastructure_LinkedList_add_node_after(self, self->tail, new_node);
    }
}

void otterop_datastructure_LinkedList_clear(otterop_datastructure_LinkedList_t *self) {
    self->head = NULL;
    self->tail = NULL;
    self->size = 0;
}

void otterop_datastructure_LinkedList_remove_first(otterop_datastructure_LinkedList_t *self) {
    
    if (self->head != NULL) {
        otterop_datastructure_LinkedList_remove_node(self, self->head);
    } else {
        otterop_datastructure_LinkedList_remove_on_empty_list(self);
    }
}

void otterop_datastructure_LinkedList_remove_last(otterop_datastructure_LinkedList_t *self) {
    
    if (self->tail != NULL) {
        otterop_datastructure_LinkedList_remove_node(self, self->tail);
    } else {
        otterop_datastructure_LinkedList_remove_on_empty_list(self);
    }
}

unsigned char otterop_datastructure_LinkedList_remove(otterop_datastructure_LinkedList_t *self, void *value) {
    otterop_datastructure_LinkedListNode_t *curr = self->head;
    
    while (curr != NULL) {
        
        if (otterop_lang_OOPObject_is(otterop_datastructure_LinkedListNode_value(curr), value)) {
            otterop_datastructure_LinkedList_remove_node(self, curr);
            return 1;
        }
        curr = otterop_datastructure_LinkedListNode_next(curr);
    }
    return 0;
}

void otterop_datastructure_LinkedList_remove_node(otterop_datastructure_LinkedList_t *self, otterop_datastructure_LinkedListNode_t *node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != self) {
        otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("node of different list"));
    }
    
    otterop_datastructure_LinkedListNode_t *prev = otterop_datastructure_LinkedListNode_prev(node);
    otterop_datastructure_LinkedListNode_t *next = otterop_datastructure_LinkedListNode_next(node);
    
    if (prev != NULL) {
        otterop_datastructure_LinkedListNode_set_next(prev, next);
    } else {
        otterop_datastructure_LinkedListNode_list(node)->head = next;
    }
    
    if (next != NULL) {
        otterop_datastructure_LinkedListNode_set_prev(next, prev);
    } else {
        otterop_datastructure_LinkedListNode_list(node)->tail = prev;
    }
    otterop_datastructure_LinkedListNode_set_prev(node, NULL);
    otterop_datastructure_LinkedListNode_set_next(node, NULL);
    self->size--;
}

int32_t otterop_datastructure_LinkedList_size(otterop_datastructure_LinkedList_t *self) {
    return self->size;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_first(otterop_datastructure_LinkedList_t *self) {
    return self->head;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_last(otterop_datastructure_LinkedList_t *self) {
    return self->tail;
}

otterop_lang_OOPIterator_t *otterop_datastructure_LinkedList_oop_iterator(otterop_datastructure_LinkedList_t *self) {
    return otterop_datastructure_LinkedListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_LinkedListIterator_new(self));
}

otterop_lang_OOPIterable_t
*otterop_datastructure_LinkedList__to_otterop_lang_OOPIterable(otterop_datastructure_LinkedList_t *self) {
    return otterop_lang_OOPIterable_new(self,
        (otterop_lang_OOPIterator_t * (*)(void *)) otterop_datastructure_LinkedList_oop_iterator);
}


