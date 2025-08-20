// Java
import org.antlr.v4.runtime.Token;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Listener that enforces strict validation rules:
 * - Each question must have exactly one correct answer (one CORRECT token).
 */
public class ARValidationListener extends ARGrammarBaseListener {

    private final List<String> errors = new ArrayList<>();
    private final Deque<Integer> correctCountStack = new ArrayDeque<>();

    @Override
    public void enterQuestion(ARGrammarParser.QuestionContext ctx) {
        correctCountStack.push(0);
    }

    @Override
    public void exitChoice(ARGrammarParser.ChoiceContext ctx) {
        if (!correctCountStack.isEmpty() && ctx.CORRECT() != null) {
            int current = correctCountStack.pop();
            correctCountStack.push(current + 1);
        }
    }

    @Override
    public void exitQuestion(ARGrammarParser.QuestionContext ctx) {
        int count = correctCountStack.isEmpty() ? 0 : correctCountStack.pop();
        if (count != 1) {
            Token t = ctx.STRING().getSymbol();
            int line = t != null ? t.getLine() : -1;
            int col = t != null ? t.getCharPositionInLine() : -1;
            String qText = unquote(ctx.STRING().getText());
            errors.add("Question '" + qText + "' must have exactly one correct answer, found " + count
                    + (line >= 0 ? (" (line " + line + ", col " + col + ")") : ""));
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    private static String unquote(String s) {
        if (s == null || s.length() < 2) return s;
        String core = s.substring(1, s.length() - 1);
        return core.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}

