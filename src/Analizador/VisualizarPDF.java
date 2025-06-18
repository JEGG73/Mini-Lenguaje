
package Analizador;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class VisualizarPDF extends JFrame {

    private JLabel pageLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel statusLabel; // mostrar el número de página actual y total

    private PDDocument currentDocument;
    private PDFRenderer pdfRenderer;
    private int currentPage = 0;
    private int totalPages = 0;
    private final static int DEFAULT_DPI = 150;

    /**
     * Constructor que inicializa la interfaz gráfica de usuario.
     */
    public VisualizarPDF() {
        super("Manual de usuario");
        setSize(800, 700);
        setLocationRelativeTo(null);

        initComponents();
        updateButtonStates();
    }

    private void initComponents() {
        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel para mostrar la imagen de la página del PDF
        JPanel imagePanel = new JPanel(new BorderLayout());
        pageLabel = new JLabel("", SwingConstants.CENTER);
        pageLabel.setPreferredSize(new Dimension(600, 400));
        JScrollPane scrollPane = new JScrollPane(pageLabel);
        imagePanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(imagePanel, BorderLayout.CENTER);

        // Panel de control (botones y estado)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        prevButton = new JButton("Anterior");
        nextButton = new JButton("Siguiente");
        statusLabel = new JLabel("Página: N/A");

        controlPanel.add(prevButton);
        controlPanel.add(nextButton);
        controlPanel.add(statusLabel);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        prevButton.addActionListener(e -> showPreviousPage());
        nextButton.addActionListener(e -> showNextPage());

        add(mainPanel);
    }

    /**
     * Abre un diálogo para seleccionar un archivo PDF y lo carga.
     */
    public void openPdfFile() {
        File file = new File("MANUAL DEL USUARIO.pdf");
        loadPdf(file);

    }

    /**
     * Carga el documento PDF especificado.
     */
    private void loadPdf(File pdfFile) {
        try {
            if (currentDocument != null) {
                currentDocument.close();
            }

            currentDocument = org.apache.pdfbox.Loader.loadPDF(pdfFile); // Cargar pdf
            pdfRenderer = new PDFRenderer(currentDocument);
            totalPages = currentDocument.getNumberOfPages();
            currentPage = 0;

            if (totalPages > 0) {
                renderPage(currentPage);
            } else {
                pageLabel.setIcon(null);
                pageLabel.setText("El PDF está vacío o no se pudo cargar.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar el archivo PDF: " + e.getMessage(),
                    "Error de Carga", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            currentDocument = null;
            pdfRenderer = null;
            totalPages = 0;
            currentPage = 0;
            pageLabel.setIcon(null);
            pageLabel.setText("Error al cargar PDF. Intente abrir otro archivo.");
        }
        updateButtonStates();
        updateStatusLabel();
    }

    /**
     * Renderiza y muestra la página especificada.
     */
    private void renderPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= totalPages) {
            return; // No hacer nada si no hay PDF
        }
        try {
            // Renderizar la página a una imagen.
            BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, DEFAULT_DPI);

            // Ancho del scroll pane
            int panelWidth = pageLabel.getParent().getWidth() - 20;
            int panelHeight = pageLabel.getParent().getHeight() - 20;

            if (panelWidth <= 0 || panelHeight <= 0) {
                panelWidth = 600;
                panelHeight = 400;
            }

            Image scaledImage = image.getScaledInstance(panelWidth, -1, Image.SCALE_SMOOTH);
            if (scaledImage.getHeight(null) > panelHeight) {
                scaledImage = image.getScaledInstance(-1, panelHeight, Image.SCALE_SMOOTH);
            }

            pageLabel.setIcon(new ImageIcon(scaledImage));
            pageLabel.setText(null); 
            currentPage = pageIndex;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al renderizar la página " + (pageIndex + 1) + ": " + e.getMessage(),
                    "Error de Renderizado", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            pageLabel.setIcon(null);
            pageLabel.setText("Error al renderizar página " + (pageIndex + 1));
        }
        updateButtonStates();
        updateStatusLabel();
    }

    /**
     * Muestra la página anterior.
     */
    private void showPreviousPage() {
        if (currentPage > 0) {
            renderPage(currentPage - 1);
        }
    }

    /**
     * Muestra la página siguiente.
     */
    private void showNextPage() {
        if (currentPage < totalPages - 1) {
            renderPage(currentPage + 1);
        }
    }

    /**
     * Actualiza el estado de los botones de navegación.
     */
    private void updateButtonStates() {
        if (currentDocument == null || totalPages == 0) {
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            prevButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage < totalPages - 1);
        }
    }

    /**
     * Actualiza la etiqueta de estado con el número de página actual.
     */
    private void updateStatusLabel() {
        if (currentDocument == null || totalPages == 0) {
            statusLabel.setText("Página: N/A");
        } else {
            statusLabel.setText("Página: " + (currentPage + 1) + " de " + totalPages);
        }
    }
}
