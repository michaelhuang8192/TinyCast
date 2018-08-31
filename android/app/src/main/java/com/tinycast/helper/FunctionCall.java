package com.tinycast.helper;

public abstract class FunctionCall<R, P> {
    private boolean hasCalled = false;

    public abstract R call(P arg);

    public R callOnce(P arg) {
        if(hasCalled) return null;
        hasCalled = true;
        return call(arg);
    }
}
