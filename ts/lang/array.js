"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
exports.Array = void 0;
const string_1 = require("./string");
class Array {
    constructor(wrapped) {
        this.wrapped = wrapped;
    }
    get(i) {
        return this.wrapped[i];
    }
    set(i, val) {
        this.wrapped[i] = val;
    }
    size() {
        return this.wrapped.length;
    }
    static wrap(wrapped) {
        return new Array(wrapped);
    }
    static wrapString(wrapped) {
        let ret = [];
        for (let i = 0; i < wrapped.length; i++) {
            ret[i] = string_1.String.wrap(wrapped[i]);
        }
        return Array.wrap(ret);
    }
}
exports.Array = Array;
