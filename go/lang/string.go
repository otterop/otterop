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


package lang

import "strings"

type String struct {
    wrapped *string
}

func stringNew(wrapped *string) *String {
    ret := new(String)
    ret.wrapped = wrapped
    return ret
}

func (this *String) Length() int {
    return len(*this.wrapped)
}

func (this *String) CompareTo(other interface{}) int {
    otherString, ok := other.(*String)
    if !ok {
        return 1
    }
    return strings.Compare(*this.wrapped, *otherString.wrapped)
}

func (this *String) String() string {
    return *this.wrapped
}

func (this *String) Unwrap() *string {
    return this.wrapped
}

func StringWrap(wrapped *string) *String {
    return stringNew(wrapped)
}

func StringLiteral(wrapped string) *string {
    return &wrapped
}

func StringWrapLiteral(wrapped string) *String {
    return stringNew(&wrapped)
}

func StringConcat(stringsIterable OOPIterable[*String]) *String {
    var sb strings.Builder
    it := stringsIterable.OOPIterator()
    for it.HasNext() {
        s := it.Next()
        sb.WriteString(*s.Unwrap())
    }
    ret := sb.String()
    return StringWrap(&ret)
}
