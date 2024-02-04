
export class Generic<T> {

    public zero() : T {
        return null;
    }

    public static isZero<T>(arg : T) : boolean {
        return arg == null;
    }
}

