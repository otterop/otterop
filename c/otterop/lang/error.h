#ifndef __otterop_lang_Error
#define __otterop_lang_Error
#include <otterop/lang/string.h>
#include <stdint.h>

typedef struct otterop_lang_String_s otterop_lang_String_t;

typedef struct otterop_lang_Error_s otterop_lang_Error_t;




otterop_lang_Error_t *otterop_lang_Error_new(int32_t code, otterop_lang_String_t *message);

int32_t otterop_lang_Error_code(otterop_lang_Error_t *self);


otterop_lang_String_t *otterop_lang_Error_message(otterop_lang_Error_t *self);

#endif
