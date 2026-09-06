package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;

public class Veiculo {

    private Long id;
    private String placa;
    private String modelo;
    private Integer ano;
    private Long quilometragem;
    private String status;
    private LocalDate ultimaRevisao;
    private LocalDate proximaRevisao;
    private Long quilometragemUltimaRevisao;
    private String observacoes;
    private String situacaoManutencao;

    public Veiculo() {
    }

    public Veiculo(Long id, String placa, String modelo, Integer ano, Long quilometragem,
                   String status, LocalDate ultimaRevisao, LocalDate proximaRevisao,
                   Long quilometragemUltimaRevisao, String observacoes, String situacaoManutencao) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.ano = ano;
        this.quilometragem = quilometragem;
        this.status = status;
        this.ultimaRevisao = ultimaRevisao;
        this.proximaRevisao = proximaRevisao;
        this.quilometragemUltimaRevisao = quilometragemUltimaRevisao;
        this.observacoes = observacoes;
        this.situacaoManutencao = situacaoManutencao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Long getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Long quilometragem) {
        this.quilometragem = quilometragem;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getUltimaRevisao() {
        return ultimaRevisao;
    }

    public void setUltimaRevisao(LocalDate ultimaRevisao) {
        this.ultimaRevisao = ultimaRevisao;
    }

    public LocalDate getProximaRevisao() {
        return proximaRevisao;
    }

    public void setProximaRevisao(LocalDate proximaRevisao) {
        this.proximaRevisao = proximaRevisao;
    }

    public Long getQuilometragemUltimaRevisao() {
        return quilometragemUltimaRevisao;
    }

    public void setQuilometragemUltimaRevisao(Long quilometragemUltimaRevisao) {
        this.quilometragemUltimaRevisao = quilometragemUltimaRevisao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getSituacaoManutencao() {
        return situacaoManutencao;
    }

    public void setSituacaoManutencao(String situacaoManutencao) {
        this.situacaoManutencao = situacaoManutencao;
    }
}
