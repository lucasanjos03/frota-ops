package br.edu.cesmac.frota_ops.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Long quilometragem = 0L;

    @Column(nullable = false, length = 30)
    private String status = "Disponível";

    @Column(name = "ultima_revisao")
    private LocalDate ultimaRevisao;

    @Column(name = "proxima_revisao")
    private LocalDate proximaRevisao;

    @Column(name = "quilometragem_ultima_revisao")
    private Long quilometragemUltimaRevisao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "situacao_manutencao", length = 30)
    private String situacaoManutencao = "Em dia";

    @Column(name = "data_criacao", updatable = false)
    private OffsetDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private OffsetDateTime dataAtualizacao;

    @PrePersist
    public void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = OffsetDateTime.now();
        }
        if (dataAtualizacao == null) {
            dataAtualizacao = OffsetDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "Disponível";
        }
        if (situacaoManutencao == null || situacaoManutencao.isBlank()) {
            situacaoManutencao = "Em dia";
        }
        if (quilometragem == null) {
            quilometragem = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        dataAtualizacao = OffsetDateTime.now();
    }

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

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(OffsetDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public OffsetDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(OffsetDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
