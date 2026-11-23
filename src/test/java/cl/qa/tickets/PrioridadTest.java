package cl.qa.tickets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrioridadTest {
    @ParameterizedTest
    @CsvSource({"critico, alta", "moderado, media", "bajo, baja"})
    void clasificaImpacto(String impacto, String esperado) {
        assertEquals(esperado, Prioridad.calcular(impacto));
    }

    @Test
    void rechazaImpactoDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> Prioridad.calcular("otro"));
    }
}
