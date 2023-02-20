#include <otterop/lang/result.h>
#include <gc.h>
#include <stdlib.h>

typedef struct otterop_lang_Result_s {
    void *_res;
    void *_err;
} otterop_lang_Result_t;

otterop_lang_Result_t *otterop_lang_Result_new(void *_res, void *_err) {
    otterop_lang_Result_t *this = GC_malloc(sizeof(otterop_lang_Result_t));
    this->_res = _res;
    this->_err = _err;
    return this;
}

int otterop_lang_Result_is_ok(otterop_lang_Result_t *this) {
    return this->_err != NULL;
}

void *otterop_lang_Result_err(otterop_lang_Result_t *this) {
    return this->_err;
}

void *otterop_lang_Result_unwrap(otterop_lang_Result_t *this) {
    return this->_res;
}

otterop_lang_Result_t *otterop_lang_Result_of(void *res, void *err) {
    return otterop_lang_Result_new(res, err);
}