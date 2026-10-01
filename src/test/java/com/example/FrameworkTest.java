package com.example;

import static org.junit.jupiter.api.Assertions.*;

import com.example.aop.AOPProxy;
import com.example.aop.LogExecutionTime;
import com.example.app.NotificationService;
import com.example.app.NotificationServiceImpl;
import com.example.app.UserService;
import com.example.app.UserServiceImpl;
import com.example.di.Container;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class FrameworkTest {

    @Test
    void injectsDependencyByInterface() throws Exception {
        Container container = new Container(UserServiceImpl.class, NotificationServiceImpl.class);
        UserService userService = container.getService(UserService.class);

        Field field = UserServiceImpl.class.getDeclaredField("notificationService");
        field.setAccessible(true);
        assertSame(container.getService(NotificationServiceImpl.class), field.get(userService));
    }

    @Test
    void registrationOrderDoesNotMatter() throws Exception {
        Container container = new Container(NotificationServiceImpl.class, UserServiceImpl.class);
        assertNotNull(container.getService(UserService.class));
    }

    @Test
    void proxyLogsExecutionTimeForAnnotatedImplementationMethod() throws Exception {
        Container container = new Container(UserServiceImpl.class, NotificationServiceImpl.class);
        UserService proxy = AOPProxy.createProxy(container.getService(UserService.class), UserService.class);

        String out = captureStdout(() -> proxy.registerUser("Alice"));

        assertTrue(out.contains("User registered: Alice"), out);
        assertTrue(out.contains("Notification sent to Alice"), out);
        assertTrue(out.contains("Execution time of registerUser"), out);
    }

    @Test
    void failsFastWhenDependencyIsNotRegistered() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new Container(UserServiceImpl.class));

        assertTrue(e.getMessage().contains("'notificationService'"), e.getMessage());
        assertTrue(e.getMessage().contains(UserServiceImpl.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(NotificationService.class.getName()), e.getMessage());
    }

    public interface Greeter {
        String greet(String name);
        @LogExecutionTime void fail();
    }

    @Test
    void unannotatedMethodsPassThroughAndExceptionsAreUnwrapped() {
        Greeter target = new Greeter() {
            public String greet(String name) { return "hi " + name; }
            public void fail() { throw new IllegalStateException("boom"); }
        };
        Greeter proxy = AOPProxy.createProxy(target, Greeter.class);

        String out = captureStdout(() -> assertEquals("hi Bob", proxy.greet("Bob")));
        assertFalse(out.contains("Execution time"), out);

        assertThrows(IllegalStateException.class, proxy::fail);
    }

    @Test
    void rejectsConcreteClass() {
        assertThrows(IllegalArgumentException.class,
                () -> AOPProxy.createProxy(new UserServiceImpl(), UserServiceImpl.class));
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}
