package cm.enspd.voyago.booking.infrastructure;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import cm.enspd.voyago.booking.domain.Ticket;

/** Génère le billet PDF (OpenPDF) : une page par passager, avec QR code signé. */
@Component
public class TicketPdfRenderer {

	private static final Color PRIMARY = new Color(30, 58, 138);
	private static final Color ACCENT = new Color(20, 184, 166);
	private static final Color MUTED = new Color(100, 116, 139);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH'h'mm",
			Locale.FRENCH);

	public byte[] render(List<Ticket> tickets, Function<Ticket, byte[]> qrPng) {
		Document doc = new Document(PageSize.A5.rotate(), 28, 28, 24, 24);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(doc, out);
			doc.open();
			boolean first = true;
			for (Ticket t : tickets) {
				if (!first) {
					doc.newPage();
				}
				first = false;
				writeTicket(doc, t, qrPng.apply(t));
			}
			doc.close();
		} catch (DocumentException | IOException e) {
			throw new IllegalStateException("Génération du billet PDF impossible", e);
		}
		return out.toByteArray();
	}

	private void writeTicket(Document doc, Ticket t, byte[] qr) throws DocumentException, IOException {
		Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE);
		Font white = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
		Font whiteBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
		Font label = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
		Font value = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
		Font big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY);
		Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

		PdfPTable header = new PdfPTable(new float[] { 3, 2 });
		header.setWidthPercentage(100);
		PdfPCell left = new PdfPCell();
		left.addElement(new Paragraph("VoyaGo", brand));
		left.addElement(new Paragraph("Billet électronique · " + t.getMode().getLabel(), white));
		style(left, PRIMARY);
		PdfPCell right = new PdfPCell();
		Paragraph ref = new Paragraph("Réf. " + t.getBookingReference(), whiteBold);
		ref.setAlignment(Element.ALIGN_RIGHT);
		Paragraph code = new Paragraph("Billet " + t.getCode(), white);
		code.setAlignment(Element.ALIGN_RIGHT);
		right.addElement(ref);
		right.addElement(code);
		style(right, PRIMARY);
		header.addCell(left);
		header.addCell(right);
		doc.add(header);

		PdfPTable band = new PdfPTable(1);
		band.setWidthPercentage(100);
		PdfPCell bandCell = new PdfPCell(new Phrase(" "));
		bandCell.setFixedHeight(4);
		style(bandCell, ACCENT);
		band.addCell(bandCell);
		doc.add(band);

		PdfPTable body = new PdfPTable(new float[] { 3, 1.4f });
		body.setWidthPercentage(100);
		body.setSpacingBefore(12);

		PdfPCell info = new PdfPCell();
		info.setBorder(Rectangle.NO_BORDER);
		info.addElement(new Paragraph(pdfText(t.getTripLabel()), big));
		PdfPTable details = new PdfPTable(2);
		details.setWidthPercentage(100);
		details.setSpacingBefore(8);
		addField(details, "DÉPART", t.getFromStation(), label, value);
		addField(details, "ARRIVÉE", t.getToStation(), label, value);
		addField(details, "DATE ET HEURE", DATE.format(t.getDepartureAt()), label, value);
		addField(details, "COMPAGNIE", t.getCompanyName(), label, value);
		addField(details, "PASSAGER", t.getPassengerName(), label, value);
		addField(details, "PIÈCE D'IDENTITÉ", t.getPassengerIdNumber(), label, value);
		addField(details, "SIÈGE", String.valueOf(t.getSeatNumber()), label, value);
		addField(details, "CLASSE", t.getServiceClassLabel(), label, value);
		addField(details, "PRIX", String.format(Locale.FRENCH, "%,d FCFA", t.getPrice()), label, value);
		addField(details, "STATUT", t.getStatus().getLabel(), label, value);
		info.addElement(details);
		body.addCell(info);

		PdfPCell qrCell = new PdfPCell();
		qrCell.setBorder(Rectangle.NO_BORDER);
		Image img = Image.getInstance(qr);
		img.scaleToFit(140, 140);
		img.setAlignment(Element.ALIGN_CENTER);
		qrCell.addElement(img);
		Paragraph scan = new Paragraph("À présenter à l'embarquement", small);
		scan.setAlignment(Element.ALIGN_CENTER);
		qrCell.addElement(scan);
		body.addCell(qrCell);
		doc.add(body);

		Paragraph footer = new Paragraph("Billet nominatif valable une seule fois, sur présentation d'une pièce d'identité. "
				+ "Le QR code est signé numériquement : toute modification le rend invalide.", small);
		footer.setSpacingBefore(10);
		doc.add(footer);
	}

	private static void addField(PdfPTable table, String name, String content, Font label, Font value) {
		PdfPCell cell = new PdfPCell();
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPaddingBottom(6);
		cell.addElement(new Paragraph(name, label));
		cell.addElement(new Paragraph(pdfText(content == null ? "-" : content), value));
		table.addCell(cell);
	}

	private static void style(PdfPCell cell, Color background) {
		cell.setBackgroundColor(background);
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPadding(10);
	}

	/** Les polices PDF standard (WinAnsi) n'ont pas la flèche « → ». */
	private static String pdfText(String s) {
		return s.replace("→", "-");
	}
}
