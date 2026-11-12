package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.ReceiptScanUnavailableException;
import be.cohabitapi.cohabitapi.Models.Item;
import be.cohabitapi.cohabitapi.Models.ReceiptScan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// The only class that talks to the AI
// It is used like a DAO: @ApplicationScoped, injected in the API and given to the model as a parameter
// It only asks the question and reads the answer: the rules (which ids are kept, which total is valid) stay in the model
@ApplicationScoped
public class ReceiptScanner {

    // The API of Groq uses the same format as the API of OpenAI
    private static final String AI_URL = "https://api.groq.com/openai/v1/chat/completions";

    // After 20 seconds without an answer, we give up
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    // The technical reason of a failure is written in the log of the server, never in the response
    private static final Logger LOGGER = Logger.getLogger(ReceiptScanner.class.getName());

    // HttpClient is in the JDK (java.net.http), so there is no new dependency
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    // ObjectMapper (Jackson, already used by Jersey) writes and reads the JSON
    private final ObjectMapper mapper = new ObjectMapper();


    // Sends the photo of the receipt and the items still to buy to the AI
    // Returns what the AI read: the ids of the items found on the receipt and the total paid
    // Throws ReceiptScanUnavailableException for every problem: no key, no network, timeout, quota, answer that cannot be read
    public ReceiptScan scan(String image, List<Item> items) {
        // The key and the model are read from the environment, like the identifiers of the db (see JpaUtil)
        String key = System.getenv("ai_key");
        String model = System.getenv("ai_model");

        if (key == null || key.isBlank() || model == null || model.isBlank())
            throw unavailable("the environment variables ai_key and ai_model are required");

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(AI_URL))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + key.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildBody(model.trim(), image, items)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 429 = quota exceeded, 401 = wrong key, 5xx = the AI is down... the reaction is the same for all of them
            if (response.statusCode() != 200)
                throw unavailable("the AI answered HTTP " + response.statusCode() + " " + response.body());

            return readAnswer(response.body());
        }
        catch (IOException e) {
            // No network, timeout, or an answer that is not JSON (JsonProcessingException is an IOException)
            throw unavailable(e.toString());
        }
        catch (InterruptedException e) {
            // The thread was stopped while it was waiting for the AI: we keep the signal and give up
            Thread.currentThread().interrupt();
            throw unavailable(e.toString());
        }
    }

    // The body of the request, in the format of OpenAI: one message with the question and the photo
    private String buildBody(String model, String image, List<Item> items) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);

        // json_object: the AI must answer with a JSON object and nothing else
        body.putObject("response_format").put("type", "json_object");

        ObjectNode message = body.putArray("messages").addObject();
        message.put("role", "user");

        // The content has two parts: the question (text), then the photo (a "data:" URL with the base64 inside)
        ArrayNode content = message.putArray("content");

        ObjectNode question = content.addObject();
        question.put("type", "text");
        question.put("text", buildQuestion(items));

        ObjectNode photo = content.addObject();
        photo.put("type", "image_url");
        photo.putObject("image_url").put("url", "data:image/jpeg;base64," + image);

        return mapper.writeValueAsString(body);
    }

    // The question for the AI: what to look for, the items still to buy (id + name) and the format of the answer
    private String buildQuestion(List<Item> items) {
        StringBuilder question = new StringBuilder();

        question.append("This is the photo of a receipt from a shop.\n");
        question.append("Here are the items of a shopping list that are still to buy, one per line (id: name):\n");

        for (Item item : items) {
            question.append(item.getProduct().getId())
                    .append(": ")
                    .append(item.getProduct().getName())
                    .append("\n");
        }

        question.append("Find the items of this list that were bought on the receipt. ");
        question.append("The receipt can use abbreviations or another language than the list.\n");
        question.append("Also read the total amount paid.\n");
        question.append("Answer only with a JSON object like {\"itemIds\": [1, 4], \"total\": 23.45}.\n");
        question.append("\"itemIds\" only contains ids of the list above, and it is empty if no item was bought.\n");
        question.append("\"total\" is a number, or null if you cannot read it.");

        return question.toString();
    }

    // Reads the response: the answer of the AI is a JSON text inside choices[0].message.content
    private ReceiptScan readAnswer(String responseBody) throws IOException {
        String content = mapper.readTree(responseBody)
                .path("choices").path(0).path("message").path("content").asText();

        JsonNode answer = mapper.readTree(content);
        JsonNode ids = answer.path("itemIds");

        // Not the JSON object we asked for: the answer cannot be used
        if (!ids.isArray())
            throw unavailable("the answer of the AI cannot be read: " + content);

        // Only the whole numbers are ids
        List<Integer> itemIds = new ArrayList<>();
        for (JsonNode id : ids) {
            if (id.isInt())
                itemIds.add(id.intValue());
        }

        // A total that is not a number is left empty
        Double total = null;
        if (answer.path("total").isNumber())
            total = answer.path("total").doubleValue();

        return new ReceiptScan(itemIds, total);
    }

    // Writes the technical reason in the log of the server and builds the exception
    // The exception has no detail: its message is the one sent to the front
    private ReceiptScanUnavailableException unavailable(String reason) {
        LOGGER.warning("Receipt scan unavailable: " + reason);
        return new ReceiptScanUnavailableException();
    }
}
