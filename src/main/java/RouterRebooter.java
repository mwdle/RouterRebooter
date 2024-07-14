import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    public static void writeToLogFile(String log, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(log);
        } catch (Exception ignored) {
        }
    }

    public static void rebootRouter() {
        // Grab password environment variables.
        String routerPassword = System.getenv("routerPassword");
        String extenderPassword = System.getenv("extenderPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito", "--headless", "--no-sandbox", "--disable-gpu", "--start-maximized", "--window-size=1920,1080");
        chromeOptions.setBinary("/usr/bin/google-chrome");
        Configuration.browserCapabilities = chromeOptions;

         /*
          Finds the IP address of the TP-Link extender (IP is not static).
         */
        String tpLinkIP = "";
        for (int i = 2; i < 254; i++) {
            String address = "192.168.0." + i;
            try {
                URL url = new URL("http://" + address);
                // Open a connection to the URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(500);
                connection.setReadTimeout(5000);
                // Get the HTTP response code
                int responseCode = connection.getResponseCode();
                // Check if the website is reachable (HTTP 2xx response code)
                if (responseCode >= 200 && responseCode < 300) {
                    StringBuilder responseContent = new StringBuilder();
                    InputStreamReader inReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader in = new BufferedReader(inReader);
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseContent.append(line);
                    }
                    in.close();
                    inReader.close();
                    if (responseContent.toString().contains("tp-link")) {
                        tpLinkIP = address;
                        connection.disconnect();
                        break;
                    }
                }
                // Close the connection
                connection.disconnect();
            } catch (Exception ignored) {
            }
        }

        // Restart the extender
        open("http://" + tpLinkIP);
        $(By.className("password-text")).shouldBe(interactable, Duration.ofSeconds(20)).sendKeys(extenderPassword);
        $(By.id("login-btn")).shouldBe(interactable, Duration.ofSeconds(10)).click();
        $(By.id("top-control-reboot")).shouldBe(interactable, Duration.ofSeconds(20)).click();
        $(By.className("msg-btn-container")).should(exist).find(By.className("btn-msg-ok")).shouldBe(interactable).click();

        // Sleep for 5s to ensure the request had time to go through.
        Selenide.sleep(5);

        // Restart the router
        open("https://192.168.0.1/cgi-bin/luci/admin/troubleshooting/restart");
        $(By.name("luci_password")).should(interactable, Duration.ofSeconds(30)).setValue(routerPassword);
        $(By.id("loginbtn")).shouldBe(interactable).click();
        $(By.partialLinkText("RESTART GATEWAY")).shouldBe(interactable, Duration.ofSeconds(10)).click();
        Selenide.switchTo().alert().accept();

        // Sleep for 5s to ensure the request had time to go through.
        Selenide.sleep(5000);

        Selenide.closeWebDriver();
    }

    public static void main(String[] args) {
        try {
            rebootRouter();
            writeToLogFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Reboot Success", "/RouterRebooter/RouterRebooter.log");
        } catch (Throwable e) {
            writeToLogFile(e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()), "/RouterRebooter/lastFailure.log");
            writeToLogFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: check lastFailure.log", "/RouterRebooter/RouterRebooter.log");
        }
    }
}