#include <otterop/lang/panic.h>
#include <gc.h>
#include <assert.h>

void otterop_lang_Panic_index_out_of_bounds(otterop_lang_String_t *message) {
    assert(!*otterop_lang_String_unwrap(message));
}

void otterop_lang_Panic_invalid_operation(otterop_lang_String_t *message) {
    assert(!*otterop_lang_String_unwrap(message));
}
