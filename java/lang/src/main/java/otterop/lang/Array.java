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


package otterop.lang;

public class Array<T> {
    private final int start;
    private final int end;
    private T[] _wrapped;

    private Array(T[] array, int start, int end) {
        this._wrapped = array;
        this.start = start;
        this.end = end;
    }

    public T get(int i) {
        return _wrapped[start + i];
    }

    public void set(int i, T value) {
        _wrapped[start + i] = value;
    }

    public int size() {
        return end - start;
    }

    public Array<T> slice(int start, int end) {
        var newStart = this.start + start;
        var newEnd = this.start + end;
        if (newStart < this.start || newStart > this.end || newEnd < newStart ||
                newEnd > this.end) throw new ArrayIndexOutOfBoundsException();
        return new Array<T>(_wrapped, newStart, newEnd);
    }

    public static <T> Array<T> wrap(T[] list) {
        return new Array<T>(list,0 ,list.length);
    }

    public static Array<String> wrapString(java.lang.String[] arg) {
        String[] ret = new String[arg.length];
        for (int i = 0; i < arg.length; i++) {
            ret[i] = String.wrap(arg[i]);
        }
        return new Array<>(ret, 0, ret.length);
    }
}

