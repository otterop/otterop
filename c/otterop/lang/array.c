#include "array.h"

typedef struct otterop_lang_Array_s {
    void **wrapped;
    int start;
    int end;
} otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_new(void **wrapped, int start, int end) {
    otterop_lang_Array_t *ret = GC_malloc(sizeof(otterop_lang_Array_t));
    ret->wrapped = wrapped;
    ret->start = start;
    ret->end = end;
    return ret;
}

otterop_lang_Array_t *otterop_lang_Array_wrap(int wrapped_count, void **wrapped) {
    return otterop_lang_Array_new(wrapped, 0, wrapped_count);
}

otterop_lang_Array_t *otterop_lang_Array_wrap_string(int wrapped_count, char **wrapped) {
    int i;
    void **ret = GC_malloc(wrapped_count * sizeof(void *));
    for (i = 0; i < wrapped_count; i++) {
        ret[i] = otterop_lang_String_wrap(wrapped[i]);
    }
    return otterop_lang_Array_new(ret, 0, wrapped_count);
}

void *otterop_lang_Array_get(otterop_lang_Array_t *this, int i) {
    return this->wrapped[this->start + i];
}

void otterop_lang_Array_set(otterop_lang_Array_t *this, int i, void* value) {
    this->wrapped[this->start + i] = value;
}

otterop_lang_Array_t *otterop_lang_Array_slice(otterop_lang_Array_t *this, int start, int end) {
    int newStart = this->start + start;
    int newEnd = this->start + end;
    if (newStart < this->start || newStart > this->end || newEnd < newStart ||
            newEnd > this->end) {
        *(int*)0 = 0; // SEGFAULT
    }
    return otterop_lang_Array_new(this->wrapped, newStart, newEnd);
}

int otterop_lang_Array_size(otterop_lang_Array_t *this) {
    return this->end - this->start;
}
