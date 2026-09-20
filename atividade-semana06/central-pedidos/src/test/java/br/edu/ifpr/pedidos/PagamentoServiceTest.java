package br.edu.ifpr.pedidos;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PagamentoServiceTest {

    private static final IllegalStateException INDISPONIVEL = new IllegalStateException("fora do ar");

    // ---------- construtor e validações ----------

    @Test
    void deveRejeitarProcessadorNulo() {
        assertThrows(NullPointerException.class, () -> new PagamentoService(null));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0})
    void deveRejeitarTotalNaoPositivoSemChamarOProcessador(long total) {
        ProcessadorStub stub = ProcessadorStub.com();

        assertThrows(IllegalArgumentException.class, () -> new PagamentoService(stub).pagar(total, 3));
        assertTrue(stub.chamadas().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 4})
    void deveRejeitarLimiteDeTentativasForaDeUmATresSemChamarOProcessador(int limite) {
        ProcessadorStub stub = ProcessadorStub.com();

        assertThrows(IllegalArgumentException.class, () -> new PagamentoService(stub).pagar(1_000, limite));
        assertTrue(stub.chamadas().isEmpty());
    }

    // ---------- aprovação e recusa ----------

    @Test
    void deveAprovarNaPrimeiraTentativa() {
        ProcessadorStub stub = ProcessadorStub.com(true);

        assertTrue(new PagamentoService(stub).pagar(1_000, 3));
        assertEquals(List.of(1_000L), stub.chamadas());
    }

    @Test
    void deveAceitarTotalMinimoDeUmCentavo() {
        ProcessadorStub stub = ProcessadorStub.com(true);

        assertTrue(new PagamentoService(stub).pagar(1, 1));
        assertEquals(List.of(1L), stub.chamadas());
    }

    @Test
    void deveRecusarImediatamenteSemRepetir() {
        ProcessadorStub stub = ProcessadorStub.com(false);

        assertFalse(new PagamentoService(stub).pagar(2_500, 3));
        assertEquals(List.of(2_500L), stub.chamadas());
    }

    // ---------- indisponibilidade temporária ----------

    @Test
    void deveRepetirAposIndisponibilidadeEAprovar() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, true);

        assertTrue(new PagamentoService(stub).pagar(3_000, 3));
        assertEquals(List.of(3_000L, 3_000L), stub.chamadas());
    }

    @Test
    void deveAprovarNaTerceiraTentativa() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, INDISPONIVEL, true);

        assertTrue(new PagamentoService(stub).pagar(3_000, 3));
        assertEquals(3, stub.chamadas().size());
    }

    @Test
    void deveRecusarSeRepeticaoForRecusada() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, false);

        assertFalse(new PagamentoService(stub).pagar(3_000, 3));
        assertEquals(2, stub.chamadas().size());
    }

    @Test
    void deveRetornarFalsoAoEsgotarTresTentativas() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, INDISPONIVEL, INDISPONIVEL);

        assertFalse(new PagamentoService(stub).pagar(3_000, 3));
        assertEquals(List.of(3_000L, 3_000L, 3_000L), stub.chamadas());
    }

    @Test
    void deveEsgotarComLimiteDeUmaTentativa() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL);

        assertFalse(new PagamentoService(stub).pagar(3_000, 1));
        assertEquals(1, stub.chamadas().size());
    }

    @Test
    void deveEsgotarComLimiteDeDuasTentativas() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, INDISPONIVEL);

        assertFalse(new PagamentoService(stub).pagar(3_000, 2));
        assertEquals(2, stub.chamadas().size());
    }

    // ---------- outras exceções ----------

    @Test
    void devePropagarExcecaoDiferenteDeIndisponibilidadeSemRepetir() {
        IllegalArgumentException erro = new IllegalArgumentException("cartão inválido");
        ProcessadorStub stub = ProcessadorStub.com(erro, true);

        IllegalArgumentException lancada = assertThrows(IllegalArgumentException.class,
            () -> new PagamentoService(stub).pagar(3_000, 3));

        assertSame(erro, lancada);
        assertEquals(1, stub.chamadas().size());
    }

    @Test
    void devePropagarExcecaoDiferenteDepoisDeUmaIndisponibilidade() {
        ProcessadorStub stub = ProcessadorStub.com(INDISPONIVEL, new UnsupportedOperationException());

        assertThrows(UnsupportedOperationException.class, () -> new PagamentoService(stub).pagar(3_000, 3));
        assertEquals(2, stub.chamadas().size());
    }
}
