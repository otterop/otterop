#ifndef __otterop_lang_Result
#define __otterop_lang_Result

typedef struct otterop_lang_Result_s otterop_lang_Result_t;

int otterop_lang_Result_is_ok(otterop_lang_Result_t *this);


void *otterop_lang_Result_err(otterop_lang_Result_t *this);


void *otterop_lang_Result_unwrap(otterop_lang_Result_t *this);


otterop_lang_Result_t *otterop_lang_Result_of(void *res, void *err);
#endif
