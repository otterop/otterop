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
        return new Array<String>(wrappedStrings);
    }
}

public class Array<T> : Array
{
    private T[] Wrapped;

    internal Array(T[] wrapped)
    {
        this.Wrapped = wrapped;
    }

    public T Get(int i)
    {
        if (i < Wrapped.Count())
        {
            return Wrapped[i];
        }
        return default(T);
    }

    public void Set(int i, T value)
    {
        if (i < Wrapped.Count())
        {
            Wrapped[i] = value;
        }
    }

    public int Size()
    {
        return Wrapped.Count();
    }
}