# funcwraps

Using annotation for wrapped a method, just like using decorators in Python.

## What is it

`funcwraps` using javassist processing class file after building, it add a wrapper method
 on your original method with annotation. This way very like using Python decorators, but
 that are very different principle.

## How to use it

Add annotation `@Wraps` on your method that will be wrapped.
 Setting values `clazz` and `method` for assign the wrapper method.

```java
@Wraps(clazz = WrapMethods.class, method = "wrap")
public Integer testWrapped(Integer x, Integer y) {
    System.out.println("inside wrapped method");
    return x + y;
}
```

Writing the wrapper method with declaration
 `public static Object wrap(Method method, Object[] args, Object target) throws Throwable`.
 Calling wrapped method by `Object ret = method.invoke(target, args)`.

```java
public class WrapMethods {
    public static Object wrap(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("### start");
        Object ret = method.invoke(target, args);
        System.out.println("### end");
        return ret;
    }
}
```

If you want passing arguments to wrapper method, you can do this:

```java
@Wraps(clazz = WrapMethods.class, method = "wrapWithParams(param1, param2)")
public String testWithParams(String x, String y) {
    System.out.println("inside wrapped method");
    return x + y;
}
```

The arguments will passing as `String[]` type.

```java
public static Object wrapWithParams(Method method, Object[] args, Object target, String[] wrapParams) throws Throwable {
    System.out.println("### start");
    Object ret = method.invoke(target, args);
    System.out.println("### end");
    // wrapParams = ["param1", "param2"]
    System.out.println(Arrays.toString(wrapParams));
    return test;
}
```

Using `exec-maven-plugin` for processing classes, it transform your classes after building.
 Or just running CLI with main class `io.github.eshizhan.funcwraps.Main` with classes path in argument.
 The java instrument also support by `java -javaagent:funcwraps.jar=package.name` argument,
  `premain` entry point also in main class.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <id>exec-transform-class</id>
            <phase>process-classes</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>io.github.eshizhan.funcwraps.Main</mainClass>
                <arguments>
                    <argument>${project.build.outputDirectory}</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```
