#ifndef __otterop_datastructure_List
#define __otterop_datastructure_List
#include <otterop/lang/array.h>
#include <otterop/lang/generic.h>
#include <otterop/lang/panic.h>
#include <otterop/lang/string.h>

typedef struct otterop_datastructure_List_s otterop_datastructure_List_t;




otterop_datastructure_List_t *otterop_datastructure_List_new();

void otterop_datastructure_List_ensure_capacity(otterop_datastructure_List_t *this, int capacity);


void otterop_datastructure_List_add(otterop_datastructure_List_t *this, void *element);


void otterop_datastructure_List_add_array(otterop_datastructure_List_t *this, otterop_lang_Array_t *src);


void otterop_datastructure_List_add_list(otterop_datastructure_List_t *this, otterop_datastructure_List_t *src);


void otterop_datastructure_List_insert(otterop_datastructure_List_t *this, int index, void *element);


void otterop_datastructure_List_insert_array(otterop_datastructure_List_t *this, int index, otterop_lang_Array_t *src);


void otterop_datastructure_List_insert_list(otterop_datastructure_List_t *this, int index, otterop_datastructure_List_t *src);


void *otterop_datastructure_List_get(otterop_datastructure_List_t *this, int index);


void *otterop_datastructure_List_remove_index(otterop_datastructure_List_t *this, int index);


otterop_datastructure_List_t *otterop_datastructure_List_remove_range(otterop_datastructure_List_t *this, int index, int count);


int otterop_datastructure_List_size(otterop_datastructure_List_t *this);

#endif
