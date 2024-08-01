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

public class Rebooter {

    public static void writeTextToFile(String text, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(text);
        }
        catch (Exception ignored) { System.out.println("Failed to write text: '" + text + "' to file: '" + fileName + "'"); }
    }

    public static void rebootRouter() {
        try {
            String routerPassword = System.getenv("ROUTER_PASSWORD");
            open("https://192.168.0.1/cgi-bin/luci/admin/troubleshooting/restart");
            $(By.name("luci_password")).should(interactable, Duration.ofSeconds(10)).setValue(routerPassword);
            $(By.id("loginbtn")).shouldBe(interactable).click();
            $(By.partialLinkText("RESTART GATEWAY")).shouldBe(interactable, Duration.ofSeconds(10)).click();
            Selenide.switchTo().alert().accept();
            Selenide.sleep(3000);
            Selenide.closeWebDriver();
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Reboot Success", "/RouterRebooter/data/RouterRebooter.log");
            System.out.println(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Router Reboot Success");
        } catch (Throwable e) {
            writeTextToFile(e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()), "/RouterRebooter/data/lastFailure.log");
            System.out.println(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: " + e.getMessage());
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: check lastFailure.log", "/RouterRebooter/data/RouterRebooter.log");
        }
    }

    public static void rebootExtender() {
        try {
            String extenderPassword = System.getenv("EXTENDER_PASSWORD");
            // Finds the IP address of the TP-Link extender (IP is not static).
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
                }
                catch (Exception ignored) {}
            }
            if (tpLinkIP.isEmpty()) {
                writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: Extender IP not found", "/RouterRebooter/data/ExtenderRebooter.log");
                System.out.println(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: Extender IP not found");
                return;
            }
            Selenide.open("http://" + tpLinkIP);
            $(By.className("password-text")).shouldBe(interactable, Duration.ofSeconds(10)).sendKeys(extenderPassword);
            $(By.id("login-btn")).shouldBe(interactable).click();
            $(By.id("top-control-reboot")).shouldBe(interactable, Duration.ofSeconds(10)).click();
            $(By.className("msg-btn-container")).should(exist).find(By.className("btn-msg-ok")).shouldBe(interactable).click();
            Selenide.sleep(3000);
            Selenide.closeWebDriver();
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Reboot Success", "/RouterRebooter/data/ExtenderRebooter.log");
            System.out.println(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": Extender Reboot Success");
        } catch (Throwable e) {
            writeTextToFile(e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace()), "/RouterRebooter/data/lastFailure.log");
            writeTextToFile(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: check lastFailure.log", "/RouterRebooter/data/ExtenderRebooter.log");
            System.out.println(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Date()) + ": FAILED: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Configure Chrome
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--incognito", "--headless", "--no-sandbox", "--disable-gpu", "--start-maximized", "--window-size=1920,1080");
        chromeOptions.setBinary("/usr/bin/google-chrome");
        Configuration.browserCapabilities = chromeOptions;
        // Handle input arguments
        if (args.length != 1) {
            System.out.println("Invalid number of arguments received. Expected 1 argument.");
            System.exit(1);
        }
        if (args[0].equals("router")) rebootRouter();
        else if (args[0].equals("extender")) rebootExtender();
        else {
            System.out.println("Invalid argument received. Valid arguments include 'router' and 'extender'.");
            System.exit(1);
        }
    }
}
