package br.com.maisedu.app.config;

import br.com.maisedu.app.model.AnoEscolar;
import br.com.maisedu.app.model.NivelDificuldade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Gera questões de demonstração com gabarito calculado (não digitado à mão), para popular o banco
 * no perfil "dev". Os tópicos usam exatamente os nomes da lista fixa da migration V3.
 *
 * Cada questão tem 5 alternativas distintas e exatamente uma correta, em posição sorteada.
 * É determinístico: a mesma semente gera sempre as mesmas questões.
 */
public final class GeradorQuestoesDemo {

    public record QuestaoDemo(String topico, NivelDificuldade nivel, AnoEscolar anoEscolar, String enunciado,
                              List<String> alternativas, int indiceCorreta) {

        public String gabarito() {
            return alternativas.get(indiceCorreta);
        }
    }

    private record Item(String enunciado, String correta, List<String> distratores) {
    }

    @FunctionalInterface
    private interface Fabrica {
        Item criar(Random random);
    }

    private record Modelo(String topico, AnoEscolar ano, NivelDificuldade nivel, Fabrica fabrica) {
    }

    private final Random random;
    private final List<Modelo> modelos = new ArrayList<>();

    public GeradorQuestoesDemo(long semente) {
        this.random = new Random(semente);
        registrarModelos();
    }

    /** {@code porNivel} questões de cada tópico em cada nível de dificuldade. */
    public List<QuestaoDemo> gerar(int porNivel) {
        List<QuestaoDemo> questoes = new ArrayList<>();
        for (Modelo modelo : modelos) {
            Set<String> enunciados = new HashSet<>();
            int tentativas = 0;
            while (enunciados.size() < porNivel && tentativas < porNivel * 50) {
                tentativas++;
                Item item = modelo.fabrica().criar(random);
                if (!enunciados.add(item.enunciado())) {
                    continue;
                }
                questoes.add(montar(modelo, item));
            }
        }
        return questoes;
    }

    public List<String> topicos() {
        Set<String> nomes = new LinkedHashSet<>();
        for (Modelo modelo : modelos) {
            nomes.add(modelo.topico());
        }
        return new ArrayList<>(nomes);
    }

    private QuestaoDemo montar(Modelo modelo, Item item) {
        Set<String> distintos = new LinkedHashSet<>();
        for (String distrator : item.distratores()) {
            if (!distrator.equals(item.correta())) {
                distintos.add(distrator);
            }
        }
        completarDistratores(distintos, item.correta());
        List<String> distratores = new ArrayList<>(distintos);
        Collections.shuffle(distratores, random);
        if (distratores.size() < 4) {
            throw new IllegalStateException("Distratores insuficientes para: " + item.enunciado());
        }
        List<String> alternativas = new ArrayList<>(distratores.subList(0, 4));
        int indiceCorreta = random.nextInt(5);
        alternativas.add(indiceCorreta, item.correta());
        return new QuestaoDemo(modelo.topico(), modelo.nivel(), modelo.ano(), item.enunciado(),
                List.copyOf(alternativas), indiceCorreta);
    }

    /**
     * Rede de segurança: se as coincidências numéricas deixarem menos de 4 distratores, cria outros
     * variando o último número da resposta correta (ex.: "x = 5" -> "x = 8", "x = 2").
     */
    private static void completarDistratores(Set<String> distratores, String correta) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+(?!.*\\d)").matcher(correta);
        if (!m.find()) {
            return;
        }
        int valor = Integer.parseInt(m.group());
        int[] deslocamentos = {3, -3, 4, -4, 5, -5, 6, 7};
        for (int deslocamento : deslocamentos) {
            if (distratores.size() >= 4) {
                return;
            }
            String variante = correta.substring(0, m.start()) + (valor + deslocamento) + correta.substring(m.end());
            if (!variante.equals(correta)) {
                distratores.add(variante);
            }
        }
    }

    private void modelo(String topico, AnoEscolar ano, NivelDificuldade nivel, Fabrica fabrica) {
        modelos.add(new Modelo(topico, ano, nivel, fabrica));
    }

    // ------------------------------------------------------------------ modelos por tópico

    private void registrarModelos() {
        numerosInteiros();
        fracoes();
        porcentagem();
        equacoes();
        potenciacao();
        perimetroEArea();
        pitagoras();
        mediaModaMediana();
    }

    private void numerosInteiros() {
        String t = "Números inteiros";
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.FACIL, r -> {
            int a = -entre(r, 2, 15);
            int b = entre(r, 1, 20);
            int c = a + b;
            return new Item("Quanto é (" + a + ") + " + b + "?", n(c),
                    lista(n(-c), n(a - b), n(-a + b + 2), n(c + 1), n(c - 1), n(Math.abs(a) + b)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.MEDIO, r -> {
            int a = entre(r, 1, 12) * (r.nextBoolean() ? 1 : -1);
            int b = -entre(r, 2, 12);
            int c = a - b;
            return new Item("Calcule " + a + " - (" + b + ").", n(c),
                    lista(n(a + b), n(-c), n(c + 1), n(c - 2), n(-(a + b)), n(c + 2)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.DIFICIL, r -> {
            int a = -entre(r, 2, 9);
            int b = entre(r, 2, 9);
            int c = -entre(r, 1, 12);
            int resultado = a * b + c;
            return new Item("Qual é o valor de (" + a + ") × " + b + " + (" + c + ")?", n(resultado),
                    lista(n(-resultado), n(a * b - c), n(a * (b + c)), n(resultado + 1), n(resultado - 1),
                            n(-a * b + c)));
        });
    }

    private void fracoes() {
        String t = "Frações";
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.FACIL, r -> {
            int d = escolher(r, 5, 7, 9, 11, 13);
            int a = entre(r, 1, d / 2);
            int b = entre(r, 1, d - a - 1);
            return new Item("Quanto é " + a + "/" + d + " + " + b + "/" + d + "?", fracao(a + b, d),
                    lista(fracaoBruta(a + b, d + d), fracaoBruta(a * b, d), fracaoBruta(a + b + 1, d),
                            fracaoBruta(Math.abs(a - b) + 1, d), fracaoBruta(a + b, d * d)));
        });
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.MEDIO, r -> {
            int a = entre(r, 2, 6);
            int b = entre(r, 2, 7);
            if (a == b) {
                b = a + 1;
            }
            return new Item("Quanto é 1/" + a + " + 1/" + b + "?", fracao(a + b, a * b),
                    lista(fracaoBruta(2, a + b), fracaoBruta(1, a + b), fracaoBruta(2, a * b),
                            fracaoBruta(a + b + 1, a * b), fracaoBruta(a * b, a + b)));
        });
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.DIFICIL, r -> {
            int denominador = escolher(r, 3, 4, 5, 6, 8);
            int numerador = entre(r, 2, denominador - 1);
            int total = denominador * entre(r, 4, 10);
            int parte = total / denominador * numerador;
            return new Item("Uma turma tem " + total + " alunos e " + numerador + "/" + denominador
                    + " deles foram à feira de ciências. Quantos alunos foram à feira?", n(parte),
                    lista(n(total / denominador), n(total - parte), n(parte + numerador), n(total / numerador),
                            n(parte - denominador), n(total * numerador)));
        });
    }

    private void porcentagem() {
        String t = "Porcentagem";
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.FACIL, r -> {
            int p = escolher(r, 10, 20, 25, 50);
            int total = 20 * entre(r, 2, 20);
            int valor = total * p / 100;
            return new Item("Quanto é " + p + "% de " + total + "?", n(valor),
                    lista(n(valor * 2), n(total - valor), n(valor + 10), n(Math.max(1, valor / 2)), n(total / 10 + p)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.MEDIO, r -> {
            int p = escolher(r, 15, 30, 35, 40, 45);
            int preco = 20 * entre(r, 3, 25);
            int desconto = preco * p / 100;
            return new Item("Um produto custa R$ " + preco + " e está com " + p
                    + "% de desconto. De quantos reais é o desconto?", reais(desconto),
                    lista(reais(preco - desconto), reais(desconto * 2), reais(p), reais(desconto + 10),
                            reais(preco / 10)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.DIFICIL, r -> {
            int preco = escolher(r, 100, 200, 300, 400, 500, 800);
            int p = escolher(r, 10, 20);
            int depoisDoAumento = preco + preco * p / 100;
            int fim = depoisDoAumento - depoisDoAumento * p / 100;
            return new Item("Um produto de R$ " + preco + " teve aumento de " + p + "% e, depois, desconto de " + p
                    + "% sobre o novo preço. Qual é o preço final?", reais(fim),
                    lista(reais(preco), reais(depoisDoAumento), reais(preco - preco * p / 100), reais(fim + p),
                            reais(preco + p)));
        });
    }

    private void equacoes() {
        String t = "Equações do 1º grau";
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.FACIL, r -> {
            int a = entre(r, 2, 15);
            int x = entre(r, 1, 20);
            int b = x + a;
            return new Item("Qual é o valor de x na equação x + " + a + " = " + b + "?", "x = " + x,
                    lista("x = " + (b + a), "x = " + (a - b), "x = " + b, "x = " + (x + 1), "x = " + (x - 1)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.MEDIO, r -> {
            int a = entre(r, 2, 6);
            int x = entre(r, -5, 10);
            if (x == 0) {
                x = 3;
            }
            int b = entre(r, 1, 12) * (r.nextBoolean() ? 1 : -1);
            int c = a * x + b;
            return new Item("Resolva a equação " + a + "x " + sinal(b) + " = " + c + ".", "x = " + x,
                    lista("x = " + (c + b), "x = " + (c - b), "x = " + (-x), "x = " + (x + 1), "x = " + (x - 2),
                            "x = " + (c * a)));
        });
        modelo(t, AnoEscolar.OITAVO_ANO, NivelDificuldade.DIFICIL, r -> {
            int a = entre(r, 2, 4);
            int c = a == 2 ? 1 : entre(r, 1, a - 1);
            int b = entre(r, 1, 6);
            int x = entre(r, 1, 9);
            int d = a * (x + b) - c * x;
            String ladoDireito = (c == 1 ? "x" : c + "x") + " " + sinal(d);
            return new Item("Resolva a equação " + a + "(x + " + b + ") = " + ladoDireito + ".", "x = " + x,
                    lista("x = " + (x + 1), "x = " + (x - 1), "x = " + (-x), "x = " + (d - a * b),
                            "x = " + (d - b), "x = " + (x + a)));
        });
    }

    private void potenciacao() {
        String t = "Potenciação e radiciação";
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.FACIL, r -> {
            int base = entre(r, 3, 15);
            return new Item("Quanto é " + base + "²?", n(base * base),
                    lista(n(2 * base), n(base + 2), n(base * base + 1),
                            n((base + 1) * (base + 1)), n(base * base - 1)));
        });
        modelo(t, AnoEscolar.OITAVO_ANO, NivelDificuldade.MEDIO, r -> {
            int raiz = entre(r, 6, 20);
            int quadrado = raiz * raiz;
            return new Item("Qual é a raiz quadrada de " + quadrado + "?", n(raiz),
                    lista(n(quadrado / 2), n(raiz + 1), n(raiz - 1), n(2 * raiz), n(raiz + 2)));
        });
        modelo(t, AnoEscolar.OITAVO_ANO, NivelDificuldade.DIFICIL, r -> {
            int base = escolher(r, 2, 3, 5, 7);
            int m = entre(r, 2, 7);
            int k = entre(r, 2, 7);
            String resposta = potencia(base, m + k);
            return new Item("Escreva " + potencia(base, m) + " · " + potencia(base, k) + " como uma única potência.",
                    resposta,
                    lista(potencia(base, m * k), potencia(base * base, m + k), potencia(base * base, m * k),
                            potencia(base, Math.abs(m - k) + 1), potencia(base, m + k + 1)));
        });
    }

    private void perimetroEArea() {
        String t = "Perímetro e área";
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.FACIL, r -> {
            int a = entre(r, 3, 15);
            int b = entre(r, 2, 12);
            if (a == b) {
                a++;
            }
            int perimetro = 2 * (a + b);
            return new Item("Qual é o perímetro de um retângulo de " + a + " cm por " + b + " cm?", perimetro + " cm",
                    lista((a * b) + " cm", (a + b) + " cm", (perimetro + 2) + " cm", (perimetro - 2) + " cm",
                            (4 * a) + " cm"));
        });
        modelo(t, AnoEscolar.SEXTO_ANO, NivelDificuldade.MEDIO, r -> {
            int a = entre(r, 3, 15);
            int b = entre(r, 2, 12);
            if (a == b) {
                a++;
            }
            int area = a * b;
            return new Item("Qual é a área de um retângulo de " + a + " m por " + b + " m?", area + " m²",
                    lista((2 * (a + b)) + " m²", (a + b) + " m²", (area + a) + " m²", (area - b) + " m²",
                            (area * 2) + " m²"));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.DIFICIL, r -> {
            int base = 2 * entre(r, 2, 10);
            int altura = entre(r, 3, 12);
            int area = base * altura / 2;
            return new Item("Qual é a área de um triângulo com base de " + base + " cm e altura de " + altura + " cm?",
                    area + " cm²",
                    lista((base * altura) + " cm²", (base + altura) + " cm²", (area + base) + " cm²",
                            (area - altura) + " cm²", (2 * (base + altura)) + " cm²"));
        });
    }

    private void pitagoras() {
        String t = "Teorema de Pitágoras";
        int[][] ternos = {{3, 4, 5}, {6, 8, 10}, {5, 12, 13}, {8, 15, 17}, {9, 12, 15}, {7, 24, 25}, {12, 16, 20},
                {20, 21, 29}, {15, 20, 25}, {10, 24, 26}};
        modelo(t, AnoEscolar.NONO_ANO, NivelDificuldade.FACIL, r -> {
            int k = entre(r, 1, 6);
            int a = 3 * k;
            int b = 4 * k;
            int c = 5 * k;
            return new Item("Os catetos de um triângulo retângulo medem " + a + " cm e " + b
                    + " cm. Quanto mede a hipotenusa?", c + " cm",
                    lista((a + b) + " cm", (c + 1) + " cm", (b + 1) + " cm", (c - 1) + " cm", (2 * c) + " cm"));
        });
        modelo(t, AnoEscolar.NONO_ANO, NivelDificuldade.MEDIO, r -> {
            int[] terno = ternos[entre(r, 2, ternos.length - 1)];
            int a = terno[0];
            int b = terno[1];
            int c = terno[2];
            return new Item("Em um triângulo retângulo, os catetos medem " + a + " m e " + b
                    + " m. Qual é a medida da hipotenusa?", c + " m",
                    lista((a + b) + " m", (c + 1) + " m", (c - 1) + " m", (c + 2) + " m", (b - a) + " m"));
        });
        modelo(t, AnoEscolar.NONO_ANO, NivelDificuldade.DIFICIL, r -> {
            int[] terno = ternos[entre(r, 0, ternos.length - 1)];
            int a = terno[0];
            int b = terno[1];
            int c = terno[2];
            return new Item("A hipotenusa de um triângulo retângulo mede " + c + " cm e um dos catetos mede " + b
                    + " cm. Quanto mede o outro cateto?", a + " cm",
                    lista((c - b) + " cm", (c + b) + " cm", (a + 1) + " cm", (a - 1) + " cm", (a + 2) + " cm"));
        });
    }

    private void mediaModaMediana() {
        String t = "Média, moda e mediana";
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.FACIL, r -> {
            int media = entre(r, 4, 9);
            int d = entre(r, 1, 3);
            int[] valores = {media - d, media, media + d};
            embaralhar(valores, r);
            int soma = 3 * media;
            return new Item("Qual é a média aritmética de " + listar(valores) + "?", n(media),
                    lista(n(soma), n(media + 1), n(media - 1), n(media + d + 1), n(soma / 2)));
        });
        modelo(t, AnoEscolar.SETIMO_ANO, NivelDificuldade.MEDIO, r -> {
            Set<Integer> conjunto = new LinkedHashSet<>();
            while (conjunto.size() < 5) {
                conjunto.add(entre(r, 1, 30));
            }
            int[] valores = new int[5];
            int i = 0;
            for (Integer v : conjunto) {
                valores[i++] = v;
            }
            int[] ordenados = valores.clone();
            Arrays.sort(ordenados);
            int mediana = ordenados[2];
            if (valores[2] == mediana) {
                int tmp = valores[2];
                valores[2] = valores[0];
                valores[0] = tmp;
            }
            return new Item("Qual é a mediana do conjunto " + listar(valores) + "?", n(mediana),
                    lista(n(valores[2]), n(ordenados[1]), n(ordenados[3]), n(ordenados[0]), n(ordenados[4]),
                            n(mediana + 1)));
        });
        modelo(t, AnoEscolar.OITAVO_ANO, NivelDificuldade.DIFICIL, r -> {
            int[] notas = new int[5];
            int soma;
            do {
                soma = 0;
                for (int i = 0; i < notas.length; i++) {
                    notas[i] = entre(r, 4, 10);
                    soma += notas[i];
                }
            } while (soma % 5 == 0);
            BigDecimal media = BigDecimal.valueOf(soma).divide(BigDecimal.valueOf(5), 1, RoundingMode.UNNECESSARY);
            return new Item("Um aluno tirou as notas " + listar(notas)
                    + " nas cinco provas do ano. Qual foi a média dele?", decimal(media),
                    lista(decimal(media.add(new BigDecimal("0.2"))), decimal(media.subtract(new BigDecimal("0.2"))),
                            decimal(BigDecimal.valueOf(soma).divide(BigDecimal.valueOf(4), 1, RoundingMode.HALF_UP)),
                            decimal(media.add(BigDecimal.ONE)), decimal(BigDecimal.valueOf(soma, 1))));
        });
    }

    // ------------------------------------------------------------------ utilitários

    private static int entre(Random r, int minimo, int maximo) {
        return minimo + r.nextInt(maximo - minimo + 1);
    }

    private static int escolher(Random r, int... opcoes) {
        return opcoes[r.nextInt(opcoes.length)];
    }

    private static List<String> lista(String... valores) {
        return Arrays.asList(valores);
    }

    private static String n(int valor) {
        return String.valueOf(valor);
    }

    private static String reais(int valor) {
        return "R$ " + valor;
    }

    private static String decimal(BigDecimal valor) {
        return valor.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String sinal(int valor) {
        return valor < 0 ? "- " + (-valor) : "+ " + valor;
    }

    private static int mdc(int a, int b) {
        return b == 0 ? Math.abs(a) : mdc(b, a % b);
    }

    /** Fração na forma irredutível. */
    private static String fracao(int numerador, int denominador) {
        int divisor = mdc(numerador, denominador);
        int num = numerador / divisor;
        int den = denominador / divisor;
        return den == 1 ? String.valueOf(num) : num + "/" + den;
    }

    /** Distrator também na forma irredutível, para não ser "equivalente" à resposta correta. */
    private static String fracaoBruta(int numerador, int denominador) {
        return fracao(numerador, denominador);
    }

    private static final String SOBRESCRITOS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private static String potencia(int base, int expoente) {
        StringBuilder sb = new StringBuilder().append(base);
        for (char digito : String.valueOf(expoente).toCharArray()) {
            sb.append(SOBRESCRITOS.charAt(digito - '0'));
        }
        return sb.toString();
    }

    private static String listar(int[] valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(i == valores.length - 1 ? " e " : ", ");
            }
            sb.append(valores[i]);
        }
        return sb.toString();
    }

    private static void embaralhar(int[] valores, Random r) {
        for (int i = valores.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int tmp = valores[i];
            valores[i] = valores[j];
            valores[j] = tmp;
        }
    }
}
