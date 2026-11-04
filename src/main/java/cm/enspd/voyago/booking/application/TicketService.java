package cm.enspd.voyago.booking.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cm.enspd.voyago.auth.application.AppUserPrincipal;
import cm.enspd.voyago.booking.domain.Ticket;
import cm.enspd.voyago.booking.domain.TicketStatus;
import cm.enspd.voyago.booking.infrastructure.QrCodeGenerator;
import cm.enspd.voyago.booking.infrastructure.TicketPdfRenderer;
import cm.enspd.voyago.booking.infrastructure.TicketRepository;
import cm.enspd.voyago.shared.audit.AuditService;
import cm.enspd.voyago.shared.domain.NotFoundException;

/** Billets : QR code, PDF et contrôle à l'embarquement (F14, F15, RG6). */
@Service
public class TicketService {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

	private final TicketRepository tickets;
	private final QrCodeSigner signer;
	private final QrCodeGenerator qr;
	private final TicketPdfRenderer pdf;
	private final AuditService audit;

	public TicketService(TicketRepository tickets, QrCodeSigner signer, QrCodeGenerator qr, TicketPdfRenderer pdf,
			AuditService audit) {
		this.tickets = tickets;
		this.signer = signer;
		this.qr = qr;
		this.pdf = pdf;
		this.audit = audit;
	}

	public record ValidationResult(boolean valid, String message, Ticket ticket) {
	}

	/**
	 * Contrôle d'un billet. {@code input} est soit le contenu complet du QR code (signature vérifiée),
	 * soit le code du billet saisi à la main par l'agent.
	 */
	@Transactional
	public ValidationResult validate(String input, AppUserPrincipal controller) {
		if (input == null || input.isBlank()) {
			return new ValidationResult(false, "Aucun code fourni", null);
		}
		String code;
		if (signer.isSignedPayload(input.trim())) {
			Optional<String> verified = signer.verify(input);
			if (verified.isEmpty()) {
				audit.record("BILLET_FALSIFIE", input);
				return new ValidationResult(false, "QR code falsifié : signature invalide", null);
			}
			code = verified.get();
		} else {
			code = input.trim().toUpperCase();
		}

		Optional<Ticket> found = tickets.findByCode(code);
		if (found.isEmpty()) {
			return new ValidationResult(false, "Billet inconnu : " + code, null);
		}
		Ticket ticket = found.get();
		if (controller.isAgent() && !ticket.getCompanyId().equals(controller.companyId())) {
			return new ValidationResult(false, "Ce billet appartient à une autre compagnie (" + ticket.getCompanyName() + ")",
					ticket);
		}
		if (ticket.getStatus() == TicketStatus.USED) {
			return new ValidationResult(false, "Billet déjà utilisé le " + TIME.format(ticket.getUsedAt()), ticket);
		}
		if (ticket.getStatus() == TicketStatus.CANCELLED) {
			return new ValidationResult(false, "Billet annulé", ticket);
		}
		ticket.markUsed(LocalDateTime.now());
		audit.record("VALIDATION_BILLET", ticket.getCode() + " - " + ticket.getPassengerName() + " - siège "
				+ ticket.getSeatNumber());
		return new ValidationResult(true, "Billet valide : embarquement autorisé", ticket);
	}

	@Transactional(readOnly = true)
	public Ticket get(UUID id) {
		return tickets.findById(id).orElseThrow(() -> new NotFoundException("Billet"));
	}

	public byte[] qrPng(Ticket ticket, int size) {
		return qr.png(signer.payload(ticket.getCode()), size);
	}

	public String qrPayload(Ticket ticket) {
		return signer.payload(ticket.getCode());
	}

	public byte[] pdf(List<Ticket> list) {
		return pdf.render(list, t -> qrPng(t, 320));
	}
}
