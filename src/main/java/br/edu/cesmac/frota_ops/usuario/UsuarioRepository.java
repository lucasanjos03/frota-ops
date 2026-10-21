package br.edu.cesmac.frota_ops.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByAtivoTrue();

    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    long countByAtivoTrue();

    @Query("""
        SELECT u FROM Usuario u
        LEFT JOIN u.motorista m
        WHERE u.ativo = true
          AND (m IS NULL OR (:usuarioIdAtual IS NOT NULL AND u.id = :usuarioIdAtual))
        ORDER BY u.id ASC
    """)
    List<Usuario> listarDisponiveisParaMotorista(@Param("usuarioIdAtual") Long usuarioIdAtual);

    @Query("""
        SELECT u FROM Usuario u
        WHERE u.ativo = true
          AND (LOWER(u.username) = LOWER(:login) OR LOWER(u.email) = LOWER(:login))
          AND u.senha = :senha
    """)
    Optional<Usuario> autenticarUsuarioAtivo(
            @Param("login") String login,
            @Param("senha") String senha);

}
