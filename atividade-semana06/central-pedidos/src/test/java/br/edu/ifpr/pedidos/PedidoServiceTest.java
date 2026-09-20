package br.edu.ifpr.pedidos;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {

    private static final Cliente COMUM = new Cliente(false, false, 1);
    private static final Cliente COMUM_NOVO = new Cliente(false, false, 0);
    private static final Cliente VIP = new Cliente(true, false, 1);
    private static final Cliente BLOQUEADO = new Cliente(false, true, 1);

    private static ItemPedido item(long preco, int quantidade, int estoque, int peso, boolean fragil) {
        return new ItemPedido("SKU", preco, quantidade, estoque, peso, fragil);
    }

    /** Linha simples: 1 unidade, estoque suficiente, 1 kg, não frágil. */
    private static ItemPedido item(long preco) {
        return item(preco, 1, 5, 1_000, false);
    }

    private static Pedido pedido(String uf, boolean expresso, String cupom, ItemPedido... itens) {
        return new Pedido(List.of(itens), uf, expresso, cupom);
    }

    private static void assertResultado(ResultadoPedido r, String status,
                                        long subtotal, long desconto, long frete, long total) {
        assertAll(
            () -> assertEquals(status, r.status()),
            () -> assertEquals(subtotal, r.subtotalCentavos()),
            () -> assertEquals(desconto, r.descontoCentavos()),
            () -> assertEquals(frete, r.freteCentavos()),
            () -> assertEquals(total, r.totalCentavos())
        );
    }

    // =====================================================================
    // Exemplo do roteiro
    // =====================================================================

    @Test
    void deveFecharPedidoDeClienteComumComFreteDoParanaEPagamentoAprovado() {
        // 1. Preparar: cliente comum, uma compra anterior e item disponível de R$ 100,00.
        Cliente cliente = new Cliente(false, false, 1);
        ItemPedido item = new ItemPedido("LIVRO-JAVA", 10_000, 1, 5, 1_000, false);
        Pedido pedido = new Pedido(List.of(item), "PR", false, null);

        // Simula o pagamento e registra as cobranças, sem banco ou serviço externo.
        List<Long> cobrancas = new ArrayList<>();
        PedidoService service = new PedidoService(total -> {
            cobrancas.add(total);
            return true;
        });

        // 2. Executar: percorrer um caminho completo do fechamento.
        ResultadoPedido resultado = service.fechar(pedido, cliente);

        // 3. Verificar: sem desconto; frete de R$ 12,00; total de R$ 112,00.
        assertAll(
            () -> assertEquals("PAGO", resultado.status()),
            () -> assertEquals(10_000L, resultado.subtotalCentavos()),
            () -> assertEquals(0L, resultado.descontoCentavos()),
            () -> assertEquals(1_200L, resultado.freteCentavos()),
            () -> assertEquals(11_200L, resultado.totalCentavos()),
            // A lista comprova uma única cobrança, com o valor correto.
            () -> assertEquals(List.of(11_200L), cobrancas)
        );
    }

    // =====================================================================
    // Referências obrigatórias
    // =====================================================================

    @Test
    void deveRejeitarProcessadorNuloNoConstrutor() {
        assertThrows(NullPointerException.class, () -> new PedidoService(null));
    }

    @Test
    void deveRejeitarPedidoNulo() {
        ProcessadorStub stub = ProcessadorStub.com();

        assertThrows(NullPointerException.class, () -> new PedidoService(stub).fechar(null, COMUM));
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveRejeitarClienteNulo() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null, item(10_000));

        assertThrows(NullPointerException.class, () -> new PedidoService(stub).fechar(pedido, null));
        assertTrue(stub.chamadas().isEmpty());
    }

    // =====================================================================
    // Retornos antecipados e ordem das regras
    // =====================================================================

    @Test
    void deveRetornarBloqueadoSemCobrarEComValoresZerados() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, BLOQUEADO);

        assertResultado(r, "BLOQUEADO", 0, 0, 0, 0);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveAvaliarBloqueioAntesDosItensEDoCupom() {
        // Lista vazia (subtotal 0 lançaria exceção) e cupom inválido: nada disso é avaliado.
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, "INVALIDO");

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, BLOQUEADO);

        assertResultado(r, "BLOQUEADO", 0, 0, 0, 0);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveRejeitarPedidoSemLinhas() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null);

        assertThrows(IllegalArgumentException.class, () -> new PedidoService(stub).fechar(pedido, COMUM));
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveRejeitarPedidoSomenteComLinhasInativas() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null, item(10_000, 0, 0, 1_000, false));

        assertThrows(IllegalArgumentException.class, () -> new PedidoService(stub).fechar(pedido, COMUM));
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveRetornarSemEstoqueQuandoPrimeiraLinhaFalta() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null,
            item(10_000, 5, 4, 1_000, false), item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "SEM_ESTOQUE", 0, 0, 0, 0);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveRetornarSemEstoqueQuandoUltimaLinhaFalta() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null,
            item(10_000), item(10_000), item(10_000, 5, 4, 1_000, false));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "SEM_ESTOQUE", 0, 0, 0, 0);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveVerificarEstoqueAntesDoCupom() {
        // Cupom inválido não é avaliado quando falta estoque.
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, "INVALIDO", item(10_000, 5, 4, 1_000, false));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertEquals("SEM_ESTOQUE", r.status());
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void devePropagarCupomDesconhecidoSemCobrar() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, "PROMO", item(10_000));

        assertThrows(IllegalArgumentException.class, () -> new PedidoService(stub).fechar(pedido, COMUM));
        assertTrue(stub.chamadas().isEmpty());
    }

    // =====================================================================
    // Desconto
    // =====================================================================

    @Test
    void deveAplicarDescontoVipDeDezPorCento() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, null, item(20_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, VIP);

        // liquido 18.000; frete PR 1.200 / 2 = 600; total 18.600
        assertResultado(r, "PAGO", 20_000, 2_000, 600, 18_600);
        assertEquals(List.of(18_600L), stub.chamadas());
    }

    @Test
    void deveTruncarODescontoVipEmCentavos() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, null, item(10_005));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, VIP);

        // desconto 1.000 (1.000,5); liquido 9.005; frete 600; total 9.605
        assertResultado(r, "PAGO", 10_005, 1_000, 600, 9_605);
    }

    @Test
    void deveAplicarCincoPorCentoParaClienteComumComFreteGratis() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, null, item(60_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        // desconto 3.000; liquido 57.000 >= 30.000 => frete zero
        assertResultado(r, "PAGO", 60_000, 3_000, 0, 57_000);
    }

    @Test
    void deveAplicarCupomBemvindoNormalizadoNaPrimeiraCompra() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, "  bemvindo ", item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_NOVO);

        // desconto 2.000; liquido 8.000; frete 1.200; total 9.200
        assertResultado(r, "PAGO", 10_000, 2_000, 1_200, 9_200);
    }

    @Test
    void deveAplicarCupomExtra10() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, "EXTRA10", item(20_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        // desconto 2.000; liquido 18.000; frete 1.200; total 19.200
        assertResultado(r, "PAGO", 20_000, 2_000, 1_200, 19_200);
    }

    @Test
    void deveLimitarDescontoAVintePorCento() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, "BEMVINDO", item(10_000));
        Cliente vipNovo = new Cliente(true, false, 0);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, vipNovo);

        // 1.000 + 2.000 limitado a 2.000; liquido 8.000; frete 600; total 8.600
        assertResultado(r, "PAGO", 10_000, 2_000, 600, 8_600);
    }

    @Test
    void deveIgnorarCupomBrancoNoFechamento() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, "   ", item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGO", 10_000, 0, 1_200, 11_200);
    }

    // =====================================================================
    // Frete
    // =====================================================================

    @Test
    void deveCobrarExcedenteDePesoEFragilNoFechamento() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        // 3 x 1.500 g = 4.500 g => 3 iterações (900); base 1.200; frágil 500.
        Pedido pedido = pedido("PR", false, null, item(5_000, 3, 3, 1_500, true));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGO", 15_000, 0, 2_600, 17_600);
    }

    @Test
    void deveCobrarExpressoParaClienteComHistorico() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", true, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        // frete 1.200 + 1.500; com histórico o expresso não vai para revisão
        assertResultado(r, "PAGO", 10_000, 0, 2_700, 12_700);
        assertEquals(List.of(12_700L), stub.chamadas());
    }

    @Test
    void deveIgnorarLinhasInativasNoSubtotalPesoEFragilidade() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, null,
            item(10_000, 0, 0, 99_000, true), item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGO", 10_000, 0, 1_200, 11_200);
    }

    @Test
    void deveAvaliarEstoquePorLinhaComSkuRepetido() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        ItemPedido a = new ItemPedido("MESMO", 10_000, 3, 3, 500, false);
        ItemPedido b = new ItemPedido("MESMO", 10_000, 3, 3, 500, false);
        Pedido pedido = new Pedido(List.of(a, b), "PR", false, null);

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        // subtotal 60.000; desconto 3.000; liquido 57.000 => frete zero
        assertResultado(r, "PAGO", 60_000, 3_000, 0, 57_000);
    }

    // =====================================================================
    // Risco
    // =====================================================================

    @Test
    void deveEncaminharParaRevisaoPrimeiraCompraAcimaDeMilReaisSemCobrar() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null, item(120_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_NOVO);

        // desconto 6.000; liquido 114.000 (frete gratuito); total 114.000 > 100.000
        assertResultado(r, "REVISAO", 120_000, 6_000, 0, 114_000);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveEncaminharParaRevisaoPrimeiraCompraExpressaSemCobrar() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", true, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM_NOVO);

        assertResultado(r, "REVISAO", 10_000, 0, 2_700, 12_700);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveEncaminharParaRevisaoClienteNaoVipComTotalAcimaDeCincoMilReais() {
        ProcessadorStub stub = ProcessadorStub.com();
        Pedido pedido = pedido("PR", false, null, item(1_000_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        // desconto 50.000; total 950.000 > 500.000
        assertResultado(r, "REVISAO", 1_000_000, 50_000, 0, 950_000);
        assertTrue(stub.chamadas().isEmpty());
    }

    @Test
    void deveCobrarClienteVipComTotalAcimaDeCincoMilReais() {
        ProcessadorStub stub = ProcessadorStub.com(true);
        Pedido pedido = pedido("PR", false, null, item(1_000_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, VIP);

        // desconto 100.000; total 900.000, mas VIP com histórico é aprovado
        assertResultado(r, "PAGO", 1_000_000, 100_000, 0, 900_000);
        assertEquals(List.of(900_000L), stub.chamadas());
    }

    // =====================================================================
    // Pagamento
    // =====================================================================

    @Test
    void deveRetornarPagamentoRecusadoSemRepetir() {
        ProcessadorStub stub = ProcessadorStub.com(false);
        Pedido pedido = pedido("PR", false, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
        assertEquals(List.of(11_200L), stub.chamadas());
    }

    @Test
    void devePagarAposUmaIndisponibilidadeTemporaria() {
        ProcessadorStub stub = ProcessadorStub.com(new IllegalStateException(), true);
        Pedido pedido = pedido("PR", false, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGO", 10_000, 0, 1_200, 11_200);
        assertEquals(List.of(11_200L, 11_200L), stub.chamadas());
    }

    @Test
    void deveRecusarPagamentoAoEsgotarTresTentativas() {
        ProcessadorStub stub = ProcessadorStub.com(
            new IllegalStateException(), new IllegalStateException(), new IllegalStateException());
        Pedido pedido = pedido("PR", false, null, item(10_000));

        ResultadoPedido r = new PedidoService(stub).fechar(pedido, COMUM);

        assertResultado(r, "PAGAMENTO_RECUSADO", 10_000, 0, 1_200, 11_200);
        assertEquals(List.of(11_200L, 11_200L, 11_200L), stub.chamadas());
    }

    @Test
    void devePropagarExcecaoInesperadaDoProcessador() {
        ProcessadorStub stub = ProcessadorStub.com(new UnsupportedOperationException("falha"));
        Pedido pedido = pedido("PR", false, null, item(10_000));

        assertThrows(UnsupportedOperationException.class,
            () -> new PedidoService(stub).fechar(pedido, COMUM));
        assertEquals(1, stub.chamadas().size());
    }
}
