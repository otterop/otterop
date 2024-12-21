#ifndef __otterop_lang_Result_int
#define __otterop_lang_Result_int

typedef struct otterop_lang_Result_s otterop_lang_Result_t;




void *otterop_lang_Result_err(otterop_lang_Result_t *self);


void *otterop_lang_Result_unwrap(otterop_lang_Result_t *self);


otterop_lang_Result_t *otterop_lang_Result_of(void *res, void *err);


otterop_lang_Result_t* otterop_lang_Result_new();
#endif
