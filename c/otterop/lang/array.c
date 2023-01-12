#include "array.h"

typedef struct otterop_lang_Array_s {
    void **wrapped;
    size_t size;
} otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_wrap(int wrapped_count, void **wrapped) {
    if (wrapped == NULL) return NULL;
    otterop_lang_Array_t *ret = GC_malloc(sizeof(otterop_lang_Array_t));
    ret->wrapped = wrapped;
    ret->size = wrapped_count;
    return ret;
}

otterop_lang_Array_t *otterop_lang_Array_wrap_string(int wrapped_count, char **wrapped) {
    int i;
    void **ret = GC_malloc(wrapped_count * sizeof(void *));
    for (i = 0; i < wrapped_count; i++) {
        ret[i] = otterop_lang_String_wrap(wrapped[i]);
    }
    return otterop_lang_Array_wrap(wrapped_count, ret);
}

void *otterop_lang_Array_get(otterop_lang_Array_t *this, int i) {
    if (this == NULL) return NULL;
    if (i >= this->size) return NULL;
    return this->wrapped[i];
}

void otterop_lang_Array_set(otterop_lang_Array_t *this, int i, void* value) {
    if (this == NULL) return;
    if (i >= this->size) return;
    this->wrapped[i] = value;
}

int otterop_lang_Array_size(otterop_lang_Array_t *this) {
    if (this == NULL) return 0;
    return this->size;
}
