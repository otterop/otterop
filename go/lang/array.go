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


package lang;

type Array[T any] struct {
    wrapped []T;
    start int
    end int
}

func arrayNew[T any](array []T, start int, end int) *Array[T] {
    ret := new(Array[T])
    ret.wrapped = array
    ret.start = start
    ret.end = end
    return ret
}

func (a *Array[T]) Size() int {
    return a.end - a.start
}

func (a *Array[T]) Get(i int) T {
    return a.wrapped[a.start + i]
}

func (a *Array[T]) Set(i int, val T) {
    a.wrapped[a.start + i] = val
}

func (a *Array[T]) Slice(start int, end int) *Array[T] {
    newStart := a.start + start
    newEnd := a.start + end
    if newStart < a.start || newStart > a.end || newEnd < newStart ||
       newEnd > a.end {
        PanicIndexOutOfBounds(StringWrapLiteral("slice arguments out of bounds"))
    }
    return arrayNew(a.wrapped, newStart, newEnd)
}

func ArrayNewArray[T any](size int, clazz T) *Array[T] {
    array := make([]T, size)
    return arrayNew(array, 0, len(array))
}

func ArrayWrap[T any](array []T) *Array[T] {
    return arrayNew(array, 0, len(array))
}

func ArrayWrapString(arg []*string) *Array[*String] {
    ret := make([]*String, len(arg))
    for i, v := range arg {
        ret[i] = StringWrap(v)
    }
    return arrayNew(ret, 0, len(ret))
}


func ArrayCopy[T any](src *Array[T], srcPos int, dst *Array[T], dstPos int, length int) {
    if src.start + srcPos + length > src.end {
        PanicIndexOutOfBounds(StringWrapLiteral("source array index out of bounds"))
    }
    if dst.start + dstPos + length > dst.end {
        PanicIndexOutOfBounds(StringWrapLiteral("destination array index out of bounds"))
    }
    copy(dst.wrapped[dst.start + dstPos:dst.start + dstPos + length],
         src.wrapped[src.start + srcPos:src.start + srcPos + length])
}

func (this *Array[T]) OOPIterator() OOPIterator[T] {
    return arrayIteratorNew[T](this)
}
