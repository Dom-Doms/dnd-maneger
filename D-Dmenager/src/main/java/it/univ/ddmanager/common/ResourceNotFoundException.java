// src/main/java/it/univ/ddmanager/common/ResourceNotFoundException.java
package it.univ.ddmanager.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}