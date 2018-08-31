package com.tinycast.helper;

public class FunctionCallResult<T> {
    private T mResult;
    private Exception mError;

    public void setResult(T result) {
        mResult = result;
    }

    public T getResult() {
        return mResult;
    }

    public void setError(Exception error) {
        mError = error;
    }

    public Exception getError() {
        return mError;
    }

    public boolean isError() {
        return mError != null;
    }

    public static <T> FunctionCallResult<T> asResult(T result) {
        FunctionCallResult<T> res = new FunctionCallResult<>();
        res.setResult(result);
        return res;
    }

    public static <T> FunctionCallResult<T> asError(Exception error) {
        FunctionCallResult<T> res = new FunctionCallResult<>();
        res.setError(error);
        return res;
    }
}
