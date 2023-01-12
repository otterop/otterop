#include "console.h"

void otterop_io_Console_println(otterop_lang_String_t *str) {
    fprintf(stdout, "%s\n", otterop_lang_String_unwrap(str));
}
