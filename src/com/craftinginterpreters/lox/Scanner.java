package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {

    private final String source; // The source code being scanned
    private final List<Token> tokens = new ArrayList<>(); // List to store the tokens

    private int start = 0; // Start index of the current lexeme
    private int current = 0; // Current index in the source code
    private int line = 1; // Current line number

    // Map to hold keywords and their corresponding TokenType
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();

        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    // Constructor that initializes the scanner with the source code
    Scanner(String source) {
        this.source = source;
    }

    // Scans the entire source code and returns a list of tokens
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // Mark the beginning of the next lexeme
            start = current;
            scanToken(); // Scan the next token
        }

        // Add an EOF token to signify the end of input
        tokens.add(new Token(EOF, "", null, line));
        return tokens; // Return the list of tokens
    }

    // Scans a single token from the source code
    //TODO Chapter 4 question 4
    private void scanToken() {
        char c = advance(); // Get the next character

        // Determine the token type based on the character
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;


            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;


            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;

            // Ignore whitespace characters
            case ' ':
            case '\r':
            case '\t':
                break;

            // Increment line count on new lines
            case '\n':
                line++;
                break;

            // Handle string literals
            case '"': string(); break;

            // Handle unexpected characters
            default:
                if (isDigit(c)) {
                    number(); // Handle numeric literals
                } else if (isAlpha(c)) {
                    identifier(); // Handle identifiers
                } else {
                    Lox.error(line, "Unexpected Character."); // Report an error
                }
        }
    }

    // Scans an identifier (variable or function name)
    private void identifier() {
        while (isAlphaNumeric(peek())) advance(); // Advance while it's alphanumeric

        String text = source.substring(start, current); // Extract the identifier text
        TokenType type = keywords.get(text); // Look up the token type in keywords
        if (type == null) type = IDENTIFIER; // Default to IDENTIFIER if not found
        addToken(type); // Add the token
    }

    // Scans a numeric literal
    private void number() {
        while (isDigit(peek())) advance(); // Advance while it's a digit

        // Check for decimal point and fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the decimal point

            while (isDigit(peek())) advance(); // Advance for the fractional part
        }

        // Create a NUMBER token
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // Scans a string literal
    private void string() {
        while (peek() != '"' && !isAtEnd()) { // Continue until the closing quote
            if (peek() == '\n') line++; // Increment line count for new lines
            advance(); // Advance the string
        }

        // If we reached the end without a closing quote, report an error
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        advance(); // Consume the closing quote

        // Extract the string value and add the STRING token
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // Matches the expected character and advances if it matches
    private boolean match(char expected) {
        if (isAtEnd()) return false; // If at end, no match
        if (source.charAt(current) != expected) return false; // If character doesn't match, no match

        current++; // Advance if it matches
        return true; // Return true for a successful match
    }

    // Peeks at the next character without advancing
    private char peek() {
        if (isAtEnd()) return '\0'; // Return null character if at end
        return source.charAt(current); // Return the current character
    }

    // Peeks at the character after the current one
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0'; // Return null if at end
        return source.charAt(current + 1); // Return the next character
    }

    // Checks if a character is an alphabetical letter
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_'; // Include underscore
    }

    // Checks if a character is alphanumeric
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c); // Check if it's either alpha or digit
    }

    // Checks if a character is a digit
    public boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Checks if we've reached the end of the source code
    private boolean isAtEnd() {
        return current >= source.length(); // Return true if current index is at or beyond length
    }

    // Advances to the next character and returns the current character
    private char advance() {
        return source.charAt(current++); // Return the current character and increment
    }

    // Adds a token without a literal value
    private void addToken(TokenType type) {
        addToken(type, null); // Call the overloaded method with null literal
    }

    // Adds a token with a literal value
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current); // Extract the token text
        tokens.add(new Token(type, text, literal, line)); // Add the new token to the list
    }

}
