package otterop.lang;

public class Result<RES,ERR> {

    private RES _res;

    private ERR _err;

    private Result(RES _res, ERR _err) {
        this._res = _res;
        this._err = _err;
    }

    public boolean isOK(){
        return  this._err != null;
    }

    public ERR err() {
        return this._err;
    }

    public RES unwrap() {
        return this._res;
    }

    public static <RES0, ERR0> Result<RES0,ERR0> of(RES0 res, ERR0 err) {
        return new Result<RES0,ERR0>(res, err);
    }
}

