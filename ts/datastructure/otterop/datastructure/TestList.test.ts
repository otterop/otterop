import { Array } from '@otterop/lang/Array';
import { Generic } from '@otterop/lang/Generic';
import { String } from '@otterop/lang/String';
import { TestBase } from '@otterop/test/TestBase';
import { List } from './List';

export class TestList extends TestBase {

    public add() : void {
        let l : List<String> = new List<String>();
        l.add(String.wrap("a"));
        l.add(String.wrap("b"));
        l.add(String.wrap("c"));
        l.add(String.wrap("d"));
        l.add(String.wrap("e"));
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
    }

    public addRange() : void {
        let genericString : Generic<String> = new Generic<String>();
        let genericT : String = genericString.zero();
        let l : List<String> = new List<String>();
        let toAdd : Array<String> = Array.newArray(5, genericT);
        toAdd.set(0, String.wrap("a"));
        toAdd.set(1, String.wrap("b"));
        toAdd.set(2, String.wrap("c"));
        toAdd.set(3, String.wrap("d"));
        toAdd.set(4, String.wrap("e"));
        l.addArray(toAdd);
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
    }

    public removeIndex() : void {
        let val : String;
        let l : List<String> = new List<String>();
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

    public removeRange() : void {
        let val : String;
        let l : List<String> = new List<String>();
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


test('add', () => {
    new TestList().add();
});

test('addRange', () => {
    new TestList().addRange();
});

test('removeIndex', () => {
    new TestList().removeIndex();
});

test('removeRange', () => {
    new TestList().removeRange();
});
