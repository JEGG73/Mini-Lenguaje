package Analizador;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class AnalizadorLexico {

    /**
     * Método principal para ejecutar la regeneración de los analizadores.
     */
    
    //int arreglo [] = new int [5];
    
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando regeneración de analizadores...");
            actualizarAnalizadores();
            System.out.println("---------------------------------------------------------");
            System.out.println("Proceso de regeneración de analizadores completado.");
            System.out.println("---------------------------------------------------------");
        } catch (Exception e) {
            System.err.println("Error durante la regeneración de analizadores:");
            e.printStackTrace();
        }
    }

    public static void actualizarAnalizadores() throws Exception {

        String projectDir = System.getProperty("user.dir");
        System.out.println("Directorio del proyecto detectado: " + projectDir);

        String flexFilePath = Paths.get(projectDir, "src", "Analizador", "LexerCup.flex").toString();
        String cupFilePath = Paths.get(projectDir, "src", "Analizador", "Sintax.cup").toString();
        String outputDirForGeneratedFiles = Paths.get(projectDir, "src", "Analizador").toString(); // Donde se colocarán los .java

        // 1. Generar Analizador Léxico (LexerCup.java)
        System.out.println("\n--- Generando Analizador Léxico ---");
        generarLexer(flexFilePath, outputDirForGeneratedFiles);

        // 2. Generar Analizador Sintáctico (Sintax.java y sym.java)
        System.out.println("\n--- Generando Analizador Sintáctico ---");
        generarParser(cupFilePath, outputDirForGeneratedFiles);
    }

    public static void generarLexer(String flexFilePath, String outputDir) throws Exception {
        System.out.println("Procesando archivo JFlex: " + flexFilePath);
        File flexFile = new File(flexFilePath);
        if (!flexFile.exists()) {
            throw new IOException("Archivo .flex no encontrado en: " + flexFilePath);
        }

        String[] jflexArgs = {
            "-d", outputDir, // Directorio de salida
            flexFilePath     // Archivo de entrada .flex
        };
        
        File lexerFile = new File(outputDir, "LexerCup.java");
        if (lexerFile.exists()) {
            System.out.println("Eliminando LexerCup.java existente para regeneración: " + lexerFile.getAbsolutePath());
            lexerFile.delete();
        }

        JFlex.Main.generate(flexFile);
        System.out.println("LexerCup.java generado/actualizado en: " + outputDir);
    }

    public static void generarParser(String cupFilePath, String outputDir) throws Exception {
        System.out.println("Procesando archivo CUP: " + cupFilePath);
        File cupFile = new File(cupFilePath);
        if (!cupFile.exists()) {
            throw new IOException("Archivo .cup no encontrado en: " + cupFilePath);
        }

        // Argumentos para Java CUP
        String[] cupArgs = {
            "-parser", "Sintax",     // Nombre de la clase Parser que se generará
            "-symbols", "sym",       // Nombre de la clase de Símbolos que se generará
            "-destdir", outputDir,   // Directorio de salida para Sintax.java y sym.java
            cupFilePath              // Archivo .cup de entrada
        };

        System.out.println("Ejecutando Java CUP con argumentos: ");
        for(String arg : cupArgs) {
            System.out.print(arg + " ");
        }
        System.out.println();

        // Eliminar archivos antiguos antes de generar para evitar problemas
        File sintaxFile = new File(outputDir, "Sintax.java");
        File symFile = new File(outputDir, "sym.java");

        if (sintaxFile.exists()) {
            System.out.println("Eliminando Sintax.java existente para regeneración: " + sintaxFile.getAbsolutePath());
            sintaxFile.delete();
        }
        if (symFile.exists()) {
            System.out.println("Eliminando sym.java existente para regeneración: " + symFile.getAbsolutePath());
            symFile.delete();
        }

        java_cup.Main.main(cupArgs); // Ejecuta CUP

        System.out.println("Sintax.java y sym.java generados/actualizados en: " + outputDir);
    }
}