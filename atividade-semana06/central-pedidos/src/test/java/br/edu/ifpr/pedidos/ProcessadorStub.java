package br.edu.ifpr.pedidos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Stub com roteiro: cada chamada consome a próxima resposta.
 * Um Boolean é devolvido; uma RuntimeException é lançada.
 * Registra o total recebido em cada chamada.
 */
class ProcessadorStub implements ProcessadorPagamento {
    private final Deque<Object> roteiro = new ArrayDeque<>();
    private final List<Long> chamadas = new ArrayList<>();

    static ProcessadorStub com(Object... respostas) {
        ProcessadorStub stub = new ProcessadorStub();
        for (Object resposta : respostas) stub.roteiro.add(resposta);
        return stub;
    }

    @Override
    public boolean autorizar(long totalCentavos) {
        chamadas.add(totalCentavos);
        Object resposta = roteiro.poll();
        if (resposta == null) throw new AssertionError("Chamada além do roteiro do stub");
        if (resposta instanceof RuntimeException erro) throw erro;
        return (Boolean) resposta;
    }

    List<Long> chamadas() {
        return chamadas;
    }
}
