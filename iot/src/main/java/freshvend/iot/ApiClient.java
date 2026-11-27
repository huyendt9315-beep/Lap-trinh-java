package freshvend.iot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Gọi REST API của nhóm với Bearer token; tự làm mới token một lần khi gặp 401. */
public final class ApiClient {
  public static final class ApiException extends Exception {
    public final int status;
    public final boolean retryable;

    public ApiException(int status, String msg, boolean retryable) {
      super(msg);
      this.status = status;
      this.retryable = retryable;
    }
  }

  public record Response(int status, String body) {}

  private final Config cfg;
  private final HttpClient http;
  private final TokenProvider tokens;

  public ApiClient(Config cfg) {
    this.cfg = cfg;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(cfg.httpTimeoutMs)).build();
    this.tokens = new TokenProvider(cfg, http);
  }

  public TokenProvider tokens() { return tokens; }

  public String machinePath(String suffix) { return "/machines/" + cfg.machineId + suffix; }

  public Response get(String path) throws ApiException { return send("GET", path, null); }

  public Response post(String path, String json) throws ApiException { return send("POST", path, json); }

  private Response send(String method, String path, String json) throws ApiException {
    for (int attempt = 0; ; attempt++) {
      String token = tokens.get();
      try {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(cfg.backendUrl + path))
            .timeout(Duration.ofMillis(cfg.httpTimeoutMs))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json");
        if ("POST".equals(method)) {
          b.header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        } else {
          b.GET();
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        int s = r.statusCode();
        if (s == 401 && attempt == 0 && cfg.usesOAuth()) { tokens.invalidate(); continue; }
        if (s / 100 == 2) return new Response(s, r.body());
        boolean retry = s == 408 || s == 429 || s >= 500 || s == 401 || s == 403;
        throw new ApiException(s, "HTTP " + s + " " + r.body(), retry);
      } catch (ApiException e) {
        throw e;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ApiException(0, "bị ngắt", true);
      } catch (Exception e) {
        throw new ApiException(0, e.getClass().getSimpleName() + ": " + e.getMessage(), true);
      }
    }
  }
}
