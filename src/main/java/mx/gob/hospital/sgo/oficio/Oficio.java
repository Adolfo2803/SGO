package mx.gob.hospital.sgo.oficio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import mx.gob.hospital.sgo.turnado.Turnado;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Oficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direccion direccion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String remitente;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false, length = 500)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoOficio estado;

    @Column(length = 500)
    private String rutaArchivo;

    @CreatedBy
    @Column(nullable = false, length = 100)
    private String creadoPor;

    @CreatedDate
    @Column(nullable = false)
    private Instant creadoEn;

    @OneToMany(mappedBy = "oficio")
    private List<Turnado> turnados = new ArrayList<>();

    public Oficio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public EstadoOficio getEstado() {
        return estado;
    }

    public void setEstado(EstadoOficio estado) {
        this.estado = estado;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public List<Turnado> getTurnados() {
        return turnados;
    }

    public void setTurnados(List<Turnado> turnados) {
        this.turnados = turnados;
    }
}
