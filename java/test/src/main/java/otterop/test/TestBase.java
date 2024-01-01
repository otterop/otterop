package otterop.test;

public class TestBase {
    private TestRun testRun;

    public void setRun(TestRun testRun) {
        this.testRun = testRun;
    }

    public TestRun run() {
       return this.testRun;
    }
}
