#include <otterop/lang/result.h>
#include <gc.h>
#include <stdlib.h>

typedef struct otterop_lang_Result_s {
    void *res;
    void *err;
} otterop_lang_Result_t;




otterop_lang_Result_t *otterop_lang_Result_new(void *res, void *err) {
    otterop_lang_Result_t *this = GC_malloc(sizeof(otterop_lang_Result_t));
    this->res = res;
    this->err = err;
    return this;
}

int otterop_lang_Result_is_ok(otterop_lang_Result_t *this) {
    return this->err != NULL;
}

void *otterop_lang_Result_err(otterop_lang_Result_t *this) {
    return this->err;
}

void *otterop_lang_Result_unwrap(otterop_lang_Result_t *this) {
    return this->res;
}

otterop_lang_Result_t *otterop_lang_Result_of(void *res, void *err) {
    return otterop_lang_Result_new(res, err);
}
