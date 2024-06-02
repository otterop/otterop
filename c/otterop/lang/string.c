/**
 * Copyright (c) 2023 The OtterOP Authors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *    * Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

#include <otterop/lang/string.h>
#include "otterop/lang/oop_iterator.h"

typedef struct otterop_lang_String_s {
    char *wrapped;
    size_t ascii_length;
    size_t unicode_length;
} otterop_lang_String_t;

static char *otterop_lang_String_substring(char *s, int unicode_idx) {
    int i = 0, j = -1;
    while (s[i]) {
        if ((s[i] & 0xC0) != 0x80)
            j++;
            if (j == unicode_idx)
                return &s[i];
        i++;
    }
    return "";
}

static void otterop_lang_String_lengths(char *s, int *ascii_length, int *unicode_length) {
    int i = 0, j = -1;
    while (s[i]) {
        if ((s[i] & 0xC0) != 0x80)
            j++;
        i++;
    }
    *ascii_length = i;
    *unicode_length = j + 1;
}

static otterop_lang_String_t *otterop_lang_String_new(char *wrapped, int ascii_length, int unicode_length) {
    otterop_lang_String_t *ret = GC_malloc(sizeof(otterop_lang_String_t));
    ret->wrapped = wrapped;
    ret->ascii_length = ascii_length;
    ret->unicode_length = unicode_length;
    return ret;
}

otterop_lang_String_t *otterop_lang_String_wrap(char *wrapped) {
    int ascii_length, unicode_length;
    if (wrapped == NULL) return NULL;
    otterop_lang_String_t *ret = GC_malloc(sizeof(otterop_lang_String_t));

    otterop_lang_String_lengths(wrapped, &ascii_length, &unicode_length);

    char *new_str = GC_malloc((ascii_length + 1) * sizeof(char));
    strcpy(new_str, wrapped);

    ret->wrapped = new_str;
    ret->ascii_length = ascii_length;
    ret->unicode_length = unicode_length;
    return ret;
}

otterop_lang_String_t *otterop_lang_String_concat(otterop_lang_OOPIterable_t *strings) {
    int ascii_length = 0;
    int unicode_length = 0;
    int pos = 0;

    otterop_lang_OOPIterator_t *it = otterop_lang_OOPIterable_oop_iterator(strings);
    while (otterop_lang_OOPIterator_has_next(it)) {
        otterop_lang_String_t *s = otterop_lang_OOPIterator_next(it);
        ascii_length += s->ascii_length;
        unicode_length += s->unicode_length;
    }

    char *ret = malloc((ascii_length + 1) * sizeof(*ret));
    it = otterop_lang_OOPIterable_oop_iterator(strings);
    while (otterop_lang_OOPIterator_has_next(it)) {
        otterop_lang_String_t *s = otterop_lang_OOPIterator_next(it);
        memcpy(&ret[pos], s->wrapped, s->ascii_length);
        pos += s->ascii_length;
    }
    ret[pos] = '\0';
    return otterop_lang_String_new(ret, ascii_length, unicode_length);
}

static int otterop_lang_String_length(otterop_lang_String_t *s) {
    return s->unicode_length;
}

char *otterop_lang_String_unwrap(otterop_lang_String_t *a) {
    return a->wrapped;
}

int otterop_lang_String_compare_to(otterop_lang_String_t *a, otterop_lang_String_t *b) {
    if (a == NULL) return b == NULL ? 0 : -1;
    return strcmp(a->wrapped,b->wrapped);
}
