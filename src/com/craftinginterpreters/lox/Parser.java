package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
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
            if (match(FUN)) return function("function");
            //checks for variable declaration, if yes, calls the varDeclaration() function, else statement().
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParserError error) {
            synchronize();
            return null;
        }
    }

    //statement      → exprStmt|ifStmt| printStmt|whileStmt| forStmt| block ;
    private Stmt statement() {
        // If the token is "for"
        if (match(FOR)) return forStatement();
        // If the token is "if", it returns the value returned by ifStatement()
        if (match(IF)) return ifStatement();
        // If the next token is "print", it returns the value returned by printStatement()
        if (match(PRINT)) return printStatement();
        // For return statement
        if (match(RETURN)) return returnStatement();
        // for while loops
        if (match(WHILE)) return whileStatement();
        // If its "{", it executes block
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        //else it returns the value returned by expressionStatement()
        return expressionStatement();
    }

    //forStmt        → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";"expression? ")" statement ;
    // for "for loop", we are using the components from the interpreter instead of making new type
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        //Initializer
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // Condition
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        //Incrementation
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after the clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    //ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
    private Stmt ifStatement() {
        // The first there lines checks the following part of the grammar '"(" expression ")"', "if" is already parsed before this function is called
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement(); // "statement"
        Stmt elseBranch = null; // initially keeping the else branch as null.
        if (match(ELSE)) { // If "else" is present, then the expression followed by it is stored in elseBranch.
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    //printStmt      → "print" expression ";" ;
    private Stmt printStatement() {
        // Expression function is called and the value returned by it is stored in "value"
        Expr value = expression();

        // Checks for semicolon after the end of the expression
        consume(SEMICOLON, "Expect ';' after value.");

        return new Stmt.Print(value);
    }

    //returnStmt     → "return" expression? ";" ;
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
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

    //whileStmt      → "while" "(" expression ")" statement ;
    private Stmt whileStatement() {
        // It is not complicated stuff, same as previous functions.
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    //Works similar to the printStatement() function
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after the expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect ')' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cant have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect Parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect {' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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

    //assignment     → IDENTIFIER "=" assignment | logic_or ;
    private Expr assignment() {
        Expr expr = or();

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

    //logic_or       → logic_and ( "or" logic_and )* ;
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    //logic_and      → equality ( "and" equality )* ;
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

    //unary          → ( "!" | "-" ) unary | call ;
    private Expr unary() {
        // if condition for ( "!" | "-" ) unary, if it doesn't start with ! or -, it returns "primary" from the grammar
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Cant have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    //call           → primary ( "(" arguments? ")" )* ;
    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }

        return expr;
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
