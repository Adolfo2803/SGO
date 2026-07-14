package mx.gob.hospital.sgo.oficio;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * NO se usa SELECT MAX(consecutivo)+1 porque tiene condición de carrera:
 * dos transacciones concurrentes leen el mismo máximo y generan el mismo folio.
 * En su lugar se usa un UPDATE atómico con RETURNING sobre una tabla contador.
 * El row-lock de Postgres serializa las transacciones concurrentes y cada una
 * obtiene un consecutivo distinto sin posibilidad de colisión.
 */
@Component
public class GeneradorFolio {

    private static final String FORMATO = "HG/DIR/%04d/%d";

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public String siguienteFolio(int anio) {
        em.createNativeQuery(
                        "INSERT INTO folio_contador (anio, ultimo_consecutivo) " +
                        "VALUES (:anio, 0) ON CONFLICT (anio) DO NOTHING")
                .setParameter("anio", anio)
                .executeUpdate();

        Object resultado = em.createNativeQuery(
                        "UPDATE folio_contador SET ultimo_consecutivo = ultimo_consecutivo + 1 " +
                        "WHERE anio = :anio RETURNING ultimo_consecutivo")
                .setParameter("anio", anio)
                .getSingleResult();

        int consecutivo = ((Number) resultado).intValue();
        return String.format(FORMATO, consecutivo, anio);
    }
}
