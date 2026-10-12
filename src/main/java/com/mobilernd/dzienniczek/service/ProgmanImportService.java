package com.mobilernd.dzienniczek.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilernd.dzienniczek.config.ProgmanProperties;
import com.mobilernd.dzienniczek.model.FoodEntry;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class ProgmanImportService {

    private static final String LOGIN_URL =
            "https://iprzedszkole.progman.pl/iprzedszkole/Authentication/Login.aspx";

    private static final String API_URL =
            "https://iprzedszkole.progman.pl/iprzedszkole/Pages/PanelRodzica/Jadlospis/ws_JadlospisPr.asmx/pobierzListe";

    private final FoodEntryService foodEntryService;
    private final ProgmanProperties properties;

    private final ObjectMapper mapper =
            new ObjectMapper();

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public int importFromDate(LocalDate fromDate) {

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        int imported = 0;

        try {

            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            String loginPage =
                    client.send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create(LOGIN_URL))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString()
                    ).body();

            Document document = Jsoup.parse(loginPage);

            String viewState =
                    document.getElementById("__VIEWSTATE")
                            .attr("value");

            String eventValidation =
                    document.getElementById("__EVENTVALIDATION")
                            .attr("value");

            String viewStateGenerator =
                    document.getElementById("__VIEWSTATEGENERATOR")
                            .attr("value");

            String form =
                    "__VIEWSTATE=" + encode(viewState)
                            + "&__VIEWSTATEGENERATOR=" + encode(viewStateGenerator)
                            + "&__EVENTVALIDATION=" + encode(eventValidation)
                            + "&ctl00$cphContent$txtDatabase=" + encode(properties.getUnit())
                            + "&ctl00$cphContent$txtLogin=" + encode(properties.getUsername())
                            + "&ctl00$cphContent$txtPassword=" + encode(properties.getPassword())
                            + "&ctl00$cphContent$ButtonLogin=Zaloguj";

            client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(LOGIN_URL))
                            .header(
                                    "Content-Type",
                                    "application/x-www-form-urlencoded"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(form)
                            )
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            for (int week = 0; week > -500; week--) {

                String payload = """
                    {
                      "idDziecko":"%s",
                      "tydzien":%d
                    }
                    """.formatted(
                        properties.getChildId(),
                        week
                );

                HttpResponse<String> response =
                        client.send(
                                HttpRequest.newBuilder()
                                        .uri(URI.create(API_URL))
                                        .header(
                                                "Content-Type",
                                                "application/json; charset=UTF-8"
                                        )
                                        .header(
                                                "X-Requested-With",
                                                "XMLHttpRequest"
                                        )
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(payload)
                                        )
                                        .build(),
                                HttpResponse.BodyHandlers.ofString()
                        );

                JsonNode root =
                        mapper.readTree(response.body());

                JsonNode days =
                        root.path("d")
                                .path("ListK");

                if (days.isEmpty()) {

                    System.out.println(
                            "Koniec dostępnych jadłospisów dla week="
                                    + week
                    );

                    break;
                }

                boolean importedAnythingThisWeek = false;

                for (JsonNode day : days) {

                    LocalDate date =
                            LocalDate.parse(
                                    day.path("dzien_data").asText(),
                                    formatter
                            );

                    /*
                     * Pomijamy dni starsze niż data graniczna,
                     * ale NIE przerywamy całego tygodnia.
                     */
                    if (date.isBefore(fromDate)) {
                        continue;
                    }

                    importedAnythingThisWeek = true;

                    String dayName =
                            capitalize(
                                    day.path("dzien_tyg").asText()
                            );

                    for (JsonNode meal : day.path("posilki")) {

                        FoodEntry entry =
                                new FoodEntry();

                        entry.setDate(date);

                        entry.setDayName(dayName);

                        entry.setMealName(
                                meal.path("Nazwa").asText()
                        );

                        entry.setDescription(
                                meal.path("Opis").asText()
                        );

                        entry.setCalories(0);

                        entry.setMealType(
                                resolveMealType(
                                        meal.path("Nazwa").asText()
                                )
                        );

                        if (foodEntryService.isMealUniqueForDay(entry)) {

                            foodEntryService.save(entry);

                            imported++;
                        }
                    }
                }

                /*
                 * Jeśli w całym tygodniu nie było ani jednego dnia
                 * >= fromDate, kończymy import.
                 */
                if (!importedAnythingThisWeek) {

                    System.out.println(
                            "Osiągnięto datę graniczną: "
                                    + fromDate
                    );

                    break;
                }
            }

            return imported;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Błąd importu iOpłatyEdu",
                    e
            );
        }
    }

    public int importWeeks(int weeks) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        int imported = 0;
        try {
            HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager)
                    .followRedirects(HttpClient.Redirect.ALWAYS).build();

            String loginPage =
                    client.send(HttpRequest.newBuilder()
                                            .uri(URI.create(LOGIN_URL))
                                            .GET()
                                            .build(),
                                    HttpResponse.BodyHandlers.ofString()
                            )
                            .body();

            Document document = Jsoup.parse(loginPage);

            String viewState = document.getElementById("__VIEWSTATE").attr("value");

            String eventValidation = document.getElementById("__EVENTVALIDATION").attr("value");

            String viewStateGenerator = document.getElementById("__VIEWSTATEGENERATOR").attr("value");

            String form =
                    "__VIEWSTATE=" + encode(viewState)
                            + "&__VIEWSTATEGENERATOR=" + encode(viewStateGenerator)
                            + "&__EVENTVALIDATION=" + encode(eventValidation)
                            + "&ctl00$cphContent$txtDatabase=" + encode(properties.getUnit())
                            + "&ctl00$cphContent$txtLogin=" + encode(properties.getUsername())
                            + "&ctl00$cphContent$txtPassword=" + encode(properties.getPassword())
                            + "&ctl00$cphContent$ButtonLogin=Zaloguj";

            client.send(HttpRequest.newBuilder()
                            .uri(URI.create(LOGIN_URL))
                            .header(
                                    "Content-Type",
                                    "application/x-www-form-urlencoded"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(form)
                            )
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            for (int week = 0; week > -weeks; week--) {

                String payload = """
                        {
                          "idDziecko":"%s",
                          "tydzien":%d
                        }
                        """.formatted(
                        properties.getChildId(),
                        week
                );

                HttpResponse<String> response =
                        client.send(
                                HttpRequest.newBuilder()
                                        .uri(
                                                URI.create(API_URL)
                                        )
                                        .header(
                                                "Content-Type",
                                                "application/json; charset=UTF-8"
                                        )
                                        .header(
                                                "X-Requested-With",
                                                "XMLHttpRequest"
                                        )
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(payload)
                                        )
                                        .build(),
                                HttpResponse.BodyHandlers.ofString()
                        );
                System.out.println("========== API RESPONSE ==========");
                System.out.println(response.body());
                System.out.println("==================================");
                JsonNode root = mapper.readTree(response.body());

                JsonNode days = root.path("d").path("ListK");

                for (JsonNode day : days) {
                    LocalDate date = LocalDate.parse(day.path("dzien_data").asText(), formatter);
                    String dayName = capitalize(day.path("dzien_tyg").asText());

                    for (JsonNode meal : day.path("posilki")) {
                        FoodEntry entry = new FoodEntry();
                        entry.setDate(date);
                        entry.setDayName(dayName);
                        entry.setMealName(meal.path("Nazwa").asText());
                        entry.setDescription(meal.path("Opis").asText());
                        entry.setCalories(0);
                        entry.setMealType(resolveMealType(meal.path("Nazwa").asText()));

                        if (foodEntryService.isMealUniqueForDay(entry)) {
                            foodEntryService.save(entry);
                            imported++;
                        }
                    }
                }
            }

            return imported;

        } catch (Exception e) {
            throw new RuntimeException("Błąd importu iOpłatyEdu", e);
        }
    }

    private String resolveMealType(String mealName) {

        return switch (mealName) {

            case "Śniadanie" -> "śniadanie";
            case "Obiad" -> "obiad";
            case "Podwieczorek" -> "przekąska";

            default -> "inne";
        };
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }
}