namespace Otterop.Lang
{
    public class Panic
    {
        public static void IndexOutOfBounds(String message)
        {
            throw new IndexOutOfRangeException(message.Unwrap());
        }

        public static void InvalidOperation(String message)
        {
            throw new InvalidOperationException(message.Unwrap());
        }

    }

}
