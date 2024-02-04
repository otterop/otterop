package otterop.datastructure;

import otterop.lang.Array;
import otterop.lang.Generic;
import otterop.lang.String;
import otterop.test.Test;
import otterop.test.TestBase;

public class TestList extends TestBase {

    @Test
    public void add() {
        List<String> l = new List<String>();
        l.add(String.wrap("a"));
        l.add(String.wrap("b"));
        l.add(String.wrap("c"));
        l.add(String.wrap("d"));
        l.add(String.wrap("e"));
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
    }

    @Test
    public void addRange() {
        Generic<String> genericString = new Generic<String>();
        String genericT = genericString.zero();
        List<String> l = new List<String>();
        Array<String> toAdd = Array.newArray(5, genericT);
        toAdd.set(0, String.wrap("a"));
        toAdd.set(1, String.wrap("b"));
        toAdd.set(2, String.wrap("c"));
        toAdd.set(3, String.wrap("d"));
        toAdd.set(4, String.wrap("e"));
        l.addArray(toAdd);
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
    }

    @Test
    public void removeIndex() {
        String val;
        List<String> l = new List<String>();
        l.add(String.wrap("a"));
        l.add(String.wrap("b"));
        l.add(String.wrap("c"));
        l.add(String.wrap("d"));
        l.add(String.wrap("e"));
        l.removeIndex(1);
        l.removeIndex(1);
        this.assertTrue(l.size() == 3, String.wrap("Size should be 3"));
        val = l.get(0);
        this.assertTrue(val.compareTo(String.wrap("a")) == 0, String.wrap("First element should be a"));
        val = l.get(1);
        this.assertTrue(val.compareTo(String.wrap("d")) == 0, String.wrap("Second element should be d"));
        val = l.get(2);
        this.assertTrue(val.compareTo(String.wrap("e")) == 0, String.wrap("Third element should be e"));
    }

    @Test
    public void removeRange() {
        String val;
        List<String> l = new List<String>();
        l.add(String.wrap("a"));
        l.add(String.wrap("b"));
        l.add(String.wrap("c"));
        l.add(String.wrap("d"));
        l.add(String.wrap("e"));
        l.removeRange(3, 2);
        this.assertTrue(l.size() == 3, String.wrap("Size should be 3"));
        val = l.get(0);
        this.assertTrue(val.compareTo(String.wrap("a")) == 0, String.wrap("First element should be a"));
        val = l.get(1);
        this.assertTrue(val.compareTo(String.wrap("b")) == 0, String.wrap("Second element should be b"));
        val = l.get(2);
        this.assertTrue(val.compareTo(String.wrap("c")) == 0, String.wrap("Third element should be c"));
    }
}
