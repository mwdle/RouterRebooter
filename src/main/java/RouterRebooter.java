import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.time.StopWatch;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {
        int port = 80;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/", new MyHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
        try {
            System.out.println("Press Enter to stop the server.");
            System.in.read();
        }
        catch (Exception ignored) {}
    }

    private static void restartRouter() {
        // Grab username and password secrets from the secret file.
        String username = System.getenv("routerUsername");
        String password = System.getenv("routerPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-web-security");
        Configuration.browserCapabilities = chromeOptions;

        try {
            open("http://192.168.0.1/?util_restart");
        }
        catch (Exception e) {
            open("http://192.168.0.1/?util_restart");
        }

        StopWatch timer = new StopWatch();
        timer.start();
        // Wait until the login screen appears to do anything
        while (!$(By.linkText("Login")).exists()) {
            Selenide.sleep(200);
            if (timer.getTime(TimeUnit.SECONDS) > 60)
                break;
        }
        timer.stop();
        timer.reset();

        // Login to the restart utility
        $(By.id("UserName")).setValue(username);
        $(By.id("Password")).setValue(password);
        $(By.className("submitBtn")).click();

            Selenide.sleep(2000);

        // Dismiss the retarded bullshit ads that popup upon login
        if ($(By.id("doNotShow")).exists()) {
            $(By.id("doNotShow")).click();
            $(By.className("ui-button")).click();
        }

        /*
          The following for loop finds the IP address of the gateway for the TP-Link extender before restarting the router, so that the extender can also be restarted after.
         */
        String tpLinkIP = "";
        for (int i = 2; i < 254; i++) {
            String address = "192.168.0." + i;
            try {
                URL url = new URL("http://" + address);
                // Open a connection to the URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(5000);
                // Get the HTTP response code
                int responseCode = connection.getResponseCode();
                // Check if the website is reachable (HTTP 2xx response code)
                if (responseCode >= 200 && responseCode < 300) {
                    StringBuilder responseContent = new StringBuilder();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseContent.append(line);
                    }
                    if (responseContent.toString().contains("tp-link")) {
                        tpLinkIP = address;
                        connection.disconnect();
                        break;
                    }
                }
                // Close the connection
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //         Click the button to restart the router
        $(By.className("submitBtn")).shouldBe(interactable).click();

        // Accept the javascript prompt alert
        Selenide.switchTo().alert().accept();

        // Sleep for 10s before exiting to ensure the request had time to go through
        Selenide.sleep(10000);

        Selenide.closeWebDriver();

        if (!tpLinkIP.equals("")) {
            open("http://" + tpLinkIP + "/");
        }

        timer.start();
        while (!$(By.className("password-text")).exists()) {
            Selenide.sleep(200);
            if (timer.getTime(TimeUnit.SECONDS) > 30)
                break;
        }
        timer.stop();
        timer.reset();

        $(By.className("password-text")).shouldBe(interactable).setValue(System.getenv("tplinkPassword"));
        $(By.id("login-btn")).shouldBe(interactable).click();

        timer.start();
        while (!$(By.id("map_router")).exists()) {
            Selenide.sleep(200);
            if (timer.getTime(TimeUnit.SECONDS) > 10)
                break;
        }
        timer.stop();
        timer.reset();

        $(By.id("top-control-reboot")).shouldBe(interactable).click();

        $(By.className("msg-btn-container")).find(By.className("btn-msg-ok")).shouldBe(interactable).click();

        // Sleep for 5 seconds before closing the webdriver and ending the script.
        Selenide.sleep(5000);
        Selenide.closeWebDriver();
        System.out.println("Script completed successfully");
    }

    private static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            String requestType = exchange.getRequestMethod();
            String requestUrl = exchange.getRequestURI().toString();
            String requestData = readRequestBody(exchange.getRequestBody());
            if ((requestType.equals("POST") && requestData.equals(System.getenv("requestToken")) && requestUrl.equals("/rebootRouter/executeAs/MikeWad/Token/" + System.getenv("URIToken")))) {
                response = "Authentication succeeded. Executing router reboot...";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                restartRouter();
                System.out.println("Router and tp-link extender should have rebooted");
            } else {
                response = "Page does not exist";
                exchange.sendResponseHeaders(403, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
                System.out.println("Rejected Request with type: '" + requestType + "'   |   Request URL: '" + requestUrl + "'   |   Request Body: " + requestData);
            }
        }
    }
    private static String readRequestBody(InputStream inputStream) throws IOException {
        StringBuilder requestData = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            requestData.append(new String(buffer, 0, bytesRead));
        }
        return requestData.toString();
    }
}