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

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class RouterRebooter {

    public static void writeTextToFile(String text, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(text);
        }
        catch (Exception ignored) { System.out.println("Failed to write text: '" + text + "' to file: '" + fileName + "'"); }
    }

    public static void rebootRouter() {
        String routerPassword = System.getenv("ROUTER_PASSWORD");

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito", "--headless", "--no-sandbox", "--disable-gpu", "--start-maximized", "--window-size=1920,1080");
        chromeOptions.setBinary("/usr/bin/google-chrome");
        Configuration.browserCapabilities = chromeOptions;

        open("https://192.168.0.1/cgi-bin/luci/admin/troubleshooting/restart");
        $(By.name("luci_password")).should(interactable, Duration.ofSeconds(10)).setValue(routerPassword);
        $(By.id("loginbtn")).shouldBe(interactable).click();
        $(By.partialLinkText("RESTART GATEWAY")).shouldBe(interactable, Duration.ofSeconds(10)).click();
        Selenide.switchTo().alert().accept();
        Selenide.sleep(3000);
        Selenide.closeWebDriver();
    }

    public static void main(String[] args) {
        try {
            rebootRouter();
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Reboot Success", "/RouterRebooter/RouterRebooter.log");
        }
	    catch (Throwable e) {
            writeTextToFile(e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()), "/RouterRebooter/lastFailure.log");
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: check lastFailure.log", "/RouterRebooter/RouterRebooter.log");
        }
    }
}
