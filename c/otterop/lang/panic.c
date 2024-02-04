#include <otterop/lang/panic.h>
#include <gc.h>
#include <assert.h>

typedef struct otterop_lang_Panic_s {
} otterop_lang_Panic_t;




void otterop_lang_Panic_index_out_of_bounds(otterop_lang_String_t *message) {
    assert(!*otterop_lang_String_unwrap(message));
}

otterop_lang_Panic_t* otterop_lang_Panic_new() {
    otterop_lang_Panic_t *this = GC_malloc(sizeof(*this));
    return this;
}
