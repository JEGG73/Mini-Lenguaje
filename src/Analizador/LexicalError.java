/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Analizador;

public class LexicalError {
    public final String message;
    public final int line;
    public final int column;
    public final String lexeme;

    public LexicalError(String message, int line, int column, String lexeme) {
        this.message = message;
        this.line = line;
        this.column = column;
        this.lexeme = lexeme;
    }

    @Override
    public String toString() {
        return String.format("Error Léxico: %s en línea %d, columna %d ['%s']",
                             message, line + 1, column + 1, lexeme);
    }
}
