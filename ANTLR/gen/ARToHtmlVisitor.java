// Java
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This visitor walks through the parsed quiz grammar (produced by ANTLR)
 * and turns it into a Java quiz model — then can render it into HTML.
 * Think of it as: text in → model → HTML out.
 */


public class ARToHtmlVisitor extends ARGrammarBaseVisitor<ARToHtmlVisitor.Node> {

    // Quiz model / structure
    // This holds the whole quiz: title + all its questions.
    public static final class Quiz {
        public final String title;
        public final List<Question> questions = new ArrayList<>();
        public Quiz(String title) { this.title = title; }
    }
    // Represents one question with its text + list of choices
    public static final class Question {
        public final String text;
        public final List<Choice> choices = new ArrayList<>();
        public Question(String text) { this.text = text; }
        public long correctCount() {
            return choices.stream().filter(c -> c.correct).count();
        }
    }
    // single possible answer choice
    public static final class Choice {
        public final String text;
        public final boolean correct;
        public Choice(String text, boolean correct) { this.text = text; this.correct = correct; }
    }

    // Visitor return wrapper
    // sealed interface is used to enforce the visitor pattern.
    public sealed interface Node permits QuizNode, QuestionNode, ChoicesNode, ChoiceNode {}
    public static final class QuizNode implements Node { public final Quiz quiz; public QuizNode(Quiz q){ this.quiz=q; } }
    public static final class QuestionNode implements Node { public final Question question; public QuestionNode(Question q){ this.question=q; } }
    public static final class ChoicesNode implements Node { public final List<Choice> choices; public ChoicesNode(List<Choice> c){ this.choices=c; } }
    public static final class ChoiceNode implements Node { public final Choice choice; public ChoiceNode(Choice c){ this.choice=c; } }

    // Visitor implementation
    @Override
    public Node visitQuiz(ARGrammarParser.QuizContext ctx) {
      // Strip quotes from title, make a Quiz object
        String title = unquote(ctx.STRING().getText());
        Quiz quiz = new Quiz(title);
        // Visit each question and add it to quiz
        for (ARGrammarParser.QuestionContext qctx : ctx.question()) {
            QuestionNode qn = (QuestionNode) visitQuestion(qctx);
            quiz.questions.add(qn.question);
        }
        return new QuizNode(quiz);
    }

    @Override
    // Visit the question context and return a QuestionNode
    public Node visitQuestion(ARGrammarParser.QuestionContext ctx) {
        String qText = unquote(ctx.STRING().getText());
        Question q = new Question(qText);
        ChoicesNode cn = (ChoicesNode) visitChoices(ctx.choices());
        q.choices.addAll(cn.choices);
        long count = q.correctCount();
        if (count != 1) {
            org.antlr.v4.runtime.Token t = ctx.STRING().getSymbol();
            int line = t != null ? t.getLine() : -1;
            int col = t != null ? t.getCharPositionInLine() : -1;
            throw new IllegalArgumentException(
                "Question '" + qText + "' must have exactly one correct answer, found " + count +
                (line >= 0 ? (" (line " + line + ", col " + col + ")") : "")
            );
        }
        return new QuestionNode(q);
    }

    @Override
    // Visit the choices context and return a ChoicesNode
    public Node visitChoices(ARGrammarParser.ChoicesContext ctx) {
        List<Choice> list = new ArrayList<>();
        for (ARGrammarParser.ChoiceContext cctx : ctx.choice()) {
            ChoiceNode ch = (ChoiceNode) visitChoice(cctx);
            list.add(ch.choice);
        }
        return new ChoicesNode(list);
    }

    @Override
    // Visit the choice context and return a ChoiceNode
    public Node visitChoice(ARGrammarParser.ChoiceContext ctx) {
        String text = unquote(ctx.STRING().getText());
        boolean correct = ctx.CORRECT() != null || hasTerminal(ctx, "CORRECT");
        return new ChoiceNode(new Choice(text, correct));
    }

    // Public render API
    // Render the quiz model to HTML
    // Documentation for rendering HTML in Java: https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringEscapeUtils.html
    public static String renderAsHtml(Quiz quiz) {
        String title = htmlEscape(quiz.title);
        StringBuilder body = new StringBuilder(2048); // large buffer for efficiency

        body.append("<main class=\"quiz\">\n");
        body.append("<header><h1> اختبار ").append(title).append("</h1></header>\n");

        int qIndex = 1;
        for (Question q : quiz.questions) {
            String qId = "q" + qIndex++;
            body.append("<section class=\"question\" id=\"").append(qId).append("\">\n");
            body.append("<h2>").append(htmlEscape(q.text)).append("</h2>\n");

            body.append("<ul class=\"choices\">\n");
            int cIndex = 0;
            for (Choice c : q.choices) {
                String choiceId = qId + "_c" + (++cIndex);
                body.append("<li>")
                    .append("<label>")
                    .append("<input type=\"radio\" name=\"").append(qId).append("\" value=\"").append(cIndex).append("\"")
                    .append(c.correct ? " data-correct=\"true\"" : "")
                    .append(">")
                    .append(htmlEscape(c.text))
                    .append("</label>")
                    .append("</li>\n");
            }
            body.append("</ul>\n");
            body.append("</section>\n");
        }
        // Buttons and score area
        body.append("<div class=\"actions\">\n")
            .append("<button id=\"checkBtn\" type=\"button\">تحقق من الإجابات</button>\n")
            .append("<button id=\"resetBtn\" type=\"button\">إعادة تعيين</button>\n")
            .append("<span id=\"score\"></span>\n")
            .append("</div>\n");
        //  Wrap the body inside a full HTML document (with CSS + JS)
        return """
              <!doctype html>
              <html lang="ar" dir="rtl">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.5; }
                  .quiz { max-width: 600px; margin: auto; }
                  h1 { font-size: 1.5rem; margin-bottom: 1rem; }
                  .question { padding: 10px; border: 1px solid #ccc; margin-bottom: 15px; border-radius: 5px; }
                  h2 { font-size: 1.1rem; margin-bottom: 0.5rem; }
                  .choices { list-style: none; padding: 0; }
                  .choices li { margin-bottom: 5px; }
                  .right { background-color: #d4edda; }
                  .wrong { background-color: #f8d7da; }
                  .actions { margin-top: 15px; display: flex; gap: 10px; align-items: center; }
                  button { padding: 5px 10px; border: 1px solid #999; background: #eee; cursor: pointer; }
                  button:hover { background: #ddd; }
                  #score { font-weight: bold; }
                </style>
              </head>
              <body>
              %s
              <script>
              (function(){
                const $$ = sel => document.querySelectorAll(sel);
                const scoreEl = document.getElementById("score");

                function clearMarks(q){
                  q.classList.remove('right','wrong');
                }

                document.getElementById("checkBtn").addEventListener("click", () => {
                  let total = 0, correct = 0;
                  $$(".question").forEach(q => {
                    clearMarks(q);
                    const selected = q.querySelector("input[type=radio]:checked");
                    if (selected) {
                      total++;
                      if (selected.dataset.correct === "true") {
                        q.classList.add("right");
                        correct++;
                      } else {
                        q.classList.add("wrong");
                      }
                    }
                  });
                  scoreEl.textContent = "النتيجة: " + correct + " / " + total;
                });

                document.getElementById("resetBtn").addEventListener("click", () => {
                  $$(".question").forEach(q => {
                    clearMarks(q);
                    q.querySelectorAll("input[type=radio]").forEach(inp => inp.checked = false);
                  });
                  scoreEl.textContent = "";
                });
              })();
              </script>
              </body>
              </html>
              """.formatted(title, body.toString());
    }

    // Helper methods
    // Remove surrounding quotes from a string and unescape
    private static String unquote(String s) {
        if (s == null || s.length() < 2) return s;
        String core = s.substring(1, s.length() - 1);
        return core.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // Escape the string to be used in HTML
    // replace <, >, &, quotes, etc.
    private static String htmlEscape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    // Check if the choice context has a terminal node with the given name
    private static boolean hasTerminal(ARGrammarParser.ChoiceContext ctx, String name) {
        int tokenType = tokenTypeOf(name);
        if (tokenType == org.antlr.v4.runtime.Token.INVALID_TYPE) {
            return false;
        }
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof org.antlr.v4.runtime.tree.TerminalNode tn &&
                    tn.getSymbol().getType() == tokenType) {
                return true;
            }
        }
        return false;
    }

    // Find the token type integer for a given symbolic or literal token name
    private static int tokenTypeOf(String name) {
        org.antlr.v4.runtime.Vocabulary v = ARGrammarParser.VOCABULARY;
        for (int t = 0; t <= v.getMaxTokenType(); t++) {
            String sym = v.getSymbolicName(t);
            String lit = v.getLiteralName(t);
            if (name.equals(sym) || name.equals(lit)) {
                return t;
            }
        }
        return org.antlr.v4.runtime.Token.INVALID_TYPE;
    }
}