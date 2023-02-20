namespace Otterop.Lang;

public class Result<RES, ERR>
{
    private RES _res;
    private ERR _err;
    private Result(RES _res, ERR _err)
    {
        this._res = _res;
        this._err = _err;
    }

    public bool IsOK()
    {
        return this._err != null;
    }

    public ERR Err()
    {
        return this._err;
    }

    public RES Unwrap()
    {
        return this._res;
    }

    public static Result<RES0, ERR0> Of<RES0, ERR0>(RES0 res, ERR0 err)
    {
        return new Result<RES0, ERR0>(res, err);
    }

}


public class Result
{
    public static Result<RES0, ERR0> Of<RES0, ERR0>(RES0 res, ERR0 err)
    {
        return Result<object, object>.Of<RES0, ERR0>(res, err);
    }

}
