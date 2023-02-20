
export class Result<RES, ERR> {

    private _res : RES;

    private _err : ERR;

    private constructor(_res : RES, _err : ERR) {
        this._res = _res;
        this._err = _err;
    }

    public isOK() : boolean {
        return this._err != null;
    }

    public err() : ERR {
        return this._err;
    }

    public unwrap() : RES {
        return this._res;
    }

    public static of<RES0, ERR0>(res : RES0, err : ERR0) : Result<RES0, ERR0> {
        return new Result<RES0,ERR0>(res, err);
    }
}

