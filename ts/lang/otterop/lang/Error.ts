import { String } from './String';

export class Error {

    #code : number;

    #message : String;

    public constructor(code : number, message : String) {
        this.#code = code;
        this.#message = message;
    }

    public code() : number {
        return this.#code;
    }

    public message() : String {
        return this.#message;
    }
}

