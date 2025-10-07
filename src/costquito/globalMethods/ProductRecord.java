package costquito.globalMethods;

import java.util.Objects;
import java.util.UUID;

public class ProductRecord {
    public String id;          // UUID, inmutable
    public String nombre;      // único (case-insensitive + trim)
    public double precio;      // 0.00 – 999999.99
    public int cantidad;       // 0 – 100000

    public ProductRecord() { }

    public ProductRecord(String nombre, double precio, int cantidad) {
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    // Helpers
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public double getPrecio() { return precio; }
    public int getCantidad() { return cantidad; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
