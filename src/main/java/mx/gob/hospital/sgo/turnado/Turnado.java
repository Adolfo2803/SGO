package mx.gob.hospital.sgo.turnado;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.oficio.Oficio;

import java.time.Instant;

@Entity
public class Turnado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficio_id", nullable = false)
    private Oficio oficio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTurnado estado;

    @Column(nullable = false)
    private int intentos;

    private Instant enviadoEn;

    @Column(length = 1000)
    private String ultimoError;

    public Turnado() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Oficio getOficio() {
        return oficio;
    }

    public void setOficio(Oficio oficio) {
        this.oficio = oficio;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public EstadoTurnado getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurnado estado) {
        this.estado = estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public Instant getEnviadoEn() {
        return enviadoEn;
    }

    public void setEnviadoEn(Instant enviadoEn) {
        this.enviadoEn = enviadoEn;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
    }
}
