namespace Otterop.Lang
{
    public class Result<RES, ERR>
    {
        private RES res;
        private ERR err;
        private Result(RES res, ERR err)
        {
            this.res = res;
            this.err = err;
        }

        public ERR Err()
        {
            return this.err;
        }

        public RES Unwrap()
        {
            return this.res;
        }

        public static Result<RES0, ERR0> Of<RES0, ERR0>(RES0 res, ERR0 err)
        {
            return new Result<RES0, ERR0>(res, err);
        }

    }


    class Result
    {
        public static Result<RES0, ERR0> Of<RES0, ERR0>(RES0 res, ERR0 err)
        {
            return Result<object, object>.Of<RES0, ERR0>(res, err);
        }

    }

}
