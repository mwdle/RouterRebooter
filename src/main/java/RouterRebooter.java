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

import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    public static void main(String[] args) {
        try {
            rebootRouter();
            writeToLogFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Reboot Success", "/RouterRebooter/Rebooter.log");
        }
        catch (Throwable e) {
            writeToLogFile(e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()), "/RouterRebooter/lastFailure.log");
            writeToLogFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: check lastFailure.log", "/RouterRebooter/Rebooter.log");
        }
    }

    public static void rebootRouter() {
        // Grab password environment variables.
        String routerPassword = System.getenv("routerPassword");
        String extenderPassword = System.getenv("tplinkPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        Configuration.browserCapabilities = chromeOptions;

         /*
          The following loop finds the IP address of the gateway for the TP-Link extender before restarting the router, so that the extender can be restarted after.
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
            } catch (Exception ignored) {}
        }

        // Open the router webpage
        open("https://192.168.0.1/cgi-bin/luci/admin/troubleshooting/restart");

        // Login to router webpage
        $(By.name("luci_password")).should(interactable, Duration.ofSeconds(30)).setValue(routerPassword);
        $(By.id("loginbtn")).click();

        // Open a new tab and navigate to the tp-link webpage
        Selenide.executeJavaScript("window.open('" + "http://" + tpLinkIP + "','_blank');");
        Selenide.switchTo().window(1);

        // Prepare for reboot by navigating to the reboot prompt
        $(By.className("password-text")).shouldBe(interactable, Duration.ofSeconds(30)).setValue(extenderPassword);
        $(By.id("login-btn")).shouldBe(interactable).click();
        $(By.id("top-control-reboot")).shouldBe(interactable, Duration.ofSeconds(10)).click();

        // Switch back to the router tab
        Selenide.switchTo().window(0);

        // Click the button to restart the router
        $(By.partialLinkText("RESTART GATEWAY")).shouldBe(interactable, Duration.ofSeconds(10)).click();

        // Accept the javascript prompt alert
        Selenide.switchTo().alert().accept();

        // Switch back to the tp-link tab
        Selenide.switchTo().window(1);

        // Click the button to restart the TP-Link extender.
        $(By.className("msg-btn-container")).find(By.className("btn-msg-ok")).shouldBe(interactable).click();

        // Sleep for 10s before exiting to ensure the requests had time to go through
        Selenide.sleep(10000);

        Selenide.closeWebDriver();
    }

    public static void writeToLogFile(String log, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(log);
        }
        catch (Exception ignored) {}
    }
}