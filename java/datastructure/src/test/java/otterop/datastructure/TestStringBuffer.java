package otterop.datastructure;

import otterop.lang.String;
import otterop.test.Test;
import otterop.test.TestBase;

public class TestStringBuffer extends TestBase {

    @Test
    public void empty() {
        StringBuffer sb = new StringBuffer();
        String s = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("")) == 0,
                String.wrap("Should be an empty string"));
    }

    @Test
    public void addMoreStrings() {
        StringBuffer sb = new StringBuffer();
        sb.add(String.wrap("a"));
        String s = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("a")) == 0,
                String.wrap("Should be equals to 'a'"));
        sb.add(String.wrap(",b"));
        s = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("a,b")) == 0,
                String.wrap("Should be equals to 'a,b'"));
    }
}
