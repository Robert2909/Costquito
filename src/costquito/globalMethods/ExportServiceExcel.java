package costquito.globalMethods;

import costquito.controllers.PanelAdminOpcion3Controller.VentaRow;
import costquito.controllers.PanelAdminOpcion3Controller.ItemRow;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Exporta un reporte XLSX con 2 hojas:
 * - "Ventas <fecha>": Folio, Hora, Vendedor, Método, Total, Pago, Cambio
 * - "Items <fecha>": Folio, Producto, PU, Cantidad, Importe
 */
public final class ExportServiceExcel {

    private ExportServiceExcel() {}

    public static void exportVentasDia(File file, LocalDate fecha, List<VentaRow> ventas) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {

            // --- Estilos ---
            Font fBold = wb.createFont();
            fBold.setBold(true);

            CellStyle header = wb.createCellStyle();
            header.setFont(fBold);

            DataFormat df = wb.createDataFormat();
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(df.getFormat("$#,##0.00"));

            // ===== Hoja 1: Ventas =====
            Sheet s1 = wb.createSheet("Ventas " + fecha);
            String[] cols1 = {"Folio", "Hora", "Vendedor", "Método", "Total", "Pago", "Cambio"};

            Row h1 = s1.createRow(0);
            for (int i = 0; i < cols1.length; i++) {
                Cell c = h1.createCell(i);
                c.setCellValue(cols1[i]);
                c.setCellStyle(header);
            }

            int r = 1;
            for (VentaRow v : ventas) {
                Row row = s1.createRow(r++);
                row.createCell(0).setCellValue(v.getFolio());
                row.createCell(1).setCellValue(v.getHora());
                row.createCell(2).setCellValue(v.getVendedor());
                row.createCell(3).setCellValue(v.getMetodo());

                Cell c4 = row.createCell(4); c4.setCellValue(v.getMontoTotal()); c4.setCellStyle(money);
                Cell c5 = row.createCell(5); c5.setCellValue(v.getMontoPago());  c5.setCellStyle(money);
                Cell c6 = row.createCell(6); c6.setCellValue(v.getMontoCambio());c6.setCellStyle(money);
            }

            for (int i = 0; i < cols1.length; i++) s1.autoSizeColumn(i);
            s1.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, r - 1), 0, cols1.length - 1));

            // ===== Hoja 2: Items =====
            Sheet s2 = wb.createSheet("Items " + fecha);
            String[] cols2 = {"Folio", "Producto", "PU", "Cantidad", "Importe"};

            Row h2 = s2.createRow(0);
            for (int i = 0; i < cols2.length; i++) {
                Cell c = h2.createCell(i);
                c.setCellValue(cols2[i]);
                c.setCellStyle(header);
            }

            int rr = 1;
            for (VentaRow v : ventas) {
                for (ItemRow it : v.getItems()) {
                    Row row = s2.createRow(rr++);
                    row.createCell(0).setCellValue(v.getFolio());
                    row.createCell(1).setCellValue(it.getNombre());

                    Cell pu  = row.createCell(2); pu.setCellValue(it.getPrecioUnit()); pu.setCellStyle(money);
                    row.createCell(3).setCellValue(it.getCantidad());
                    Cell imp = row.createCell(4); imp.setCellValue(it.getImporte());   imp.setCellStyle(money);
                }
            }

            for (int i = 0; i < cols2.length; i++) s2.autoSizeColumn(i);
            s2.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rr - 1), 0, cols2.length - 1));

            // Guardar archivo
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
}
