package br.edu.ifpr.pedidos;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraFreteTest {

    private final CalculadoraFrete calculadora = new CalculadoraFrete();

    private static final Cliente COMUM = new Cliente(false, false, 1);
    private static final Cliente VIP = new Cliente(true, false, 1);

    /** Pedido de uma linha ativa com o peso total desejado. */
    private static Pedido pedido(String uf, int pesoGramas, boolean expresso, boolean fragil) {
        ItemPedido item = new ItemPedido("SKU", 100, 1, 1, pesoGramas, fragil);
        return new Pedido(List.of(item), uf, expresso, null);
    }

    private long frete(Pedido pedido, Cliente cliente, long liquido) {
        return calculadora.calcular(pedido, cliente, liquido);
    }

    @Test
    void deveRejeitarLiquidoNegativo() {
        assertThrows(IllegalArgumentException.class,
            () -> frete(pedido("PR", 1_000, false, false), COMUM, -1));
    }

    // ---------- tarifa base por UF ----------

    @ParameterizedTest(name = "UF {0} => frete base {1}")
    @CsvSource({"PR, 1_200", "SP, 2_000", "RJ, 2_000", "MG, 3_000", "ZZ, 3_000"})
    void deveUsarTarifaBasePorUf(String uf, long esperado) {
        assertEquals(esperado, frete(pedido(uf, 1_000, false, false), COMUM, 0));
    }

    // ---------- excedente de peso (while) ----------

    @ParameterizedTest(name = "peso {0} g => frete PR {1}")
    @CsvSource({
        "1_000, 1_200",     // abaixo de 2 kg: 0 iterações
        "2_000, 1_200",     // exatamente 2 kg: 0 iterações
        "2_001, 1_500",     // 1 g além: fração => 1 iteração
        "3_000, 1_500",     // 1 kg adicional exato: 1 iteração
        "3_001, 1_800",     // fração do segundo kg: 2 iterações
        "4_000, 1_800",     // 2 kg adicionais exatos: 2 iterações
        "5_000, 2_100",     // várias iterações: 3
        "100_000, 30_600"   // 98 kg adicionais: 98 x 300 + 1.200
    })
    void deveCobrarTresReaisPorKgAdicionalOuFracao(int peso, long esperado) {
        assertEquals(esperado, frete(pedido("PR", peso, false, false), COMUM, 0));
    }

    @Test
    void deveSomarPesoDeVariasLinhasEQuantidades() {
        ItemPedido a = new ItemPedido("A", 100, 3, 3, 1_000, false); // 3.000 g
        ItemPedido b = new ItemPedido("B", 100, 1, 1, 500, false);   //   500 g
        Pedido pedido = new Pedido(List.of(a, b), "PR", false, null); // 3.500 g => 2 iterações

        assertEquals(1_800L, frete(pedido, COMUM, 0));
    }

    // ---------- gratuidade ----------

    @Test
    void naoDeveZerarFreteAbaixoDeTrezentosReais() {
        assertEquals(1_200L, frete(pedido("PR", 1_000, false, false), COMUM, 29_999));
    }

    @Test
    void deveZerarBaseEPesoNoLimiteDeTrezentosReais() {
        // Peso alto (5 kg) também é zerado.
        assertEquals(0L, frete(pedido("PR", 5_000, false, false), COMUM, 30_000));
    }

    @Test
    void deveZerarFreteAcimaDoLimite() {
        assertEquals(0L, frete(pedido("MG", 1_000, false, false), COMUM, 1_000_000));
    }

    @Test
    void naoDeveZerarFreteQuandoExpressoMesmoAcimaDoLimite() {
        // Base 1.200 + expresso 1.500.
        assertEquals(2_700L, frete(pedido("PR", 1_000, true, false), COMUM, 30_000));
    }

    // ---------- VIP ----------

    @Test
    void vipPagaMetadeDoFrete() {
        assertEquals(600L, frete(pedido("PR", 1_000, false, false), VIP, 0));
    }

    @Test
    void vipPagaMetadeInclusiveDoExcedenteDePeso() {
        // (3.000 + 2 x 300) / 2 = 1.800.
        assertEquals(1_800L, frete(pedido("MG", 3_500, false, false), VIP, 0));
    }

    @Test
    void vipComFreteGratisContinuaZerado() {
        assertEquals(0L, frete(pedido("PR", 1_000, false, false), VIP, 30_000));
    }

    // ---------- adicionais: expresso e frágil ----------

    @Test
    void expressoAcrescentaQuinzeReais() {
        assertEquals(2_700L, frete(pedido("PR", 1_000, true, false), COMUM, 0));
    }

    @Test
    void expressoNaoEhReduzidoPelaMetadeDoVip() {
        // 1.200 / 2 + 1.500 = 2.100.
        assertEquals(2_100L, frete(pedido("PR", 1_000, true, false), VIP, 0));
    }

    @Test
    void fragilAcrescentaCincoReaisUmaUnicaVez() {
        ItemPedido a = new ItemPedido("A", 100, 1, 1, 500, true);
        ItemPedido b = new ItemPedido("B", 100, 2, 2, 500, true);
        Pedido pedido = new Pedido(List.of(a, b), "PR", false, null);

        assertEquals(1_700L, frete(pedido, COMUM, 0));
    }

    @Test
    void fragilIncideMesmoQuandoBaseFoiZerada() {
        assertEquals(500L, frete(pedido("PR", 1_000, false, true), COMUM, 30_000));
    }

    @Test
    void fragilInativoNaoAcrescentaNada() {
        ItemPedido inativo = new ItemPedido("I", 100, 0, 0, 500, true);
        ItemPedido ativo = new ItemPedido("A", 100, 1, 1, 500, false);
        Pedido pedido = new Pedido(List.of(inativo, ativo), "PR", false, null);

        assertEquals(1_200L, frete(pedido, COMUM, 0));
    }

    @Test
    void deveCombinarVipExpressoEFragilComExcedenteDePeso() {
        // MG 3.000 + 2 x 300 = 3.600; VIP => 1.800; + 1.500 + 500 = 3.800.
        assertEquals(3_800L, frete(pedido("MG", 3_001, true, true), VIP, 0));
    }

    @Test
    void deveCombinarGratuidadeComFragilSemExpresso() {
        // Base zerada; VIP metade de zero; frágil +500.
        assertEquals(500L, frete(pedido("SP", 1_000, false, true), VIP, 50_000));
    }
}
