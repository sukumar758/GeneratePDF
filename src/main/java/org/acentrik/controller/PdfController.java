package org.acentrik.controller;


import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import jakarta.servlet.http.HttpServletResponse;
import org.acentrik.model.FormData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class PdfController {

    @PostMapping("/generatePdf")
    public void downloadPDF(@ModelAttribute FormData formData, HttpServletResponse response) throws IOException, java.io.IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename="+formData.getFirstName()+formData.getLastName()+".pdf");

        try (OutputStream out = response.getOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont font= PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
            document.setFont(font);
            document.setFontSize(12);

            Paragraph paragraph = new Paragraph();
            paragraph.add(new Text("Acentrik Technology Solutions LLC\n")
                    .setBold()
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER));
            paragraph.add(new Text("Passion, Innovation & Trust")
                    .setBold()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(paragraph.setTextAlignment(TextAlignment.CENTER));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy");
            String formattedJoiningDate = formData.getJoiningDate().format(formatter);

            LocalDate today = LocalDate.now();
            String formattedToday = today.format(formatter);

            document.add(new Paragraph(formattedToday)
                    .setTextAlignment(TextAlignment.LEFT));

            document.add(new Paragraph("Offer of Internship")
                    .setBold()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.LEFT));

            document.add(new Paragraph("Dear " + formData.getFirstName() + " " + formData.getLastName() + ","));

            document.add(new Paragraph("It's my pleasure to offer you a " + formData.getRole() + "  role at Acentrik. Your internship will be from " + formattedJoiningDate+ ".").setTextAlignment(TextAlignment.JUSTIFIED));

            document.add(new Paragraph("On behalf of Acentrik Technology Solutions, LLC, 4425 W Airport fwy, Suite 117, Irving, TX-75062, we are pleased to welcome you. You will report to " + formData.getManager() + " (Reporting Manager) during your internship period with us.").setTextAlignment(TextAlignment.JUSTIFIED));

            document.add(new Paragraph("Your Job responsibilities include:")
                    .setBold());

            document.add(new Paragraph(
                    "\t• Conduct business process analysis to understand existing workflow and systems, document current processes and systems.\n" +
                            "\t• Create requirements for new processes, develop use cases and manage requirement changes.\n" +
                            "\t• Have strong technical acumen with the ability to translate information and research into non-technical language as necessary to effectively communicate across teams.\n" +
                            "\t• Gather, summarize, and verify information used to populate reports and deliverables.\n" +
                            "\t• Facilitate requirement reviews with stakeholders, perform QA testing and conduct UAT with business/process teams.\n" +
                            "\t• Maintain documentation related to CMDB processes, procedures, and configurations."
            ).setTextAlignment(TextAlignment.JUSTIFIED));


            document.add(new Paragraph()
                    .add(new Text("Company Agreements: ").setBold())
                    .add("For the purpose of Federal Immigration Law, you will be required to provide the evidence of your identity and eligibility for internship in the United States. Such documentation must be provided to us within three business days of your date of hire with Acentrik Technology Solutions, LLC, or your internship may be terminated.").setTextAlignment(TextAlignment.JUSTIFIED)
            );

            document.add(new Paragraph()
                    .add(new Text("At-Will Employment: ").setBold())
                    .add("If you accept this offer, you understand and agree that your employment with the Company is for no specified period and constitutes \"at-will\" employment. As a result, you will be free to resign at any time or for any reason or no reason. The company will similarly have the right to end its employment relationship with you at any time, with or without notice and with or without cause. You understand and agree that any representation to the contrary is unauthorized and not valid unless obtained, written, and signed by the company manager.").setTextAlignment(TextAlignment.JUSTIFIED)
            );
            document.add(new Paragraph("If you have any questions or need further information, please feel free to contact me at 972-799-6164 or kishore.medikonda@acentriktech.com. We look forward to seeing you and we offer you a very warm welcome.").setTextAlignment(TextAlignment.JUSTIFIED));

            document.add(new Paragraph("Sincerely,"));

            document.add(new Paragraph("Kishore Medikonda\nHR Director\nAcentrik Technology Solutions LLC"));

            document.close();
        }
    }
}