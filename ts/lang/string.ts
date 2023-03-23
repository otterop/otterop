export class String {
    private wrapped : string;

    private constructor(wrapped: string) {
        this.wrapped = wrapped;
    }

    public static wrap(wrapped: string) : String {
        return new String(wrapped);
    }

    public compareTo(other: String) : number {
        if (!other) return -1;
        if (this.wrapped < other.wrapped) return -1;
        else if (this.wrapped > other.wrapped) return 1;
        else return 0;
    }

    toString() : string {
        return this.wrapped;
    }
}
