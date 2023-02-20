namespace Otterop.Lang;
public class Error
{
    int code;
    Otterop.Lang.String message;

    public Error(int code, Otterop.Lang.String message)
    {
        this.code = code;
        this.message = message;
    }

    public int Code()
    {
        return this.code;
    }

    public Otterop.Lang.String Message()
    {
        return this.message;
    }
}