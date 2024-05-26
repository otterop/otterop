package otterop.lang;

class ArrayIterator<T> implements OOPIterator<T> {

    private Array<T> array;
    private int i;

    ArrayIterator(Array<T> array) {
        this.array = array;
        this.i = 0;
    }

    public boolean hasNext() {
        return this.i < this.array.size();
    }

    public T next() {
        T ret = this.array.get(this.i);
        this.i++;
        return ret;
    }
}
