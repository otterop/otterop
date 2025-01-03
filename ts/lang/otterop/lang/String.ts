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

import { OOPIterable } from "./OOPIterable";


export class String {

    private wrapped : string;

    private constructor(wrapped: string) {
        this.wrapped = wrapped;
    }

    public static concat(strings: OOPIterable<String>) : String {
        let sb = [];
        let it = strings.OOPIterator();
        while (it.hasNext()) {
            let s = it.next();
            sb.push(s.unwrap());
        }
        return String.wrap(sb.join(""));
    }

    public static wrap(wrapped: string) : String {
        return new String(wrapped);
    }

    public static unwrap(wrapper: String) : string {
        return wrapper.wrapped;
    }

    public compareTo(other: String) : number {
        if (!other) return -1;
        if (this.wrapped < other.wrapped) return -1;
        else if (this.wrapped > other.wrapped) return 1;
        else return 0;
    }

    public length() : number {
        return this.wrapped.length;
    }

    public unwrap() : string {
        return this.wrapped;
    }

    toString() : string {
        return this.wrapped;
    }
}
