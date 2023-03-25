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
 *    * Neither the name of Confluent Inc. nor the names of its
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

typedef struct otterop_lang_String_s {
    char *wrapped;
    size_t length;
} otterop_lang_String_t;

otterop_lang_String_t *otterop_lang_String_wrap(char *wrapped) {
    if (wrapped == NULL) return NULL;
    otterop_lang_String_t *ret = GC_malloc(sizeof(otterop_lang_String_t));
    int length = strlen(wrapped);
    char *new_str = GC_malloc(length * (sizeof(char) + 1));
    strcpy(new_str, wrapped);
    ret->wrapped = new_str;
    ret->length = length;
    return ret;
}

char *otterop_lang_String_unwrap(otterop_lang_String_t *a) {
    return a->wrapped;
}

int otterop_lang_String_compare_to(otterop_lang_String_t *a, otterop_lang_String_t *b) {
    if (a == NULL) return b == NULL ? 0 : -1;
    return strcmp(a->wrapped,b->wrapped);
}
