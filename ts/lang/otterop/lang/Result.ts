
export class Result<RES, ERR> {

    #res : RES;

    #err : ERR;

    private constructor(res : RES, err : ERR) {
        this.#res = res;
        this.#err = err;
    }

    public err() : ERR {
        return this.#err;
    }

    public unwrap() : RES {
        return this.#res;
    }

    public static of<RES0, ERR0>(res : RES0, err : ERR0) : Result<RES0, ERR0> {
        return new Result<RES0,ERR0>(res, err);
    }
}

