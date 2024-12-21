#include <otterop/lang/int/result.h>
#include <gc.h>

typedef struct otterop_lang_Result_s {
    void *res;
    void *err;
} otterop_lang_Result_t;




otterop_lang_Result_t *otterop_lang_Result_new(void *res, void *err) {
    otterop_lang_Result_t *self = GC_malloc(sizeof(otterop_lang_Result_t));
    self->res = res;
    self->err = err;
    return self;
}

void *otterop_lang_Result_err(otterop_lang_Result_t *self) {
    return self->err;
}

void *otterop_lang_Result_unwrap(otterop_lang_Result_t *self) {
    return self->res;
}

otterop_lang_Result_t *otterop_lang_Result_of(void *res, void *err) {
    return otterop_lang_Result_new(res, err);
}
