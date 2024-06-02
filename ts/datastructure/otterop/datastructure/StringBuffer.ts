import { OOPIterable } from '@otterop/lang/OOPIterable';
import { String } from '@otterop/lang/String';
import { LinkedList } from './LinkedList';

export class StringBuffer {

    #strings : LinkedList<String>;

    public constructor() {
        this.#strings = new LinkedList<String>();
    }

    public add(s : String) : void {
        this.#strings.addLast(s);
    }

    public OOPString() : String {
        let strings : OOPIterable<String> = this.#strings;
        return String.concat(strings);
    }
}

