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


import { String } from './String';
import { Panic } from './Panic';
import { OOPIterable } from './OOPIterable';
import { OOPIterator } from './OOPIterator';
import { ArrayIterator } from './ArrayIterator';
import { PureIterator } from './PureIterator';
class ArrayOtterOP<T> implements OOPIterable<T>, Iterable<T>{
    private wrapped : T[];
    private start : number;
    private end : number;

    constructor(wrapped: T[], start: number, end: number) {
        this.wrapped = wrapped;
        this.start = start;
        this.end = end;
    }

    public get(i: number) : T {
        return this.wrapped[this.start + i];
    }

    public set(i: number, val: T) : void {
        this.wrapped[this.start + i] = val;
    }

    public size() : number {
        return this.end - this.start;
    }

    public slice(start: number, end: number) {
        const newStart = this.start + start;
        const newEnd = this.start + end;
        if (newStart < this.start || newStart > this.end || newEnd < newStart ||
            newEnd > this.end) throw new Error("slice arguments out of bounds");
        return new ArrayOtterOP<T>(this.wrapped, newStart, newEnd);
    }

    public static newArray<T>(size: number, clazz: T) : ArrayOtterOP<T> {
        const wrapped = new Array(size);
        return new ArrayOtterOP<T>(wrapped, 0, wrapped.length);
    }

    public static wrap<T>(wrapped: T[]) : ArrayOtterOP<T> {
        return new ArrayOtterOP<T>(wrapped, 0, wrapped.length)
    }

    public static wrapString(wrapped: string[]) : ArrayOtterOP<String> {
        let ret : String[] = [];
        for(let i = 0; i < wrapped.length; i++) {
            ret[i] = String.wrap(wrapped[i]);
        }
        return new ArrayOtterOP<String>(ret, 0, ret.length);
    }

    public static copy<T>(src: ArrayOtterOP<T>, srcPos: number, dst: ArrayOtterOP<T>, dstPos: number, length:  number) {
        if (src.start + srcPos + length > src.end)
            Panic.indexOutOfBounds(String.wrap("source index out of bounds"));
        if (dst.start + dstPos + length > dst.end)
            Panic.indexOutOfBounds(String.wrap("destination index out of bounds"));
        dst.wrapped = dst.wrapped.slice(dst.start, dst.start + dstPos).concat(src.wrapped.slice(src.start + srcPos, src.start + srcPos + length))
            .concat(dst.wrapped.slice(dst.start + dstPos + length));
    }

    public OOPIterator(): OOPIterator<T> {
        return new ArrayIterator(this);
    }

    [Symbol.iterator](): Iterator<T, any, undefined> {
        return PureIterator.newIterator(this.OOPIterator());
    }
}


export { ArrayOtterOP as Array };
