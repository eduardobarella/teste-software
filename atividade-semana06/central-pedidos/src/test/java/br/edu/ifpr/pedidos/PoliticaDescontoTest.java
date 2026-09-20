package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class PoliticaDescontoTest {

    private final PoliticaDesconto politica = new PoliticaDesconto();

    private static final Cliente COMUM_COM_HISTORICO = new Cliente(false, false, 2);
    private static final Cliente COMUM_NOVO = new Cliente(false, false, 0);
    private static final Cliente VIP_COM_HISTORICO = new Cliente(true, false, 2);
    private static final Cliente VIP_NOVO = new Cliente(true, false, 0);

    @Test
    void deveRejeitarSubtotalNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> politica.calcular(COMUM_COM_HISTORICO, -1, null));
    }

    // ---------- desconto base, sem cupom ----------

    @ParameterizedTest(name = "VIP, subtotal {0} => desconto {1}")
    @CsvSource({"0, 0", "10_000, 1_000", "10_005, 1_000", "10_009, 1_000", "99_999, 9_999"})
    void vipRecebeDezPorCentoTruncado(long subtotal, long esperado) {
        assertEquals(esperado, politica.calcular(VIP_COM_HISTORICO, subtotal, null));
    }

    @ParameterizedTest(name = "comum, subtotal {0} => desconto {1}")
    @CsvSource({"1, 0", "49_999, 0", "50_000, 2_500", "50_001, 2_500", "50_020, 2_501", "100_000, 5_000"})
    void clienteComumRecebeCincoPorCentoApenasAPartirDeQuinhentosReais(long subtotal, long esperado) {
        assertEquals(esperado, politica.calcular(COMUM_COM_HISTORICO, subtotal, null));
    }

    @Test
    void cupomBrancoMantemODescontoBase() {
        assertEquals(2_500L, politica.calcular(COMUM_COM_HISTORICO, 50_000, "   "));
        assertEquals(2_500L, politica.calcular(COMUM_COM_HISTORICO, 50_000, ""));
    }

    // ---------- BEMVINDO ----------

    @Test
    void bemvindoSomaVinteReaisNaPrimeiraCompraNoLimiteDeCemReais() {
        assertEquals(2_000L, politica.calcular(COMUM_NOVO, 10_000, "BEMVINDO"));
    }

    @Test
    void bemvindoNaoSomaAbaixoDeCemReais() {
        assertEquals(0L, politica.calcular(COMUM_NOVO, 9_999, "BEMVINDO"));
    }

    @Test
    void bemvindoNaoSomaQuandoHaComprasAnteriores() {
        assertEquals(0L, politica.calcular(COMUM_COM_HISTORICO, 10_000, "BEMVINDO"));
    }

    @Test
    void bemvindoNormalizaEspacosEMinusculas() {
        assertEquals(2_000L, politica.calcular(COMUM_NOVO, 10_000, "  bemVindo  "));
    }

    @Test
    void bemvindoSomaAoDescontoBaseDeClienteComumAcimaDeQuinhentosReais() {
        // 5% de 50.000 = 2.500, + 2.000 = 4.500 (teto de 20% = 10.000).
        assertEquals(4_500L, politica.calcular(COMUM_NOVO, 50_000, "BEMVINDO"));
    }

    // ---------- EXTRA10 ----------

    @Test
    void extra10SomaDezPorCentoNoLimiteDeDuzentosReais() {
        assertEquals(2_000L, politica.calcular(COMUM_COM_HISTORICO, 20_000, "EXTRA10"));
    }

    @Test
    void extra10NaoSomaAbaixoDeDuzentosReais() {
        assertEquals(0L, politica.calcular(COMUM_COM_HISTORICO, 19_999, "EXTRA10"));
    }

    @Test
    void extra10SomaAoDescontoBase() {
        // 5% de 50.000 = 2.500, + 10% = 5.000, total 7.500 (< teto de 10.000).
        assertEquals(7_500L, politica.calcular(COMUM_COM_HISTORICO, 50_000, "extra10"));
    }

    @Test
    void extra10TruncaCadaParcelaSeparadamente() {
        // subtotal 50.019: base 2.500 (2.500,95), extra 5.001 (5.001,9).
        assertEquals(7_501L, politica.calcular(COMUM_COM_HISTORICO, 50_019, "EXTRA10"));
    }

    // ---------- teto de 20% ----------

    @Test
    void deveLimitarDescontoCombinadoAVintePorCento() {
        // VIP novo: 1.000 + 2.000 = 3.000, mas o teto de 20% de 10.000 é 2.000.
        assertEquals(2_000L, politica.calcular(VIP_NOVO, 10_000, "BEMVINDO"));
    }

    @Test
    void naoDeveLimitarQuandoDescontoIgualaOTeto() {
        // VIP + EXTRA10: 10% + 10% = exatamente 20%.
        assertEquals(4_000L, politica.calcular(VIP_COM_HISTORICO, 20_000, "EXTRA10"));
    }

    @Test
    void naoDeveLimitarQuandoDescontoFicaAbaixoDoTeto() {
        // VIP com histórico: BEMVINDO inelegível, fica só o 10% base.
        assertEquals(1_000L, politica.calcular(VIP_COM_HISTORICO, 10_000, "BEMVINDO"));
    }

    // ---------- cupom desconhecido ----------

    @Test
    void deveRejeitarCupomDesconhecido() {
        assertThrows(IllegalArgumentException.class,
            () -> politica.calcular(COMUM_COM_HISTORICO, 50_000, "PROMO"));
    }

    @Test
    void deveRejeitarCupomDesconhecidoMesmoSemElegibilidadeParaDesconto() {
        assertThrows(IllegalArgumentException.class,
            () -> politica.calcular(COMUM_COM_HISTORICO, 1, "XYZ"));
    }
}
