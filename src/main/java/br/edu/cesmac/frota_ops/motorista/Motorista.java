package br.edu.cesmac.frota_ops.motorista;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import br.edu.cesmac.frota_ops.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "motorista")
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O usuário do sistema é obrigatório para o motorista.")
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "O CPF é obrigatório.")
    @Size(min = 11, max = 14, message = "O CPF deve ter entre 11 e 14 caracteres.")
    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @NotBlank(message = "O número da CNH é obrigatório.")
    @Size(min = 5, max = 20, message = "O número da CNH deve ter entre 5 e 20 caracteres.")
    @Column(name = "numero_cnh", nullable = false, unique = true, length = 20)
    private String numeroCnh;

    @NotBlank(message = "A categoria da CNH é obrigatória.")
    @Size(max = 5, message = "A categoria da CNH deve ter no máximo 5 caracteres.")
    @Column(name = "categoria_cnh", nullable = false, length = 5)
    private String categoriaCnh;

    @NotNull(message = "A validade da CNH é obrigatória.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "validade_cnh", nullable = false)
    private LocalDate validadeCnh;

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
    @Column(length = 20)
    private String telefone;

    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    @Column(length = 150)
    private String email;

    @NotBlank(message = "O status é obrigatório.")
    @Column(nullable = false, length = 30)
    private String status = "Ativo";

    public Motorista() {
    }

    public Motorista(Long id, String nome, String cpf, String numeroCnh,
                     String categoriaCnh, LocalDate validadeCnh, String telefone) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.numeroCnh = numeroCnh;
        this.categoriaCnh = categoriaCnh;
        this.validadeCnh = validadeCnh;
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNumeroCnh() {
        return numeroCnh;
    }

    public void setNumeroCnh(String numeroCnh) {
        this.numeroCnh = numeroCnh;
    }

    public String getCnh() {
        return numeroCnh;
    }

    public void setCnh(String cnh) {
        this.numeroCnh = cnh;
    }

    public String getCategoriaCnh() {
        return categoriaCnh;
    }

    public void setCategoriaCnh(String categoriaCnh) {
        this.categoriaCnh = categoriaCnh;
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        this.validadeCnh = validadeCnh;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        Motorista other = (Motorista) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
