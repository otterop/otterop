import { String } from './string';

export class Array<T> {
    private wrapped : T[];
    private start : number;
    private end : number;

    private constructor(wrapped: T[], start : number, end: number) {
        this.wrapped = wrapped;
        this.start = start;
        this.end = end;
    }

    public get(i: number) : T {
        return this.wrapped[this.start + i];
    }

    public set(i: number, val: T) : void {
        this.wrapped[this.start + i] = val;
    }

    public size() : number {
        return this.end - this.start;
    }

    public slice(start: number, end: number) {
        const newStart = this.start + start;
        const newEnd = this.start + end;
        if (newStart < this.start || newStart > this.end || newEnd < newStart ||
            newEnd > this.end) throw new Error("slice arguments out of bounds");
        return new Array<T>(this.wrapped, newStart, newEnd);
    }

    public static wrap<T>(wrapped: T[]) : Array<T> {
        return new Array<T>(wrapped, 0, wrapped.length)
    }

    public static wrapString(wrapped: string[]) : Array<String> {
        let ret : String[] = [];
        for(let i = 0; i < wrapped.length; i++) {
            ret[i] = String.wrap(wrapped[i]);
        }
        return new Array<String>(ret, 0, ret.length);
    }
}
