import { String } from './String';

export class Error {

    private _code : number;

    private _message : String;

    public constructor(code : number, message : String) {
        this._code = code;
        this._message = message;
    }

    public code() : number {
        return this._code;
    }

    public message() : String {
        return this._message;
    }
}

