package br.edu.cesmac.frota_ops.usuario;

import java.time.OffsetDateTime;

import br.edu.cesmac.frota_ops.motorista.Motorista;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome de usuário (username) é obrigatório.")
    @Size(min = 3, max = 100, message = "O username deve ter entre 3 e 100 caracteres.")
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank(message = "O nome completo é obrigatório.")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 4, max = 255, message = "A senha deve ter no mínimo 4 caracteres.")
    @Column(nullable = false, length = 255)
    private String senha;

    @NotBlank(message = "O perfil de acesso é obrigatório.")
    @Column(nullable = false, length = 30)
    private String perfil = "OPERADOR";

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", updatable = false)
    private OffsetDateTime dataCriacao;

    @Column(name = "data_modificacao")
    private OffsetDateTime dataModificacao;

    @OneToOne(mappedBy = "usuario")
    private Motorista motorista;

    public Usuario() {
    }

    public Usuario(Long id, String username, String nome, String email, String senha, String perfil, Boolean ativo) {
        this.id = id;
        this.username = username;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.ativo = ativo;
    }

    @PrePersist
    public void aoCriar() {
        OffsetDateTime agora = OffsetDateTime.now();
        if (this.dataCriacao == null) {
            this.dataCriacao = agora;
        }
        this.dataModificacao = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.dataModificacao = OffsetDateTime.now();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.perfil);
    }

    public boolean isOperador() {
        return "OPERADOR".equalsIgnoreCase(this.perfil);
    }

    public boolean isMotorista() {
        return "MOTORISTA".equalsIgnoreCase(this.perfil);
    }

    public boolean podeEditarUsuario(Long targetUsuarioId) {
        if (isAdmin()) {
            return true;
        }
        return isMotorista() && this.id != null && this.id.equals(targetUsuarioId);
    }

    public boolean podeEditarMotorista(Long targetMotoristaId) {
        if (isAdmin()) {
            return true;
        }
        return isMotorista()
                && this.motorista != null
                && this.motorista.getId() != null
                && this.motorista.getId().equals(targetMotoristaId);
    }

    public boolean podeEditarVeiculo(Long motoristaIdDoVeiculo) {
        if (isAdmin()) {
            return true;
        }
        return isMotorista()
                && motoristaIdDoVeiculo != null
                && this.motorista != null
                && this.motorista.getId() != null
                && this.motorista.getId().equals(motoristaIdDoVeiculo);
    }

    public boolean podeGerenciarManutencoes() {
        return isAdmin() || isMotorista();
    }

    public boolean podeGerenciarTags() {
        return isAdmin() || isMotorista();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public OffsetDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(OffsetDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public OffsetDateTime getDataModificacao() {
        return dataModificacao;
    }

    public void setDataModificacao(OffsetDateTime dataModificacao) {
        this.dataModificacao = dataModificacao;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
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
        Usuario other = (Usuario) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
