#include <otterop/lang/error.h>

typedef struct example_quicksort_Error_s {
    int code;
    otterop_lang_String_t *message;
} example_quicksort_Error_t;

example_quicksort_Error_t *example_quicksort_Error_new(int code, otterop_lang_String_t *message) {
    example_quicksort_Error_t *this = GC_malloc(sizeof(example_quicksort_Error_t));
    this->code = code;
    this->message = message;
    return this;
}

int example_quicksort_Error_code(example_quicksort_Error_t *this) {
    return this->code;
}

otterop_lang_String_t *example_quicksort_Error_message(example_quicksort_Error_t *this) {
    return this->message;
}