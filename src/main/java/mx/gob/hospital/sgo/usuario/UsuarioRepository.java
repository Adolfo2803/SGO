package mx.gob.hospital.sgo.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameAndActivoTrue(String username);

    boolean existsByUsername(String username);
}
