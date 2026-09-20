package br.edu.ifpr.boletim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticipacaoTest {

    // As quatro combinações dos dois booleanos: (entregou, participou).

    @Test
    void deveSomarTresPontosQuandoEntregouEParticipou() {
        assertEquals(3, new Participacao().calcularPontos(true, true));
    }

    @Test
    void deveSomarDoisPontosQuandoSoEntregou() {
        assertEquals(2, new Participacao().calcularPontos(true, false));
    }

    @Test
    void deveSomarUmPontoQuandoSoParticipou() {
        assertEquals(1, new Participacao().calcularPontos(false, true));
    }

    @Test
    void deveSomarZeroPontosQuandoNaoEntregouNemParticipou() {
        assertEquals(0, new Participacao().calcularPontos(false, false));
    }
}
