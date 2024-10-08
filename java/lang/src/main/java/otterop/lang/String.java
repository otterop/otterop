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

public class String implements Comparable {
    private java.lang.String wrapped;

    private String(java.lang.String wrapped) {
        this.wrapped = wrapped;
    }

    public int length() {
        return this.wrapped.length();
    }

    public java.lang.String toString() {
        return wrapped;
    }

    public static String wrap(java.lang.String wrapped) {
        if (wrapped == null) return null;
        return new String(wrapped);
    }

    public static java.lang.String unwrap(String wrapped) {
        return wrapped.unwrap();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof String)) return 1;
        return this.wrapped.compareTo(((String)o).wrapped);
    }

    public java.lang.String unwrap() {
        return this.wrapped;
    }

    public static String concat(OOPIterable<String> strings) {
        java.lang.StringBuffer sb = new java.lang.StringBuffer();
        OOPIterator<String> it = strings.OOPIterator();
        while (it.hasNext()) {
            String s = it.next();
            sb.append(s.unwrap());
        }
        return String.wrap(sb.toString());
    }
}
