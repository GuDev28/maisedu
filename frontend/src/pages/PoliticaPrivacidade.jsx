const SECAO = { marginBottom: "1.5rem" };
const TITULO_SECAO = { marginBottom: "0.4rem" };

export default function PoliticaPrivacidade() {
  return (
    <div className="documento-legal">
      <h2>Política de Privacidade do MaisEdu</h2>
      <p className="text-body-secondary">
        Versão 1 — última atualização: 4 de novembro de 2026. Este documento descreve, de forma específica ao
        MaisEdu (plataforma de aprendizagem adaptativa de Matemática para o Ensino Fundamental II, desenvolvida
        como Projeto Final de Curso), quais dados pessoais o sistema coleta, para quê, com quem são
        compartilhados, por quanto tempo são guardados e como o titular exerce os seus direitos, nos termos da
        Lei nº 13.709/2018 (LGPD).
      </p>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>1. Quem trata os dados</h3>
        <p>
          O tratamento é feito pela instituição de ensino contratante do MaisEdu, que atua como
          controladora dos dados de seus professores e alunos. O administrador cadastrado por cada
          instituição é o ponto de contato para o exercício de direitos (ver seção 6) e só tem acesso aos
          dados e aos registros de auditoria da própria instituição.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>2. Quais dados coletamos, e de quem</h3>
        <p><strong>Professores e administradores:</strong> nome completo e e-mail (identificador de acesso),
        senha (nunca em texto puro — ver seção 5), papel de acesso (professor/admin), instituição vinculada e
        turmas em que leciona.</p>
        <p><strong>Alunos:</strong> nome completo e um login definido pela própria escola (ex.: matrícula) —
        <strong> o aluno não tem e-mail cadastrado no sistema.</strong> Essa é uma decisão deliberada de
        minimização de dados: como o Ensino Fundamental II reúne majoritariamente crianças e
        adolescentes, evitamos coletar um dado de contato direto que não é necessário para o
        funcionamento do sistema. Quando o aluno esquece a senha, o professor da turma gera uma nova senha
        provisória e a entrega em sala.</p>
        <p><strong>Dados acadêmicos:</strong> turmas do aluno; atividades iniciadas e as respostas dadas
        (alternativa escolhida, se acertou e data/hora); e o desempenho calculado a partir delas — total de
        respostas, acertos e percentual de acerto por tópico de Matemática e nível de dificuldade. Para
        professores, as questões que criaram e os votos que deram na validação colaborativa.</p>
        <p><strong>Dados de uso e segurança (todos os perfis):</strong> registros de acesso — login,
        logout, troca de senha, tentativas de autenticação malsucedidas — com data/hora, endereço IP e
        identificação do navegador (user-agent) de quem realizou a ação. Também registramos as ações
        realizadas no sistema (cadastros, vínculos com turmas, criação e avaliação de questões, criação e
        início de atividades, geração de práticas adaptativas, redefinição de senha, exportação e
        anonimização de dados), sempre associadas a quem executou a ação.</p>
        <p>O MaisEdu não coleta CPF, endereço, telefone, data de nascimento, foto, geolocalização nem dados
        sensíveis (art. 5º, II, LGPD).</p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>3. Para que usamos cada dado (finalidade e base legal)</h3>
        <ul>
          <li><strong>Prestar o serviço educacional</strong> (controle de acesso por perfil, vínculo
            aluno-turma-professor, banco colaborativo de questões, atividades e correção automática):
            execução de contrato com a instituição de ensino (art. 7º, V).</li>
          <li><strong>Desempenho do aluno:</strong> usado para (a) montar a prática adaptativa, que prioriza os
            tópicos em que o aluno mais erra e ajusta o nível de dificuldade; e (b) permitir que o próprio aluno
            e os professores das turmas dele acompanhem a evolução por tópico. O desempenho de um aluno nunca é
            mostrado a outros alunos nem a professores de turmas em que ele não está. Não há decisão
            automatizada que afete o aluno além da escolha das questões de prática.</li>
          <li><strong>Segurança e auditoria</strong> (registro de acessos e ações, bloqueio após tentativas de
            login malsucedidas): legítimo interesse do controlador em proteger o sistema contra acesso indevido
            e fraude (art. 7º, IX), de forma proporcional — coletamos IP e user-agent, não geolocalização.</li>
          <li><strong>Dados de alunos (crianças e adolescentes):</strong> tratados no seu melhor interesse
            (art. 14), com base nas hipóteses dos arts. 7º e 11, conforme o Enunciado CD/ANPD nº 1/2023, no
            contexto da relação educacional já estabelecida entre a instituição e o responsável legal. O aluno
            não se autocadastra: quem cria a conta é o professor. O aceite exibido ao aluno no primeiro acesso
            tem caráter de ciência das regras de uso, e não é a base legal do tratamento.</li>
        </ul>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>4. Com quem compartilhamos</h3>
        <p>
          <strong>Resend</strong> (serviço de e-mail transacional sediado nos Estados Unidos): recebe apenas
          nome, e-mail e a senha provisória de professores e administradores, para entregar o e-mail de
          acesso ou de redefinição de senha. Trata-se de transferência internacional de dados (art. 33),
          limitada a essa finalidade. <strong>Dados de alunos nunca são enviados ao Resend</strong> — o aluno
          não tem e-mail no sistema.
        </p>
        <p>
          <strong>Banco de questões compartilhado:</strong> questões aprovadas ficam disponíveis para
          professores de todas as instituições da plataforma, com o nome do(a) professor(a) autor(a). Nenhum dado
          de aluno é compartilhado entre instituições.
        </p>
        <p>Não vendemos dados nem os compartilhamos com nenhum outro terceiro.</p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>5. Como protegemos os dados</h3>
        <ul>
          <li>Senhas armazenadas com hash BCrypt — nunca em texto puro, nem mesmo para os administradores.</li>
          <li>Controle de acesso por perfil (aluno, professor, admin), verificado a cada requisição no servidor;
            o professor só acessa dados das turmas em que leciona.</li>
          <li>Sessão em cookie HttpOnly (inacessível a scripts no navegador), encerrada no servidor ao sair, ao
            trocar ou redefinir a senha e ao desativar a conta; bloqueio temporário após tentativas seguidas de
            senha incorreta.</li>
          <li>Registro de auditoria das ações sensíveis, consultável apenas pelo administrador da própria
            instituição.</li>
        </ul>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>6. Direitos do titular e como exercê-los</h3>
        <p>
          Nos termos do art. 18 da LGPD, você pode solicitar à instituição de ensino, por meio do administrador
          responsável pelo seu cadastro, e ele atende diretamente no sistema:
        </p>
        <ul>
          <li><strong>confirmação, acesso e portabilidade:</strong> exportação de todos os seus dados (cadastro,
            turmas, respostas, desempenho e registros de auditoria) em arquivo JSON;</li>
          <li><strong>correção</strong> de dados incompletos ou desatualizados (nome);</li>
          <li><strong>bloqueio:</strong> desativação da conta, que deixa de autenticar imediatamente;</li>
          <li><strong>eliminação:</strong> anonimização da conta — nome, e-mail, login e vínculos com turmas são
            apagados e os registros de auditoria deixam de identificar você. Respostas e desempenho são mantidos
            sem identificação, apenas para estatísticas agregadas (dados anonimizados não são dados pessoais,
            art. 12);</li>
          <li><strong>informação</strong> sobre com quem os dados são compartilhados (seção 4).</li>
        </ul>
        <p>Para alunos menores de idade, esses direitos são exercidos pelo responsável legal junto à
          instituição.</p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>7. Por quanto tempo guardamos</h3>
        <ul>
          <li><strong>Cadastro, respostas e desempenho:</strong> enquanto durar o vínculo do usuário com a
            instituição; ao fim do vínculo, a instituição pode desativar ou anonimizar a conta.</li>
          <li><strong>Registros de auditoria:</strong> 365 dias. Uma rotina diária apaga automaticamente os
            registros mais antigos que esse prazo.</li>
        </ul>
        <p>Em caso de incidente de segurança que possa acarretar risco aos titulares, a instituição seguirá
          os prazos e procedimentos de comunicação previstos na regulamentação da ANPD.</p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>8. Alterações desta política</h3>
        <p>Quando esta política ou os Termos de Uso mudarem de forma relevante, a versão é atualizada e todos os
          usuários precisam ler e aceitar a nova versão no próximo acesso.</p>
      </div>

      <p className="text-body-secondary small">
        Dúvidas sobre esta política podem ser encaminhadas ao administrador responsável pela sua
        instituição no MaisEdu.
      </p>
    </div>
  );
}
