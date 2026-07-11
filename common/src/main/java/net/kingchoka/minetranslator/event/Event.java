package net.kingchoka.minetranslator.event;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

public final class Event<T extends Event.Callback> {
    private T[] callbacks;
    private final Function<T[], T> merger;
    private volatile T invoker;
    private final T proxy;

    Event(Class<? extends T> type, Function<T[], T> merger) {
        this.merger = merger;
        this.callbacks = (T[]) Array.newInstance(type, 0);
        this.invoker = merger.apply(this.callbacks);
        this.proxy = (T) java.lang.reflect.Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (p, method, args) -> method.invoke(invoker, args)
        );
    }

    public synchronized void register(T callback) {
        int oldLength = callbacks.length;
        callbacks = Arrays.copyOf(callbacks, oldLength + 1);
        callbacks[oldLength] = callback;
        this.invoker = merger.apply(callbacks);
    }

    public T merge() {
        return proxy;
    }

    public T getInvoker() {
        return proxy;
    }

    /**
     * A base interface for event callbacks.
     */
    interface Callback {
    }
}
