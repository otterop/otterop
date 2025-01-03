#ifndef __otterop_datastructure_ListIterator_int
#define __otterop_datastructure_ListIterator_int
#include <otterop/lang/oop_iterator.h>
#include <otterop/datastructure/int/list.h>

typedef struct otterop_lang_OOPIterator_s otterop_lang_OOPIterator_t;
typedef struct otterop_datastructure_List_s otterop_datastructure_List_t;

typedef struct otterop_datastructure_ListIterator_s otterop_datastructure_ListIterator_t;




otterop_datastructure_ListIterator_t *otterop_datastructure_ListIterator_new(otterop_datastructure_List_t *list);

unsigned char otterop_datastructure_ListIterator_has_next(otterop_datastructure_ListIterator_t *self);


void *otterop_datastructure_ListIterator_next(otterop_datastructure_ListIterator_t *self);


otterop_lang_OOPIterator_t
*otterop_datastructure_ListIterator__to_otterop_lang_OOPIterator(otterop_datastructure_ListIterator_t *self);


#endif
