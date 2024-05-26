namespace Otterop.Lang;

public class PureIterator
{
    public static IEnumerator<T> NewIterator<T>(OOPIterator<T> Iterator)
    {
        return new PureIterator<T>(Iterator);
    }
}

public class PureIterator<T> : System.Collections.Generic.IEnumerator<T>, System.Collections.IEnumerator
{
    private OOPIterator<T> Iterator;

    public T _Current = default(T);

    internal PureIterator(OOPIterator<T> Iterator)
    {
        this.Iterator = Iterator;
    }

    public bool MoveNext()
    {
        bool ret = this.Iterator.HasNext();
        if (ret)
        {
            this._Current = this.Iterator.Next();
        }
        return ret;
    }

    public void Reset()
    {
        throw new NotSupportedException();
    }

    T System.Collections.Generic.IEnumerator<T>.Current
    {
        get { return this._Current; }
    }
    object System.Collections.IEnumerator.Current
    {
        get { return this._Current; }
    }

    public void Dispose()
    {

    }

    public static IEnumerator<T> NewIterator(OOPIterator<T> Iterator)
    {
        return new PureIterator<T>(Iterator);
    }
}
