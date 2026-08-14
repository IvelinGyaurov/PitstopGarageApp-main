package com.pitstop.garage.repair.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.model.UsedPart;
import com.pitstop.garage.user.model.User;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class RepairInvoicePdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Color LINE = new Color(200, 200, 200);
    private static final Color HEADER_BG = new Color(245, 245, 245);

    private final MessageSource messageSource;

    public RepairInvoicePdfService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public byte[] generate(ServiceRepair repair, Locale locale) {
        Locale lang = locale == null ? Locale.ENGLISH : locale;
        try {
            BaseFont regular = loadFont("fonts/DejaVuSans.ttf");
            BaseFont bold = loadFont("fonts/DejaVuSans-Bold.ttf");

            Font titleFont = new Font(bold, 16);
            Font headingFont = new Font(bold, 11);
            Font labelFont = new Font(bold, 9);
            Font bodyFont = new Font(regular, 9);
            Font smallFont = new Font(regular, 8);

            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, output);
            document.open();

            String invoiceNo = repair.getId().toString().substring(0, 8);
            String invoiceDate = formatDate(repair.getCompletedAt());

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1.2f, 1f});
            header.addCell(leftCell(new Phrase("PitStop Garage", titleFont)));
            PdfPCell right = leftCell(new Phrase(msg("invoice.title", lang), titleFont));
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            header.addCell(right);
            header.addCell(leftCell(new Phrase(msg("invoice.subtitle", lang), smallFont)));
            PdfPCell meta = leftCell(new Phrase(
                    msg("invoice.number", lang) + " " + invoiceNo + "\n"
                            + msg("invoice.date", lang) + " " + invoiceDate,
                    bodyFont));
            meta.setHorizontalAlignment(Element.ALIGN_RIGHT);
            header.addCell(meta);
            document.add(header);
            document.add(spacer(10));

            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setWidths(new float[]{1f, 1f});
            parties.addCell(boxCell(msg("invoice.billTo", lang), clientBlock(repair.getClient()), headingFont, bodyFont));
            parties.addCell(boxCell(msg("invoice.vehicle", lang), vehicleBlock(repair.getCar()), headingFont, bodyFont));
            document.add(parties);
            document.add(spacer(8));

            document.add(labeledLine(msg("invoice.mechanic", lang), mechanicName(repair.getMechanic()), labelFont, bodyFont));
            document.add(labeledLine(msg("invoice.timeline", lang), timeline(repair, lang), labelFont, bodyFont));
            document.add(spacer(4));
            document.add(new Paragraph(msg("invoice.problem", lang), headingFont));
            Paragraph problem = new Paragraph(
                    repair.getProblemDescription() == null ? "-" : repair.getProblemDescription(),
                    bodyFont);
            problem.setSpacingAfter(10f);
            document.add(problem);

            document.add(new Paragraph(msg("invoice.parts", lang), headingFont));
            document.add(spacer(4));

            BigDecimal partsTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (repair.getUsedParts() == null || repair.getUsedParts().isEmpty()) {
                document.add(new Paragraph(msg("invoice.noParts", lang), bodyFont));
            } else {
                PdfPTable parts = new PdfPTable(4);
                parts.setWidthPercentage(100);
                parts.setWidths(new float[]{3f, 1f, 1.4f, 1.4f});
                parts.addCell(headerCell(msg("invoice.part", lang), labelFont));
                parts.addCell(headerCell(msg("invoice.qty", lang), labelFont));
                parts.addCell(headerCell(msg("invoice.unitPrice", lang), labelFont));
                parts.addCell(headerCell(msg("invoice.lineTotal", lang), labelFont));

                for (UsedPart used : repair.getUsedParts()) {
                    BigDecimal line = used.getUnitPrice()
                            .multiply(BigDecimal.valueOf(used.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    partsTotal = partsTotal.add(line);
                    parts.addCell(bodyCell(used.getPartName(), bodyFont, Element.ALIGN_LEFT));
                    parts.addCell(bodyCell(String.valueOf(used.getQuantity()), bodyFont, Element.ALIGN_CENTER));
                    parts.addCell(bodyCell(money(used.getUnitPrice(), lang), bodyFont, Element.ALIGN_RIGHT));
                    parts.addCell(bodyCell(money(line, lang), bodyFont, Element.ALIGN_RIGHT));
                }
                document.add(parts);
            }

            document.add(spacer(12));
            BigDecimal labor = repair.getLaborCost() == null
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : repair.getLaborCost().setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = labor.add(partsTotal).setScale(2, RoundingMode.HALF_UP);

            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.addCell(bodyCell(msg("invoice.labor", lang), labelFont, Element.ALIGN_LEFT));
            totals.addCell(bodyCell(money(labor, lang), bodyFont, Element.ALIGN_RIGHT));
            totals.addCell(bodyCell(msg("invoice.total", lang), headingFont, Element.ALIGN_LEFT));
            totals.addCell(bodyCell(money(total, lang), headingFont, Element.ALIGN_RIGHT));
            document.add(totals);

            document.add(spacer(20));
            Paragraph footer = new Paragraph(msg("invoice.footer", lang), smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate invoice PDF", ex);
        }
    }

    private String msg(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }

    private String msg(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private BaseFont loadFont(String classpathLocation) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return BaseFont.createFont(
                classpathLocation,
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED,
                true,
                bytes,
                null);
    }

    private String clientBlock(User client) {
        StringBuilder sb = new StringBuilder();
        String fullName = fullName(client);
        if (!fullName.isBlank()) {
            sb.append(fullName).append('\n');
        }
        sb.append(client.getUsername()).append('\n');
        sb.append(client.getEmail());
        return sb.toString();
    }

    private String vehicleBlock(Car car) {
        StringBuilder sb = new StringBuilder();
        sb.append(car.getBrand()).append(' ').append(car.getModel());
        if (car.getYear() != null) {
            sb.append(' ').append(car.getYear());
        }
        sb.append('\n').append(car.getPlateNumber());
        sb.append('\n').append("VIN: ").append(car.getVin());
        return sb.toString();
    }

    private String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        return (first + " " + last).trim();
    }

    private String mechanicName(User mechanic) {
        return mechanic == null ? "-" : mechanic.getUsername();
    }

    private String timeline(ServiceRepair repair, Locale locale) {
        return msg("invoice.accepted", locale) + " " + formatDate(repair.getAcceptedAt())
                + "  ·  "
                + msg("invoice.started", locale) + " " + formatDate(repair.getStartedAt())
                + "  ·  "
                + msg("invoice.completed", locale) + " " + formatDate(repair.getCompletedAt());
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME.format(value);
    }

    private String money(BigDecimal amount, Locale locale) {
        return msg("common.eur", locale, amount.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private Paragraph labeledLine(String label, String value, Font labelFont, Font bodyFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(label + ": ", labelFont));
        phrase.add(new Phrase(value, bodyFont));
        Paragraph paragraph = new Paragraph(phrase);
        paragraph.setSpacingAfter(3f);
        return paragraph;
    }

    private Paragraph spacer(float points) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(points);
        return paragraph;
    }

    private PdfPCell leftCell(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell boxCell(String title, String body, Font titleFont, Font bodyFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(title + "\n", titleFont));
        phrase.add(new Phrase(body, bodyFont));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorderColor(LINE);
        cell.setPadding(8f);
        return cell;
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
