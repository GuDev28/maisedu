-- Dados fixos que no PostgreSQL sao inseridos pelas migrations V1 e V3.
-- No H2 de teste o Flyway fica desligado, entao este script repete esses dados.

INSERT INTO role (nome) VALUES ('ALUNO');
INSERT INTO role (nome) VALUES ('PROFESSOR');
INSERT INTO role (nome) VALUES ('ADMIN');

INSERT INTO topico (nome) VALUES ('Números naturais e operações');
INSERT INTO topico (nome) VALUES ('Números inteiros');
INSERT INTO topico (nome) VALUES ('Frações');
INSERT INTO topico (nome) VALUES ('Números decimais');
INSERT INTO topico (nome) VALUES ('Potenciação e radiciação');
INSERT INTO topico (nome) VALUES ('Porcentagem');
INSERT INTO topico (nome) VALUES ('Razão e proporção');
INSERT INTO topico (nome) VALUES ('Expressões algébricas');
INSERT INTO topico (nome) VALUES ('Equações do 1º grau');
INSERT INTO topico (nome) VALUES ('Sistemas de equações do 1º grau');
INSERT INTO topico (nome) VALUES ('Equações do 2º grau');
INSERT INTO topico (nome) VALUES ('Ângulos');
INSERT INTO topico (nome) VALUES ('Triângulos e quadriláteros');
INSERT INTO topico (nome) VALUES ('Circunferência e círculo');
INSERT INTO topico (nome) VALUES ('Teorema de Pitágoras');
INSERT INTO topico (nome) VALUES ('Perímetro e área');
INSERT INTO topico (nome) VALUES ('Volume e capacidade');
INSERT INTO topico (nome) VALUES ('Probabilidade');
INSERT INTO topico (nome) VALUES ('Gráficos e tabelas');
INSERT INTO topico (nome) VALUES ('Média, moda e mediana');
