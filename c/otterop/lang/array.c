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

#include "array.h"

typedef struct otterop_lang_Array_s {
    void **wrapped;
    int start;
    int end;
} otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_new(void **wrapped, int start, int end) {
    otterop_lang_Array_t *ret = GC_malloc(sizeof(otterop_lang_Array_t));
    ret->wrapped = wrapped;
    ret->start = start;
    ret->end = end;
    return ret;
}

otterop_lang_Array_t *otterop_lang_Array_wrap(void *wrapped, int wrapped_cnt) {
    return otterop_lang_Array_new((void **) wrapped, 0, wrapped_cnt);
}

otterop_lang_Array_t *otterop_lang_Array_wrap_string(char **wrapped, int wrapped_cnt) {
    int i;
    void **ret = GC_malloc(wrapped_cnt * sizeof(void *));
    for (i = 0; i < wrapped_cnt; i++) {
        ret[i] = otterop_lang_String_wrap(wrapped[i]);
    }
    return otterop_lang_Array_new(ret, 0, wrapped_cnt);
}

void *otterop_lang_Array_get(otterop_lang_Array_t *this, int i) {
    return this->wrapped[this->start + i];
}

void otterop_lang_Array_set(otterop_lang_Array_t *this, int i, void* value) {
    this->wrapped[this->start + i] = value;
}

otterop_lang_Array_t *otterop_lang_Array_slice(otterop_lang_Array_t *this, int start, int end) {
    int newStart = this->start + start;
    int newEnd = this->start + end;
    if (newStart < this->start || newStart > this->end || newEnd < newStart ||
            newEnd > this->end) {
        *(int*)0 = 0; // SEGFAULT
    }
    return otterop_lang_Array_new(this->wrapped, newStart, newEnd);
}

int otterop_lang_Array_size(otterop_lang_Array_t *this) {
    return this->end - this->start;
}
