﻿/**
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


namespace Otterop.Lang;

using System.Text;

public class String
{
    private string Wrapped;

    private String(string wrapped)
    {
        this.Wrapped = wrapped;
    }

    public int CompareTo(String other)
    {
        if (other == null) return -1;
        return this.Wrapped.CompareTo(other.Wrapped);
    }

    public override string ToString()
    {
        return Wrapped;
    }

    public string Unwrap()
    {
        return this.Wrapped;
    }

    public int Length()
    {
        return this.Wrapped.Length;
    }


    public static String Wrap(string towrap)
    {
        return new String(towrap);
    }

    public static string Unwrap(String wrapped)
    {
        return wrapped.Unwrap();
    }


    public static String Concat(OOPIterable<String> strings)
    {
        OOPIterator<String> it = strings.OOPIterator();
        var sb = new StringBuilder();
        while (it.HasNext())
        {
            String s = it.Next();
            sb.Append(s.Unwrap());
        }
        return String.Wrap(sb.ToString());
    }
}
