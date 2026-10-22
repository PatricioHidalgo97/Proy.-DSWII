package com.cibertec.gestion.cash.exceptions;

/**
 * El catálogo no respondió (timeout o conexión rechazada). Se traduce a 503 y
 * nunca deja filas insertadas (ESP-03 §8).
 */
public class DependenciaNoDisponibleException extends RuntimeException {
    public DependenciaNoDisponibleException(String message) {
        super(message);
    }
}
