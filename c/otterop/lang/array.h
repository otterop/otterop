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

#ifndef __otterop_lang_Array
#define __otterop_lang_Array
#include <stddef.h>
#include <stdlib.h>
#include <gc.h>
#include <otterop/lang/string.h>
#include <otterop/lang/int/oop_iterable.h>
#include <otterop/lang/int/oop_iterator.h>

typedef struct otterop_lang_Array_s otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_new_array(int size, void *clazz);

otterop_lang_Array_t *otterop_lang_Array_wrap(void *wrapped, int wrapped_cnt);

otterop_lang_Array_t *otterop_lang_Array_wrap_string(char **wrapped, int wrapped_cnt);

void otterop_lang_Array_copy(otterop_lang_Array_t *src, int src_pos,
                             otterop_lang_Array_t *dst, int dst_pos, int size);

void *otterop_lang_Array_get(otterop_lang_Array_t *self, int i);

void otterop_lang_Array_set(otterop_lang_Array_t *self, int i, void* value);

otterop_lang_Array_t *otterop_lang_Array_slice(otterop_lang_Array_t *self, int start, int end);

int otterop_lang_Array_size(otterop_lang_Array_t *self);

otterop_lang_OOPIterator_t *otterop_lang_Array_oop_iterator(otterop_lang_Array_t *self);

otterop_lang_OOPIterable_t
*otterop_lang_Array__to_otterop_lang_OOPIterable(otterop_lang_Array_t *self);

#endif
