import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    public static void main(String[] args) {
        StringBuilder log = new StringBuilder();
        try {
            rebootRouter(log);
        }
        catch (Throwable e) {
            log.append(System.lineSeparator()).append(e.getMessage());
            log.append(System.lineSeparator()).append(ExceptionUtils.getStackTrace(e));
        }
        writeToLogFile(log.toString());
    }

    public static void rebootRouter(StringBuilder log) {
        log.append(System.lineSeparator()).append("Initiating restart script");

        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

        // Grab username and password secrets from the secret file.
        String username = System.getenv("routerUsername");
        String password = System.getenv("routerPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        Configuration.browserCapabilities = chromeOptions;

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
            } catch (Exception ignored) {
            }
        }

        // Open the router webpage
        try {
            open("http://192.168.0.1/?util_restart");
        } catch (Exception e) {
            open("http://192.168.0.1/?util_restart");
        }

        // Login to router webpage
        $(By.id("UserName")).should(exist, Duration.ofSeconds(60)).setValue(username);
        $(By.id("Password")).setValue(password);
        $(By.className("submitBtn")).click();

        Selenide.sleep(2000);

        // Dismiss the retarded bullshit ads that popup upon login
        if ($(By.id("doNotShow")).exists()) {
            $(By.id("doNotShow")).click();
            $(By.className("ui-button")).click();
        }

        // Open a new tab and navigate to the tp-link webpage
        Selenide.executeJavaScript("window.open('" + "http://" + tpLinkIP + "/" + "','_blank');");
        Selenide.switchTo().window(1);

        // Prepare for reboot by navigating to the reboot prompt
        $(By.className("password-text")).shouldBe(interactable, Duration.ofSeconds(30)).setValue(System.getenv("tplinkPassword"));
        $(By.id("login-btn")).shouldBe(interactable).click();
        $(By.id("top-control-reboot")).shouldBe(interactable, Duration.ofSeconds(10)).click();

        // Switch back to the router tab
        Selenide.switchTo().window(0);

        // Click the button to restart the router
        $(By.className("submitBtn")).shouldBe(interactable).click();
        // Accept the javascript prompt alert
        Selenide.switchTo().alert().accept();

        // Switch back to the tp-link tab
        Selenide.switchTo().window(1);

        // Click the button to restart the TP-Link extender.
        $(By.className("msg-btn-container")).find(By.className("btn-msg-ok")).shouldBe(interactable).click();

        // Sleep for 10s before exiting to ensure the requests had time to go through
        Selenide.sleep(10000);

        Selenide.closeWebDriver();

        log.append(System.lineSeparator()).append("Home access points should have rebooted successfully.");
    }

    public static void writeToLogFile(String log) {
        try (FileWriter writer = new FileWriter("/RouterRebooter/Rebooter.log")) {
            writer.write(log);
        }
        catch (Exception ignored) {
            System.out.println("Failed to create and write to log file.");
        }
    }
}