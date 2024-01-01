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


namespace Otterop.Lang;
public class Array
{
    internal Array() { }

    public static Array<String> WrapString(string[] args)
    {
        String[] wrappedStrings = new String[args.Count()];
        int i = 0;
        foreach(var arg in args)
        {
            wrappedStrings[i] = String.Wrap(arg);
            i++;
        }
        return new Array<String>(wrappedStrings, 0, wrappedStrings.Count());
    }

    public static Array<T> Wrap<T>(T[] args)
    {
        T[] wrapped = new T[args.Count()];
        int i = 0;
        foreach(var arg in args)
        {
            wrapped[i] = arg;
            i++;
        }
        return new Array<T>(wrapped, 0, wrapped.Count());
    }
}

public class Array<T> : Array
{
    private T[] wrapped;

    private int start;

    private int end;

    internal Array(T[] wrapped, int start, int end)
    {
        this.wrapped = wrapped;
        this.start = start;
        this.end = end;
    }

    public T Get(int i)
    {
        return wrapped[start + i];
    }

    public void Set(int i, T value)
    {
        wrapped[start + i] = value;
    }

    public Array<T> Slice(int start, int end) {
        var newStart = this.start + start;
        var newEnd = this.start + end;
        if (newStart < this.start || newStart > this.end || newEnd < newStart ||
            newEnd > this.end) {
                throw new IndexOutOfRangeException();
        }
        return new Array<T>(wrapped, newStart, newEnd);
    }

    public int Size()
    {
        return end - start;
    }
}
