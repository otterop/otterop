namespace Otterop.Lang
{
    public interface OOPIterable<T> : IEnumerable<T>
    {
        OOPIterator<T> OOPIterator();
    }

}
