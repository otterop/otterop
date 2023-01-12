#ifndef __otterop_lang_String
#define __otterop_lang_String
#include <string.h>
#include <stdlib.h>
#include <gc.h>
#include <stdio.h>

typedef struct otterop_lang_String_s otterop_lang_String_t;

otterop_lang_String_t *otterop_lang_String_wrap(char *a);

char *otterop_lang_String_string(otterop_lang_String_t *a);

char *otterop_lang_String_unwrap(otterop_lang_String_t *a);

int otterop_lang_String_compare_to(otterop_lang_String_t *a, otterop_lang_String_t *b);
#endif
