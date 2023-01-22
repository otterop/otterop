/*
 * Copyright (c) 2022, Emanuele Sabellico
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

import { String } from './string';

export class Array<T> {
    private wrapped : T[];

    constructor(wrapped: T[]) {
        this.wrapped = wrapped;
    }

    public get(i: number) : T {
        return this.wrapped[i];
    }

    public set(i: number, val: T) : void {
        this.wrapped[i] = val;
    }

    public size() : number {
        return this.wrapped.length;
    }

    public static wrap<T>(wrapped: T[]) : Array<T> {
        return new Array<T>(wrapped)
    }

    public static wrapString(wrapped: string[]) : Array<String> {
        let ret : String[] = [];
        for(let i = 0; i < wrapped.length; i++) {
            ret[i] = String.wrap(wrapped[i]);
        }
        return Array.wrap(ret);
    }
}
