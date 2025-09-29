package ru.netology.services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

class CardOrderTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeAll
    static void setupAll() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        driver.get("http://localhost:9999");
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void shouldSubmitRequestSuccessfully() {
        // data-test-id селекторы + вложенность к input
        driver.findElement(By.cssSelector("[data-test-id='name'] input"))
                .sendKeys("Иван-Петров Иван");
        driver.findElement(By.cssSelector("[data-test-id='phone'] input"))
                .sendKeys("+79991234567");
        driver.findElement(By.cssSelector("[data-test-id='agreement'] .checkbox__box"))
                .click();
        driver.findElement(By.cssSelector("button.button")).click();

        WebElement success = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test-id='order-success']"))
        );
        String expected = "Ваша заявка успешно отправлена! Наш менеджер свяжется с вами в ближайшее время.";
        Assertions.assertEquals(expected, success.getText().trim());
    }
}