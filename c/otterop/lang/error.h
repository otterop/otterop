#ifndef __example_quicksort_Error
#define __example_quicksort_Error
#include <otterop/lang/string.h>

typedef struct example_quicksort_Error_s example_quicksort_Error_t;

example_quicksort_Error_t *example_quicksort_Error_new(int code, otterop_lang_String_t *message);

int example_quicksort_Error_code(example_quicksort_Error_t *this);

otterop_lang_String_t *example_quicksort_Error_message(example_quicksort_Error_t *this);
#endif
