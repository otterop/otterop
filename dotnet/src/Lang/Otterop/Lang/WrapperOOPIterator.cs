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

public class WrapperOOPIterator
{
    public static OOPIterable<OOP> Wrap<PURE, OOP>(IEnumerable<PURE> It, Func<PURE,OOP> Wrap) where OOP : class where PURE : class
    {
        if (Wrap == null && It is WrapperOOPIterable<PURE, OOP>)
        {
            return (WrapperOOPIterable<PURE, OOP>) It;
        }
        return new WrapperOOPIterable<PURE, OOP>(It, Wrap);
    }

    public static IEnumerable<PURE> Unwrap<OOP, PURE>(OOPIterable<OOP> It, Func<OOP, PURE> Unwrap) where OOP : class where PURE : class
    {
        if (Unwrap == null) {
            return (IEnumerable<PURE>) It;
        }
        return new WrapperOOPIterable<OOP, PURE>(It, Unwrap);
    }
}

public class WrapperOOPIterator<FROM, TO> : OOPIterator<TO>, IEnumerator<TO> where FROM : class where TO : class
{
    IEnumerator<FROM> It;
    Func<FROM, TO> WrapFunc;

    bool PreCurrent = true;

    bool CurrentFetched = false;

    bool NextFetched = false;

    bool HasCurrentElement = false;

    bool HasNextElement = false;

    TO CurrentOOP;

    TO NextOOP;

    internal WrapperOOPIterator(IEnumerator<FROM> It, Func<FROM, TO> WrapFunc)
    {
        this.It = It;
        this.WrapFunc = WrapFunc != null ? WrapFunc : (FROM x) => {
            if (x is TO)
                return x as TO;
            else
                Panic.InvalidOperation(String.Wrap("Generic type FROM is not the same as type TO"));
            return default(TO);
        };
    }

    bool MoveNextOOP()
    {
        if (this.PreCurrent)
        {
            if (!this.CurrentFetched)
                this.PreFetchCurrentOOP();
            this.PreCurrent = false;
            return this.HasCurrentElement;
        }
        if (!this.HasCurrentElement)
            Panic.IndexOutOfBounds(String.Wrap("no current element"));

        if (!this.NextFetched)
            this.PreFetchNextOOP();
        this.CurrentOOP = this.NextOOP;
        this.HasCurrentElement = this.HasNextElement;
        this.NextFetched = false;
        return this.HasCurrentElement;
    }

    bool PreFetchCurrentOOP()
    {
        this.HasCurrentElement = this.It.MoveNext();
        if (this.HasCurrentElement)
            this.CurrentOOP = this.WrapFunc(this.It.Current);
        this.CurrentFetched = true;
        return this.HasCurrentElement;
    }

    bool PreFetchNextOOP()
    {
        this.HasNextElement = this.It.MoveNext();
        if (this.HasNextElement)
            this.NextOOP = this.WrapFunc(this.It.Current);
        this.NextFetched = true;
        return this.HasNextElement;
    }

    public bool HasNext()
    {
        if (this.PreCurrent)
        {
            if (!this.CurrentFetched)
                this.PreFetchCurrentOOP();
            return this.HasCurrentElement;
        }
        else if (!this.NextFetched)
        {
            this.PreFetchNextOOP();
        }

        return this.HasNextElement;
    }

    public void Reset()
    {
        throw new NotImplementedException();
    }

    public void Dispose()
    {
        this.It.Dispose();
    }

    public TO Next()
    {
        this.MoveNextOOP();
        if (this.HasCurrentElement)
            return this.CurrentOOP;
        else
            Panic.IndexOutOfBounds(String.Wrap("reached end of iterator"));
        return default(TO);
    }

    public TO Current
    {
        get
        {
            if (this.PreCurrent)
            {
                Panic.IndexOutOfBounds(String.Wrap("before start of iteration"));
            }
            return this.CurrentOOP;
        }
    }

    object System.Collections.IEnumerator.Current
    {
        get
        {
            if (this.PreCurrent)
            {
                Panic.IndexOutOfBounds(String.Wrap("before start of iteration"));
            }
            return this.CurrentOOP;
        }
    }

    public bool MoveNext()
    {
        return this.MoveNextOOP();
    }

}
