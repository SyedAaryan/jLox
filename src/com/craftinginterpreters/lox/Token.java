package com.craftinginterpreters.lox;

class Token {

    final TokenType type; // The type of the token (e.g., identifier, keyword, literal)
    final String lexeme;  // The actual text of the token as it appeared in the source
    final Object literal; // The value of the token (if it's a literal, e.g., a number or string)
    final int line;      // The line number in the source code where the token was found

    // Constructor to initialize a Token with its type, lexeme, literal value, and line number
    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    // Override the toString method to provide a string representation of the token
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
