package br.edu.ifpr.pedidos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    private static ItemPedido item(long preco, int quantidade, int estoque, int peso, boolean fragil) {
        return new ItemPedido("SKU", preco, quantidade, estoque, peso, fragil);
    }

    private static Pedido pedido(ItemPedido... itens) {
        return new Pedido(List.of(itens), "PR", false, null);
    }

    // ---------- construtor ----------

    @Test
    void deveGuardarOsValoresInformados() {
        ItemPedido item = item(100, 1, 1, 1, false);
        Pedido pedido = new Pedido(List.of(item), "SP", true, "EXTRA10");

        assertAll(
            () -> assertEquals(List.of(item), pedido.itens()),
            () -> assertEquals("SP", pedido.uf()),
            () -> assertTrue(pedido.expresso()),
            () -> assertEquals("EXTRA10", pedido.cupom())
        );
    }

    @Test
    void deveAceitarListaVaziaNaConstrucao() {
        assertTrue(pedido().itens().isEmpty());
    }

    @Test
    void deveAceitarCupomNulo() {
        assertNull(pedido().cupom());
    }

    @Test
    void deveRejeitarListaNula() {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(null, "PR", false, null));
    }

    @Test
    void deveAceitarCemLinhas() {
        List<ItemPedido> cem = Collections.nCopies(100, item(100, 1, 1, 1, false));
        assertEquals(100, new Pedido(cem, "PR", false, null).itens().size());
    }

    @Test
    void deveRejeitarMaisDeCemLinhas() {
        List<ItemPedido> cemEUma = Collections.nCopies(101, item(100, 1, 1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(cemEUma, "PR", false, null));
    }

    @Test
    void deveRejeitarElementoNulo() {
        List<ItemPedido> comNulo = new ArrayList<>();
        comNulo.add(null);
        assertThrows(NullPointerException.class, () -> new Pedido(comNulo, "PR", false, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"P", "PRR", "pr", "Pr", "P1", "P ", "ÁB"})
    void deveRejeitarUfInvalida(String uf) {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(List.of(), uf, false, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PR", "MG", "ZZ"})
    void deveAceitarQualquerUfComDuasLetrasMaiusculas(String uf) {
        assertEquals(uf, new Pedido(List.of(), uf, false, null).uf());
    }

    @Test
    void deveCopiarAListaDefensivamente() {
        List<ItemPedido> original = new ArrayList<>();
        original.add(item(100, 1, 1, 1, false));
        Pedido pedido = new Pedido(original, "PR", false, null);

        original.add(item(200, 1, 1, 1, false));

        assertEquals(1, pedido.itens().size());
        assertThrows(UnsupportedOperationException.class,
            () -> pedido.itens().add(item(300, 1, 1, 1, false)));
    }

    // ---------- subtotalCentavos ----------

    @Test
    void deveTerSubtotalZeroSemLinhas() {
        assertEquals(0L, pedido().subtotalCentavos());
    }

    @Test
    void deveSomarSubtotalDeVariasLinhas() {
        Pedido pedido = pedido(item(10_000, 2, 2, 1, false), item(500, 3, 3, 1, false));

        assertEquals(21_500L, pedido.subtotalCentavos());
    }

    @Test
    void deveIgnorarLinhasInativasNoSubtotal() {
        Pedido pedido = pedido(item(9_999, 0, 0, 1, false), item(500, 3, 3, 1, false),
            item(7_777, 0, 5, 1, false));

        assertEquals(1_500L, pedido.subtotalCentavos());
    }

    @Test
    void deveTerSubtotalZeroSeTodasAsLinhasSaoInativas() {
        assertEquals(0L, pedido(item(100, 0, 0, 1, false)).subtotalCentavos());
    }

    // ---------- pesoGramas ----------

    @Test
    void deveTerPesoZeroSemLinhas() {
        assertEquals(0, pedido().pesoGramas());
    }

    @Test
    void deveSomarPesoTotalConsiderandoQuantidades() {
        Pedido pedido = pedido(item(100, 2, 2, 1_000, false), item(100, 3, 3, 500, false));

        assertEquals(3_500, pedido.pesoGramas());
    }

    @Test
    void naoDeveSomarPesoDeLinhaInativa() {
        Pedido pedido = pedido(item(100, 0, 0, 50_000, false), item(100, 1, 1, 400, false));

        assertEquals(400, pedido.pesoGramas());
    }

    // ---------- temFragil ----------

    @Test
    void naoDeveTerFragilSemLinhas() {
        assertFalse(pedido().temFragil());
    }

    @Test
    void naoDeveTerFragilQuandoNenhumItemEFragil() {
        assertFalse(pedido(item(100, 1, 1, 1, false), item(100, 2, 2, 1, false)).temFragil());
    }

    @Test
    void naoDeveContarFragilInativo() {
        assertFalse(pedido(item(100, 0, 0, 1, true), item(100, 1, 1, 1, false)).temFragil());
    }

    @Test
    void deveDetectarFragilAtivoNaPrimeiraLinha() {
        assertTrue(pedido(item(100, 1, 1, 1, true)).temFragil());
    }

    @Test
    void deveDetectarFragilAtivoDepoisDeOutrasLinhas() {
        Pedido pedido = pedido(item(100, 1, 1, 1, false), item(100, 0, 0, 1, true),
            item(100, 1, 1, 1, true));

        assertTrue(pedido.temFragil());
    }

    // ---------- estoqueSuficiente ----------

    @Test
    void deveTerEstoqueSuficienteSemLinhas() {
        assertTrue(pedido().estoqueSuficiente());
    }

    @Test
    void deveTerEstoqueSuficienteQuandoTodasAsLinhasSaoAtendidas() {
        assertTrue(pedido(item(100, 1, 1, 1, false), item(100, 2, 9, 1, false)).estoqueSuficiente());
    }

    @Test
    void deveDetectarFaltaNaPrimeiraLinha() {
        assertFalse(pedido(item(100, 5, 4, 1, false), item(100, 1, 1, 1, false)).estoqueSuficiente());
    }

    @Test
    void deveDetectarFaltaNaUltimaLinha() {
        assertFalse(pedido(item(100, 1, 1, 1, false), item(100, 1, 1, 1, false),
            item(100, 5, 4, 1, false)).estoqueSuficiente());
    }

    @Test
    void deveAvaliarEstoquePorLinhaMesmoComSkuRepetido() {
        // Duas linhas do mesmo SKU: cada uma cabe no seu estoque (não se somam).
        ItemPedido a = new ItemPedido("MESMO", 100, 3, 3, 1, false);
        ItemPedido b = new ItemPedido("MESMO", 100, 3, 3, 1, false);

        assertTrue(pedido(a, b).estoqueSuficiente());
    }
}
