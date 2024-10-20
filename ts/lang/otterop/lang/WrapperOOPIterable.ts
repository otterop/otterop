/**
 * Copyright (c) 2024 The OtterOP Authors. All rights reserved.
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

import { OOPIterable } from "./OOPIterable"
import { OOPIterator } from "./OOPIterator";
import { WrapperOOPIterator } from "./WrapperOOPIterator"
import { WrapperIterator } from "./WrapperIterator"

export class WrapperOOPIterable<FROM,TO> implements OOPIterable<TO> {

    private iterable : Iterable<FROM>
    private wrap : (el: FROM) => TO

    constructor(iterable : Iterable<FROM>, wrap: (el: FROM) => TO = (x) => x as unknown as TO) {
        this.iterable = iterable;
        this.wrap = wrap;
    }

    OOPIterator() : OOPIterator<TO> {
        return new WrapperOOPIterator(this.iterable[Symbol.iterator](), this.wrap);
    }

    [Symbol.iterator](): Iterator<TO, TO, TO> {
        return new WrapperIterator(this.iterable[Symbol.iterator](), this.wrap);
    }

    static wrap<OOP, PURE>(iterable : Iterable<PURE>, wrap : (el: PURE) => OOP) : WrapperOOPIterable<PURE, OOP> {
        if (!wrap && iterable instanceof WrapperOOPIterable)
            return iterable;
        return new WrapperOOPIterable(iterable, wrap);
    }

    static unwrap<OOP, PURE>(oopIterable : OOPIterable<OOP>, unwrap : (el: OOP) => PURE) : Iterable<PURE> {
        if (!unwrap && oopIterable instanceof WrapperOOPIterable)
            return oopIterable;
        return new WrapperOOPIterable(oopIterable, unwrap);
    }
}

