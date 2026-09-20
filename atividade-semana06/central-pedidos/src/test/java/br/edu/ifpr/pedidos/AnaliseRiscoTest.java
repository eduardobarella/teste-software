package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnaliseRiscoTest {

    private final AnaliseRisco risco = new AnaliseRisco();

    private static final Cliente NOVO = new Cliente(false, false, 0);
    private static final Cliente NOVO_VIP = new Cliente(true, false, 0);
    private static final Cliente RECORRENTE = new Cliente(false, false, 3);
    private static final Cliente RECORRENTE_VIP = new Cliente(true, false, 3);
    private static final Cliente BLOQUEADO = new Cliente(false, true, 3);

    @Test
    void deveRejeitarTotalNegativo() {
        assertThrows(IllegalArgumentException.class, () -> risco.avaliar(NOVO, -1, false));
    }

    @Test
    void deveValidarTotalAntesDeAvaliarBloqueio() {
        assertThrows(IllegalArgumentException.class, () -> risco.avaliar(BLOQUEADO, -1, false));
    }

    // ---------- bloqueado ----------

    @Test
    void deveRecusarClienteBloqueado() {
        assertEquals("RECUSADO", risco.avaliar(BLOQUEADO, 1_000, false));
    }

    @Test
    void deveRecusarBloqueadoMesmoComTotalAltoEExpresso() {
        assertEquals("RECUSADO", risco.avaliar(new Cliente(true, true, 0), 9_000_000, true));
    }

    // ---------- sem compras anteriores ----------

    @Test
    void deveAprovarPrimeiraCompraNoLimiteDeMilReais() {
        assertEquals("APROVADO", risco.avaliar(NOVO, 100_000, false));
    }

    @Test
    void deveRevisarPrimeiraCompraAcimaDeMilReais() {
        // Operando esquerdo verdadeiro: expresso nem precisa ser avaliado.
        assertEquals("REVISAO", risco.avaliar(NOVO, 100_001, false));
    }

    @Test
    void deveRevisarPrimeiraCompraExpressaMesmoDeValorBaixo() {
        // Operando esquerdo falso, direito verdadeiro.
        assertEquals("REVISAO", risco.avaliar(NOVO, 1_000, true));
    }

    @Test
    void deveRevisarPrimeiraCompraCaraEExpressa() {
        assertEquals("REVISAO", risco.avaliar(NOVO, 200_000, true));
    }

    @Test
    void deveAprovarPrimeiraCompraBarataENormal() {
        assertEquals("APROVADO", risco.avaliar(NOVO, 1_000, false));
    }

    @Test
    void naoDeveIsentarVipDaRevisaoNaPrimeiraCompra() {
        assertEquals("REVISAO", risco.avaliar(NOVO_VIP, 100_001, false));
    }

    // ---------- com compras anteriores ----------

    @Test
    void deveAprovarRecorrenteNoLimiteDeCincoMilReais() {
        assertEquals("APROVADO", risco.avaliar(RECORRENTE, 500_000, false));
    }

    @Test
    void deveRevisarRecorrenteNaoVipAcimaDeCincoMilReais() {
        assertEquals("REVISAO", risco.avaliar(RECORRENTE, 500_001, false));
    }

    @Test
    void deveAprovarRecorrenteVipAcimaDeCincoMilReais() {
        // Operando esquerdo verdadeiro, direito (!vip) falso.
        assertEquals("APROVADO", risco.avaliar(RECORRENTE_VIP, 500_001, false));
    }

    @Test
    void deveAprovarRecorrenteVipNoLimite() {
        // Operando esquerdo falso: !vip não é avaliado.
        assertEquals("APROVADO", risco.avaliar(RECORRENTE_VIP, 500_000, false));
    }

    @Test
    void deveIgnorarExpressoParaClienteComHistorico() {
        assertEquals("APROVADO", risco.avaliar(RECORRENTE, 1_000, true));
    }

    @Test
    void deveAprovarRecorrenteComTotalZero() {
        assertEquals("APROVADO", risco.avaliar(RECORRENTE, 0, false));
    }
}
