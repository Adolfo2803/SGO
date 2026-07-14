package mx.gob.hospital.sgo.area;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByActivaTrue();
}
