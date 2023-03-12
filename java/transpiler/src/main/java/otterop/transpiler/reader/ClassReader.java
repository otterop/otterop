package otterop.transpiler.reader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class ClassReader {

    public Optional<Class<?>> getClass(String binaryName) {
        try {
            return Optional.of(getClass().getClassLoader().loadClass(binaryName));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public Collection<String> findMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        var ret = new ArrayList<String>(methods.length);
        for (Method m : methods) {
            ret.add(m.getName());
        }
        return ret;
    }
}
