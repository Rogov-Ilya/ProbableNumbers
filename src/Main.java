import java.util.*;


public class Main {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String expression = in.nextLine();
        List<Lexeme> lexeme = analyzeString(expression);
        LexemeBuffer lexemes = new LexemeBuffer(lexeme);
        Map<Integer, Float> result = new TreeMap<>(expr(lexemes));
        result.forEach((k, v) -> System.out.println(k + " " + String.format("%.2f", v * 100).replace(",", ".")));
    }

    //Анализ выражение с разбиением на отдельные части -> Массив Лексем
    public static List<Lexeme> analyzeString(String expText) {
        ArrayList<Lexeme> lexemes = new ArrayList<>();
        int position = 0;
        while (position < expText.length()) {
            char symbol = expText.charAt(position);
            //Обработаем строку заполнив масив - Тип,Символ
            switch (symbol) {
                case '(':
                    lexemes.add(new Lexeme(LexemeType.LEFT_BRACKET, symbol));
                    position++;
                    continue;

                case ')':
                    lexemes.add(new Lexeme(LexemeType.RIGHT_BRACKET, symbol));
                    position++;
                    continue;
                case '+':
                    lexemes.add(new Lexeme(LexemeType.PLUS, symbol));
                    position++;
                    continue;
                case '-':
                    lexemes.add(new Lexeme(LexemeType.MINUS, symbol));
                    position++;
                    continue;
                case '*':
                    lexemes.add(new Lexeme(LexemeType.MUL, symbol));
                    position++;
                    continue;
                case '/':
                    lexemes.add(new Lexeme(LexemeType.DIV, symbol));
                    position++;
                    continue;
                case '>':
                    lexemes.add(new Lexeme(LexemeType.MORE, symbol));
                    position++;
                    continue;
                case '<':
                    lexemes.add(new Lexeme(LexemeType.LESS, symbol));
                    position++;
                    continue;
                default:
                    if (symbol <= '9' && symbol >= '0') {
                        StringBuilder sb = new StringBuilder();
                        do {
                            sb.append(symbol);
                            position++;
                            if (position >= expText.length()) {
                                break;
                            }
                            symbol = expText.charAt(position);
                        } while (symbol <= '9' && symbol >= '0');
                        lexemes.add(new Lexeme(LexemeType.NUMBER, sb.toString()));
                    } else {
                        if (symbol != ' ') {//Не равно пробел
                            if (symbol == 'd') {//|| symbol <= '9' && symbol >= '0' && expText.charAt(position - 1) == 'd') {//Равен d или число и предыдущий символ = d
                                StringBuilder sb = new StringBuilder();
                                do {
                                    sb.append(symbol);
                                    position++;
                                    if (position >= expText.length()) {
                                        break;
                                    }
                                    symbol = expText.charAt(position);
                                } while (symbol == 'd' || symbol <= '9' && symbol >= '0');
                                lexemes.add(new LexemeDynamic(LexemeType.DYNAMIC, sb.toString()));
                            } else {
                                throw new RuntimeException("Неизвестный символ: " + symbol);
                            }
                        } else
                            position++;
                    }
            }
        }
        lexemes.add(new Lexeme(LexemeType.EOF, ""));
        return lexemes;
    }

    //Проверим что значение не пустое
    public static Map<Integer, Float> expr(LexemeBuffer lexemes) {
        Lexeme lexeme = lexemes.get();
        if (lexeme.type == LexemeType.EOF) {
            return Map.of(1, (float) 100);
        }
        return moreLess(lexemes);
    }

    //Больше, Меньше
    public static Map<Integer, Float> moreLess(LexemeBuffer lexemes) {
        Map<Integer, Float> value = plusMinus(lexemes);
        while (true) {
            Lexeme lexeme = lexemes.next();
            switch (lexeme.type) {
                case MORE:
                    Map<Integer, Float> factor = plusMinus(lexemes);
                    Map<Integer, Float> finalValue = new HashMap<>();
                    value.forEach((k, v) ->
                            factor.forEach((m, s) ->
                                    finalValue.merge(moreLessExt(k, m), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;
                case LESS:
                    factor = plusMinus(lexemes);
                    finalValue = new HashMap<>();
                    value.forEach((k, v) ->
                            factor.forEach((m, s) ->
                                    finalValue.merge(moreLessExt(m, k), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;

                default:
                    lexemes.back();
                    return value;

            }
        }
    }

    //Плюс, Минус
    public static Map<Integer, Float> plusMinus(LexemeBuffer lexemes) {
        Map<Integer, Float> value = multDiv(lexemes);//multDiv(lexemes);
        while (true) {
            Lexeme lexeme = lexemes.next();
            switch (lexeme.type) {
                case PLUS:
                    Map<Integer, Float> factor = multDiv(lexemes);
                    Map<Integer, Float> finalValue = new HashMap<>();
                    value.forEach((k, v) ->
                            factor.forEach((m, s) ->
                                    finalValue.merge((k + m), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;
                case MINUS:
                    factor = multDiv(lexemes);
                    finalValue = new HashMap<>();
                    value.forEach((k, v) ->
                            factor.forEach((m, s) ->
                                    finalValue.merge((k - m), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;
                default:
                    lexemes.back();
                    return value;
            }
        }
    }

    //Умножить,Поделить
    public static Map<Integer, Float> multDiv(LexemeBuffer lexemes) {
        Map<Integer, Float> value = factor(lexemes);
        while (true) {
            Lexeme lexeme = lexemes.next();
            switch (lexeme.type) {
                case MUL:
                    Map<Integer, Float> factor = factor(lexemes);
                    Map<Integer, Float> finalValue = new HashMap<>();
                    value.forEach((k, v) ->
                            factor.forEach((m, s) ->
                                    finalValue.merge((k * m), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;
                case DIV:
                    factor = factor(lexemes);
                    finalValue = new HashMap<>();
                    value.forEach((k, v) ->  //Пройдем по масиву мапов value
                            factor.forEach((m, s) -> //Пройдем по масиву следующих значений
                                    finalValue.merge((k / m), (v * s), (a, b) -> b = (a + v * s))
                            )
                    );
                    value = finalValue;
                    break;
                default:
                    lexemes.back();
                    return value;

            }
        }
    }

    //Унарный минус,Число,Скобка,Д
    public static Map<Integer, Float> factor(LexemeBuffer lexemes) {
        Lexeme lexeme = lexemes.next();
        Map<Integer, Float> value = new HashMap<>();
        switch (lexeme.type) {
            case MINUS:
                Map<Integer, Float> unary = factor(lexemes);
                Map<Integer, Float> finalValue = value;
                unary.forEach((k, v) -> finalValue.put(-k, v));
                return finalValue;
            case NUMBER:
                    value.put(Integer.valueOf(lexeme.value), (float) 1.0);
                    return value;

            case LEFT_BRACKET:
                value = expr(lexemes);
                lexeme = lexemes.next();
                if (lexeme.type != LexemeType.RIGHT_BRACKET) {
                    throw new RuntimeException("Не обнаруженна правя скобка");
                }
                return value;
            case DYNAMIC:
                LexemeDynamic lexemeD = (LexemeDynamic) lexeme;
                value.putAll(lexemeD.arr);
                return value;

            default:
                throw new RuntimeException("Не опознаное выражение" + lexeme.value);
        }
    }

    //Сравним два выражения для Лямбды
    public static Integer moreLessExt(int a, int b) {
        if (a > b) {
            return 1;
        } else {
            return 0;
        }
    }

    //Тип лексем
    public enum LexemeType {
        LEFT_BRACKET,//Левая скобка
        RIGHT_BRACKET,//Правая скобка
        PLUS,//Плюс
        MINUS,//Минус
        DIV,//Разделить
        MUL,//Умножить
        DYNAMIC,//D - Несколько вариантов
        MORE,//Больше
        LESS,//Меньше
        NUMBER,//Число
        EOF//Конец
    }

    //Лексема (Тип,Значение)
    public static class Lexeme {
        LexemeType type;
        String value;

        public Lexeme(LexemeType type, String value) {
            this.type = type;
            this.value = value;
        }

        public Lexeme(LexemeType type, Character value) {
            this.type = type;
            this.value = value.toString();
        }

        @Override
        public String toString() {
            return "Lexeme{" +
                    "type=" + type +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    //Лексема (Тип,Массив Д) наследуется от Lexeme
    public static class LexemeDynamic extends Lexeme {

        public Map<Integer, Float> arr;

        public LexemeDynamic(LexemeType type, String value) {
            super(type, value);
            int count = Integer.parseInt(value.substring(1));
            Map<Integer, Float> arr = new HashMap<>();
            for (int i = 1; i <= count; i++) {
                arr.put(i, (float) 1 / count);
            }
            this.arr = arr;
        }

    }

    //Буфер -> Массив и  перемещения по нему;
    public static class LexemeBuffer {
        public List<Lexeme> lexemes;
        private int position;

        public LexemeBuffer(List<Lexeme> lexemes) {
            this.lexemes = lexemes;
        }

        public Lexeme next() {
            return lexemes.get(position++);
        }

        public void back() {
            position--;
        }

        public Lexeme get() {
            return lexemes.get(position);
        }

        @Override
        public String toString() {
            return "LexemeBuffer{" +
                    "lexemes=" + lexemes +
                    ", position=" + position +
                    '}';
        }
    }

}
