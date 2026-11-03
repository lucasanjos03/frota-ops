package br.edu.cesmac.frota_ops.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Contagem de usuários ativos agrupada por perfil (ADMIN, OPERADOR, MOTORISTA). */
    interface ContagemPorPerfil {
        String getPerfil();

        long getTotal();
    }

    List<Usuario> findAllByAtivoTrue();

    Optional<Usuario> findByIdAndAtivoTrue(Long id);

    long countByAtivoTrue();

    Optional<Usuario> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Query("select u.perfil as perfil, count(u) as total from Usuario u "
            + "where u.ativo = true group by u.perfil")
    List<ContagemPorPerfil> contarAtivosPorPerfil();

}
