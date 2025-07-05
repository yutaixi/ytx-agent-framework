package com.ytx.ai.base.exception;

public class BizException extends Exception{


    public BizException(){

    }

    public BizException(String message){
        super(message);
    }

    public BizException(String message,Throwable cause){
        super(message,cause);
    }

    public BizException(Throwable cause){
        super(cause);
    }

}
