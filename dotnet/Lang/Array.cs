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