package com.mobilernd.dzienniczek.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.svg.converter.SvgConverter;
import com.mobilernd.dzienniczek.model.FoodEntry;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PdfService {

    public byte[] generateFoodEntriesPdf(List<FoodEntry> entries) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // 🔥 CZCIONKA PL
            InputStream fontStream = getClass().getResourceAsStream("/static/fonts/NotoSans-Regular.ttf");
            PdfFont font = PdfFontFactory.createFont(
                    fontStream.readAllBytes(),
                    PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );
            document.setFont(font);

            // 🔥 Nagłówek PDF — BEZ „Premium”
            Paragraph header = new Paragraph("Dzienniczek żywieniowy")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(new DeviceRgb(44, 62, 80))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(25);

            document.add(header);

            // ⭐ GRUPOWANIE PO DACIE + SORTOWANIE MALEJĄCO
            Map<LocalDate, List<FoodEntry>> entriesByDate = entries.stream()
                    .collect(Collectors.groupingBy(FoodEntry::getDate))
                    .entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<FoodEntry>>comparingByKey().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            // ⭐ GENEROWANIE PDF Z GRUPOWANIEM
            for (Map.Entry<LocalDate, List<FoodEntry>> day : entriesByDate.entrySet()) {

                FoodEntry first = day.getValue().get(0);

                // 🔥 Nagłówek dnia: DATA — DZIEŃ TYGODNIA
                Paragraph dayHeader = new Paragraph(
                        day.getKey() + " — " + first.getDayName()
                )
                        .setFontSize(18)
                        .setBold()
                        .setFontColor(new DeviceRgb(52, 73, 94))
                        .setMarginTop(20)
                        .setMarginBottom(10);

                document.add(dayHeader);

                // 🔥 Wpisy dla dnia
                for (FoodEntry entry : day.getValue()) {

                    Table card = new Table(UnitValue.createPercentArray(new float[]{1, 6}))
                            .useAllAvailableWidth()
                            .setMarginTop(10);

                    // IKONA — TERAZ PO mealType (jak w index.html)
                    Cell iconCell = new Cell()
                            .setBorder(Border.NO_BORDER)
                            .setPadding(10)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setTextAlignment(TextAlignment.CENTER);

                    Image icon = loadSvgIconByMealType(entry.getMealType(), pdf);

                    if (icon != null) {
                        icon.setWidth(40);
                        icon.setHeight(40);
                        icon.setAutoScale(true);
                        iconCell.add(icon);
                    }

                    card.addCell(iconCell);

                    // TREŚĆ
                    Cell contentCell = new Cell()
                            .setBorder(Border.NO_BORDER)
                            .setPadding(14);

                    Paragraph title = new Paragraph(
                            entry.getMealName() + " — " + entry.getCalories() + " kcal"
                    )
                            .setFont(font)
                            .setFontSize(14)
                            .setBold()
                            .setFontColor(new DeviceRgb(44, 62, 80))
                            .setMarginBottom(8);

                    Paragraph desc = new Paragraph(entry.getDescription())
                            .setFont(font)
                            .setFontSize(11)
                            .setFontColor(new DeviceRgb(80, 80, 80));

                    contentCell.add(title);
                    contentCell.add(desc);

                    card.addCell(contentCell);

                    document.add(card);
                }

                // ⭐ NOWA STRONA PO KAŻDYM DNIU (oprócz ostatniego)
                if (!day.equals(entriesByDate.entrySet().toArray()[entriesByDate.size() - 1])) {
                    document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                }
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Błąd generowania PDF", e);
        }
    }

    // 🔥 Ikony SVG — TERAZ PO mealType (zgodnie z index.html)
    private Image loadSvgIconByMealType(String mealType, PdfDocument pdf) {
        try {
            String resourcePath = switch (mealType) {
                case "śniadanie" -> "/static/icons/breakfast.svg";
                case "obiad"     -> "/static/icons/lunch.svg";
                case "kolacja"   -> "/static/icons/dinner.svg";
                case "przekąska" -> "/static/icons/snack.svg";
                default          -> "/static/icons/other.svg";
            };

            InputStream svgStream = getClass().getResourceAsStream(resourcePath);

            if (svgStream == null) {
                System.out.println("Nie znaleziono ikony SVG dla mealType: " + mealType);
                return null;
            }

            return SvgConverter.convertToImage(svgStream, pdf);

        } catch (Exception e) {
            System.out.println("Błąd ładowania SVG dla mealType: " + mealType);
            return null;
        }
    }
}