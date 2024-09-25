package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

//This class automates the generation of AST classes for the interpreter
//We have to compile and run it everytime
public class GenerateAst {

    public static void main(String[] args) throws IOException {
        // Check if the output directory is provided as an argument
        if (args.length != 1) {
            System.err.println("Usage: generateAst <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        // Define the AST structure for expressions
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary   : Expr left, Token operator, Expr right",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Unary    : Token operator, Expr right"
        ));
    }

    // Generates the AST class definitions and writes them to a file
    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java"; // Define the output file path
        PrintWriter writer = new PrintWriter(path, "UTF-8"); // Create a PrintWriter to write to the file

        // Write the package declaration
        writer.println("package com.craftinginterpreters.lox;");
        writer.println();

        // Import necessary classes
        writer.println("import java.util.List;");
        writer.println();

        // Write the abstract class declaration
        writer.println("//This class is generated using GenerateAst ");
        writer.println("abstract class " + baseName + " {");

        //defining visitor interface
        defineVisitor(writer,baseName, types);

        // Generate each type of expression
        for (String type : types) {
            String className = type.split(":")[0].trim(); // Extract class name
            String fields = type.split(":")[1].trim(); // Extract fields
            defineType(writer, baseName, className, fields); // Define the type
        }

        //The base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println();
        writer.println("}"); // End of abstract class
        writer.close(); // Close the writer
    }

    //This generates the visitor interface for all the types
    private static void defineVisitor (PrintWriter writer, String baseName, List<String> types){
        //Interface Definition
        writer.println("    interface Visitor<R> {");


        for (String type : types){
            String typename = type.split(":")[0].trim();
            writer.println("        R visit" + typename + baseName + "(" + typename + " " + baseName.toLowerCase() + ");");
            writer.println();
        }

        writer.println("    }");
        writer.println();

    }

    // Defines a specific AST node type
    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        // Write the class definition for the AST node
        writer.println("    static class " + className + " extends " + baseName + " {");

        // Constructor for the AST node
        writer.println("        " + className + "(" + fieldList + ") {");

        // Store constructor parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1]; // Get the field name
            writer.println("            this." + name + " = " + name + ";"); // Initialize the field
        }

        writer.println("        }"); // End of constructor

        //Visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");

        // Declare fields
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";"); // Declare each field as final
        }

        writer.println();
        writer.println("    }");// End of class definition
        writer.println();
    }

}
