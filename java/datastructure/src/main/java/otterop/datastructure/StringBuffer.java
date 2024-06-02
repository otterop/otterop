package otterop.datastructure;

import otterop.lang.OOPIterable;
import otterop.lang.String;

public class StringBuffer {

    private LinkedList<String> strings;

    public StringBuffer() {
        this.strings = new LinkedList<String>();
    }

    public void add(String s) {
        this.strings.addLast(s);
    }

    public String OOPString() {
        OOPIterable<String> strings = this.strings;
        return String.concat(strings);
    }
}
