// src/main/java/it/univ/ddmanager/common/UnauthorizedException.java
package it.univ.ddmanager.common;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}