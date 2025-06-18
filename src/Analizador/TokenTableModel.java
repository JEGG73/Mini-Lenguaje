package Analizador;

import javax.swing.table.DefaultTableModel;

public class TokenTableModel extends DefaultTableModel {
    
    public TokenTableModel() {
        // Cambiar el orden de las columnas según lo requerido
        super(new Object[]{"Línea", "Columna", "TIPO_TOKEN", "LEXEMA/VALOR"}, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}