package lang



type Result[RES any, ERR any] struct {
    res RES
    err ERR
}




func resultNew[RES any, ERR any](res RES, err ERR) *Result[RES, ERR] {
    this := new(Result[RES, ERR])
    this.res = res
    this.err = err
    return this
}

func (this *Result[RES, ERR]) Err() ERR {
    return this.err
}

func (this *Result[RES, ERR]) Unwrap() RES {
    return this.res
}

func Of[RES0 any, ERR0 any](res RES0, err ERR0) *Result[RES0, ERR0] {
    return resultNew[RES0, ERR0](res, err)
}