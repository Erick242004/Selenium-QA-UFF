package br.com.aulaSelenium;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;

/**
 * Test Case 1: Register User.
 *
 * Classes de equivalência (CE) e valores limite (VL) para nome e senha:
 *
 *  Cadastro completo com sucesso (passos 1 a 18):
 *    VL1 nome com 1 caractere (limite inferior)
 *    VL2 nome com 2 caracteres (limite inferior + 1)
 *    CE1 nome típico
 *    CE2 nome com acentos e hífen
 *    VL3 nome com 50 caracteres (limite superior adotado como hipótese)
 *    VL4 senha com 1 caractere
 *    VL5 senha com 64 caracteres
 *
 *  Bloqueadas na etapa de Signup (validação HTML5, não avança para /signup):
 *    CE4 nome vazio | CE5 e-mail vazio | CE6 sem '@' | CE7 sem domínio | CE8 sem parte local
 *
 *  Rejeitada pelo servidor:
 *    CE9 e-mail já cadastrado ("Email Address already exist!")
 */
public class RegistrarUsuarioTest extends BaseTest {

	private static final By MSG_EMAIL_EXISTENTE = By
			.xpath("//p[contains(text(),'Email Address already exist!')]");

	static Stream<Arguments> dadosValidos() {
		return Stream.of(
				Arguments.of("VL1 - nome com 1 caractere", "A", SENHA_PADRAO),
				Arguments.of("VL2 - nome com 2 caracteres", "Jo", SENHA_PADRAO),
				Arguments.of("CE1 - nome típico", "Maria da Silva", SENHA_PADRAO),
				Arguments.of("CE2 - nome com acentos e hífen", "José Ávila-Souza", SENHA_PADRAO),
				Arguments.of("VL3 - nome com 50 caracteres", "Aluno".repeat(10), SENHA_PADRAO),
				Arguments.of("VL4 - senha com 1 caractere", "Maria da Silva", "1"),
				Arguments.of("VL5 - senha com 64 caracteres", "Maria da Silva", "S1".repeat(32)));
	}

	static Stream<Arguments> dadosBloqueadosNoSignup() {
		return Stream.of(
				Arguments.of("CE4 - nome vazio", "", emailUnico()),
				Arguments.of("CE5 - e-mail vazio", "Maria", ""),
				Arguments.of("CE6 - e-mail sem '@'", "Maria", "maria.semarroba.com"),
				Arguments.of("CE7 - e-mail sem domínio", "Maria", "maria@"),
				Arguments.of("CE8 - e-mail sem parte local", "Maria", "@teste.com"));
	}

	/** Test Case 1 completo (passos 1 a 18), repetido para cada entrada válida. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("dadosValidos")
	public void registrarUsuario(String descricao, String nome, String senha) {
		abrirHome(); // 2 e 3
		abrirTelaLogin(); // 4
		criarConta(nome, emailUnico(), senha); // 5 a 16
		excluirConta(); // 17 e 18
	}

	/** Entradas inválidas que impedem de sair da etapa "New User Signup!". */
	@ParameterizedTest(name = "{0}")
	@MethodSource("dadosBloqueadosNoSignup")
	public void signupComEntradaInvalidaNaoAvanca(String descricao, String nome, String email) {
		abrirHome();
		abrirTelaLogin();
		verificaTexto("New User Signup!", By.cssSelector(".signup-form h2"));

		digitar(CAMPO_SIGNUP_NOME, nome);
		digitar(CAMPO_SIGNUP_EMAIL, email);

		assertFalse(formularioValido(BOTAO_SIGNUP), "A validação HTML5 deveria reprovar: " + descricao);

		clicar(BOTAO_SIGNUP);

		assertFalse(driver.getCurrentUrl().contains("/signup"), "Não deveria avançar para a etapa 2 do cadastro");
		assertTrue(driver.getCurrentUrl().contains("/login"), "Deveria permanecer em /login");
	}

	/** CE9: e-mail já cadastrado é rejeitado pelo servidor. */
	@Test
	public void signupComEmailJaCadastrado() {
		String nome = "Usuario Duplicado";
		String email = emailUnico();

		abrirHome();
		abrirTelaLogin();
		criarConta(nome, email, SENHA_PADRAO);

		try {
			// sai da conta e tenta cadastrar novamente com o mesmo e-mail
			clicar(By.cssSelector("a[href='/logout']"));
			verificaTexto("New User Signup!", By.cssSelector(".signup-form h2"));
			iniciarCadastro(nome, email);
			verificaTexto("Email Address already exist!", MSG_EMAIL_EXISTENTE);
		} finally {
			// limpeza: entra e exclui a conta criada para não deixar lixo no site
			if (!existe(By.cssSelector("a[href='/delete_account']"))) {
				driver.get(URL_BASE + "/login");
				fazerLogin(email, SENHA_PADRAO);
			}
			excluirConta();
		}
	}
}
