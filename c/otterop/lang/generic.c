#include <otterop/lang/generic.h>
#include <stdlib.h>
#include <gc.h>

typedef struct otterop_lang_Generic_s {
} otterop_lang_Generic_t;




void *otterop_lang_Generic_zero(otterop_lang_Generic_t *this) {
    return NULL;
}

int otterop_lang_Generic_is_zero(void *arg) {
    return arg == NULL;
}

otterop_lang_Generic_t* otterop_lang_Generic_new() {
    otterop_lang_Generic_t *this = GC_malloc(sizeof(*this));
    return this;
}
