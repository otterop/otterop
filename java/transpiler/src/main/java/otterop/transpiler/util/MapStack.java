package otterop.transpiler.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MapStack<K,V> {
    private Stack<Map<K,V>> stack = new Stack<Map<K, V>>();

    public MapStack() {
        this.stack.push(new HashMap<>());
    }

    public V get(K key) {
        V ret = null;
        for (int i = stack.size() - 1; i>=0; i--) {
            ret = this.stack.get(i).get(key);
            if (ret != null)
                break;
        }
        return ret;
    }

    public V put(K key, V value) {
        return this.stack.peek().put(key, value);
    }

    public boolean containsKey(K key) {
        return this.get(key) != null;
    }

    public void newContext() {
        this.stack.push(new HashMap<>());
    }

    public void endContext() {
        if (this.stack.size() == 1)
            throw new IllegalStateException();
        this.stack.pop();
    }
}
