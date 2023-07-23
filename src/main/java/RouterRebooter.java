import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.apache.commons.lang3.time.StopWatch;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    public static void main(String[] args) {

        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("/home/mwdle/Desktop/JavaProjects/RouterRebooter/src/main/resources/secrets.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Grab username and password secrets from the secret file.
        String username = properties.getProperty("routerUsername");
        String password = properties.getProperty("routerPassword");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito");
        // Set the Chrome options in Selenide Configuration
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
            } catch (Exception ignored) {
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

        $(By.className("password-text")).shouldBe(interactable).setValue(properties.getProperty("tplinkPassword"));
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
    }
}