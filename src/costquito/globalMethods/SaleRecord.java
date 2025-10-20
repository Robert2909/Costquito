package costquito.globalMethods;

import java.util.ArrayList;
import java.util.List;

public class SaleRecord {

    public static class Item {
        public String productId;
        public String nombreSnapshot;
        public double precioUnitario; // 2 decimales
        public int cantidad;
        public double totalLinea;     // precioUnitario * cantidad (2 decimales)
    }

    public String uuid;              // UUID de la venta
    public long folio;               // consecutivo legible
    public String fecha;             // dd/MM/yyyy
    public String hora;              // HH:mm:ss
    public String cashierUsername;   // vendedor que registró
    public String metodoPago;        // "💵 Efectivo", "💳 Tarjeta", "📨 Transferencia"

    public double total;             // suma de totalLinea
    public double pago;              // monto recibido (si tarjeta/transferencia == total)
    public double cambio;            // pago - total (>=0)
    public boolean inventoryApplied = true; // si la baja de inventario se aplicó con éxito

    public List<Item> items = new ArrayList<>();
}
