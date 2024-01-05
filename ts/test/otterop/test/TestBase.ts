import { String } from '@otterop/lang/String';

export class TestBase {

    public assertTrue(value : boolean, message : String) : void {
        if (!value)
            expect(value).toBe(message.unwrap());
    }
}

