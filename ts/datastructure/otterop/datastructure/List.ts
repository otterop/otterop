import { Array } from '@otterop/lang/Array';
import { Generic } from '@otterop/lang/Generic';
import { Panic } from '@otterop/lang/Panic';
import { String } from '@otterop/lang/String';

export class List<T> {

    private _array : Array<T>;

    private _capacity : number;

    private _size : number;

    private _tZero : T;

    public constructor() {
        this._size = 0;
        this._capacity = 4;
        this._tZero = new Generic<T>().zero();
        this._array = Array.newArray(this._capacity, this._tZero);
    }

    ensureCapacity(capacity : number) : void {
        
        if (this._capacity < capacity) {
            this._capacity = this._capacity * 2;
            let newArray : Array<T> = Array.newArray(this._capacity, this._tZero);
            Array.copy(this._array, 0, newArray, 0, this._size);
            this._array = newArray;
        }
    }

    public add(element : T) : void {
        this.ensureCapacity(this._size + 1);
        this._array.set(this._size, element);
        this._size++;
    }

    public addArray(src : Array<T>) : void {
        this.ensureCapacity(this._size + src.size());
        Array.copy(src, 0, this._array, this._size, src.size());
        this._size += src.size();
    }

    public addList(src : List<T>) : void {
        this.addArray(src._array);
    }

    checkIndexOutOfBounds(index : number) : void {
        
        if (index < 0 || index > this._size) {
            Panic.indexOutOfBounds(String.wrap("index is outside list bounds"));
        }
    }

    public insert(index : number, element : T) : void {
        this.checkIndexOutOfBounds(index);
        this.ensureCapacity(this._size + 1);
        
        if (index < this._size) {
            Array.copy(this._array, index, this._array, index + 1, this._size - index);
        }
        this._array.set(index, element);
        this._size++;
    }

    public insertArray(index : number, src : Array<T>) : void {
        this.checkIndexOutOfBounds(index);
        this.ensureCapacity(this._size + src.size());
        
        if (index < this._size) {
            Array.copy(this._array, index, this._array, index + src.size(), this._size - index);
        }
        Array.copy(src, 0, this._array, index, src.size());
        this._size += src.size();
    }

    public insertList(index : number, src : List<T>) : void {
        this.insertArray(index, src._array);
    }

    public get(index : number) : T {
        this.checkIndexOutOfBounds(index);
        return this._array.get(index);
    }

    public removeIndex(index : number) : T {
        this.checkIndexOutOfBounds(index);
        let ret : T = this._array.get(index);
        
        if (index + 1 < this._size) {
            Array.copy(this._array, index + 1, this._array, index, this._size - index - 1);
        }
        this._size--;
        return ret;
    }

    public removeRange(index : number, count : number) : List<T> {
        this.checkIndexOutOfBounds(index);
        
        if (index + count > this._size) {
            count = this._size - index;
        }
        let ret : List<T> = new List<T>();
        let removed : Array<T> = Array.newArray(count, this._tZero);
        Array.copy(this._array, index, removed, 0, count);
        ret.addArray(removed);
        
        if (index + count < this._size) {
            Array.copy(this._array, index + count, this._array, index, this._size - index - count);
        }
        this._size = this._size - count;
        return ret;
    }

    public size() : number {
        return this._size;
    }
}

