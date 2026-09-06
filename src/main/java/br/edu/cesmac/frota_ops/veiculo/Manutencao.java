package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;

public class Manutencao {

    private Long id;
    private LocalDate data;
    private Long quilometragem;
    private String servicoRealizado;
    private String responsavel;
    private String observacao;
    private Long veiculoId;

    public Manutencao() {
    }

    public Manutencao(Long id, LocalDate data, Long quilometragem, String servicoRealizado,
                      String responsavel, String observacao, Long veiculoId) {
        this.id = id;
        this.data = data;
        this.quilometragem = quilometragem;
        this.servicoRealizado = servicoRealizado;
        this.responsavel = responsavel;
        this.observacao = observacao;
        this.veiculoId = veiculoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Long getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Long quilometragem) {
        this.quilometragem = quilometragem;
    }

    public String getServicoRealizado() {
        return servicoRealizado;
    }

    public void setServicoRealizado(String servicoRealizado) {
        this.servicoRealizado = servicoRealizado;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Long getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(Long veiculoId) {
        this.veiculoId = veiculoId;
    }
}
