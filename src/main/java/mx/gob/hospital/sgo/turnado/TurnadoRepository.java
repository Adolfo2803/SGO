package mx.gob.hospital.sgo.turnado;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TurnadoRepository extends JpaRepository<Turnado, Long> {

    boolean existsByOficioIdAndAreaId(Long oficioId, Long areaId);

    List<Turnado> findByEstadoInAndIntentosLessThan(Collection<EstadoTurnado> estados, int maxIntentos);

    @EntityGraph(attributePaths = {"area"})
    List<Turnado> findByOficioIdOrderByIdAsc(Long oficioId);

    @EntityGraph(attributePaths = {"oficio", "area"})
    Optional<Turnado> findConOficioYAreaById(Long id);
}
