package br.edu.ifpr.pedidos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ItemPedidoTest {

    private static ItemPedido item(long preco, int quantidade, int estoque, int peso) {
        return new ItemPedido("SKU", preco, quantidade, estoque, peso, false);
    }

    // ---------- construtor ----------

    @Test
    void deveGuardarOsValoresInformados() {
        ItemPedido item = new ItemPedido("ABC", 1_500, 2, 5, 300, true);

        assertAll(
            () -> assertEquals("ABC", item.sku()),
            () -> assertEquals(1_500L, item.precoCentavos()),
            () -> assertEquals(2, item.quantidade()),
            () -> assertEquals(5, item.estoque()),
            () -> assertEquals(300, item.pesoGramas()),
            () -> assertTrue(item.fragil())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarSkuNuloOuBranco(String sku) {
        assertThrows(IllegalArgumentException.class,
            () -> new ItemPedido(sku, 100, 1, 1, 1, false));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 1_000_001})
    void deveRejeitarPrecoForaDoIntervalo(long preco) {
        assertThrows(IllegalArgumentException.class, () -> item(preco, 1, 1, 1));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 1_000_000})
    void deveAceitarPrecosNosLimites(long preco) {
        assertEquals(preco, item(preco, 1, 1, 1).precoCentavos());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void deveRejeitarQuantidadeForaDoIntervalo(int quantidade) {
        assertThrows(IllegalArgumentException.class, () -> item(100, quantidade, 1, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void deveAceitarQuantidadesNosLimites(int quantidade) {
        assertEquals(quantidade, item(100, quantidade, 1, 1).quantidade());
    }

    @Test
    void deveRejeitarEstoqueNegativo() {
        assertThrows(IllegalArgumentException.class, () -> item(100, 1, -1, 1));
    }

    @Test
    void deveAceitarEstoqueZero() {
        assertEquals(0, item(100, 0, 0, 1).estoque());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 100_001})
    void deveRejeitarPesoForaDoIntervalo(int peso) {
        assertThrows(IllegalArgumentException.class, () -> item(100, 1, 1, peso));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100_000})
    void deveAceitarPesosNosLimites(int peso) {
        assertEquals(peso, item(100, 1, 1, peso).pesoGramas());
    }

    // ---------- totalCentavos ----------

    @Test
    void deveMultiplicarPrecoPelaQuantidade() {
        assertEquals(3_750L, item(1_250, 3, 3, 1).totalCentavos());
    }

    @Test
    void deveTotalizarZeroParaLinhaInativa() {
        assertEquals(0L, item(1_250, 0, 0, 1).totalCentavos());
    }

    @Test
    void deveTotalizarNoMaximoDoDominio() {
        assertEquals(100_000_000L, item(1_000_000, 100, 100, 1).totalCentavos());
    }

    // ---------- disponivel ----------

    @Test
    void deveEstarDisponivelQuandoEstoqueSuperaQuantidade() {
        assertTrue(item(100, 2, 3, 1).disponivel());
    }

    @Test
    void deveEstarDisponivelQuandoEstoqueIgualaQuantidade() {
        assertTrue(item(100, 3, 3, 1).disponivel());
    }

    @Test
    void naoDeveEstarDisponivelQuandoQuantidadeSuperaEstoque() {
        assertFalse(item(100, 4, 3, 1).disponivel());
    }

    @Test
    void deveEstarDisponivelLinhaInativaSemEstoque() {
        assertTrue(item(100, 0, 0, 1).disponivel());
    }
}
