const SECAO = { marginBottom: "1.5rem" };
const TITULO_SECAO = { marginBottom: "0.4rem" };

export default function PoliticaPrivacidade() {
  return (
    <div style={{ maxWidth: 720, margin: "2rem auto", lineHeight: 1.6 }}>
      <h2>Política de Privacidade do MaisEdu</h2>
      <p style={{ color: "#555" }}>
        Última atualização: 25 setembro de 2026. Este documento descreve, de forma específica ao MaisEdu
        (plataforma de aprendizagem adaptativa de Matemática para o Ensino Fundamental II, desenvolvida
        como Projeto Final de Curso), quais dados pessoais o sistema coleta, para quê, e quais direitos
        o titular tem sobre eles, nos termos da Lei nº 13.709/2018 (LGPD).
      </p>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>1. Quem trata os dados</h3>
        <p>
          O tratamento é feito pela instituição de ensino contratante do MaisEdu, que atua como
          controladora dos dados de seus professores e alunos. O administrador cadastrado por cada
          instituição é o ponto de contato para exercício de direitos (ver seção 6).
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>2. Quais dados coletamos, e de quem</h3>
        <p><strong>Professores e administradores:</strong> nome completo e e-mail (identificador de acesso),
        senha (nunca em texto puro — ver seção 5), papel de acesso (professor/admin) e instituição
        vinculada.</p>
        <p><strong>Alunos:</strong> nome completo e um login gerado pela própria escola (ex.: matrícula) —
        <strong> o aluno não tem e-mail cadastrado no sistema.</strong> Essa é uma decisão deliberada de
        minimização de dados: como o Ensino Fundamental II reúne majoritariamente crianças e
        adolescentes, evitamos coletar um dado de contato direto que não é necessário para o
        funcionamento do sistema. O vínculo do aluno com turmas e o acompanhamento pedagógico são
        conduzidos pela instituição e pelo professor responsável, não diretamente com o aluno fora do
        ambiente escolar.</p>
        <p><strong>Dados de uso e segurança (todos os perfis):</strong> registros de acesso — login,
        logout, troca de senha, tentativas de autenticação malsucedidas — com data/hora, endereço IP e
        identificação do navegador (user-agent) de quem realizou a ação. Também registramos as ações
        realizadas no sistema (cadastro de usuários, vínculo de turmas, avaliação colaborativa de
        questões), sempre associadas a quem executou a ação.</p>
        <p><strong>Dados acadêmicos:</strong> questões cadastradas e avaliadas colaborativamente por
        professores, vínculos entre alunos, professores e turmas. Em etapas futuras do projeto, também o
        histórico de respostas de atividades e o desempenho do aluno por tópico/dificuldade, usado
        exclusivamente para ajustar a dificuldade das próximas questões apresentadas a ele.</p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>3. Para que usamos cada dado (finalidade e base legal)</h3>
        <ul>
          <li><strong>Prestar o serviço educacional</strong> (controle de acesso por perfil, vínculo
            aluno-turma-professor, banco colaborativo de questões): execução de contrato/procedimento
            preparatório com a instituição de ensino (art. 7º, V, LGPD).</li>
          <li><strong>Segurança e auditoria</strong> (log de acessos e ações, bloqueio por tentativas de
            login malsucedidas): legítimo interesse do controlador em proteger o sistema contra acesso
            indevido e fraude (art. 7º, IX), de forma proporcional — coletamos IP e user-agent, não
            dados de geolocalização ou de dispositivo além disso.</li>
          <li><strong>Dados de alunos (crianças e adolescentes):</strong> tratados no melhor interesse do
            titular, no contexto de uma relação educacional já estabelecida entre a instituição de
            ensino e o responsável legal do aluno (art. 14, LGPD, e orientação da ANPD sobre tratamento
            de dados de crianças e adolescentes). O aluno não se autocadastra: quem cria a conta é o
            professor, dentro do vínculo já existente com a escola.</li>
        </ul>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>4. Com quem compartilhamos</h3>
        <p>
          Usamos o <strong>Resend</strong> (serviço de envio de e-mail transacional, sediado nos Estados
          Unidos) para um único fim: entregar a senha provisória a um professor ou administrador
          recém-cadastrado. <strong>Dados de aluno nunca são compartilhados com o Resend</strong> — o
          aluno não tem e-mail no sistema, então esse canal simplesmente não se aplica a ele. Não
          compartilhamos dados com nenhum outro serviço de terceiros.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>5. Como protegemos os dados</h3>
        <ul>
          <li>Senhas são armazenadas com hash BCrypt — nunca em texto puro, nem mesmo para os
            administradores do sistema.</li>
          <li>Controle de acesso por perfil (aluno, professor, admin): cada funcionalidade só é acessível
            a quem tem o papel necessário, verificado a cada requisição no servidor (não apenas
            escondendo botões na tela).</li>
          <li>Sessão de login via cookie HttpOnly (inacessível a scripts no navegador) e bloqueio
            automático da conta após tentativas seguidas de senha incorreta.</li>
          <li>Toda ação sensível (login, cadastro, vínculo de turma, avaliação de questão) fica registrada
            em log de auditoria, permitindo rastrear quem fez o quê e quando.</li>
        </ul>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>6. Direitos do titular</h3>
        <p>
          Nos termos do art. 18 da LGPD, você pode solicitar à instituição de ensino (por meio do
          administrador responsável pelo seu cadastro): confirmação de tratamento, acesso aos dados,
          correção de dados incompletos ou desatualizados, anonimização/eliminação de dados
          desnecessários, portabilidade e informação sobre com quem os dados são compartilhados. Para
          alunos menores de idade, esses direitos são exercidos pelo responsável legal junto à
          instituição.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>7. Retenção e incidentes de segurança</h3>
        <p>
          Os dados são mantidos enquanto durar o vínculo do usuário com a instituição de ensino. Os
          registros de auditoria são mantidos pelo prazo necessário à finalidade de segurança descrita
          na seção 3, observando os prazos legais aplicáveis. Em caso de incidente de segurança que
          possa acarretar risco aos titulares, a instituição seguirá os prazos e procedimentos de
          comunicação previstos na regulamentação da ANPD.
        </p>
      </div>

      <p style={{ color: "#555", marginTop: "2rem" }}>
        Dúvidas sobre esta política podem ser encaminhadas ao administrador responsável pela sua
        instituição no MaisEdu.
      </p>
    </div>
  );
}
