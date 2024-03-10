#include <otterop/datastructure/list.h>
#include <gc.h>

typedef struct otterop_datastructure_List_s otterop_datastructure_List_t;

typedef struct otterop_datastructure_List_s {
    otterop_lang_Array_t *array;
    int capacity;
    int size;
    void *t_zero;
} otterop_datastructure_List_t;




void otterop_datastructure_List_check_index_out_of_bounds(otterop_datastructure_List_t *this, int index);


otterop_datastructure_List_t *otterop_datastructure_List_new() {
    otterop_datastructure_List_t *this = GC_malloc(sizeof(otterop_datastructure_List_t));
    this->size = 0;
    this->capacity = 4;
    otterop_lang_Generic_t *generic_t = otterop_lang_Generic_new();
    this->t_zero = otterop_lang_Generic_zero(generic_t);
    this->array = otterop_lang_Array_new_array(this->capacity, this->t_zero);
    return this;
}

void otterop_datastructure_List_ensure_capacity(otterop_datastructure_List_t *this, int capacity) {
    
    if (this->capacity < capacity) {
        this->capacity = this->capacity * 2;
        otterop_lang_Array_t *new_array = otterop_lang_Array_new_array(this->capacity, this->t_zero);
        otterop_lang_Array_copy(this->array, 0, new_array, 0, this->size);
        this->array = new_array;
    }
}

void otterop_datastructure_List_add(otterop_datastructure_List_t *this, void *element) {
    otterop_datastructure_List_ensure_capacity(this, this->size + 1);
    otterop_lang_Array_set(this->array, this->size, element);
    this->size++;
}

void otterop_datastructure_List_add_array(otterop_datastructure_List_t *this, otterop_lang_Array_t *src) {
    otterop_datastructure_List_ensure_capacity(this, this->size + otterop_lang_Array_size(src));
    otterop_lang_Array_copy(src, 0, this->array, this->size, otterop_lang_Array_size(src));
    this->size += otterop_lang_Array_size(src);
}

void otterop_datastructure_List_add_list(otterop_datastructure_List_t *this, otterop_datastructure_List_t *src) {
    otterop_datastructure_List_add_array(this, src->array);
}

void otterop_datastructure_List_check_index_out_of_bounds(otterop_datastructure_List_t *this, int index) {
    
    if (index < 0 || index > this->size) {
        otterop_lang_Panic_index_out_of_bounds(otterop_lang_String_wrap("index is outside list bounds"));
    }
}

void otterop_datastructure_List_insert(otterop_datastructure_List_t *this, int index, void *element) {
    otterop_datastructure_List_check_index_out_of_bounds(this, index);
    otterop_datastructure_List_ensure_capacity(this, this->size + 1);
    
    if (index < this->size) {
        otterop_lang_Array_copy(this->array, index, this->array, index + 1, this->size - index);
    }
    otterop_lang_Array_set(this->array, index, element);
    this->size++;
}

void otterop_datastructure_List_insert_array(otterop_datastructure_List_t *this, int index, otterop_lang_Array_t *src) {
    otterop_datastructure_List_check_index_out_of_bounds(this, index);
    otterop_datastructure_List_ensure_capacity(this, this->size + otterop_lang_Array_size(src));
    
    if (index < this->size) {
        otterop_lang_Array_copy(this->array, index, this->array, index + otterop_lang_Array_size(src), this->size - index);
    }
    otterop_lang_Array_copy(src, 0, this->array, index, otterop_lang_Array_size(src));
    this->size += otterop_lang_Array_size(src);
}

void otterop_datastructure_List_insert_list(otterop_datastructure_List_t *this, int index, otterop_datastructure_List_t *src) {
    otterop_datastructure_List_insert_array(this, index, src->array);
}

void *otterop_datastructure_List_get(otterop_datastructure_List_t *this, int index) {
    otterop_datastructure_List_check_index_out_of_bounds(this, index);
    return otterop_lang_Array_get(this->array, index);
}

void *otterop_datastructure_List_remove_index(otterop_datastructure_List_t *this, int index) {
    otterop_datastructure_List_check_index_out_of_bounds(this, index);
    void *ret = otterop_lang_Array_get(this->array, index);
    
    if (index + 1 < this->size) {
        otterop_lang_Array_copy(this->array, index + 1, this->array, index, this->size - index - 1);
    }
    this->size--;
    return ret;
}

otterop_datastructure_List_t *otterop_datastructure_List_remove_range(otterop_datastructure_List_t *this, int index, int count) {
    otterop_datastructure_List_check_index_out_of_bounds(this, index);
    
    if (index + count > this->size) {
        count = this->size - index;
    }
    otterop_datastructure_List_t *ret = otterop_datastructure_List_new();
    otterop_lang_Array_t *removed = otterop_lang_Array_new_array(count, this->t_zero);
    otterop_lang_Array_copy(this->array, index, removed, 0, count);
    otterop_datastructure_List_add_array(ret, removed);
    
    if (index + count < this->size) {
        otterop_lang_Array_copy(this->array, index + count, this->array, index, this->size - index - count);
    }
    this->size = this->size - count;
    return ret;
}

int otterop_datastructure_List_size(otterop_datastructure_List_t *this) {
    return this->size;
}
