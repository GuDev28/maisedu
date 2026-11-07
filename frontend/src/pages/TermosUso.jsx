const SECAO = { marginBottom: "1.5rem" };
const TITULO_SECAO = { marginBottom: "0.4rem" };

export default function TermosUso() {
  return (
    <div className="documento-legal">
      <h2>Termos de Uso do MaisEdu</h2>
      <p className="text-body-secondary">
        Versão 1 — última atualização: 4 de novembro de 2026. Ao aceitar estes termos no primeiro acesso, você
        concorda com as condições abaixo para uso do MaisEdu.
      </p>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>1. Sobre o sistema</h3>
        <p>
          O MaisEdu é uma plataforma de apoio ao ensino de Matemática para o Ensino Fundamental II,
          desenvolvida como Projeto Final de Curso (PFC). Oferece um banco de questões validado
          colaborativamente por professores de várias escolas, atividades com correção automática e práticas
          adaptativas, que escolhem questões conforme o desempenho de cada aluno por tópico.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>2. Contas e perfis de acesso</h3>
        <ul>
          <li><strong>Administrador:</strong> cadastra professores e turmas da própria instituição, vincula
            professores às turmas, consulta a auditoria e atende as solicitações de dados pessoais.</li>
          <li><strong>Professor:</strong> cadastra alunos e os vincula às turmas em que leciona, cria questões,
            participa da validação colaborativa, cria atividades e acompanha o desempenho das suas turmas.</li>
          <li><strong>Aluno:</strong> acessa com um login definido pela escola (sem e-mail cadastrado) e uma
            senha provisória entregue pelo professor, que deve ser trocada no primeiro acesso; responde às
            atividades e às práticas adaptativas e acompanha o próprio desempenho.</li>
        </ul>
        <p>
          Cada conta é pessoal e intransferível. Você é responsável por manter sua senha em sigilo e por
          toda atividade realizada com o seu login — o sistema registra em log quem fez cada ação (ver seção 4).
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
          cadastros, vínculos, questões, atividades, redefinições de senha), incluindo data/hora, endereço IP e
          navegador utilizado. Esse registro existe para rastreabilidade e investigação de uso indevido, é
          visível apenas ao administrador da sua instituição e é apagado após o prazo da Política de Privacidade.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>5. Banco de questões colaborativo e autoria</h3>
        <p>
          Questões cadastradas por um professor passam por avaliação de outros professores antes de
          entrarem em uso — uma questão só é aprovada com 3 votos favoráveis (ou rejeitada com 3 votos
          contrários) entre até 5 avaliadores. O autor não avalia a própria questão. Depois de enviada para
          validação, a questão não pode mais ser alterada.
        </p>
        <p>
          Ao enviar uma questão, o professor declara ser o autor do conteúdo ou ter autorização para usá-lo, e
          autoriza que ela, uma vez aprovada, seja usada por professores das demais instituições da plataforma,
          com indicação do seu nome como autor(a). Não envie conteúdo protegido por direitos autorais sem
          autorização (por exemplo, cópias de livros didáticos ou apostilas).
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>6. Atividades e respostas</h3>
        <p>
          Cada questão de uma atividade aceita uma única resposta, que não pode ser alterada depois de enviada.
          As questões de uma atividade ficam fixas a partir do momento em que o aluno a inicia.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>7. Disponibilidade</h3>
        <p>
          O MaisEdu é um projeto acadêmico em desenvolvimento e não garante disponibilidade
          ininterrupta (SLA). Funcionalidades podem ser ajustadas ou ampliadas ao longo do projeto.
        </p>
      </div>

      <div style={SECAO}>
        <h3 style={TITULO_SECAO}>8. Alterações destes termos</h3>
        <p>
          Estes termos podem ser atualizados conforme o sistema evolui. Quando houver mudança relevante, a
          versão é atualizada e o sistema exige um novo aceite no próximo acesso, antes de qualquer outra ação.
        </p>
      </div>

      <p className="text-body-secondary small">
        Veja também a Política de Privacidade, que detalha quais dados pessoais são coletados e
        como são tratados.
      </p>
    </div>
  );
}
