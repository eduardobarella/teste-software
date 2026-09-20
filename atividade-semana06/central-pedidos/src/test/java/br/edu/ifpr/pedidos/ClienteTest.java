package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    void deveGuardarOsValoresInformados() {
        Cliente cliente = new Cliente(true, false, 3);

        assertAll(
            () -> assertTrue(cliente.vip()),
            () -> assertFalse(cliente.bloqueado()),
            () -> assertEquals(3, cliente.comprasAnteriores())
        );
    }

    @Test
    void deveAceitarHistoricoZero() {
        assertEquals(0, new Cliente(false, true, 0).comprasAnteriores());
    }

    @Test
    void deveRejeitarHistoricoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente(false, false, -1));
    }
}
