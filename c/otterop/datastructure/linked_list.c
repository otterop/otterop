#include <otterop/datastructure/int/linked_list.h>
#include <gc.h>
#include <stdlib.h>
#include <otterop/datastructure/int/linked_list_iterator.h>

typedef struct otterop_datastructure_LinkedList_s otterop_datastructure_LinkedList_t;
typedef struct otterop_datastructure_LinkedListIterator_s otterop_datastructure_LinkedListIterator_t;

typedef struct otterop_datastructure_LinkedList_s {
    otterop_datastructure_LinkedListNode_t *head;
    otterop_datastructure_LinkedListNode_t *tail;
    int size;
} otterop_datastructure_LinkedList_t;




void otterop_datastructure_LinkedList_node_of_different_list(otterop_datastructure_LinkedList_t *this);


void otterop_datastructure_LinkedList_remove_on_empty_list(otterop_datastructure_LinkedList_t *this);


otterop_datastructure_LinkedList_t *otterop_datastructure_LinkedList_new() {
    otterop_datastructure_LinkedList_t *this = GC_malloc(sizeof(otterop_datastructure_LinkedList_t));
    this->head = NULL;
    this->tail = NULL;
    this->size = 0;
    return this;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_before(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *node, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, this);
    otterop_datastructure_LinkedList_add_node_before(this, node, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_node_of_different_list(otterop_datastructure_LinkedList_t *this) {
    otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("node of different list"));
}

void otterop_datastructure_LinkedList_remove_on_empty_list(otterop_datastructure_LinkedList_t *this) {
    otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("remove called on empty list"));
}

void otterop_datastructure_LinkedList_add_node_before(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *node, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != otterop_datastructure_LinkedListNode_list(new_node) || otterop_datastructure_LinkedListNode_list(node) != this)
        otterop_datastructure_LinkedList_node_of_different_list(this);
    
    otterop_datastructure_LinkedListNode_t *prev_node = otterop_datastructure_LinkedListNode_prev(node);
    
    if (prev_node == NULL)
        otterop_datastructure_LinkedListNode_list(new_node)->head = new_node;
     else
        otterop_datastructure_LinkedListNode_set_next(prev_node, new_node);
    
    otterop_datastructure_LinkedListNode_set_prev(new_node, prev_node);
    otterop_datastructure_LinkedListNode_set_next(new_node, node);
    otterop_datastructure_LinkedListNode_set_prev(node, new_node);
    this->size++;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_after(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *node, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, this);
    otterop_datastructure_LinkedList_add_node_after(this, node, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_after(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *node, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != otterop_datastructure_LinkedListNode_list(new_node) || otterop_datastructure_LinkedListNode_list(node) != this)
        otterop_datastructure_LinkedList_node_of_different_list(this);
    
    otterop_datastructure_LinkedListNode_t *next_node = otterop_datastructure_LinkedListNode_next(node);
    
    if (next_node == NULL)
        otterop_datastructure_LinkedListNode_list(new_node)->tail = new_node;
     else
        otterop_datastructure_LinkedListNode_set_prev(next_node, new_node);
    
    otterop_datastructure_LinkedListNode_set_next(new_node, next_node);
    otterop_datastructure_LinkedListNode_set_prev(new_node, node);
    otterop_datastructure_LinkedListNode_set_next(node, new_node);
    this->size++;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_first(otterop_datastructure_LinkedList_t *this, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, this);
    otterop_datastructure_LinkedList_add_node_first(this, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_first(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (this->head == NULL) {
        
        if (otterop_datastructure_LinkedListNode_list(new_node) != this)
            otterop_datastructure_LinkedList_node_of_different_list(this);
        
        this->head = new_node;
        this->tail = new_node;
        this->size++;
    } else {
        otterop_datastructure_LinkedList_add_node_before(this, this->head, new_node);
    }
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_add_last(otterop_datastructure_LinkedList_t *this, void *value) {
    otterop_datastructure_LinkedListNode_t *new_node = otterop_datastructure_LinkedListNode_new(value);
    otterop_datastructure_LinkedListNode_set_list(new_node, this);
    otterop_datastructure_LinkedList_add_node_last(this, new_node);
    return new_node;
}

void otterop_datastructure_LinkedList_add_node_last(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *new_node) {
    
    if (this->tail == NULL) {
        
        if (otterop_datastructure_LinkedListNode_list(new_node) != this)
            otterop_datastructure_LinkedList_node_of_different_list(this);
        
        this->head = new_node;
        this->tail = new_node;
        this->size++;
    } else {
        otterop_datastructure_LinkedList_add_node_after(this, this->tail, new_node);
    }
}

void otterop_datastructure_LinkedList_clear(otterop_datastructure_LinkedList_t *this) {
    this->head = NULL;
    this->tail = NULL;
    this->size = 0;
}

void otterop_datastructure_LinkedList_remove_first(otterop_datastructure_LinkedList_t *this) {
    
    if (this->head != NULL) {
        otterop_datastructure_LinkedList_remove_node(this, this->head);
    } else {
        otterop_datastructure_LinkedList_remove_on_empty_list(this);
    }
}

void otterop_datastructure_LinkedList_remove_last(otterop_datastructure_LinkedList_t *this) {
    
    if (this->tail != NULL) {
        otterop_datastructure_LinkedList_remove_node(this, this->tail);
    } else {
        otterop_datastructure_LinkedList_remove_on_empty_list(this);
    }
}

int otterop_datastructure_LinkedList_remove(otterop_datastructure_LinkedList_t *this, void *value) {
    otterop_datastructure_LinkedListNode_t *curr = this->head;
    
    while (curr != NULL) {
        
        if (otterop_lang_OOPObject_is(otterop_datastructure_LinkedListNode_value(curr), value)) {
            otterop_datastructure_LinkedList_remove_node(this, curr);
            return 1;
        }
        curr = otterop_datastructure_LinkedListNode_next(curr);
    }
    return 0;
}

void otterop_datastructure_LinkedList_remove_node(otterop_datastructure_LinkedList_t *this, otterop_datastructure_LinkedListNode_t *node) {
    
    if (otterop_datastructure_LinkedListNode_list(node) != this)
        otterop_lang_Panic_invalid_operation(otterop_lang_String_wrap("node of different list"));
    
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
    this->size--;
}

int otterop_datastructure_LinkedList_size(otterop_datastructure_LinkedList_t *this) {
    return this->size;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_first(otterop_datastructure_LinkedList_t *this) {
    return this->head;
}

otterop_datastructure_LinkedListNode_t *otterop_datastructure_LinkedList_last(otterop_datastructure_LinkedList_t *this) {
    return this->tail;
}

otterop_lang_OOPIterator_t *otterop_datastructure_LinkedList_oop_iterator(otterop_datastructure_LinkedList_t *this) {
    return otterop_datastructure_LinkedListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_LinkedListIterator_new(this));
}

otterop_lang_OOPIterable_t
*otterop_datastructure_LinkedList__to_otterop_lang_OOPIterable(otterop_datastructure_LinkedList_t *this) {
    return otterop_lang_OOPIterable_new(this,
        (otterop_lang_OOPIterator_t * (*)(void *)) otterop_datastructure_LinkedList_oop_iterator);
}


