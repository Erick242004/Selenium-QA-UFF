package br.com.aulaSelenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Classe base: cria/fecha o driver e concentra os passos reutilizados
 * pelos Test Cases 1 (Register User) e 3 (Login com dados incorretos).
 */
public abstract class BaseTest {

	protected static final String URL_BASE = "https://automationexercise.com";
	protected static final String SENHA_PADRAO = "Senha@12345";

	protected static final By BOTAO_LOGIN = By.cssSelector("button[data-qa='login-button']");
	protected static final By CAMPO_LOGIN_EMAIL = By.cssSelector("input[data-qa='login-email']");
	protected static final By CAMPO_LOGIN_SENHA = By.cssSelector("input[data-qa='login-password']");
	protected static final By BOTAO_SIGNUP = By.cssSelector("button[data-qa='signup-button']");
	protected static final By CAMPO_SIGNUP_NOME = By.cssSelector("input[data-qa='signup-name']");
	protected static final By CAMPO_SIGNUP_EMAIL = By.cssSelector("input[data-qa='signup-email']");
	protected static final By BOTAO_CONTINUE = By.cssSelector("a[data-qa='continue-button']");

	protected WebDriver driver;
	protected WebDriverWait wait;

	// ------------------------------------------------------------------
	// Ciclo de vida
	// ------------------------------------------------------------------

	@BeforeEach
	public void criaDriver() {
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--disable-notifications", "--disable-popup-blocking");
		if (Boolean.getBoolean("headless")) { // mvn test -Dheadless=true
			options.addArguments("--headless=new", "--window-size=1920,1080");
		} else {
			options.addArguments("--start-maximized");
		}
		// Passo 1: Launch browser
		driver = WebDriverManager.chromedriver().capabilities(options).create();
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	}

	@AfterEach
	public void fechaDriver() {
		if (driver != null) {
			driver.quit();
		}
	}

	// ------------------------------------------------------------------
	// Passos comuns (Test Case 1 e Test Case 3)
	// ------------------------------------------------------------------

	/** Passos 2 e 3: navega até a home e verifica que ela está visível. */
	protected void abrirHome() {
		driver.get(URL_BASE);
		aceitarCookies();
		removerAnuncios();
		assertTrue(driver.getTitle().contains("Automation Exercise"),
				"Título inesperado na home: " + driver.getTitle());
		WebElement slider = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("slider")));
		assertTrue(slider.isDisplayed(), "A home page não está visível");
	}

	/** Passo 4: clica em 'Signup / Login'. */
	protected void abrirTelaLogin() {
		clicar(By.cssSelector("a[href='/login']"));
	}

	/** Preenche nome + e-mail e clica em 'Signup' (passos 6 e 7 do TC1). */
	protected void iniciarCadastro(String nome, String email) {
		digitar(CAMPO_SIGNUP_NOME, nome);
		digitar(CAMPO_SIGNUP_EMAIL, email);
		clicar(BOTAO_SIGNUP);
	}

	/** Preenche e-mail + senha e clica em 'Login'. */
	protected void fazerLogin(String email, String senha) {
		digitar(CAMPO_LOGIN_EMAIL, email);
		digitar(CAMPO_LOGIN_SENHA, senha);
		clicar(BOTAO_LOGIN);
	}

	/**
	 * Passos 5 a 16 do Test Case 1: parte da tela Signup/Login, cadastra o
	 * usuário e termina verificando 'Logged in as <nome>'.
	 */
	protected void criarConta(String nome, String email, String senha) {
		// 5. Verify 'New User Signup!' is visible
		verificaTexto("New User Signup!", By.cssSelector(".signup-form h2"));

		// 6 e 7. Nome + e-mail e botão Signup
		iniciarCadastro(nome, email);

		// 8. Verify that 'ENTER ACCOUNT INFORMATION' is visible
		verificaTexto("Enter Account Information", By.cssSelector("h2.title.text-center"));

		// 9. Title, Name, Email, Password, Date of birth
		clicar(By.id("id_gender1"));
		garantirValor(By.id("name"), nome);
		digitar(By.id("password"), senha);
		selecionarPorValor(By.id("days"), "10");
		selecionarPorValor(By.id("months"), "5");
		selecionarPorValor(By.id("years"), "1995");

		// 10 e 11. Checkboxes de newsletter e ofertas
		clicar(By.id("newsletter"));
		clicar(By.id("optin"));

		// 12. Dados de endereço
		digitar(By.id("first_name"), "Aluno");
		digitar(By.id("last_name"), "Teste");
		digitar(By.id("company"), "Empresa Teste");
		digitar(By.id("address1"), "Rua Teste, 100");
		digitar(By.id("address2"), "Apto 101");
		new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("country"))))
				.selectByVisibleText("Canada");
		digitar(By.id("state"), "Ontario");
		digitar(By.id("city"), "Toronto");
		digitar(By.id("zipcode"), "M5V 2T6");
		digitar(By.id("mobile_number"), "21999999999");

		// 13. Click 'Create Account'
		clicar(By.cssSelector("button[data-qa='create-account']"));

		// 14. Verify that 'ACCOUNT CREATED!' is visible
		verificaTexto("Account Created!", By.cssSelector("h2[data-qa='account-created']"));

		// 15. Click 'Continue'
		clicar(BOTAO_CONTINUE);

		// 16. Verify that 'Logged in as username' is visible
		By logado = By.xpath("//a[contains(.,'Logged in as')]");
		wait.until(ExpectedConditions.visibilityOfElementLocated(logado));
		assertEquals(nome, textoDe(By.xpath("//a[contains(.,'Logged in as')]/b")).trim(),
				"Nome exibido em 'Logged in as' diferente do cadastrado");
	}

	/** Passos 17 e 18 do Test Case 1: excluir a conta. */
	protected void excluirConta() {
		clicar(By.cssSelector("a[href='/delete_account']"));
		verificaTexto("Account Deleted!", By.cssSelector("h2[data-qa='account-deleted']"));
		clicar(BOTAO_CONTINUE);
	}

	// ------------------------------------------------------------------
	// Utilitários
	// ------------------------------------------------------------------

	/** E-mail único a cada execução (o site rejeita e-mails já cadastrados). */
	protected static String emailUnico() {
		return "aluno." + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
	}

	/** Clique resistente aos anúncios do site (fallback via JavaScript). */
	protected void clicar(By locator) {
		removerAnuncios();
		WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
		try {
			wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
		} catch (ElementClickInterceptedException | TimeoutException e) {
			removerAnuncios();
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
		}
	}

	/** Limpa o campo e digita o texto (texto vazio apenas limpa o campo). */
	protected void digitar(By locator, String texto) {
		WebElement campo = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		campo.clear();
		if (texto != null && !texto.isEmpty()) {
			campo.sendKeys(texto);
		}
	}

	/** Só digita se o campo ainda estiver vazio (ex.: nome já vem preenchido). */
	protected void garantirValor(By locator, String texto) {
		WebElement campo = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		String atual = campo.getAttribute("value");
		if (atual == null || atual.isEmpty()) {
			campo.sendKeys(texto);
		}
	}

	protected void selecionarPorValor(By locator, String valor) {
		new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(locator))).selectByValue(valor);
	}

	protected String textoDe(By locator) {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText();
	}

	/** Verifica o texto ignorando maiúsculas/minúsculas (o site usa CSS uppercase). */
	protected void verificaTexto(String esperado, By locator) {
		String atual = textoDe(locator);
		assertTrue(atual.toLowerCase().contains(esperado.toLowerCase()),
				"Esperava encontrar '" + esperado + "' mas o texto era '" + atual + "'");
	}

	/** Indica se a validação HTML5 do formulário do botão informado passaria. */
	protected boolean formularioValido(By botaoSubmit) {
		WebElement botao = driver.findElement(botaoSubmit);
		return (Boolean) ((JavascriptExecutor) driver)
				.executeScript("return arguments[0].closest('form').checkValidity();", botao);
	}

	protected boolean existe(By locator) {
		return !driver.findElements(locator).isEmpty();
	}

	private void aceitarCookies() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(3))
					.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.fc-cta-consent")))
					.click();
		} catch (TimeoutException e) {
			// banner de consentimento não apareceu
		}
	}

	private void removerAnuncios() {
		((JavascriptExecutor) driver).executeScript(
				"document.querySelectorAll('ins.adsbygoogle, iframe[id^=\"aswift\"], "
						+ "iframe[id^=\"google_ads\"], .fc-consent-root').forEach(function(e){ e.remove(); });");
	}
}
