package br.edu.ifpr.boletim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoletimTest {

    private static final double TOLERANCIA = 0.0001;

    // ---------- calcularMedia ----------

    @Test
    void deveCalcularMediaIgualCinco() {
        Boletim boletim = new Boletim();

        double resultado = boletim.calcularMedia(5, 5);

        assertEquals(5, resultado, TOLERANCIA);
    }

    @Test
    void deveCalcularMediaComParteDecimal() {
        Boletim boletim = new Boletim();

        // (7 + 8) / 2 = 7,5
        assertEquals(7.5, boletim.calcularMedia(7, 8), TOLERANCIA);
    }

    @Test
    void deveCalcularMediaComNotasDecimais() {
        Boletim boletim = new Boletim();

        // (6,3 + 7,4) / 2 = 6,85 (sem arredondamento)
        assertEquals(6.85, boletim.calcularMedia(6.3, 7.4), TOLERANCIA);
    }

    @Test
    void deveCalcularMediaComNotasExtremas() {
        Boletim boletim = new Boletim();

        assertEquals(0, boletim.calcularMedia(0, 0), TOLERANCIA);
        assertEquals(5, boletim.calcularMedia(0, 10), TOLERANCIA);
        assertEquals(10, boletim.calcularMedia(10, 10), TOLERANCIA);
    }

    // ---------- verificarSituacao ----------

    @Test
    void deveAprovarAlunoComMediaOito() {
        // Preparar: criar o objeto que será testado.
        Boletim boletim = new Boletim();

        // Executar: chamar um único método com uma entrada conhecida.
        String resultado = boletim.verificarSituacao(8);

        // Verificar: comparar o resultado esperado com o resultado obtido.
        assertEquals("APROVADO", resultado);
    }

    @Test
    void deveRecuperarAlunoComMediaCincoEMeio() {
        assertEquals("RECUPERACAO", new Boletim().verificarSituacao(5.5));
    }

    @Test
    void deveReprovarAlunoComMediaDois() {
        assertEquals("REPROVADO", new Boletim().verificarSituacao(2));
    }

    @Test
    void deveAprovarNoLimiteExatoSete() {
        assertEquals("APROVADO", new Boletim().verificarSituacao(7));
    }

    @Test
    void deveRecuperarLogoAbaixoDeSete() {
        assertEquals("RECUPERACAO", new Boletim().verificarSituacao(6.99));
    }

    @Test
    void deveRecuperarNoLimiteExatoQuatro() {
        assertEquals("RECUPERACAO", new Boletim().verificarSituacao(4));
    }

    @Test
    void deveReprovarLogoAbaixoDeQuatro() {
        assertEquals("REPROVADO", new Boletim().verificarSituacao(3.99));
    }

    @Test
    void deveReprovarComMediaZeroEAprovarComDez() {
        Boletim boletim = new Boletim();

        assertEquals("REPROVADO", boletim.verificarSituacao(0));
        assertEquals("APROVADO", boletim.verificarSituacao(10));
    }

    // ---------- contarAprovados ----------

    @Test
    void deveRetornarZeroParaArrayVazio() {
        assertEquals(0, new Boletim().contarAprovados(new double[] {}));
    }

    @Test
    void deveContarUmElementoAprovado() {
        assertEquals(1, new Boletim().contarAprovados(new double[] {8}));
    }

    @Test
    void deveContarZeroParaUmElementoNaoAprovado() {
        assertEquals(0, new Boletim().contarAprovados(new double[] {5}));
    }

    @Test
    void deveContarApenasAprovadosEmVariosElementos() {
        // 8 e 7 (limite) aprovam; 5 não.
        assertEquals(2, new Boletim().contarAprovados(new double[] {8, 5, 7}));
    }

    @Test
    void deveContarTodosQuandoTodosAprovados() {
        assertEquals(3, new Boletim().contarAprovados(new double[] {7, 9.5, 10}));
    }

    @Test
    void deveContarZeroQuandoNenhumAprovado() {
        assertEquals(0, new Boletim().contarAprovados(new double[] {0, 4, 6.99}));
    }
}
