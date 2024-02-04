import { String } from './String';

export class Panic {

    public static indexOutOfBounds(message : String) : void {
        throw new Error("indexOutOfBounds");
    }
}

