import { String } from '@otterop/lang/String';
import { TestBase } from '@otterop/test/TestBase';
import { StringBuffer } from './StringBuffer';

export class TestStringBuffer extends TestBase {

    public empty() : void {
        let sb : StringBuffer = new StringBuffer();
        let s : String = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("")) == 0, String.wrap("Should be an empty string"));
    }

    public addMoreStrings() : void {
        let sb : StringBuffer = new StringBuffer();
        sb.add(String.wrap("a"));
        let s : String = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("a")) == 0, String.wrap("Should be equals to 'a'"));
        sb.add(String.wrap(",b"));
        s = sb.OOPString();
        this.assertTrue(s.compareTo(String.wrap("a,b")) == 0, String.wrap("Should be equals to 'a,b'"));
    }
}


test('empty', () => {
    new TestStringBuffer().empty();
});

test('addMoreStrings', () => {
    new TestStringBuffer().addMoreStrings();
});
