import { Array } from '@otterop/lang/Array';
import { String } from '@otterop/lang/String';
import { TestBase } from '@otterop/test/TestBase';
import { LinkedList } from './LinkedList';

export class TestLinkedList extends TestBase {

    public add() : void {
        let a : String = String.wrap("a");
        let b : String = String.wrap("b");
        let c : String = String.wrap("c");
        let d : String = String.wrap("d");
        let e : String = String.wrap("e");
        let strings : Array<String> = Array.newArray(5, a);
        strings.set(0, a);
        strings.set(1, b);
        strings.set(2, c);
        strings.set(3, d);
        strings.set(4, e);
        let expected : Array<String> = Array.newArray(5, a);
        expected.set(0, e);
        expected.set(1, d);
        expected.set(2, a);
        expected.set(3, b);
        expected.set(4, c);
        let l : LinkedList<String> = new LinkedList<String>();
        l.addLast(a);
        l.addLast(b);
        l.addLast(c);
        l.addFirst(d);
        l.addFirst(e);
        this.assertTrue(l.size() == 5, String.wrap("Size should be 5"));
        let i : number = 0;
        for (let s of l) {
            this.assertTrue(s.compareTo(expected.get(i)) == 0, String.wrap("Element mismatch"));
            i++;
        }
    }
}


test('add', () => {
    new TestLinkedList().add();
});
