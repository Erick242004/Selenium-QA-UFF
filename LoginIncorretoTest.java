package br.com.aulaSelenium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;

/**
 * Test Case 3: Login User with incorrect email and password.
 *
 * Classes de equivalência (CE) e valores limite (VL) para e-mail e senha:
 *
 *  Rejeitadas pelo servidor (mensagem "Your email or password is incorrect!"):
 *    CE1 e-mail bem formado e não cadastrado + senha qualquer
 *    CE2 e-mail/senha com caracteres especiais
 *    CE3 senha com texto tipo SQL
 *    VL1 tamanho mínimo (e-mail "a@b.co", senha com 1 caractere)
 *    VL2 tamanho máximo da parte local (64) e senha longa (128)
 *    VL3 logo acima do máximo (parte local com 65)
 *
 *  Bloqueadas pela validação HTML5 do navegador (formulário nem é enviado):
 *    CE4 e-mail vazio | CE5 sem '@' | CE6 sem domínio | CE7 sem parte local | CE8 senha vazia
 */
public class LoginIncorretoTest extends BaseTest {

	private static final By MSG_ERRO = By
			.xpath("//p[contains(text(),'Your email or password is incorrect!')]");

	static Stream<Arguments> credenciaisRejeitadasPeloServidor() {
		return Stream.of(
				Arguments.of("CE1 - e-mail válido não cadastrado", "naocadastrado@teste.com", "SenhaErrada123"),
				Arguments.of("VL1 - limite inferior: e-mail 'a@b.co' e senha com 1 caractere", "a@b.co", "1"),
				Arguments.of("VL2 - limite superior: parte local com 64 e senha com 128 caracteres",
						"a".repeat(64) + "@teste.com", "S".repeat(128)),
				Arguments.of("VL3 - acima do limite: parte local com 65 caracteres",
						"a".repeat(65) + "@teste.com", "SenhaErrada123"),
				Arguments.of("CE2 - caracteres especiais no e-mail e na senha",
						"usuario+tag@sub.dominio.com.br", "P@ssw0rd!#$%&*"),
				Arguments.of("CE3 - senha com texto tipo SQL", "naocadastrado@teste.com", "' OR '1'='1"));
	}

	static Stream<Arguments> entradasBloqueadasPeloNavegador() {
		return Stream.of(
				Arguments.of("CE4 - e-mail vazio", "", "Senha123"),
				Arguments.of("CE5 - e-mail sem '@'", "usuario.semarroba.com", "Senha123"),
				Arguments.of("CE6 - e-mail sem domínio", "usuario@", "Senha123"),
				Arguments.of("CE7 - e-mail sem parte local", "@teste.com", "Senha123"),
				Arguments.of("CE8 - senha vazia", "valido@teste.com", ""));
	}

	/** Cenário principal do TC3 (passos 1 a 8), repetido para cada entrada. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("credenciaisRejeitadasPeloServidor")
	public void loginComCredenciaisIncorretas(String descricao, String email, String senha) {
		abrirHome(); // 2 e 3
		abrirTelaLogin(); // 4
		verificaTexto("Login to your account", By.cssSelector(".login-form h2")); // 5

		fazerLogin(email, senha); // 6 e 7

		verificaTexto("Your email or password is incorrect!", MSG_ERRO); // 8
	}

	/** Entradas inválidas que o navegador barra antes de chegar ao servidor. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("entradasBloqueadasPeloNavegador")
	public void loginComEntradaInvalidaNaoEhEnviado(String descricao, String email, String senha) {
		abrirHome();
		abrirTelaLogin();
		verificaTexto("Login to your account", By.cssSelector(".login-form h2"));

		digitar(CAMPO_LOGIN_EMAIL, email);
		digitar(CAMPO_LOGIN_SENHA, senha);

		assertFalse(formularioValido(BOTAO_LOGIN), "A validação HTML5 deveria reprovar: " + descricao);

		clicar(BOTAO_LOGIN);

		assertTrue(driver.getCurrentUrl().contains("/login"), "Deveria permanecer na tela de login");
		assertFalse(existe(MSG_ERRO), "Não deveria haver resposta do servidor (formulário não enviado)");
	}
}
