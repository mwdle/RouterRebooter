import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
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
        System.out.println("Initiating restart script");

        Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        System.setProperty("webdriver.chrome.driver", "/RouterRebooter/chromedriver");

        // Grab username and password secrets from the secret file.
        String username = System.getenv("routerUsername");
        String password = System.getenv("routerPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito");
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-gpu");
        Configuration.browserCapabilities = chromeOptions;


        try {
            open("http://192.168.0.1/?util_restart");
        }
        catch (Exception e) {
            open("http://192.168.0.1/?util_restart");
        }

        $(By.id("UserName")).should(exist, Duration.ofSeconds(60)).setValue(username);
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
            } catch (Exception ignored) {}
        }

        // Click the button to restart the router
        $(By.className("submitBtn")).shouldBe(interactable).click();

        // Accept the javascript prompt alert
        Selenide.switchTo().alert().accept();
        Selenide.switchTo().defaultContent();

        // Sleep for 5s before exiting to ensure the request had time to go through
        Selenide.sleep(5000);

        Selenide.closeWebDriver();

        if (!tpLinkIP.equals("")) {
            open("http://" + tpLinkIP + "/");
        }

        $(By.className("password-text")).shouldBe(interactable, Duration.ofSeconds(30)).setValue(System.getenv("tplinkPassword"));
        $(By.id("login-btn")).shouldBe(interactable).click();

        $(By.id("top-control-reboot")).shouldBe(interactable, Duration.ofSeconds(10)).click();
        $(By.className("msg-btn-container")).find(By.className("btn-msg-ok")).shouldBe(interactable).click();

        // Sleep for 5 seconds before closing the webdriver and ending the script.
        Selenide.sleep(5000);
        Selenide.closeWebDriver();
        System.out.println("Home access points should have rebooted successfully.");
    }

}