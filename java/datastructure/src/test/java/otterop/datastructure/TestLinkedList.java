package otterop.datastructure;

import otterop.lang.Array;
import otterop.lang.String;
import otterop.test.Test;
import otterop.test.TestBase;

public class TestLinkedList extends TestBase {

    @Test
    public void add() {
        String a = String.wrap("a");
        String b = String.wrap("b");
        String c = String.wrap("c");
        String d = String.wrap("d");
        String e = String.wrap("e");

        Array<String> strings = Array.newArray(5, a);
        strings.set(0, a);
        strings.set(1, b);
        strings.set(2, c);
        strings.set(3, d);
        strings.set(4, e);
        Array<String> expected = Array.newArray(5, a);
        expected.set(0, e);
        expected.set(1, d);
        expected.set(2, a);
        expected.set(3, b);
        expected.set(4, c);

        LinkedList<String> l = new LinkedList<String>();
        l.addLast(a);
        l.addLast(b);
        l.addLast(c);
        l.addFirst(d);
        l.addFirst(e);
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
        int i = 0;
        for (String s : l) {
            this.assertTrue(s.compareTo(expected.get(i)) == 0,
                    String.wrap("Element mismatch"));
            i++;
        }
    }
}
