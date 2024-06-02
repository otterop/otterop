#include <otterop/datastructure/int/linked_list.h>
#include <otterop/datastructure/int/string_buffer.h>
#include <gc.h>

typedef struct otterop_datastructure_LinkedList_s otterop_datastructure_LinkedList_t;
typedef struct otterop_datastructure_StringBuffer_s otterop_datastructure_StringBuffer_t;

typedef struct otterop_datastructure_StringBuffer_s {
    otterop_datastructure_LinkedList_t *strings;
} otterop_datastructure_StringBuffer_t;




otterop_datastructure_StringBuffer_t *otterop_datastructure_StringBuffer_new() {
    otterop_datastructure_StringBuffer_t *this = GC_malloc(sizeof(otterop_datastructure_StringBuffer_t));
    this->strings = otterop_datastructure_LinkedList_new();
    return this;
}

void otterop_datastructure_StringBuffer_add(otterop_datastructure_StringBuffer_t *this, otterop_lang_String_t *s) {
    otterop_datastructure_LinkedList_add_last(this->strings, s);
}

otterop_lang_String_t *otterop_datastructure_StringBuffer_oop_string(otterop_datastructure_StringBuffer_t *this) {
    otterop_lang_OOPIterable_t *strings = otterop_datastructure_LinkedList__to_otterop_lang_OOPIterable(this->strings);
    return otterop_lang_String_concat(strings);
}
