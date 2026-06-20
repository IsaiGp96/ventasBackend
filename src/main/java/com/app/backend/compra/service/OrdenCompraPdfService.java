package com.app.backend.compra.service;

import com.app.backend.compra.entity.DetalleOrdenCompra;
import com.app.backend.compra.entity.OrdenCompra;
import com.app.backend.compra.repository.OrdenCompraRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrdenCompraPdfService {

        private final OrdenCompraRepository ordenCompraRepository;

        // Colores DAW Store
        private static final DeviceRgb COLOR_NEGRO = new DeviceRgb(23, 23, 23);
        private static final DeviceRgb COLOR_GRIS = new DeviceRgb(115, 115, 115);
        private static final DeviceRgb COLOR_GRIS_CLR = new DeviceRgb(245, 245, 245);
        private static final DeviceRgb COLOR_BORDE = new DeviceRgb(229, 229, 229);

        @Transactional(readOnly = true)
        public byte[] generarPdf(Integer idOrden) {
                OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(idOrden)
                                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document doc = new Document(pdf, PageSize.A4);
                        doc.setMargins(40, 50, 40, 50);

                        PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

                        // ─── HEADER ──────────────────────────────────────────────────────
                        Table header = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setBorder(Border.NO_BORDER);

                        // Logo / nombre empresa
                        Cell celdaEmpresa = new Cell().setBorder(Border.NO_BORDER);
                        celdaEmpresa.add(new Paragraph("DAW Store")
                                        .setFont(fontBold).setFontSize(20).setFontColor(COLOR_NEGRO));
                        celdaEmpresa.add(new Paragraph("Orden de Compra")
                                        .setFont(fontRegular).setFontSize(11).setFontColor(COLOR_GRIS));
                        header.addCell(celdaEmpresa);

                        // Número y fecha
                        Cell celdaNumero = new Cell().setBorder(Border.NO_BORDER)
                                        .setTextAlignment(TextAlignment.RIGHT);
                        celdaNumero.add(new Paragraph(
                                        "OC #" + String.format("%04d", orden.getId()))
                                        .setFont(fontBold).setFontSize(16).setFontColor(COLOR_NEGRO));
                        celdaNumero.add(new Paragraph(
                                        orden.getFecha().format(
                                                        DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                                                                        new Locale("es", "MX"))))
                                        .setFont(fontRegular).setFontSize(10).setFontColor(COLOR_GRIS));
                        celdaNumero.add(new Paragraph("Estatus: " +
                                        orden.getEstatus().toUpperCase())
                                        .setFont(fontBold).setFontSize(9)
                                        .setFontColor(new DeviceRgb(180, 120, 0)));
                        header.addCell(celdaNumero);

                        doc.add(header);
                        doc.add(new LineSeparator(
                                        new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f))
                                        .setMarginTop(10).setMarginBottom(20));

                        // ─── PROVEEDOR ────────────────────────────────────────────────────
                        Table seccionProv = new Table(
                                        UnitValue.createPercentArray(new float[] { 50, 50 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setBorder(Border.NO_BORDER)
                                        .setMarginBottom(24);

                        Cell celdaProv = new Cell().setBorder(Border.NO_BORDER);
                        celdaProv.add(new Paragraph("PROVEEDOR")
                                        .setFont(fontBold).setFontSize(8).setFontColor(COLOR_GRIS)
                                        .setCharacterSpacing(1));
                        celdaProv.add(new Paragraph(orden.getProveedor().getNombre())
                                        .setFont(fontBold).setFontSize(13).setFontColor(COLOR_NEGRO));

                        if (orden.getProveedor().getTelefono() != null) {
                                celdaProv.add(new Paragraph(orden.getProveedor().getTelefono())
                                                .setFont(fontRegular).setFontSize(10).setFontColor(COLOR_GRIS));
                        }
                        if (orden.getProveedor().getCorreo() != null) {
                                celdaProv.add(new Paragraph(orden.getProveedor().getCorreo())
                                                .setFont(fontRegular).setFontSize(10).setFontColor(COLOR_GRIS));
                        }
                        if (orden.getProveedor().getRfc() != null) {
                                celdaProv.add(new Paragraph("RFC: " + orden.getProveedor().getRfc())
                                                .setFont(fontRegular).setFontSize(10).setFontColor(COLOR_GRIS));
                        }
                        seccionProv.addCell(celdaProv);
                        seccionProv.addCell(new Cell().setBorder(Border.NO_BORDER));
                        doc.add(seccionProv);

                        // ─── TABLA DE PRODUCTOS ───────────────────────────────────────────
                        Table tabla = new Table(
                                        UnitValue.createPercentArray(new float[] { 12, 30, 28, 15, 15 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setMarginBottom(16);

                        // Encabezados
                        String[] headers = { "SKU", "Producto / Variante",
                                        "Atributos", "Cantidad", "Costo Unit." };
                        for (String h : headers) {
                                tabla.addHeaderCell(
                                                new Cell().setBackgroundColor(COLOR_NEGRO)
                                                                .setBorder(Border.NO_BORDER)
                                                                .setPadding(8)
                                                                .add(new Paragraph(h)
                                                                                .setFont(fontBold).setFontSize(9)
                                                                                .setFontColor(ColorConstants.WHITE)
                                                                                .setTextAlignment(TextAlignment.LEFT)));
                        }

                        // Filas
                        List<DetalleOrdenCompra> detalles = orden.getDetalles();
                        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
                        BigDecimal total = BigDecimal.ZERO;
                        boolean par = false;

                        for (DetalleOrdenCompra d : detalles) {
                                DeviceRgb bg = par ? COLOR_GRIS_CLR : new DeviceRgb(255, 255, 255);
                                par = !par;

                                BigDecimal subtotal = d.getCostoUnitario()
                                                .multiply(BigDecimal.valueOf(d.getCantidad()));
                                total = total.add(subtotal);
                                
                                String atributosStr = d.getVariante().getAtributosMap()
                                                .entrySet().stream()
                                                .map(e -> e.getKey() + ": " + e.getValue())
                                                .collect(java.util.stream.Collectors.joining(", "));
                                String[] celdas = {
                                                d.getVariante().getSku(),
                                                d.getVariante().getProducto().getNombre(),
                                                atributosStr,
                                                String.valueOf(d.getCantidad()),
                                                fmt.format(d.getCostoUnitario()),
                                };

                                for (int i = 0; i < celdas.length; i++) {
                                        Cell c = new Cell()
                                                        .setBackgroundColor(bg)
                                                        .setBorder(Border.NO_BORDER)
                                                        .setBorderBottom(new SolidBorder(COLOR_BORDE, 0.5f))
                                                        .setPadding(7);

                                        Paragraph p = new Paragraph(celdas[i])
                                                        .setFont(i == 1 ? fontBold : fontRegular)
                                                        .setFontSize(9)
                                                        .setFontColor(COLOR_NEGRO);

                                        if (i >= 4)
                                                p.setTextAlignment(TextAlignment.RIGHT);
                                        c.add(p);
                                        tabla.addCell(c);
                                }
                        }

                        doc.add(tabla);

                        // ─── TOTAL ────────────────────────────────────────────────────────
                        Table tablaTotal = new Table(
                                        UnitValue.createPercentArray(new float[] { 70, 30 }))
                                        .setWidth(UnitValue.createPercentValue(100))
                                        .setMarginBottom(32);

                        tablaTotal.addCell(new Cell().setBorder(Border.NO_BORDER));
                        Cell celdaTotal = new Cell()
                                        .setBackgroundColor(COLOR_NEGRO)
                                        .setBorder(Border.NO_BORDER)
                                        .setPadding(10);
                        celdaTotal.add(new Paragraph("TOTAL")
                                        .setFont(fontBold).setFontSize(8)
                                        .setFontColor(new DeviceRgb(180, 180, 180))
                                        .setCharacterSpacing(1));
                        celdaTotal.add(new Paragraph(fmt.format(total))
                                        .setFont(fontBold).setFontSize(16)
                                        .setFontColor(ColorConstants.WHITE)
                                        .setTextAlignment(TextAlignment.RIGHT));
                        tablaTotal.addCell(celdaTotal);
                        doc.add(tablaTotal);

                        // ─── NOTAS / PIE ──────────────────────────────────────────────────
                        doc.add(new Paragraph(
                                        "Este documento es una orden de compra oficial de DAW Store. " +
                                                        "Favor de confirmar disponibilidad y fecha de entrega.")
                                        .setFont(fontRegular).setFontSize(9).setFontColor(COLOR_GRIS)
                                        .setMarginBottom(4));

                        doc.add(new Paragraph(
                                        "Generado el " + java.time.LocalDateTime.now().format(
                                                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                                        .setFont(fontRegular).setFontSize(8)
                                        .setFontColor(new DeviceRgb(180, 180, 180)));

                        doc.close();
                        return baos.toByteArray();

                } catch (Exception e) {
                        throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
                }
        }
}