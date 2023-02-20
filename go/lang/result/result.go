package result

import (
)

type Result[RES any, ERR any] struct {
    _res *RES
    _err *ERR
}

func newResult[RES any, ERR any](_res *RES, _err *ERR) *Result[RES, ERR] {
    this := new(Result[RES, ERR])
    this._res = _res
    this._err = _err
    return this
}

func (this *Result[RES, ERR]) IsOK() bool {
    return this._err != nil
}

func (this *Result[RES, ERR]) Err() *ERR {
    return this._err
}

func (this *Result[RES, ERR]) Unwrap() *RES {
    return this._res
}

func Of[RES0 any, ERR0 any](res *RES0, err *ERR0) *Result[RES0, ERR0] {
    return newResult[RES0, ERR0](res, err)
}