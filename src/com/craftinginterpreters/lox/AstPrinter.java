package com.craftinginterpreters.lox;

import java.security.spec.ECPoint;

// A class to debug the parser and interpreters syntax tree
class AstPrinter implements Expr.Visitor<String> {

    String print(Expr expr) {
        return expr.accept(this);
    }

    // Since this class IMPLEMENTS Expr.Visitor, we have to override the method
    // If you don't understand, check more about IMPLEMENTS
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    // Since this class IMPLEMENTS Expr.Visitor, we have to override the method
    // If you don't understand, check more about IMPLEMENTS
    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    // Since this class IMPLEMENTS Expr.Visitor, we have to override the method
    // If you don't understand, check more about IMPLEMENTS
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        // We are not calling the parenthesize method here as this just converts the value into a string after checking for null
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    // Since this class IMPLEMENTS Expr.Visitor, we have to override the method
    // If you don't understand, check more about IMPLEMENTS
    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    /*A helper method for other expressions that have sub expressions
     *It takes a name and a list of subexpressions and wraps them all up in parentheses, yielding a string like: (+ 1 2)*/
    private String parenthesize(String name, Expr... exprs) {

        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {

            builder.append(" ");

            // This calls accept and passes itself, This is the recursive step that lets us print an entire tree.
            builder.append(expr.accept(this));

        }

        builder.append(")");

        return builder.toString();

    }


}
