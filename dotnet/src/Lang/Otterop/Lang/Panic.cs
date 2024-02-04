namespace Otterop.Lang
{
    public class Panic
    {
        public static void IndexOutOfBounds(String message)
        {
            throw new IndexOutOfRangeException(message.unwrap());
        }

    }

}
