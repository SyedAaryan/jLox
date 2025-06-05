package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

// We store the list of tokens and use "current" to point to the next token
// For the grammar, check docs/grammar, we are basically converting our grammar into the java code
// In every method, I have provided the grammar of that method
class Parser {

    private static class ParserError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // Method to kick the parsing
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    //expression     → assignment ;
    private Expr expression() {
        return assignment();
    }

    // declaration    → varDecl| statement ;
    private Stmt declaration() {
        try {
            //checks for variable declaration, if yes, calls the varDeclaration() function, else statement().
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParserError error) {
            synchronize();
            return null;
        }
    }

    //statement      → exprStmt| printStmt| block ;
    private Stmt statement() {
        // If the next token is "print", it returns the value returned by printStatement()
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        //else it returns the value returned by expressionStatement()
        return expressionStatement();
    }

    //printStmt      → "print" expression ";" ;
    private Stmt printStatement() {
        // Expression function is called and the value returned by it is stored in "value"
        Expr value = expression();

        // Checks for semicolon after the end of the expression
        consume(SEMICOLON, "Expect ';' after value.");

        return new Stmt.Print(value);
    }

    //varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    private Stmt varDeclaration() {
        // Consumes the "IDENTIFIER", i,e the name of the variable
        Token name = consume(IDENTIFIER, "Expect variable Name.");

        // Initially keeps the initializer, i,e after the variable declaration as null
        Expr initializer = null;
        if (match(EQUAL)) { // Checks for "=", if present, stores the expression after "=" in initializer
            initializer = expression();
        }

        // Semicolon must be there after the statement
        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    //Works similar to the printStatement() function
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after the expression.");
        return new Stmt.Expression(expr);
    }

    // Used to parse blocks
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        //Checks for "{" and for "EOF"
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            // Calls the declaration function for every statement in the block
            statements.add(declaration());
        }

        // Checks for "}" at the end of the block.
        consume(RIGHT_BRACE, "Expect '}' after a block");
        return statements;
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {

        // Story the left comparison in expr variable
        Expr expr = comparison();

        /*for the ( ( "!=" | "==" ) comparison )*, we are checking if it matches with != or == with a while loop
        if we don't see them, it means we are done with that sequence*/
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            // Operator is previous since we "match" above, and we advanced in the token list, hence the operator will the "previous"
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    //comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {

        // Storing the 1st term in the var "expr"
        Expr expr = term();

        // while loop similar to the "equality" function
        while (match(GREATER_EQUAL, GREATER, LESS, LESS_EQUAL)) {
            // Operator is previous since we "match" above, and we advanced in the token list, hence the operator will the "previous"
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);

        }

        return expr;

    }

    //term           → factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {

        // This works similar to "equality" and "comparison" methods
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    //factor         → unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {

        // This works similar to "equality" and "comparison" methods
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    //unary          → ( "!" | "-" ) unary | primary ;
    private Expr unary() {
        // if condition for ( "!" | "-" ) unary, if it doesn't start with ! or -, it returns "primary" from the grammar
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    //primary        → "true" | "false" | "nil"| NUMBER | STRING| "(" expression ")"| IDENTIFIER ;
    // Since most of the cases are terminals, this is pretty straight forward
    private Expr primary() {

        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            // Remember that the reason for using "previous" is "match" advances the token list after checking, hence the previous
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");

    }

    // To see if the token matches the token type
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            // To check the type
            if (check(type)) {
                // Advances to the next token is the type is matched
                advance();
                return true;
            }
        }
        return false;
    }

    // It is similar to match, if the given token is of the type, it returns the token, else throws the "error"
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // This returns true if the token is of its given type
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // It consumes the current token and returns it
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Checks if all tokens are parsed, does that by returning true if the current token type is "EOF"
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    // returns the current token
    private Token peek() {
        return tokens.get(current);
    }

    // returns the previous token
    private Token previous() {
        return tokens.get(current - 1);
    }

    // Throws the error
    private ParserError error(Token token, String message) {
        Lox.error(token, message);
        return new ParserError();
    }

    //For parser error
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

}
