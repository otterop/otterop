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

public class WrapperOOPIterable
{
    public static OOPIterable<OOP> Wrap<OOP, PURE>(IEnumerable<PURE> It, Func<PURE,OOP> Wrap, OOP OOPClass) where OOP : class where PURE : class
    {
        if (Wrap == null && It is WrapperOOPIterable<PURE, OOP>)
        {
            return (WrapperOOPIterable<PURE, OOP>) It;
        }
        return new WrapperOOPIterable<PURE, OOP>(It, Wrap);
    }

    public static IEnumerable<PURE> Unwrap<OOP, PURE>(OOPIterable<OOP> It, Func<OOP,PURE> Unwrap, PURE PureClass) where OOP : class where PURE : class
    {
        if (Unwrap == null) {
            return (IEnumerable<PURE>) It;
        }
        return new WrapperOOPIterable<OOP, PURE>(It, Unwrap);
    }
}

public class WrapperOOPIterable<FROM, TO> : OOPIterable<TO> where FROM : class where TO : class
{
    IEnumerable<FROM> it;
    Func<FROM, TO> wrap;

    internal WrapperOOPIterable(IEnumerable<FROM> it, Func<FROM, TO> wrap)
    {
        this.it = it;
        this.wrap = wrap != null ? wrap : (FROM x) => x as TO;
    }

    public OOPIterator<TO> OOPIterator()
    {
        return new WrapperOOPIterator<FROM, TO>(this.it.GetEnumerator(), this.wrap);
    }

    public IEnumerator<TO> GetEnumerator()
    {
        return new WrapperOOPIterator<FROM, TO>(this.it.GetEnumerator(), this.wrap);
    }

    System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator()
    {
        return GetEnumerator();
    }

}
