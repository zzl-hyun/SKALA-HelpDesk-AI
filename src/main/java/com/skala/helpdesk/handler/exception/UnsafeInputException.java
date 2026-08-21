package com.skala.helpdesk.handler.exception;

/** SafetyAdvisor가 인젝션·민감어 입력을 걸러낼 때 던진다. */
public class UnsafeInputException extends RuntimeException {

    public UnsafeInputException(String message) {
        super(message);
    }
}
