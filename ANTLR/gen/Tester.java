// Java
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Tester {
    public static void main(String[] args) {
        String src = String.join("\n",
                "اختبار \"اختبار عينة\"",
                "سؤال \"ما هي عاصمة فرنسا؟\"",
                "اختيارات:",
                "\"باريس\" الجواب",
                "\"لندن\"",
                "\"روما\"",
                ""
        );

        ARGrammarLexer lexer = new ARGrammarLexer(CharStreams.fromString(src));
        ARGrammarParser parser = new ARGrammarParser(new CommonTokenStream(lexer));
        ARGrammarParser.QuizContext quiz = parser.quiz();

        for (ARGrammarParser.QuestionContext q : quiz.question()) {
            for (ARGrammarParser.ChoiceContext c : q.choices().choice()) {
                boolean hasCorrect = false;
                for (int i = 0; i < c.getChildCount(); i++) {
                    if (c.getChild(i) instanceof TerminalNode tn) {
                        if (tn.getSymbol().getType() == ARGrammarParser.CORRECT) {
                            hasCorrect = true;
                            break;
                        }
                    }
                }
                String choiceText = c.getChild(0).getText(); // the STRING token (with quotes)
                System.out.println(choiceText + " => " + (hasCorrect ? "CORRECT" : "NOT CORRECT"));
            }
        }
    }
}