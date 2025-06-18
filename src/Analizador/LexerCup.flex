package Analizador;

// Importa la clase Symbol de Java CUP y la clase sym generada
import java_cup.runtime.Symbol;
import static Analizador.sym.*; // Importa estáticamente los símbolos

import java.util.List;
import java.util.ArrayList;
import Analizador.LexicalError; 

%%

// --- Directivas JFlex para CUP ---
%class LexerCup
%implements java_cup.runtime.Scanner
%type java_cup.runtime.Symbol
%cup
%unicode
%line
%char
%column

// --- Macros ---
Letter = [a-zA-Z]
Underscore = [_]
Digit = [0-9]
Whitespace = [ \t\r\f\n]+ 

%{
    // Lista para almacenar los errores léxicos encontrados
    private List<LexicalError> lexicalErrors = new ArrayList<>();

    // Método público para que la GUI obtenga los errores después de escanear
    public List<LexicalError> getLexicalErrors() {
        return lexicalErrors;
    }

    // Método para añadir errores léxicos a la lista
    private void addError(String message, String lexemeText) {
        lexicalErrors.add(new LexicalError(message, yyline, yycolumn, lexemeText));
    }

    // Metodo para crear Símbolos con valor
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}

%%

// --- Reglas Léxicas ---

/* -- ESPACIOS EN BLANCO Y SALTOS DE LINEA -- */
{Whitespace} { /* Ignore */ }

/* -- COMENTARIOS -- */
"#" [^\r\n]*                      { return symbol(COMENTARIO, yytext()); }
"#*" ( [^*] | \*+[^*/] )* \*+ "#" { return symbol(COMENTARIO_MULTILINEA, yytext()); }


/* -- PALABRAS RESERVADAS -- */
"Start"     { return symbol(INICIO_PROGRAMA, yytext()); }
"End"       { return symbol(FIN_PROGRAMA, yytext()); }
"public"    { return symbol(PUBLICO, yytext()); }
"private"   { return symbol(PRIVADO, yytext()); }
"protected" { return symbol(PROTEGIDO, yytext()); }
"static"    { return symbol(ESTATICO, yytext()); }
"fun"       { return symbol(FUNCION, yytext()); }
"return"    { return symbol(RETORNO, yytext()); }
"if"        { return symbol(IF, yytext()); }
"else"      { return symbol(ELSE, yytext()); }
"when"      { return symbol(WHEN, yytext()); }
"for"       { return symbol(FOR, yytext()); }
"while"     { return symbol(WHILE, yytext()); }
"do"        { return symbol(DO, yytext()); }
"new"       { return symbol(NEW, yytext()); }
"String"    { return symbol(TIPO_STRING, yytext()); }
"char"      { return symbol(TIPO_CHAR, yytext()); }
"int"       { return symbol(TIPO_INT, yytext()); }
"float"     { return symbol(TIPO_FLOAT, yytext()); }
"double"    { return symbol(TIPO_DOUBLE, yytext()); }
"boolean"   { return symbol(TIPO_BOOLEAN, yytext()); }
"read"      { return symbol(ENTRADA, yytext()); }
"write"     { return symbol(IMPRIMIR, yytext()); }
"writeln"   { return symbol(IMPRIMIR_CON_SALTO, yytext()); }
"true"      { return symbol(TRUE, Boolean.TRUE); }
"false"     { return symbol(FALSE, Boolean.FALSE); }

/* -- OPERADORES -- */
"++"        { return symbol(OPERADOR_INCREMENTO, yytext()); }
"--"        { return symbol(OPERADOR_DECREMENTO, yytext()); }
"+="        { return symbol(SUMA_Y_ASIGNACION, yytext()); }
"-="        { return symbol(RESTA_Y_ASIGNACION, yytext()); }
"*="        { return symbol(MULTIPLICACION_Y_ASIGNACION, yytext()); }
"/="        { return symbol(DIVISION_Y_ASIGNACION, yytext()); }
"%="        { return symbol(MODULO_Y_ASIGNACION, yytext()); }
"=="        { return symbol(IGUAL_A, yytext()); }
"!="        { return symbol(DIFERENTE_DE, yytext()); }
">="        { return symbol(MAYOR_IGUAL, yytext()); }
"<="        { return symbol(MENOR_IGUAL, yytext()); }
">"         { return symbol(MAYOR_QUE, yytext()); }
"<"         { return symbol(MENOR_QUE, yytext()); }
"&&"        { return symbol(AND, yytext()); }
"||"        { return symbol(OR, yytext()); }
"!"         { return symbol(NEGACION, yytext()); }
"+"         { return symbol(SUMA, yytext()); }
"-"         { return symbol(RESTA, yytext()); }
"*"         { return symbol(MULTIPLICACION, yytext()); }
"/"         { return symbol(DIVISION, yytext()); }
"%"         { return symbol(MODULO, yytext()); }
"="         { return symbol(ASIGNACION_VALOR, yytext()); }

/* -- LITERALES -- */
({Letter}({Letter}|{Digit}|{Underscore})*) | ({Underscore}({Letter}|{Digit}|{Underscore})+) { return symbol(IDENTIFICADOR, yytext()); } 

{Digit}+ {
    try {
        return symbol(NUMERO_ENTERO, Integer.valueOf(yytext()));
    } catch (NumberFormatException e) {
        addError("Entero fuera de rango", yytext());
        return symbol(ERROR, yytext());
    }
}
{Digit}+ "." {Digit}+  {
    try {
        return symbol(NUMERODECIMAL, Double.valueOf(yytext()));
    } catch (NumberFormatException e) {
        addError("Decimal mal formado", yytext());
        return symbol(ERROR, yytext());
    }
}
\" ( [^\"\\\n\r] | \\. )* \" {
    String val = yytext();
    val = val.substring(1, val.length() - 1);
    return symbol(CADENA, val);
}
\' ( [^'\\\n\r] | \\. ) \' {
    String val = yytext();
    if (val.length() == 3) { 
        return symbol(CARACTER, Character.valueOf(val.charAt(1)));
    } else if (val.length() > 3) { 
         addError("Secuencia de escape inválida en caracter", val);
         return symbol(ERROR, val);
    } else { 
         addError("Literal de caracter inválido", val);
         return symbol(ERROR, val);
    }
}


/* -- SIMBOLOS ESPECIALES -- */
"("       { return symbol(PARENTESIS_APERTURA, yytext()); }
")"       { return symbol(PARENTESIS_CIERRE, yytext()); }
"["       { return symbol(CORCHETE_APERTURA, yytext()); }
"]"       { return symbol(CORCHETE_CIERRE, yytext()); }
"{"       { return symbol(LLAVE_APERTURA, yytext()); }
"}"       { return symbol(LLAVE_CIERRE, yytext()); }
":"       { return symbol(DOS_PUNTOS, yytext()); }
";"       { return symbol(PUNTO_COMA, yytext()); }
","       { return symbol(COMA, yytext()); }

/* -- MANEJO DE ERRORES LÉXICOS -- */

"_" { 
    addError ("Identificador inválido: guion bajo solo", yytext()); 
    return symbol(ERROR, yytext()); 
}

. {
    addError("Caracter inesperado", yytext()); 
    return symbol(ERROR, yytext()); 
}