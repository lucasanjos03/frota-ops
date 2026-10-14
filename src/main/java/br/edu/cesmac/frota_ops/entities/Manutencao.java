package br.edu.cesmac.frota_ops.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_manutencao", nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private Long quilometragem;

    @Column(name = "servico_realizado", nullable = false, length = 255)
    private String servicoRealizado;

    @Column(nullable = false, length = 150)
    private String responsavel;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Column(name = "data_registro")
    private OffsetDateTime dataRegistro;

    @PrePersist
    public void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = OffsetDateTime.now();
        }
    }

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

    public OffsetDateTime getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(OffsetDateTime dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
