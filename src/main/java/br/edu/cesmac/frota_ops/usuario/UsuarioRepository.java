package br.edu.cesmac.frota_ops.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByAtivoTrue();

    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    long countByAtivoTrue();

    Optional<Usuario> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

}
