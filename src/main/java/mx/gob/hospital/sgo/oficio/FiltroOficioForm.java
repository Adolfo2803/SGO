package mx.gob.hospital.sgo.oficio;

import java.time.LocalDate;

public class FiltroOficioForm {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Direccion direccion;
    private EstadoOficio estado;
    private String texto;
    private Long areaId;

    public FiltroOficioForm() {
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public EstadoOficio getEstado() {
        return estado;
    }

    public void setEstado(EstadoOficio estado) {
        this.estado = estado;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }
}
