package ru.netology.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class FormValidationTest {

    WebDriver driver;
    WebDriverWait wait;

    // Валидируем только эти поля и в таком порядке
    final List<String> FIELD_ORDER = List.of("name", "phone", "agreement");

    // Валидные значения
    final Map<String, String> VALID_DATA = Map.of(
            "name", "Иван Петров",
            "phone", "+79990001122"
    );

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        String baseUrl = System.getProperty("baseUrl", "http://localhost:9999");
        driver.get(baseUrl);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    By fieldBox(String id) {
        return By.cssSelector("[data-test-id='" + id + "']");
    }

    By fieldInput(String id) {
        return By.cssSelector(
                "[data-test-id='" + id + "'] input, " +
                        "[data-test-id='" + id + "'] textarea, " +
                        "[data-test-id='" + id + "'] select"
        );
    }

    By fieldInvalid(String id) {
        return By.cssSelector("[data-test-id='" + id + "'].input_invalid");
    }

    By fieldErrorText(String id) {
        return By.cssSelector("[data-test-id='" + id + "'].input_invalid .input__sub");
    }

    By allInvalid() {
        return By.cssSelector("[data-test-id].input_invalid");
    }

    By submitBtn() {
        return By.cssSelector("button[type='submit']");
    }

    // Возможные селекторы успешной отправки (подберите под свою реализацию)
    By successBox() {
        return By.cssSelector("[data-test-id='success'], [data-test-id='order-success'], [data-test-id='notification'].notification_status_ok");
    }

    void clickContinue() {
        driver.findElement(submitBtn()).click();
    }

    void setValue(String id, String value) {
        WebElement el = driver.findElement(fieldInput(id));
        el.clear();
        el.sendKeys(value);
    }

    void setAgreement(boolean value) {
        String id = "agreement";
        WebElement box = driver.findElement(fieldBox(id));
        WebElement checkbox = box.findElement(By.cssSelector("input[type='checkbox']"));
        boolean checked = checkbox.isSelected();
        if (checked != value) {
            // Пытаемся кликнуть по лейблу/контейнеру, чтобы сменить состояние
            try {
                box.click();
            } catch (Exception e) {
                checkbox.click();
            }
        }
    }

    void waitForAnyInvalid() {
        wait.until(ExpectedConditions.presenceOfElementLocated(allInvalid()));
    }

    void waitForSuccess() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(successBox()));
    }

    String getOnlyInvalidFieldId() {
        waitForAnyInvalid();
        List<WebElement> invalids = driver.findElements(allInvalid());
        Assertions.assertEquals(1, invalids.size(), "Ожидали только одно подсвеченное поле");
        return invalids.get(0).getAttribute("data-test-id");
    }

    boolean hasAnyInvalid() {
        return !driver.findElements(allInvalid()).isEmpty();
    }

    // 1) Пустая форма: подсвечивается только первое обязательное поле (name)
    @Test
    void onlyFirstInvalidIsHighlighted_whenFormIsEmpty() {
        clickContinue();
        String firstInvalid = getOnlyInvalidFieldId();
        Assertions.assertEquals(FIELD_ORDER.get(0), firstInvalid, "Подсвечено не первое обязательное поле");
    }

    // 2) Ошибка «переезжает» на следующее поле по мере исправления предыдущего: name -> phone -> agreement
    @Test
    void errorMovesToNextField_asUserFixesPreviousOnes() {
        // Стартуем с пустой формы
        clickContinue();        Assertions.assertEquals(FIELD_ORDER.get(0), getOnlyInvalidFieldId());

        // Исправляем name
        setValue("name", VALID_DATA.get("name"));
        clickContinue();
        Assertions.assertEquals(FIELD_ORDER.get(1), getOnlyInvalidFieldId());

        // Исправляем phone
        setValue("phone", VALID_DATA.get("phone"));
        clickContinue();
        Assertions.assertEquals(FIELD_ORDER.get(2), getOnlyInvalidFieldId());

        // Ставим согласие
        setAgreement(true);
        clickContinue();

        // Нет ошибок, форма успешно отправлена
        Assertions.assertFalse(hasAnyInvalid(), "Ошибки валидации не должны оставаться после исправлений");
        waitForSuccess();
    }

    // 3) Имя: принимает русские буквы, дефисы и пробелы
    @Test
    void name_allowsRussianHyphenSpace() {
        setValue("name", "Анна-Мария Иванова");
        setValue("phone", VALID_DATA.get("phone"));
        setAgreement(true);
        clickContinue();

        Assertions.assertFalse(hasAnyInvalid(), "Имя с русскими буквами, дефисом и пробелом должно считаться валидным");
        waitForSuccess();
    }

    // 4) Имя: отклоняет любые неразрешенные символы (латиница, цифры, спецсимволы)
    @ParameterizedTest
    @ValueSource(strings = {
            "Ivan Petrov",   // латиница
            "Иван1",         // цифра
            "Иван@",         // спецсимвол
            "Иван_",         // спецсимвол
            "Петр#",         // спецсимвол
            " "              // только пробел
    })
    void name_rejectsNonAllowedChars(String badName) {
        setValue("name", badName);
        setValue("phone", VALID_DATA.get("phone"));
        setAgreement(true);
        clickContinue();

        Assertions.assertEquals("name", getOnlyInvalidFieldId(), "Невалидное имя должно подсвечивать поле name");
    }

    // 5) Телефон: принимает строго 11 цифр и + на первом месте
    @Test
    void phone_allowsPlusAnd11Digits() {
        setValue("name", VALID_DATA.get("name"));
        setValue("phone", "+79001234567");
        setAgreement(true);
        clickContinue();

        Assertions.assertFalse(hasAnyInvalid(), "Телефон в формате +7XXXXXXXXXX (11 цифр) должен считаться валидным");
        waitForSuccess();
    }

    // 6) Телефон: отклоняет неправильные форматы
    @ParameterizedTest
    @ValueSource(strings = {
            "79001234567",       // нет +
            "+7900123456",       // 10 цифр
            "+790012345678",     // 12 цифр
            "+7(900)123-45-67",  // посторонние символы
            "+7 9001234567",     // пробел
            "+7abcdefghij"       // буквы
    })
    void phone_rejectsWrongFormats(String badPhone) {
        setValue("name", VALID_DATA.get("name"));
        setValue("phone", badPhone);
        setAgreement(true);
        clickContinue();

        Assertions.assertEquals("phone", getOnlyInvalidFieldId(), "Неверный телефон должен подсвечивать поле phone");
    }

    // 7) Флажок согласия обязателен
    @Test
    void agreement_mustBeChecked() {
        setValue("name", VALID_DATA.get("name"));
        setValue("phone", VALID_DATA.get("phone"));
        setAgreement(false);
        clickContinue();

        Assertions.assertEquals("agreement", getOnlyInvalidFieldId(), "При отсутствии согласия должно подсвечиваться поле agreement");
    }

    // 8) Успешная отправка формы с валидными данными
    @Test
    void submit_success_withValidData() {
        setValue("name", VALID_DATA.get("name"));
        setValue("phone", VALID_DATA.get("phone"));
        setAgreement(true);
        clickContinue();

        Assertions.assertFalse(hasAnyInvalid(), "Не должно быть ошибок валидации при валидных данных");
        waitForSuccess();
    }
}