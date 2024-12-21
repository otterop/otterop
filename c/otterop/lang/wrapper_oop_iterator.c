#include <otterop/lang/int/wrapper_oop_iterator.h>
#include <gc.h>
#include <otterop/lang/array.h>

typedef struct otterop_lang_WrapperOOPIteratorArray_s {
    int i;
    void **array;
    int array_cnt;
    void *(*wrap)(void *);
} otterop_lang_WrapperOOPIteratorArray_t;

typedef struct otterop_lang_WrapperOOPIterator_s {
    void *it_self;
    int (*it)(void *self, void **next);
    void *next;
    int has_next;
    int next_loaded;
    void *(*wrap)(void *);
} otterop_lang_WrapperOOPIterator_t;

typedef struct otterop_lang_UnwrappedOOPIterator_s {
    otterop_lang_OOPIterator_t *it;
    void *(*unwrap)(void *);
} otterop_lang_UnwrappedOOPIterator_t;

static int otterop_lang_WrapperOOPIteratorArray_has_next(void *self_void) {
    otterop_lang_WrapperOOPIteratorArray_t *self = self_void;
    return self->i < self->array_cnt;
}

static void *otterop_lang_WrapperOOPIteratorArray_next(void *self_void) {
    otterop_lang_WrapperOOPIteratorArray_t *self = self_void;
    if (self->i >= self->array_cnt)
        return NULL;
    if (self->wrap)
        return self->wrap(self->array[self->i++]);
    return self->array[self->i++];
}

static int otterop_lang_WrapperOOPIterator_has_next(void *self_void) {
    otterop_lang_WrapperOOPIterator_t *self = self_void;
    if (self->next_loaded)
        return self->has_next;

    self->has_next = self->it(self->it_self, &self->next);
    if (self->next && self->wrap)
        self->next = self->wrap(self->next);
    self->next_loaded = 1;
    return self->has_next;
}

static void *otterop_lang_WrapperOOPIterator_next(void *self_void) {
    otterop_lang_WrapperOOPIterator_t *self = self_void;
    if (!self->next_loaded && !otterop_lang_WrapperOOPIterator_has_next(self_void))
        return self->next;

    self->next_loaded = 0;
    return self->next;
}

static int otterop_lang_UnwrappedOOPIterator_next(void *it_self, void **next) {
    otterop_lang_UnwrappedOOPIterator_t *unwrapped = it_self;
    int ret = otterop_lang_OOPIterator_has_next(unwrapped->it);
    if (!ret)
        *next = NULL;
    else {
        *next = otterop_lang_OOPIterator_next(unwrapped->it);
        if (unwrapped->unwrap)
            *next = unwrapped->unwrap(*next);
    }
    return ret;
}

otterop_lang_OOPIterator_t*
otterop_lang_WrapperOOPIterator_wrap_array(void **array, int array_cnt, void *(*wrap)(void *)) {
    otterop_lang_WrapperOOPIteratorArray_t *self = GC_malloc(sizeof(otterop_lang_WrapperOOPIteratorArray_t));
    self->array = array;
    self->array_cnt = array_cnt;
    self->wrap = wrap;
    return otterop_lang_OOPIterator_new(self,
        otterop_lang_WrapperOOPIteratorArray_has_next,
        otterop_lang_WrapperOOPIteratorArray_next);
}

void **
otterop_lang_WrapperOOPIterator_unwrap_array(otterop_lang_OOPIterator_t *it, void *(*unwrap)(void *), int *array_cnt) {
    int capacity = 1, i = 0;
    if (!array_cnt)
        return NULL;

    void **ret = GC_malloc(sizeof(*ret) * capacity);
    while (otterop_lang_OOPIterator_has_next(it)) {
        if (i == capacity) {
            capacity = capacity << 1;
            ret = GC_realloc(ret, sizeof(*ret) * capacity);
        }
        void *next = otterop_lang_OOPIterator_next(it);
        if (unwrap) {
            next = unwrap(next);
        }
        ret[i++] = next;
    }
    *array_cnt = i;
    return ret;
}

otterop_lang_OOPIterator_t*
otterop_lang_WrapperOOPIterator_wrap(void *it_self, int (*it)(void *self, void **next), void *(*wrap)(void *)) {
    otterop_lang_WrapperOOPIterator_t *self = GC_malloc(sizeof(otterop_lang_WrapperOOPIterator_t));
    self->it_self = it_self;
    self->it = it;
    self->wrap = wrap;
    return otterop_lang_OOPIterator_new(self,
        otterop_lang_WrapperOOPIterator_has_next,
        otterop_lang_WrapperOOPIterator_next);
}

void *otterop_lang_WrapperOOPIterator_unwrap(otterop_lang_OOPIterator_t *self, void *(*unwrap)(void *), int (**_ret_it)(void *self, void **next)) {
    if (!_ret_it)
        return NULL;
    otterop_lang_UnwrappedOOPIterator_t *unwrapped = GC_malloc(sizeof(otterop_lang_UnwrappedOOPIterator_t));
    unwrapped->it = self;
    unwrapped->unwrap = unwrap;
    *_ret_it = otterop_lang_UnwrappedOOPIterator_next;
    return unwrapped;
}
