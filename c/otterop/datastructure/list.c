#include <stdint.h>
#include <otterop/datastructure/int/list.h>
#include <gc.h>
#include <otterop/datastructure/int/list_iterator.h>

typedef struct otterop_datastructure_List_s {
    otterop_lang_Array_t *array;
    int32_t capacity;
    int32_t size;
    void *t_zero;
} otterop_datastructure_List_t;




void otterop_datastructure_List_check_index_out_of_bounds(otterop_datastructure_List_t *self, int32_t index);


otterop_datastructure_List_t *otterop_datastructure_List_new() {
    otterop_datastructure_List_t *self = GC_malloc(sizeof(otterop_datastructure_List_t));
    self->size = 0;
    self->capacity = 4;
    otterop_lang_Generic_t *generic_t = otterop_lang_Generic_new();
    self->t_zero = otterop_lang_Generic_zero(generic_t);
    self->array = otterop_lang_Array_new_array(self->capacity, self->t_zero);
    return self;
}

void otterop_datastructure_List_ensure_capacity(otterop_datastructure_List_t *self, int32_t capacity) {
    
    if (self->capacity < capacity) {
        self->capacity = self->capacity * 2;
        otterop_lang_Array_t *new_array = otterop_lang_Array_new_array(self->capacity, self->t_zero);
        otterop_lang_Array_copy(self->array, 0, new_array, 0, self->size);
        self->array = new_array;
    }
}

void otterop_datastructure_List_add(otterop_datastructure_List_t *self, void *element) {
    otterop_datastructure_List_ensure_capacity(self, self->size + 1);
    otterop_lang_Array_set(self->array, self->size, element);
    self->size++;
}

void otterop_datastructure_List_add_array(otterop_datastructure_List_t *self, otterop_lang_Array_t *src) {
    otterop_datastructure_List_ensure_capacity(self, self->size + otterop_lang_Array_size(src));
    otterop_lang_Array_copy(src, 0, self->array, self->size, otterop_lang_Array_size(src));
    self->size += otterop_lang_Array_size(src);
}

void otterop_datastructure_List_add_list(otterop_datastructure_List_t *self, otterop_datastructure_List_t *src) {
    otterop_datastructure_List_add_array(self, src->array);
}

void otterop_datastructure_List_check_index_out_of_bounds(otterop_datastructure_List_t *self, int32_t index) {
    
    if (index < 0 || index > self->size) {
        otterop_lang_Panic_index_out_of_bounds(otterop_lang_String_wrap("index is outside list bounds"));
    }
}

void otterop_datastructure_List_insert(otterop_datastructure_List_t *self, int32_t index, void *element) {
    otterop_datastructure_List_check_index_out_of_bounds(self, index);
    otterop_datastructure_List_ensure_capacity(self, self->size + 1);
    
    if (index < self->size) {
        otterop_lang_Array_copy(self->array, index, self->array, index + 1, self->size - index);
    }
    otterop_lang_Array_set(self->array, index, element);
    self->size++;
}

void otterop_datastructure_List_insert_array(otterop_datastructure_List_t *self, int32_t index, otterop_lang_Array_t *src) {
    otterop_datastructure_List_check_index_out_of_bounds(self, index);
    otterop_datastructure_List_ensure_capacity(self, self->size + otterop_lang_Array_size(src));
    
    if (index < self->size) {
        otterop_lang_Array_copy(self->array, index, self->array, index + otterop_lang_Array_size(src), self->size - index);
    }
    otterop_lang_Array_copy(src, 0, self->array, index, otterop_lang_Array_size(src));
    self->size += otterop_lang_Array_size(src);
}

void otterop_datastructure_List_insert_list(otterop_datastructure_List_t *self, int32_t index, otterop_datastructure_List_t *src) {
    otterop_datastructure_List_insert_array(self, index, src->array);
}

void *otterop_datastructure_List_get(otterop_datastructure_List_t *self, int32_t index) {
    otterop_datastructure_List_check_index_out_of_bounds(self, index);
    return otterop_lang_Array_get(self->array, index);
}

void *otterop_datastructure_List_remove_index(otterop_datastructure_List_t *self, int32_t index) {
    otterop_datastructure_List_check_index_out_of_bounds(self, index);
    void *ret = otterop_lang_Array_get(self->array, index);
    
    if (index + 1 < self->size) {
        otterop_lang_Array_copy(self->array, index + 1, self->array, index, self->size - index - 1);
    }
    self->size--;
    return ret;
}

otterop_datastructure_List_t *otterop_datastructure_List_remove_range(otterop_datastructure_List_t *self, int32_t index, int32_t count) {
    otterop_datastructure_List_check_index_out_of_bounds(self, index);
    
    if (index + count > self->size) {
        count = self->size - index;
    }
    otterop_datastructure_List_t *ret = otterop_datastructure_List_new();
    otterop_lang_Array_t *removed = otterop_lang_Array_new_array(count, self->t_zero);
    otterop_lang_Array_copy(self->array, index, removed, 0, count);
    otterop_datastructure_List_add_array(ret, removed);
    
    if (index + count < self->size) {
        otterop_lang_Array_copy(self->array, index + count, self->array, index, self->size - index - count);
    }
    self->size = self->size - count;
    return ret;
}

int32_t otterop_datastructure_List_size(otterop_datastructure_List_t *self) {
    return self->size;
}

otterop_lang_OOPIterator_t *otterop_datastructure_List_oop_iterator(otterop_datastructure_List_t *self) {
    return otterop_datastructure_ListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_ListIterator_new(self));
}

otterop_lang_OOPIterable_t
*otterop_datastructure_List__to_otterop_lang_OOPIterable(otterop_datastructure_List_t *self) {
    return otterop_lang_OOPIterable_new(self,
        (otterop_lang_OOPIterator_t * (*)(void *)) otterop_datastructure_List_oop_iterator);
}


