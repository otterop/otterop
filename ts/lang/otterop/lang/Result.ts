
export class Result<RES, ERR> {

    private _res : RES;

    private _err : ERR;

    private constructor(res : RES, err : ERR) {
        this._res = res;
        this._err = err;
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

