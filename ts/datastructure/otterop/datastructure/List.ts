import { Array } from '@otterop/lang/Array';
import { Generic } from '@otterop/lang/Generic';
import { Panic } from '@otterop/lang/Panic';
import { String } from '@otterop/lang/String';

export class List<T> {

    #array : Array<T>;

    #capacity : number;

    #size : number;

    #tZero : T;

    public constructor() {
        this.#size = 0;
        this.#capacity = 4;
        let genericT : Generic<T> = new Generic<T>();
        this.#tZero = genericT.zero();
        this.#array = Array.newArray(this.#capacity, this.#tZero);
    }

    #ensureCapacity(capacity : number) : void {
        
        if (this.#capacity < capacity) {
            this.#capacity = this.#capacity * 2;
            let newArray : Array<T> = Array.newArray(this.#capacity, this.#tZero);
            Array.copy(this.#array, 0, newArray, 0, this.#size);
            this.#array = newArray;
        }
    }

    public add(element : T) : void {
        this.#ensureCapacity(this.#size + 1);
        this.#array.set(this.#size, element);
        this.#size++;
    }

    public addArray(src : Array<T>) : void {
        this.#ensureCapacity(this.#size + src.size());
        Array.copy(src, 0, this.#array, this.#size, src.size());
        this.#size += src.size();
    }

    public addList(src : List<T>) : void {
        this.addArray(src.#array);
    }

    #checkIndexOutOfBounds(index : number) : void {
        
        if (index < 0 || index > this.#size) {
            Panic.indexOutOfBounds(String.wrap("index is outside list bounds"));
        }
    }

    public insert(index : number, element : T) : void {
        this.#checkIndexOutOfBounds(index);
        this.#ensureCapacity(this.#size + 1);
        
        if (index < this.#size) {
            Array.copy(this.#array, index, this.#array, index + 1, this.#size - index);
        }
        this.#array.set(index, element);
        this.#size++;
    }

    public insertArray(index : number, src : Array<T>) : void {
        this.#checkIndexOutOfBounds(index);
        this.#ensureCapacity(this.#size + src.size());
        
        if (index < this.#size) {
            Array.copy(this.#array, index, this.#array, index + src.size(), this.#size - index);
        }
        Array.copy(src, 0, this.#array, index, src.size());
        this.#size += src.size();
    }

    public insertList(index : number, src : List<T>) : void {
        this.insertArray(index, src.#array);
    }

    public get(index : number) : T {
        this.#checkIndexOutOfBounds(index);
        return this.#array.get(index);
    }

    public removeIndex(index : number) : T {
        this.#checkIndexOutOfBounds(index);
        let ret : T = this.#array.get(index);
        
        if (index + 1 < this.#size) {
            Array.copy(this.#array, index + 1, this.#array, index, this.#size - index - 1);
        }
        this.#size--;
        return ret;
    }

    public removeRange(index : number, count : number) : List<T> {
        this.#checkIndexOutOfBounds(index);
        
        if (index + count > this.#size) {
            count = this.#size - index;
        }
        let ret : List<T> = new List<T>();
        let removed : Array<T> = Array.newArray(count, this.#tZero);
        Array.copy(this.#array, index, removed, 0, count);
        ret.addArray(removed);
        
        if (index + count < this.#size) {
            Array.copy(this.#array, index + count, this.#array, index, this.#size - index - count);
        }
        this.#size = this.#size - count;
        return ret;
    }

    public size() : number {
        return this.#size;
    }
}

