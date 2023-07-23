import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Properties;

import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class Rebooter {

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("/home/mwdle/Desktop/JavaProjects/RouterRebooter/src/test/java/secrets.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Grab username and password secrets from the secret file.
        String username = properties.getProperty("routerUsername");
        String password = properties.getProperty("routerPassword");

        System.setProperty("webdriver.chrome.driver", "/home/mwdle/Desktop/JavaProjects/RouterRebooter/chromedriver");

        open("http://192.168.0.1/?util_restart");

        // Wait until the login screen appears to do anything
        while (!$(By.linkText("Login")).exists()) {
            Selenide.sleep(200);
        }

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

        // Click the button to restart the router
        //$(By.className("submitBtn")).shouldBe(interactable).click();

        //// Accept the javascript prompt alert
        //Selenide.switchTo().alert().accept();

        //// Sleep for 10s before exiting to ensure the request had time to go through
        //Selenide.sleep(10000);

        Selenide.closeWebDriver();

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

        if (!tpLinkIP.equals("")) {
            open("http://" + tpLinkIP + "/");
        }

        Selenide.closeWebDriver();
    }
}