namespace Otterop.Lang;
public class String
{
    private string Wrapped;

    private String(string wrapped)
    {
        this.Wrapped = wrapped;
    }

    public static String Wrap(string wrapped)
    {
        return new String(wrapped);
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
}