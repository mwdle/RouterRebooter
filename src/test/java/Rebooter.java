import com.codeborne.selenide.Selenide;
import org.openqa.selenium.By;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class Rebooter {

    public static void main(String[] args) {

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
        $(By.className("submitBtn")).click();

        Selenide.sleep(10000);
    }
}
