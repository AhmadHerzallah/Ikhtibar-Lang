grammar ARGrammar;

// Parser Rules
quiz      : QUIZ STRING NEWLINE question+ EOF;

question  : QUESTION STRING NEWLINE choices;

choices   : CHOICES COLON NEWLINE choice+;

choice    : STRING (CORRECT)? NEWLINE;

// Lexer Rules
QUIZ      : 'اختبار';
QUESTION  : 'سؤال';
CHOICES   : 'اختيارات';
CORRECT   : 'الجواب';
COLON     : ':';
STRING    : '"' (~["\r\n])* '"'; // Match Arabic text inside quotes
NEWLINE   : [\r\n]+;
WS        : [ \t]+ -> skip;
