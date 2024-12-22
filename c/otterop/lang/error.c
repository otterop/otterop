#include <stdint.h>
#include <otterop/lang/int/error.h>
#include <gc.h>

typedef struct otterop_lang_Error_s {
    int32_t code;
    otterop_lang_String_t *message;
} otterop_lang_Error_t;




otterop_lang_Error_t *otterop_lang_Error_new(int32_t code, otterop_lang_String_t *message) {
    otterop_lang_Error_t *self = GC_malloc(sizeof(otterop_lang_Error_t));
    self->code = code;
    self->message = message;
    return self;
}

int32_t otterop_lang_Error_code(otterop_lang_Error_t *self) {
    return self->code;
}

otterop_lang_String_t *otterop_lang_Error_message(otterop_lang_Error_t *self) {
    return self->message;
}
