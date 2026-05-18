package Recursos;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.mercadopago.resources.payment.Payment;
import Entidades.Persona.Alumno;
import Entidades.Carreras.Cohorte;
import java.awt.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeneradorComprobanteMP {

    /**
     * Sobrecarga para mantener compatibilidad hacia atrás.
     */
    public static ByteArrayOutputStream generarComprobante(Payment payment) throws Exception {
        return generarComprobante(payment, null, null);
    }

    /**
     * Genera un comprobante en PDF con diseño moderno y profesional.
     */
    public static ByteArrayOutputStream generarComprobante(Payment payment, Alumno alumno, Cohorte cohorte) throws Exception {
        // Inicializar documento A4 con márgenes adecuados
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // --- SISTEMA DE DISEÑO / PALETA DE COLORES ---
        Color primaryColor = new Color(30, 58, 138);     // Azul profundo (#1e3a8a) - Identidad de la Facultad
        Color secondaryColor = new Color(59, 130, 246);  // Azul vibrante (#3b82f6)
        Color textColor = new Color(15, 23, 42);         // Slate-900 (#0f172a)
        Color textMuted = new Color(100, 116, 139);      // Slate-500 (#64748b)
        Color successColor = new Color(16, 185, 129);    // Emerald-500 (#10b981) - Éxito de la transacción
        Color lightGray = new Color(248, 250, 252);      // Slate-50 (#f8fafc) - Fondo de las celdas
        Color borderGray = new Color(226, 232, 240);     // Slate-200 (#e2e8f0) - Bordes elegantes

        // --- FUENTES ---
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryColor);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textMuted);
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryColor);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textMuted);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
        Font valueBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor);
        Font totalLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, primaryColor);
        Font totalValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, successColor);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, textMuted);

        // --- 1. ENCABEZADO INSTITUCIONAL ---
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        // Celda Izquierda: Identidad de la Institución
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("FACULTAD DE HUMANIDADES", titleFont));
        leftCell.addElement(new Paragraph("Universidad Nacional de Catamarca", subtitleFont));
        headerTable.addCell(leftCell);

        // Celda Derecha: Tipo de Documento y Fecha
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        Paragraph docType = new Paragraph("COMPROBANTE DE PAGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, secondaryColor));
        docType.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(docType);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph datePara = new Paragraph("Fecha: " + sdf.format(new Date()), subtitleFont);
        datePara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(datePara);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Línea Divisoria Decorativa
        document.add(new Paragraph(" "));
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.BOTTOM);
        lineCell.setBorderWidth(2f);
        lineCell.setBorderColor(primaryColor);
        lineTable.addCell(lineCell);
        document.add(lineTable);
        document.add(new Paragraph(" "));

        // --- 2. SECCIÓN: DATOS DEL ALUMNO ---
        PdfPTable sectionHeader = new PdfPTable(1);
        sectionHeader.setWidthPercentage(100);
        PdfPCell secCell = new PdfPCell(new Phrase("DATOS DEL ALUMNO", sectionTitleFont));
        secCell.setBorder(Rectangle.NO_BORDER);
        secCell.setPaddingBottom(6);
        sectionHeader.addCell(secCell);
        document.add(sectionHeader);

        PdfPTable alumnoTable = new PdfPTable(2);
        alumnoTable.setWidthPercentage(100);
        alumnoTable.setWidths(new float[]{50, 50});
        alumnoTable.setKeepTogether(true);

        addDetailCell(alumnoTable, "Nombre Completo", alumno != null ? alumno.getApellido() + ", " + alumno.getNombre() : "No disponible", labelFont, valueBoldFont, lightGray, borderGray);
        addDetailCell(alumnoTable, "DNI / Documento", alumno != null ? alumno.getDni() : "No disponible", labelFont, valueFont, lightGray, borderGray);
        addDetailCell(alumnoTable, "Carrera / Postgrado", cohorte != null && cohorte.getCarrera() != null ? cohorte.getCarrera().getDescripcion() : "No disponible", labelFont, valueFont, lightGray, borderGray);
        addDetailCell(alumnoTable, "Cohorte Inscripta", cohorte != null ? cohorte.getDescripcion() : "No disponible", labelFont, valueFont, lightGray, borderGray);

        document.add(alumnoTable);
        document.add(new Paragraph(" "));

        // --- 3. SECCIÓN: DETALLE DEL PAGO ---
        PdfPTable sectionHeader2 = new PdfPTable(1);
        sectionHeader2.setWidthPercentage(100);
        PdfPCell secCell2 = new PdfPCell(new Phrase("DETALLE DE LA TRANSACCIÓN", sectionTitleFont));
        secCell2.setBorder(Rectangle.NO_BORDER);
        secCell2.setPaddingBottom(6);
        sectionHeader2.addCell(secCell2);
        document.add(sectionHeader2);

        PdfPTable pagoTable = new PdfPTable(2);
        pagoTable.setWidthPercentage(100);
        pagoTable.setWidths(new float[]{50, 50});
        pagoTable.setKeepTogether(true);

        addDetailCell(pagoTable, "Concepto", "Pago de Cuota - Cohorte " + (cohorte != null ? cohorte.getDescripcion() : "N/A"), labelFont, valueFont, lightGray, borderGray);
        addDetailCell(pagoTable, "ID de Operación (MercadoPago)", payment.getId() != null ? String.valueOf(payment.getId()) : "No disponible", labelFont, valueFont, lightGray, borderGray);
        addDetailCell(pagoTable, "Método de Pago", payment.getPaymentMethodId() != null ? payment.getPaymentMethodId().toUpperCase() : "No disponible", labelFont, valueFont, lightGray, borderGray);

        // Celda Especial para el Estado (Aprobado en color verde)
        PdfPCell stateCell = new PdfPCell();
        stateCell.setBackgroundColor(lightGray);
        stateCell.setBorderColor(borderGray);
        stateCell.setPadding(8);
        stateCell.addElement(new Paragraph("Estado de Transacción", labelFont));
        
        String cleanStatus = payment.getStatus() != null ? payment.getStatus().toUpperCase() : "APROBADO";
        Paragraph statePara = new Paragraph(cleanStatus, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, successColor));
        stateCell.addElement(statePara);
        pagoTable.addCell(stateCell);

        document.add(pagoTable);
        document.add(new Paragraph(" "));

        // --- 4. SECCIÓN: TOTAL FACTURADO ---
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setWidths(new float[]{60, 40});
        totalTable.setKeepTogether(true);

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL ABONADO", totalLabelFont));
        totalLabelCell.setBackgroundColor(new Color(241, 245, 249)); // Gris pizarra muy claro (#f1f5f9)
        totalLabelCell.setBorderColor(borderGray);
        totalLabelCell.setPadding(12);
        totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalTable.addCell(totalLabelCell);

        String rawAmount = payment.getTransactionAmount() != null ? payment.getTransactionAmount().toString() : "0.00";
        PdfPCell totalValCell = new PdfPCell(new Phrase("$" + rawAmount, totalValueFont));
        totalValCell.setBackgroundColor(new Color(241, 245, 249));
        totalValCell.setBorderColor(borderGray);
        totalValCell.setPadding(12);
        totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        totalTable.addCell(totalValCell);

        document.add(totalTable);

        // --- 5. PIE DE PÁGINA ---
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph footer1 = new Paragraph("Este es un comprobante digital emitido automáticamente por el sistema de gestión económica de la Facultad de Humanidades.", footerFont);
        footer1.setAlignment(Element.ALIGN_CENTER);
        document.add(footer1);

        Paragraph footer2 = new Paragraph("Universidad Nacional de Catamarca - Av. Belgrano Nº 300 - San Fernando del Valle de Catamarca.", footerFont);
        footer2.setAlignment(Element.ALIGN_CENTER);
        document.add(footer2);

        document.close();
        return out;
    }

    /**
     * Helper para agregar celdas de información con formato consistente de etiquetas y valores.
     */
    private static void addDetailCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont, Color bg, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setPadding(8);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }
}
