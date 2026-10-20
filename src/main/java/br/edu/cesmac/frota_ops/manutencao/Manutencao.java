package br.edu.cesmac.frota_ops.manutencao;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.cesmac.frota_ops.veiculo.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "A data da manutenção é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "data_manutencao", nullable = false)
    private LocalDate data;

    @NotNull(message = "A quilometragem é obrigatória.")
    @Min(value = 0, message = "A quilometragem não pode ser negativa.")
    @Column(nullable = false)
    private Long quilometragem;

    @NotBlank(message = "O serviço realizado é obrigatório.")
    @Size(max = 255, message = "O serviço realizado deve ter no máximo 255 caracteres.")
    @Column(name = "servico_realizado", nullable = false, length = 255)
    private String servicoRealizado;

    @NotBlank(message = "O responsável é obrigatório.")
    @Size(max = 150, message = "O responsável deve ter no máximo 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String responsavel;

    @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
    @Column(columnDefinition = "TEXT")
    private String observacao;

    @NotNull(message = "O veículo é obrigatório.")
    @ManyToOne
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    public Manutencao() {
    }

    public Manutencao(Long id, LocalDate data, Long quilometragem, String servicoRealizado,
                      String responsavel, String observacao, Veiculo veiculo) {
        this.id = id;
        this.data = data;
        this.quilometragem = quilometragem;
        this.servicoRealizado = servicoRealizado;
        this.responsavel = responsavel;
        this.observacao = observacao;
        this.veiculo = veiculo;
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

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Manutencao other = (Manutencao) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
