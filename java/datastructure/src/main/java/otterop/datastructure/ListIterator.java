package otterop.datastructure;

import otterop.lang.OOPIterator;

class ListIterator<T> implements OOPIterator<T> {

    private List<T> list;
    private int index;

    ListIterator(List<T> list) {
        this.list = list;
        this.index = 0;
    }

    public boolean hasNext() {
        return this.index < this.list.size();
    }

    public T next() {
        T ret = this.list.get(this.index);
        this.index++;
        return ret;
    }
}
