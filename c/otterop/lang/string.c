#include <otterop/lang/string.h>

typedef struct otterop_lang_String_s {
    char *wrapped;
    size_t length;
} otterop_lang_String_t;

otterop_lang_String_t *otterop_lang_String_wrap(char *wrapped) {
    if (wrapped == NULL) return NULL;
    otterop_lang_String_t *ret = GC_malloc(sizeof(otterop_lang_String_t));
    int length = strlen(wrapped);
    char *new_str = GC_malloc(length * (sizeof(char) + 1));
    strcpy(new_str, wrapped);
    ret->wrapped = new_str;
    ret->length = length;
    return ret;
}

char *otterop_lang_String_unwrap(otterop_lang_String_t *a) {
    return a->wrapped;
}

int otterop_lang_String_compare_to(otterop_lang_String_t *a, otterop_lang_String_t *b) {
    if (a == NULL) return b == NULL ? 0 : -1;
    return strcmp(a->wrapped,b->wrapped);
}
