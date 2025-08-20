// Java
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ARHtmlMain {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length > 2) {
            System.err.println("Usage: java ARHtmlMain <input.txt> [output.html]");
            System.exit(1);
        }
        Path in = Path.of(args[0]);
        Path out = args.length == 2 ? Path.of(args[1]) : replaceExt(in, ".html");

        CharStream input = CharStreams.fromPath(in, StandardCharsets.UTF_8);
        ARGrammarLexer lexer = new ARGrammarLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ARGrammarParser parser = new ARGrammarParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new DiagnosticErrorListener());

        ParseTree tree = parser.quiz();

        // Pass 1: Listener-based validation
        org.antlr.v4.runtime.tree.ParseTreeWalker.DEFAULT.walk(new ARValidationListener() {
            @Override
            public void exitQuiz(ARGrammarParser.QuizContext ctx) {
                if (hasErrors()) {
                    System.err.println("Validation errors:");
                    for (String e : getErrors()) {
                        System.err.println(" - " + e);
                    }
                    System.exit(2);
                }
            }
        }, tree);

        // Pass 2: Visitor build + render
        ARToHtmlVisitor visitor = new ARToHtmlVisitor();
        // Visit the parse tree and build the quiz model
        ARToHtmlVisitor.QuizNode qn;
        // qn is the root of the quiz model
        qn = (ARToHtmlVisitor.QuizNode) visitor.visit(tree);

        String html = ARToHtmlVisitor.renderAsHtml(qn.quiz); // Render the quiz model to HTML

        Files.writeString(out, html, StandardCharsets.UTF_8);
        System.out.println("Wrote: " + out.toAbsolutePath());
    }
    
    // Replace the extension of the path with the new extension
    private static Path replaceExt(Path path, String newExt) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0 ? name.substring(0, dot) : name);
        return path.resolveSibling(base + newExt);
    }
}