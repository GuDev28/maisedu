export default function TermosUso() {
  return (
    <div className="texto-legal">
      <h1 className="me-titulo">Termos de Uso do MaisEdu</h1>
      <p className="text-secondary">
        Versão 2 — última atualização: 16 de novembro de 2026. Ao aceitar estes termos no primeiro acesso, você
        concorda com as condições abaixo para uso do MaisEdu.
      </p>

      <div className="mb-4">
        <h3>1. Sobre o sistema</h3>
        <p>
          O MaisEdu é uma plataforma de apoio ao ensino de Matemática para o Ensino Fundamental II,
          desenvolvida como Projeto Final de Curso (PFC). Oferece um banco de questões validado
          colaborativamente por professores de várias escolas, atividades com correção automática e práticas
          adaptativas, que escolhem questões conforme o desempenho de cada aluno por tópico.
        </p>
      </div>

      <div className="mb-4">
        <h3>2. Contas e perfis de acesso</h3>
        <ul>
          <li><strong>Administrador:</strong> cadastra professores e turmas da própria instituição, vincula
            professores às turmas, consulta a auditoria e atende as solicitações de dados pessoais.</li>
          <li><strong>Professor:</strong> cadastra alunos, informando o e-mail de um responsável, e os vincula
            às turmas em que leciona (ou os retira delas); corrige o e-mail do responsável e gera senhas novas
            para os alunos dessas turmas; cria questões, participa da validação colaborativa, cria atividades e acompanha o desempenho
            das suas turmas.</li>
          <li><strong>Aluno:</strong> acessa com um login definido pela escola (o aluno não tem e-mail
            cadastrado) e uma senha provisória enviada ao e-mail do seu responsável, que deve ser trocada no
            primeiro acesso; responde às atividades e às práticas adaptativas e acompanha o próprio
            desempenho.</li>
        </ul>
        <p>
          Cada conta é pessoal e intransferível. Você é responsável por manter sua senha em sigilo e por
          toda atividade realizada com o seu login — o sistema registra em log quem fez cada ação (ver seção 4).
        </p>
      </div>

      <div className="mb-4">
        <h3>3. Uso adequado</h3>
        <p>
          O MaisEdu deve ser usado exclusivamente para fins educacionais, dentro do vínculo institucional
          que originou seu cadastro. Não é permitido: compartilhar credenciais de acesso, tentar acessar
          dados ou funcionalidades fora do seu perfil, inserir conteúdo ofensivo ou inadequado no banco
          de questões, ou usar o sistema de forma automatizada sem autorização. O uso indevido pode levar
          à suspensão da conta pela instituição.
        </p>
      </div>

      <div className="mb-4">
        <h3>4. Registro de atividade</h3>
        <p>
          Por segurança, o sistema mantém um log de auditoria com as ações realizadas (login, logout,
          cadastros, vínculos, questões, atividades, redefinições de senha), incluindo data/hora, endereço IP e
          navegador utilizado. Esse registro existe para rastreabilidade e investigação de uso indevido, é
          visível apenas ao administrador da sua instituição e é apagado após o prazo da Política de Privacidade.
        </p>
      </div>

      <div className="mb-4">
        <h3>5. Banco de questões colaborativo e autoria</h3>
        <p>
          Questões cadastradas por um professor passam por avaliação de outros professores antes de
          entrarem em uso — uma questão só é aprovada com 3 votos favoráveis (ou rejeitada com 3 votos
          contrários) entre até 5 avaliadores. O autor não avalia a própria questão. Quem rejeita escreve o
          motivo, que o autor lê junto com o placar da votação. Enquanto é rascunho, a questão pode ser editada
          ou excluída pelo autor; depois de enviada para validação, não pode mais ser alterada.
        </p>
        <p>
          Ao enviar uma questão, o professor declara ser o autor do conteúdo ou ter autorização para usá-lo, e
          autoriza que ela, uma vez aprovada, seja usada por professores das demais instituições da plataforma,
          com indicação do seu nome como autor(a). Não envie conteúdo protegido por direitos autorais sem
          autorização (por exemplo, cópias de livros didáticos ou apostilas).
        </p>
      </div>

      <div className="mb-4">
        <h3>6. Atividades e respostas</h3>
        <p>
          Ao clicar numa alternativa, o aluno apenas a marca: a marcação fica guardada e pode ser trocada. A
          resposta só vale quando o aluno clica em “Confirmar resposta”; cada questão aceita uma única resposta
          confirmada, que não pode ser alterada depois. As questões de uma atividade ficam fixas a partir do
          momento em que o aluno a inicia.
        </p>
      </div>

      <div className="mb-4">
        <h3>7. Disponibilidade</h3>
        <p>
          O MaisEdu é um projeto acadêmico em desenvolvimento e não garante disponibilidade
          ininterrupta (SLA). Funcionalidades podem ser ajustadas ou ampliadas ao longo do projeto.
        </p>
      </div>

      <div className="mb-4">
        <h3>8. Alterações destes termos</h3>
        <p>
          Estes termos podem ser atualizados conforme o sistema evolui. Quando houver mudança relevante, a
          versão é atualizada e o sistema exige um novo aceite no próximo acesso, antes de qualquer outra ação.
        </p>
      </div>

      <p className="text-secondary small">
        Veja também a Política de Privacidade, que detalha quais dados pessoais são coletados e
        como são tratados.
      </p>
    </div>
  );
}
