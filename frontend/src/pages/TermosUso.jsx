const SECAO = { marginBottom: "1.5rem" };
const TITULO_SECAO = { marginBottom: "0.4rem" };

export default function TermosUso() {
  return (
    <div style={{ maxWidth: 720, margin: "2rem auto", lineHeight: 1.6 }}>
      <h2>Termos de Uso do MaisEdu</h2>
      <p style={{ color: "#555" }}>
        Última atualização: 25 de setembro de 2026. Ao aceitar estes termos no primeiro acesso, você concorda
        com as condições abaixo para uso do MaisEdu.
      </p>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>1. Sobre o sistema</h3>
        <p>
          O MaisEdu é uma plataforma de apoio ao ensino de Matemática para o Ensino Fundamental II,
          desenvolvida como Projeto Final de Curso (PFC). Oferece um banco de questões validado
          colaborativamente por professores e, em etapas futuras, ajuste adaptativo de dificuldade
          conforme o desempenho de cada aluno.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>2. Contas e perfis de acesso</h3>
        <ul>
          <li><strong>Administrador:</strong> cadastra professores da própria instituição.</li>
          <li><strong>Professor:</strong> cadastra alunos, vincula-os a turmas em que leciona, cria
            questões e participa da validação colaborativa das questões de outros professores.</li>
          <li><strong>Aluno:</strong> acessa com um login gerado pela escola (sem e-mail cadastrado) e uma
            senha provisória definida pelo professor, que deve ser trocada no primeiro acesso.</li>
        </ul>
        <p>
          Cada conta é pessoal e intransferível. Você é responsável por manter sua senha em sigilo e por
          toda atividade realizada com o seu login — o sistema registra em log quem fez cada ação (ver
          seção 4).
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>3. Uso adequado</h3>
        <p>
          O MaisEdu deve ser usado exclusivamente para fins educacionais, dentro do vínculo institucional
          que originou seu cadastro. Não é permitido: compartilhar credenciais de acesso, tentar acessar
          dados ou funcionalidades fora do seu perfil, inserir conteúdo ofensivo ou inadequado no banco
          de questões, ou usar o sistema de forma automatizada sem autorização. O uso indevido pode levar
          à suspensão da conta pela instituição.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>4. Registro de atividade</h3>
        <p>
          Por segurança, o sistema mantém um log de auditoria com as ações realizadas (login, logout,
          cadastros, vínculos, avaliações de questões), incluindo data/hora, endereço IP e navegador
          utilizado. Esse registro existe para rastreabilidade e investigação de uso indevido — detalhes
          sobre quais dados são coletados e por quê estão na <em>Política de Privacidade</em>.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>5. Banco de questões colaborativo</h3>
        <p>
          Questões cadastradas por um professor passam por avaliação de outros professores antes de
          entrarem em uso — uma questão só é aprovada com pelo menos 3 votos favoráveis (ou rejeitada com
          3 votos contrários) entre até 5 avaliadores. O autor de uma questão não pode avaliar a própria
          questão. Questões aprovadas ficam disponíveis para uso colaborativo entre professores.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>6. Disponibilidade</h3>
        <p>
          O MaisEdu é um projeto acadêmico em desenvolvimento e não garante disponibilidade
          ininterrupta (SLA). Funcionalidades podem ser ajustadas ou ampliadas ao longo do curso do
          projeto.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>7. Alterações destes termos</h3>
        <p>
          Estes termos podem ser atualizados conforme o sistema evolui. Mudanças relevantes serão
          comunicadas e poderão exigir novo aceite no próximo acesso.
        </p>
      </div>

      <p style={{ color: "#555", marginTop: "2rem" }}>
        Veja também a <em>Política de Privacidade</em>, que detalha quais dados pessoais são coletados e
        como são tratados.
      </p>
    </div>
  );
}
