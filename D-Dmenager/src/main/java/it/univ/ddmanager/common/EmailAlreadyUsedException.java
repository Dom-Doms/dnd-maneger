// src/main/java/it/univ/ddmanager/common/EmailAlreadyUsedException.java
package it.univ.ddmanager.common;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String message) {
        super(message);
    }
}
