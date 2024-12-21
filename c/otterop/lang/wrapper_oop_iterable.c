#include <otterop/lang/int/wrapper_oop_iterable.h>
#include <otterop/lang/int/wrapper_oop_iterator.h>
#include <gc.h>

typedef struct otterop_lang_WrapperOOPIterable_s {
    void **it_self;
    int (*it)(void *self, void **next);
    void *(*wrap)(void *);
} otterop_lang_WrapperOOPIterable_t;

typedef struct otterop_lang_WrapperOOPIterableArray_s {
    void **array;
    int array_cnt;
    void *(*wrap)(void *);
} otterop_lang_WrapperOOPIterableArray_t;

static otterop_lang_WrapperOOPIterableArray_t*
otterop_lang_WrapperOOPIterable_new_array(void **array, int array_cnt, void *(*wrap)(void *)) {
    otterop_lang_WrapperOOPIterableArray_t *self = GC_malloc(sizeof(otterop_lang_WrapperOOPIterableArray_t));
    self->array = array;
    self->array_cnt = array_cnt;
    self->wrap = wrap;
    return self;
}

static otterop_lang_WrapperOOPIterable_t*
otterop_lang_WrapperOOPIterable_new(void *it_self, int (*it)(void *self, void **next), void *(*wrap)(void *)) {
    otterop_lang_WrapperOOPIterable_t *self = GC_malloc(sizeof(otterop_lang_WrapperOOPIterable_t));
    self->it_self = it_self;
    self->it = it;
    self->wrap = wrap;
    return self;
}

static otterop_lang_OOPIterator_t *
otterop_lang_WrapperOOPIterable_oop_iterator_array(void *self) {
    otterop_lang_WrapperOOPIterableArray_t *iterable = self;
    return otterop_lang_WrapperOOPIterator_wrap_array(
        iterable->array,
        iterable->array_cnt,
        iterable->wrap
    );
}

static otterop_lang_OOPIterator_t *
otterop_lang_WrapperOOPIterable_oop_iterator(void *self) {
    otterop_lang_WrapperOOPIterable_t *iterable = self;
    return otterop_lang_WrapperOOPIterator_wrap(
        iterable->it_self,
        iterable->it,
        iterable->wrap
    );
}

static otterop_lang_OOPIterable_t
*otterop_lang_WrapperOOPIterable__to_otterop_lang_OOPIterable(otterop_lang_WrapperOOPIterable_t *self) {
    return otterop_lang_OOPIterable_new(self, otterop_lang_WrapperOOPIterable_oop_iterator);
}

otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap_array(void **array, int array_cnt,  void *(*wrap)(void *)) {
    return otterop_lang_OOPIterable_new(
        otterop_lang_WrapperOOPIterable_new_array(array, array_cnt, wrap),
        otterop_lang_WrapperOOPIterable_oop_iterator_array);
}

void **
otterop_lang_WrapperOOPIterable_unwrap_array(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), int *array_cnt) {
    return otterop_lang_WrapperOOPIterator_unwrap_array(
        otterop_lang_OOPIterable_oop_iterator(iterable),
        unwrap, array_cnt);
}

otterop_lang_OOPIterable_t*
otterop_lang_WrapperOOPIterable_wrap(void *it_self, int (*it)(void *self, void **next),  void *(*wrap)(void *)) {
    return otterop_lang_OOPIterable_new(
        otterop_lang_WrapperOOPIterable_new(it_self, it, wrap),
        otterop_lang_WrapperOOPIterable_oop_iterator);
}

void *otterop_lang_WrapperOOPIterable_unwrap(otterop_lang_OOPIterable_t *iterable, void *(*unwrap)(void *), int (**_ret_it)(void *self, void **next)) {
    return otterop_lang_WrapperOOPIterator_unwrap(
        otterop_lang_OOPIterable_oop_iterator(iterable),
        unwrap, _ret_it);
}
