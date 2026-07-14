package mx.gob.hospital.sgo.oficio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OficioRepository extends JpaRepository<Oficio, Long>, JpaSpecificationExecutor<Oficio> {

    boolean existsByFolio(String folio);
}
