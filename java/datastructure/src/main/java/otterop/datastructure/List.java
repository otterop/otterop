package otterop.datastructure;

import otterop.lang.Array;
import otterop.lang.Generic;
import otterop.lang.Panic;
import otterop.lang.String;

public class List<T> {

    private Array<T> array;
    private int capacity;
    private int size;
    private T tZero;

    public List() {
        this.size = 0;
        this.capacity = 4;
        Generic<T> genericT = new Generic<T>();
        this.tZero = genericT.zero();
        this.array = Array.newArray(this.capacity, this.tZero);
    }

    private void ensureCapacity(int capacity) {
        if (this.capacity < capacity) {
            this.capacity = this.capacity * 2;
            Array<T> newArray = Array.newArray(this.capacity, this.tZero);
            Array.copy(this.array, 0, newArray, 0, this.size);
            this.array = newArray;
        }
    }

    public void add(T element) {
        this.ensureCapacity(this.size + 1);
        this.array.set(this.size, element);
        this.size++;
    }

    public void addArray(Array<T> src) {
        this.ensureCapacity(this.size + src.size());
        Array.copy(src, 0, this.array, this.size, src.size());
        this.size += src.size();
    }

    public void addList(List<T> src) {
        this.addArray(src.array);
    }

    private void checkIndexOutOfBounds(int index) {
        if (index < 0 || index > this.size) {
            Panic.indexOutOfBounds(String.wrap("index is outside list bounds"));
        }
    }

    public void insert(int index, T element) {
        this.checkIndexOutOfBounds(index);
        this.ensureCapacity(this.size + 1);
        if (index < this.size) {
            Array.copy(this.array, index, this.array, index + 1, this.size - index);
        }
        this.array.set(index, element);
        this.size++;
    }

    public void insertArray(int index, Array<T> src) {
        this.checkIndexOutOfBounds(index);
        this.ensureCapacity(this.size + src.size());
        if (index < this.size) {
            Array.copy(this.array, index, this.array, index + src.size(), this.size - index);
        }
        Array.copy(src, 0, this.array, index, src.size());
        this.size += src.size();
    }

    public void insertList(int index, List<T> src) {
        this.insertArray(index, src.array);
    }

    public T get(int index) {
        this.checkIndexOutOfBounds(index);
        return this.array.get(index);
    }

    public T removeIndex(int index) {
        this.checkIndexOutOfBounds(index);
        T ret = this.array.get(index);
        if (index + 1 < this.size) {
            Array.copy(this.array, index + 1, this.array, index, this.size - index - 1);
        }
        this.size--;
        return ret;
    }

    public List<T> removeRange(int index, int count) {
        this.checkIndexOutOfBounds(index);
        if (index + count > this.size) {
            count = this.size - index;
        }

        List<T> ret = new List<T>();
        Array<T> removed = Array.newArray(count, this.tZero);
        Array.copy(this.array, index, removed, 0, count);
        ret.addArray(removed);

        if (index + count < this.size) {
            Array.copy(this.array, index + count, this.array, index, this.size - index - count);
        }
        this.size = this.size - count;
        return ret;
    }

    public int size() {
        return this.size;
    }
}
