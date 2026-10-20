package br.edu.cesmac.frota_ops.veiculo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.cesmac.frota_ops.manutencao.Manutencao;
import br.edu.cesmac.frota_ops.motorista.Motorista;
import br.edu.cesmac.frota_ops.tag.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A placa é obrigatória.")
    @Size(min = 7, max = 10, message = "A placa deve ter entre 7 e 10 caracteres.")
    @Column(nullable = false, unique = true, length = 10)
    private String placa;

    @NotBlank(message = "O modelo é obrigatório.")
    @Size(min = 3, max = 120, message = "O modelo deve ter entre 3 e 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String modelo;

    @NotNull(message = "O ano é obrigatório.")
    @Min(value = 1900, message = "O ano deve ser a partir de 1900.")
    @Column(nullable = false)
    private Integer ano;

    @NotNull(message = "A quilometragem é obrigatória.")
    @Min(value = 0, message = "A quilometragem não pode ser negativa.")
    @Column(nullable = false)
    private Long quilometragem;

    @NotBlank(message = "O status é obrigatório.")
    @Column(nullable = false, length = 30)
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "ultima_revisao")
    private LocalDate ultimaRevisao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "proxima_revisao")
    private LocalDate proximaRevisao;

    @Column(name = "quilometragem_ultima_revisao")
    private Long quilometragemUltimaRevisao;

    @Size(max = 1000, message = "As observações devem possuir no máximo 1000 caracteres.")
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "situacao_manutencao", length = 30)
    private String situacaoManutencao;

    @ManyToOne
    @JoinColumn(name = "id_motorista")
    private Motorista motorista;

    @ManyToMany
    @JoinTable(
        name = "veiculo_tags",
        joinColumns = @JoinColumn(name = "id_veiculo"),
        inverseJoinColumns = @JoinColumn(name = "id_tag")
    )
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "veiculo")
    @OrderBy("data DESC")
    private List<Manutencao> manutencoes = new ArrayList<>();

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

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Manutencao> getManutencoes() {
        return manutencoes;
    }

    public void setManutencoes(List<Manutencao> manutencoes) {
        this.manutencoes = manutencoes;
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
        Veiculo other = (Veiculo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
