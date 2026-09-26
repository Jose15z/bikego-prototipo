package com.bikego;

/** Cada contexto de prueba usa su propia base H2 en memoria. */
final class TestDb {

    static final String URL = "spring.datasource.url=jdbc:h2:mem:test-${random.uuid};LOCK_TIMEOUT=20000;DB_CLOSE_DELAY=-1";

    private TestDb() { }
}
