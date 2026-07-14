package mx.gob.hospital.sgo.oficio;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class OficioSpecifications {

    private OficioSpecifications() {
    }

    public static Specification<Oficio> fechaDesde(LocalDate desde) {
        if (desde == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), desde);
    }

    public static Specification<Oficio> fechaHasta(LocalDate hasta) {
        if (hasta == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), hasta);
    }

    public static Specification<Oficio> direccion(Direccion direccion) {
        if (direccion == null) return null;
        return (root, query, cb) -> cb.equal(root.get("direccion"), direccion);
    }

    public static Specification<Oficio> estado(EstadoOficio estado) {
        if (estado == null) return null;
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Oficio> textoLibre(String texto) {
        if (texto == null || texto.isBlank()) return null;
        return (root, query, cb) -> {
            String patron = "%" + texto.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("asunto")), patron),
                    cb.like(cb.lower(root.get("remitente")), patron),
                    cb.like(cb.lower(root.get("folio")), patron)
            );
        };
    }

    public static Specification<Oficio> areaTurnada(Long areaId) {
        if (areaId == null) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.join("turnados", JoinType.INNER).get("area").get("id"), areaId);
        };
    }
}
