#include <otterop/lang/error.h>
#include <gc.h>

typedef struct otterop_lang_Error_s otterop_lang_Error_t;

typedef struct otterop_lang_Error_s {
    int code;
    otterop_lang_String_t *message;
} otterop_lang_Error_t;




otterop_lang_Error_t *otterop_lang_Error_new(int code, otterop_lang_String_t *message) {
    otterop_lang_Error_t *this = GC_malloc(sizeof(otterop_lang_Error_t));
    this->code = code;
    this->message = message;
    return this;
}

int otterop_lang_Error_code(otterop_lang_Error_t *this) {
    return this->code;
}

otterop_lang_String_t *otterop_lang_Error_message(otterop_lang_Error_t *this) {
    return this->message;
}
