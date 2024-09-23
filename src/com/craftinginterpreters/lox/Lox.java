package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    // Boolean to track if an error has occurred
    static Boolean hadError = false;

    public static void main(String[] args) throws IOException {
        // Check the number of command-line arguments
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            // If a script file is provided, run the file
            runFile(args[0]);
        } else {
            // If no arguments, enter the interactive prompt
            runPrompt();
        }
    }

    //This will run when the script file is provided
    private static void runFile(String path) throws IOException {
        // Read the file contents into a byte array
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        // Convert the bytes to a String and run it
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65); // Exit with error code 65
    }

    //This run when the language is typed in the prompt
    private static void runPrompt() throws IOException {
        // Set up input reading from the console
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> "); // Prompt the user for input
            String line = reader.readLine(); // Read a line of input
            if (line == null) break; // Exit on EOF
            run(line); // Run the input line
            hadError = false; // Reset error status for the next input
        }
    }

    //Both runPrompt and run file uses it
    private static void run(String source) {
        // Create a scanner for the input source
        //The "Scanner" used here is not the java scanner, but out scanner (it gave me error when I 1st did it)
        Scanner scanner = new Scanner(source);
        // Scan tokens from the source
        List<Token> tokens = scanner.scanTokens();

        // For now, just print the tokens to the console
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    // Method to report an error with line number and message
    static void error(int line, String message) {
        report(line, "", message);
    }

    // Method to format and print error messages
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error " + where + ": " + message); // Print the error message
        hadError = true; // Set hadError to true
    }
}
