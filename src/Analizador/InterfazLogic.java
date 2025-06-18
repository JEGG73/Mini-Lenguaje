package Analizador;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document; 
import javax.swing.text.Element;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent; 
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java_cup.runtime.Symbol;
import java.lang.reflect.InvocationTargetException;

public class InterfazLogic {

    private Interfaz ui; 

    private Map<Component, File> editorFileMap = new HashMap<>();
    private Map<Component, UndoManager> editorUndoManagerMap = new HashMap<>();
    private File projectBaseDir = null;

    private Action undoAction, redoAction, cutAction, copyAction, pasteAction;
    private Action compileAction, compileAndExecuteAction; // Accion a Futuro

    public InterfazLogic(Interfaz uiInstance) {
        this.ui = uiInstance;
    }

    public Action getUndoAction() {
        return undoAction;
    }

    public Action getRedoAction() {
        return redoAction;
    }

    public Action getCutAction() {
        return cutAction;
    }

    public Action getCopyAction() {
        return copyAction;
    }

    public Action getPasteAction() {
        return pasteAction;
    }

    public Action getCompileAction() {
        return compileAction;
    }

    public Action getCompileAndExecuteAction() {
        return compileAndExecuteAction;
    }

    public boolean isEditorFileMapEmpty() {
        return editorFileMap.isEmpty();
    }

    public void setupInitialState() {
        ui.createInitialPlaceholderTabUI();
        updateStatus("Listo");
        ui.lineLabel.setText("Ln 1, Col 1");
        updateUndoRedoStatus();
    }

    public void createActions() {
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        undoAction = new AbstractAction("Deshacer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                UndoManager manager = getActiveUndoManager();
                if (manager != null && manager.canUndo()) {
                    try {
                        manager.undo();
                    } catch (CannotUndoException ex) {
                        logError("Error al deshacer", ex);
                    }
                }
                updateUndoRedoStatus();
            }
        };
        undoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask));
        undoAction.setEnabled(false);

        redoAction = new AbstractAction("Rehacer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                UndoManager manager = getActiveUndoManager();
                if (manager != null && manager.canRedo()) {
                    try {
                        manager.redo();
                    } catch (CannotRedoException ex) {
                        logError("Error al rehacer", ex);
                    }
                }
                updateUndoRedoStatus();
            }
        };
        redoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask));
        redoAction.setEnabled(false);

        cutAction = new AbstractAction("Cortar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextArea active = getActiveTextArea();
                if (active != null) {
                    active.cut();
                }
            }
        };
        cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutMask));

        copyAction = new AbstractAction("Copiar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextArea active = getActiveTextArea();
                if (active != null) {
                    active.copy();
                }
            }
        };
        copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));

        pasteAction = new AbstractAction("Pegar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextArea active = getActiveTextArea();
                if (active != null) {
                    active.paste();
                }
            }
        };
        pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));

        compileAction = new AbstractAction("Compilar") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    compileCode();
                } catch (IOException ex) {
                    logError("Error de E/S durante la compilación", ex);
                }
            }
        };
        compileAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

    }

    public void handleNewProject() {
        String projectName = JOptionPane.showInputDialog(
                ui, "Ingrese el nombre del proyecto:", "Nuevo Proyecto", JOptionPane.PLAIN_MESSAGE
        );
        if (projectName != null && !projectName.trim().isEmpty()) {
            createNewProjectLogic(projectName.trim());
        }
    }

    public void handleNewFile() {
        if (this.projectBaseDir == null) {
            JOptionPane.showMessageDialog(ui,
                    "Primero debe crear o abrir un proyecto para poder crear un archivo.",
                    "Proyecto Requerido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String fileName = JOptionPane.showInputDialog(
                ui, "Ingrese nombre archivo (con extensión .rvn):", "Nuevo Archivo", JOptionPane.PLAIN_MESSAGE
        );
        if (fileName != null && !fileName.trim().isEmpty()) {
            String trimmedName = fileName.trim();
            if (!trimmedName.toLowerCase().endsWith(".rvn")) {
                trimmedName += ".rvn";
            }
            createNewFileLogic(trimmedName);
        }
    }

    public void handleOpenProject() {
        openProjectLogic();
    }

    public void handleSaveFile(boolean forceSaveAs) {
        saveFileLogic(forceSaveAs);
    }

    public void handleExit() {
        System.exit(0);
    }

    public void handleShowUserManual() {
        showUserManualLogic();
    }

    public void handleShowAboutDialog() {
        showAboutDialogLogic();
    }

    private void openProjectLogic() {
        JFileChooser projectChooser = new JFileChooser();
        projectChooser.setDialogTitle("Abrir Proyecto");
        projectChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        projectChooser.setAcceptAllFileFilterUsed(false);

        if (this.projectBaseDir != null) {
            projectChooser.setCurrentDirectory(this.projectBaseDir.getParentFile());
        }

        int result = projectChooser.showOpenDialog(ui);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedProjectDir = projectChooser.getSelectedFile();
            if (selectedProjectDir == null || !selectedProjectDir.isDirectory()) {
                JOptionPane.showMessageDialog(ui, "No se seleccionó un directorio válido.", "Error al Abrir Proyecto", JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.projectBaseDir = selectedProjectDir;
            updateStatus("Abriendo proyecto: " + this.projectBaseDir.getAbsolutePath());
            System.out.println("Ruta base del proyecto establecida en: " + this.projectBaseDir.getAbsolutePath());

            closeAllTabsLogic();
            ui.projectRoot.setUserObject(this.projectBaseDir.getName());
            ui.projectRoot.removeAllChildren();
            populateProjectTreeFromDisk(this.projectBaseDir, ui.projectRoot);
            ui.treeModel.reload(ui.projectRoot);
            ui.projectsTree.expandPath(new TreePath(ui.projectRoot.getPath()));
            updateStatus("Proyecto abierto: " + this.projectBaseDir.getName());
        } else {
            updateStatus("Apertura de proyecto cancelada por el usuario.");
        }
    }

    private void populateProjectTreeFromDisk(File dir, DefaultMutableTreeNode parentNode) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        for (File file : files) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileNode(file));
            parentNode.add(node);
            if (file.isDirectory()) {
                populateProjectTreeFromDisk(file, node);
            }
        }
    }

    public void handleProjectTreeDoubleClick(MouseEvent e) {
        TreePath selPath = ui.projectsTree.getPathForLocation(e.getX(), e.getY());
        if (selPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof FileNode) {
                File fileToOpen = ((FileNode) selectedNode.getUserObject()).getFile();
                if (fileToOpen.isFile()) {
                    loadFileIntoEditorLogic(fileToOpen);
                }
            }
        }
    }

    private void loadFileIntoEditorLogic(File selectedFile) {
        if (isFileAlreadyOpen(selectedFile)) {
            JOptionPane.showMessageDialog(ui, "El archivo ya está abierto en otra pestaña.", "Archivo Abierto", JOptionPane.INFORMATION_MESSAGE);
            focusTabForFile(selectedFile);
            return;
        }

        JPanel editorPanel = createEditorPanelWithTextArea(selectedFile);
        ui.addEditorTabUI(selectedFile.getName(), editorPanel);
        editorFileMap.put(editorPanel, selectedFile); 

        JTextArea activeTextArea = getTextAreaFromPanel(editorPanel);
        if (activeTextArea != null) {
            try ( BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                activeTextArea.read(reader, null); 
                UndoManager manager = editorUndoManagerMap.get(editorPanel); 
                if (manager != null) {
                    manager.discardAllEdits();
                }

                updateStatus("Abierto: " + selectedFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(ui, "Error al leer archivo:\n" + ex.getMessage(), "Error de Lectura", JOptionPane.ERROR_MESSAGE);
                updateStatus("Error al abrir archivo.");
            }
        }
        updateUndoRedoStatus(); 
    }

    private boolean isFileAlreadyOpen(File file) {
        return editorFileMap.containsValue(file);
    }

    private void focusTabForFile(File file) {
        for (Map.Entry<Component, File> entry : editorFileMap.entrySet()) {
            if (file.equals(entry.getValue())) {
                ui.editorTabbedPane.setSelectedComponent(entry.getKey());
                break;
            }
        }
    }

    private void saveFileLogic(boolean forceSaveAs) {
        Component selectedComponent = ui.editorTabbedPane.getSelectedComponent();
        if (!(selectedComponent instanceof JPanel) || getTextAreaFromPanel((JPanel) selectedComponent) == null) {
            JOptionPane.showMessageDialog(ui, "No hay un archivo activo para guardar.", "Error", JOptionPane.ERROR_MESSAGE);
            updateStatus("Error: No hay archivo activo.");
            return;
        }

        JPanel activeEditorPanel = (JPanel) selectedComponent;
        JTextArea activeTextArea = getTextAreaFromPanel(activeEditorPanel);
        File fileToSave = editorFileMap.get(activeEditorPanel);

        if (forceSaveAs || fileToSave == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(forceSaveAs ? "Guardar Como" : "Guardar Archivo");
            if (this.projectBaseDir != null) {
                fileChooser.setCurrentDirectory(this.projectBaseDir);
            }

            if (fileToSave == null) { 
                String currentTabTitle = ui.editorTabbedPane.getTitleAt(ui.editorTabbedPane.getSelectedIndex());
                if (!currentTabTitle.equals("Sin título") && !currentTabTitle.isEmpty() && !currentTabTitle.equals("Bienvenido")) {
                    fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), currentTabTitle));
                }
            } else {
                fileChooser.setSelectedFile(fileToSave);
            }

            FileNameExtensionFilter rvnFilter = new FileNameExtensionFilter("Archivo Raven (*.rvn)", "rvn");
            fileChooser.addChoosableFileFilter(rvnFilter);
            fileChooser.setFileFilter(rvnFilter);

            int result = fileChooser.showSaveDialog(ui);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String name = selectedFile.getName();
                if (!name.toLowerCase().endsWith(".rvn")) {
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex > 0) {
                        name = name.substring(0, dotIndex);
                    }
                    selectedFile = new File(selectedFile.getParentFile(), name + ".rvn");
                }

                if (selectedFile.exists()) {
                    int overwriteResult = JOptionPane.showConfirmDialog(ui,
                            "El archivo '" + selectedFile.getName() + "' ya existe.\n¿Desea sobrescribirlo?",
                            "Confirmar Sobrescritura", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (overwriteResult == JOptionPane.NO_OPTION) {
                        updateStatus("Guardado cancelado.");
                        return;
                    }
                }
                fileToSave = selectedFile;
                editorFileMap.put(activeEditorPanel, fileToSave); 
                ui.editorTabbedPane.setTitleAt(ui.editorTabbedPane.getSelectedIndex(), fileToSave.getName());

                if (ui.projectRoot != null && !ui.projectRoot.getUserObject().equals("Ningún proyecto") && this.projectBaseDir != null) {
                    if (fileToSave.getAbsolutePath().startsWith(this.projectBaseDir.getAbsolutePath())) {
                        addOrUpdateTreeNode(new FileNode(fileToSave)); 
                    }
                }
            } else {
                updateStatus("Guardado cancelado.");
                return;
            }
        }

        if (fileToSave != null) {
            try {
                File parentDir = fileToSave.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        throw new IOException("No se pudo crear el directorio: " + parentDir.getAbsolutePath());
                    }
                }
                try ( BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                    activeTextArea.write(writer);
                    updateStatus("Archivo guardado: " + fileToSave.getName());
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(ui, "Error al guardar archivo:\n" + ex.getMessage(), "Error de Escritura", JOptionPane.ERROR_MESSAGE);
                updateStatus("Error al guardar.");
            }
        }
    }

    private JPanel createEditorPanelWithTextArea(File associatedFile) {
        final JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setName("EditorPanel"); 
        JTextArea newTextArea = new JTextArea();
        newTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        UndoManager undoManager = new UndoManager();
        newTextArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            updateUndoRedoStatus();
            updateStatus("Modificado");
        });
        editorUndoManagerMap.put(editorPanel, undoManager); 

        JTextArea lineNumbersArea = new JTextArea("1");
        lineNumbersArea.setBackground(Color.LIGHT_GRAY);
        lineNumbersArea.setEditable(false);
        lineNumbersArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        lineNumbersArea.setMargin(new Insets(0, 5, 0, 5));

        JScrollPane scrollPane = new JScrollPane(newTextArea);
        scrollPane.setRowHeaderView(lineNumbersArea);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            JViewport rowHeaderViewport = scrollPane.getRowHeader();
            if (rowHeaderViewport != null) {
                Point currentViewPos = scrollPane.getViewport().getViewPosition();
                Point headerViewPos = rowHeaderViewport.getViewPosition();
                if (headerViewPos.y != currentViewPos.y) {
                    rowHeaderViewport.setViewPosition(new Point(0, currentViewPos.y));
                }
            }
        });
        editorPanel.add(scrollPane, BorderLayout.CENTER);

        final DocumentListener lineUpdaterListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbersForPanel(editorPanel);
            }

            public void removeUpdate(DocumentEvent e) {
                updateLineNumbersForPanel(editorPanel);
            }

            public void changedUpdate(DocumentEvent e) {
            }
        };

        newTextArea.getDocument().addDocumentListener(lineUpdaterListener);

        newTextArea.addPropertyChangeListener("document", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (evt.getOldValue() instanceof Document) {
                    ((Document) evt.getOldValue()).removeDocumentListener(lineUpdaterListener);
                }
                if (evt.getNewValue() instanceof Document) {
                    ((Document) evt.getNewValue()).addDocumentListener(lineUpdaterListener);
                    updateLineNumbersForPanel(editorPanel);
                }
            }
        });

        newTextArea.addCaretListener(e -> updateCaretPosition(newTextArea));

        SwingUtilities.invokeLater(() -> updateLineNumbersForPanel(editorPanel));


        newTextArea.requestFocusInWindow();

        return editorPanel;
    }

    private void createNewProjectLogic(String projectName) {
        JFileChooser projectChooser = new JFileChooser();
        projectChooser.setDialogTitle("Seleccionar Carpeta Base del Proyecto: " + projectName);
        projectChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        projectChooser.setAcceptAllFileFilterUsed(false);

        int result = projectChooser.showSaveDialog(ui);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = projectChooser.getSelectedFile();
            File potentialProjectDir = new File(selectedFolder, projectName);

            if (!potentialProjectDir.exists()) {
                if (!potentialProjectDir.mkdirs()) {
                    JOptionPane.showMessageDialog(ui, "No se pudo crear la carpeta del proyecto en:\n" + potentialProjectDir.getAbsolutePath(), "Error al Crear Proyecto", JOptionPane.ERROR_MESSAGE);
                    this.projectBaseDir = null;
                    return;
                }
                this.projectBaseDir = potentialProjectDir;
                updateStatus("Carpeta del proyecto creada en: " + this.projectBaseDir.getAbsolutePath());
            } else {
                int choice = JOptionPane.showConfirmDialog(ui,
                        "La carpeta '" + potentialProjectDir.getName() + "' ya existe.\n¿Desea usarla como base del proyecto?",
                        "Carpeta Existente", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.NO_OPTION) {
                    this.projectBaseDir = null;
                    updateStatus("Creación de proyecto cancelada.");
                    return;
                }
                this.projectBaseDir = potentialProjectDir;
                updateStatus("Usando carpeta existente para el proyecto: " + this.projectBaseDir.getAbsolutePath());
            }

            ui.projectRoot.setUserObject(projectName);
            ui.projectRoot.removeAllChildren(); 
            if (this.projectBaseDir.exists() && this.projectBaseDir.isDirectory()) {
                populateProjectTreeFromDisk(this.projectBaseDir, ui.projectRoot);
            }
            ui.treeModel.reload(ui.projectRoot); 
            closeAllTabsLogic();
            updateStatus("Nuevo Proyecto Creado/Abierto: " + projectName);
        } else {
            updateStatus("Creación de proyecto cancelada.");
        }
    }

    private void createNewFileLogic(String fileName) {
        JPanel newEditorPanel = createEditorPanelWithTextArea(null); 
        ui.addEditorTabUI(fileName, newEditorPanel);
        editorFileMap.put(newEditorPanel, null); 

        if (projectBaseDir != null) {
            addOrUpdateTreeNode(new FileNode(new File(projectBaseDir, fileName))); 
        } else {
            // Opcional si el usuario no abre un proyecto
        }

        updateStatus("Nuevo Archivo Creado (sin guardar): " + fileName);
        updateUndoRedoStatus();
    }

    private void closeAllTabsLogic() {
        ui.editorTabbedPane.removeAll();
        editorFileMap.clear();
        editorUndoManagerMap.clear();
        updateUndoRedoStatus();
        updateStatus("Listo");
        ui.lineLabel.setText("Ln 1, Col 1");
        ui.createInitialPlaceholderTabUI();
    }

    private void addOrUpdateTreeNode(FileNode fileNodeObject) { 
        if (ui.projectRoot == null || ui.projectRoot.getUserObject().equals("Ningún proyecto")) {
            return;
        }
        String nodeNameInTree = fileNodeObject.toString(); 

        boolean foundNode = false;
        for (int i = 0; i < ui.projectRoot.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ui.projectRoot.getChildAt(i);
            if (node.getUserObject() instanceof FileNode fileNodeInTree) { // Java 16+ pattern matching
                if (fileNodeInTree.getFile().equals(fileNodeObject.getFile())) {
                    foundNode = true;
                    break;
                }
            } else if (node.getUserObject().toString().equals(nodeNameInTree)) { 
            }
        }
        if (!foundNode) {
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(fileNodeObject); 
            ui.projectRoot.add(treeNode);
            ui.treeModel.nodesWereInserted(ui.projectRoot, new int[]{ui.projectRoot.getIndex(treeNode)});
        }
    }

    private UndoManager getActiveUndoManager() {
        Component selectedComponent = ui.editorTabbedPane.getSelectedComponent();
        if (selectedComponent instanceof JPanel) { 
            return editorUndoManagerMap.get(selectedComponent);
        }
        return null;
    }

    private void updateUndoRedoStatus() {
        UndoManager manager = getActiveUndoManager();
        undoAction.setEnabled(manager != null && manager.canUndo());
        redoAction.setEnabled(manager != null && manager.canRedo());
    }

    private JTextArea getTextAreaFromPanel(JPanel panel) {
        if (panel == null) {
            return null;
        }
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component view = scrollPane.getViewport().getView();
                if (view instanceof JTextArea) {
                    return (JTextArea) view;
                }
            }
        }
        return null;
    }

    private JTextArea getActiveTextArea() {
        Component selectedComponent = ui.editorTabbedPane.getSelectedComponent();
        if (selectedComponent instanceof JPanel) {
            return getTextAreaFromPanel((JPanel) selectedComponent);
        }
        return null;
    }

    private JTextArea getLineNumbersAreaFromPanel(JPanel panel) {
        if (panel == null) {
            return null;
        }
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                if (scrollPane.getRowHeader() != null && scrollPane.getRowHeader().getView() instanceof JTextArea) {
                    return (JTextArea) scrollPane.getRowHeader().getView();
                }
            }
        }
        return null;
    }

    public void handleTabChange() {
        Component selectedComp = ui.editorTabbedPane.getSelectedComponent();
        JTextArea activeTextArea = getActiveTextArea(); 

        if (activeTextArea != null && selectedComp instanceof JPanel) {
            JPanel activePanel = (JPanel) selectedComp; 
            updateCaretPosition(activeTextArea);
            updateLineNumbersForPanel(activePanel); 
            File currentFile = editorFileMap.get(activePanel);
            if (currentFile != null) {
                updateStatus("Archivo: " + currentFile.getName());
            } else {
                String tabTitle = ui.editorTabbedPane.getTitleAt(ui.editorTabbedPane.getSelectedIndex());
                updateStatus("Archivo sin guardar: " + tabTitle);
            }
        } else {
            ui.lineLabel.setText("Ln -, Col -");
            updateStatus("Listo");
        }
        updateUndoRedoStatus();
    }

    private void updateLineNumbersForPanel(JPanel editorPanel) {
        SwingUtilities.invokeLater(() -> {
            JTextArea targetTextArea = getTextAreaFromPanel(editorPanel);
            JTextArea targetLineNumbersArea = getLineNumbersAreaFromPanel(editorPanel);
            if (targetTextArea == null || targetLineNumbersArea == null || targetTextArea.getDocument() == null) {
                if (targetLineNumbersArea != null) {
                    targetLineNumbersArea.setText("1");
                }
                return;
            }
            Element root = targetTextArea.getDocument().getDefaultRootElement();
            int lineCount = root.getElementCount();
            StringBuilder numbers = new StringBuilder();
            for (int i = 1; i <= lineCount; i++) {
                numbers.append(i).append("\n");
            }
            if (numbers.length() > 0) {
                numbers.setLength(numbers.length() - 1);
            } else {
                numbers.append("1");
            }
            if (!targetLineNumbersArea.getText().equals(numbers.toString())) {
                targetLineNumbersArea.setText(numbers.toString());
            }
        });
    }

    private void updateCaretPosition(JTextArea editor) {
        if (editor == null || ui.lineLabel == null) {
            return;
        }
        try {
            int caretPos = editor.getCaretPosition();
            int line = editor.getLineOfOffset(caretPos);
            int column = caretPos - editor.getLineStartOffset(line);
            ui.lineLabel.setText("Ln " + (line + 1) + ", Col " + (column + 1));
        } catch (BadLocationException e) {
            ui.lineLabel.setText("Ln ?, Col ?");
        }
    }

    private void updateStatus(String message) {
        if (ui.statusLabel != null) {
            ui.statusLabel.setText(message);
        }
    }

    private void showUserManualLogic() {
        SwingUtilities.invokeLater(() -> {
            VisualizarPDF viewer = new VisualizarPDF();
            viewer.setVisible(true);
            viewer.openPdfFile();
        });
    }

    private void showAboutDialogLogic() {
        JOptionPane.showMessageDialog(ui,
                "IDE Raven\n"
                + "----------------------------------------------------------------\n"
                + "Carrera: Ingeniera en Sistemas Computacionales\n"
                + "Materia: Lenguajes y Autómatas I\n"
                + "Profesor: MANUEL HERNÁNDEZ HERNÁNDEZ\n"
                + "Equipo: 1\n"
                + "Integrantes: \n"
                + "Cesar Morales Francisco\nAmerica Denise Del Angel Guzman\nJohan Emmanuelle Garcia Gonzalez\nJorge Ivan Hernandez Del Angel\nDiana Ivone Morales Alejo\n"
                + "Fecha Creación: 09/05/2025\n" 
                + "Versión: 1.0 \n"
                + "----------------------------------------------------------------",
                "Acerca de IDE Raven", JOptionPane.INFORMATION_MESSAGE);
        updateStatus("Diálogo 'Acerca de' mostrado.");
    }

    // --- Lógica de Compilación ---
    private void compileCode() throws IOException {
        JTextArea activeEditor = getActiveTextArea();
        if (activeEditor == null) {
            updateStatus("Error: No hay archivo activo para compilar.");
            JOptionPane.showMessageDialog(ui, "Abra o seleccione un archivo para compilar.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sourceCode = activeEditor.getText();
        if (sourceCode.trim().isEmpty()) {
            updateStatus("Error: El archivo está vacío.");
            ui.generalOutputArea.setText("Error: No hay código fuente para compilar.");
            ui.outputTabbedPane.setSelectedIndex(2);
            return;
        }

        updateStatus("Compilando...");
        ui.lexicalOutputArea.setText("");
        ui.syntaxOutputArea.setText("--- Análisis Sintáctico ---\n");
        ui.generalOutputArea.setText("--- Iniciando Compilación ---\n");
        ui.outputTabbedPane.setSelectedIndex(0);

        boolean lexicalSuccess = runLexicalAnalysisLogic(sourceCode);
        boolean syntaxSuccess = false; 
        if (lexicalSuccess) {
            syntaxSuccess = runSyntaxAnalysisLogic(sourceCode); 
        }

        if (lexicalSuccess && syntaxSuccess) {
            ui.generalOutputArea.append("--- Análisis Léxico y Sintáctico completados sin errores ---\n");
            updateStatus("Compilación completada.");
        } else if (lexicalSuccess) { 
            ui.generalOutputArea.append("--- Compilación fallida: Errores sintácticos ---\n");
            updateStatus("Error de análisis sintáctico.");
            ui.outputTabbedPane.setSelectedIndex(1); 
        } else { 
            ui.generalOutputArea.append("--- Compilación fallida: Errores léxicos ---\n");
            updateStatus("Error de análisis léxico.");
        }
    }

    private boolean runLexicalAnalysisLogic(String code) throws IOException {
        StringBuilder tokenOutput = new StringBuilder();

        tokenOutput.append("--- Iniciando Análisis Léxico ---\n");
        ui.generalOutputArea.append("1. Iniciando Análisis Léxico...\n");
        updateStatus("Analizando léxico...");
        ui.lexicalOutputArea.setText(tokenOutput.toString());
        
        

        LexerCup lexerCup = new LexerCup(new StringReader(code));
        Symbol simbolo;
        boolean lexicalErrorsFound = false;

        // Limpiar la tabla antes de comenzar
        SwingUtilities.invokeLater(() -> {
            ((TokenTableModel) ui.tokenTable.getModel()).setRowCount(0);
        });

        while (true) {
            try {
                simbolo = lexerCup.next_token();
                if (simbolo == null || simbolo.sym == sym.EOF) {
                    tokenOutput.append("----------------------------------------------------------\n");
                    tokenOutput.append("--- Fin del Análisis Léxico ---\n");
                    break;
                }
                
                int tipoTokenId = simbolo.sym;
                
                // Actualizar la tabla
                updateTokenTable(simbolo);
                
                String tipoTokenNombre = (tipoTokenId >= 0 && tipoTokenId < sym.terminalNames.length) ? sym.terminalNames[tipoTokenId] : "DESCONOCIDO";
                if (tipoTokenId == sym.EOF) {
                    tipoTokenNombre = "EOF";
                } else if (tipoTokenId == sym.ERROR) {
                    tipoTokenNombre = "ERROR";
                }

               
                if (tipoTokenId == sym.ERROR) {
                    lexicalErrorsFound = true;
                    tokenOutput.append("    >>> ERROR LÉXICO: Símbolo/Caracter inesperado <<<\n");
                }
            } catch (IOException e) {
                logError("Error inesperado en análisis léxico", e);
                tokenOutput.append("\n--- ERROR INESPERADO LÉXICO: ").append(e.getMessage()).append(" ---\n");
                lexicalErrorsFound = true;
                break;
            }
        }

        List<LexicalError> detailedErrors = null;
        try {
            java.lang.reflect.Method getErrorsMethod = lexerCup.getClass().getMethod("getLexicalErrors");
            @SuppressWarnings("unchecked")
            List<LexicalError> errors = (List<LexicalError>) getErrorsMethod.invoke(lexerCup);
            detailedErrors = errors;
        } catch (NoSuchMethodException nsme) {
            /* No hacer nada */ } catch (IllegalAccessException | SecurityException | InvocationTargetException e) {
            logError("Error obteniendo errores detallados del lexer", e);
        }

        StringBuilder errorDetailsOutput = new StringBuilder();
        if (detailedErrors != null && !detailedErrors.isEmpty()) {
            errorDetailsOutput.append("--- Errores Léxicos Detectados (Detalle) ---\n");
            for (LexicalError error : detailedErrors) {
                errorDetailsOutput.append(error.toString()).append("\n");
            }
            errorDetailsOutput.append("-------------------------------------------\n");
            lexicalErrorsFound = true;
        }

        final String finalTokenText = tokenOutput.toString();
        final String finalErrorText = errorDetailsOutput.toString();
        final boolean hasErrors = lexicalErrorsFound;

        SwingUtilities.invokeLater(() -> {
            ui.lexicalOutputArea.setText(finalTokenText);
            if (hasErrors) {
                ui.generalOutputArea.setText(finalErrorText.isEmpty() ? "--- Se encontraron errores léxicos (ver marcadores en Análisis Léxico) ---\n" : finalErrorText);
                ui.outputTabbedPane.setSelectedIndex(2); // Salida para errores léxicos
            } else {
                ui.generalOutputArea.append("Análisis Léxico .\n"); // Añadir a salida general
            }
        });
        return !lexicalErrorsFound;
    }

    private boolean runSyntaxAnalysisLogic(String code) throws IOException {
        ui.syntaxOutputArea.setText("--- Iniciando Análisis Sintáctico ---\n");
        ui.generalOutputArea.append("2. Iniciando Análisis Sintáctico...\n");
        updateStatus("Analizando sintaxis...");

        LexerCup lexerCup = new LexerCup(new StringReader(code));
        java_cup.runtime.Scanner scanner = lexerCup; // Cast to interface if needed
        Sintax parser = new Sintax(scanner);
        boolean syntaxErrorsFound = false;
        try {
            parser.parse();
            List<String> syntaxErrorsList = parser.getSyntaxErrors();
            if (syntaxErrorsList.isEmpty()) {
                ui.syntaxOutputArea.append("\n--- Análisis sintáctico completado sin errores ---\n");
                ui.generalOutputArea.append("Análisis Sintáctico OK.\n");
                updateStatus("Análisis sintáctico completado.");
            } else {
                syntaxErrorsFound = true;
                ui.syntaxOutputArea.append("\n--- Errores Sintácticos Detectados ---\n");
                for (String error : syntaxErrorsList) {
                    ui.syntaxOutputArea.append(error + "\n");

                }
                ui.generalOutputArea.append("Error de Análisis Sintáctico.\n");
                updateStatus("Error de análisis sintáctico.");
            }
        } catch (Exception e) {
            syntaxErrorsFound = true;
            logError("Error fatal durante análisis sintáctico", e);
            ui.syntaxOutputArea.append("\n--- ERROR FATAL sintáctico: " + e.getMessage() + " ---\n");
            List<String> accumulatedErrors = parser.getSyntaxErrors();
            if (!accumulatedErrors.isEmpty()) {
                ui.syntaxOutputArea.append("\n--- Errores acumulados antes del fallo: ---\n");
                for (String err : accumulatedErrors) {
                    ui.syntaxOutputArea.append(err + "\n");
                }
            }
            ui.generalOutputArea.append("Error fatal de Análisis Sintáctico.\n");
            updateStatus("Error fatal de análisis sintáctico.");
        }
        final boolean hasErrors = syntaxErrorsFound;
        SwingUtilities.invokeLater(() -> {
            if (hasErrors) {
                ui.outputTabbedPane.setSelectedIndex(1); 
            }
        });
        return !syntaxErrorsFound;
    }

    private void logError(String message, Exception ex) {
        Logger.getLogger(InterfazLogic.class.getName()).log(Level.SEVERE, message, ex);
    }

    // Agrega una fila a la tabla de tokens en la interfaz
    private void updateTokenTable(int tipoTokenId, String lexema, int linea, int columna, String valor) {
        SwingUtilities.invokeLater(() -> {
            TokenTableModel model = (TokenTableModel) ui.tokenTable.getModel();
            model.addRow(new Object[]{linea, columna, tipoTokenId, lexema, valor});
        });
    }

    private void updateTokenTable(Symbol simbolo) {
        SwingUtilities.invokeLater(() -> {
            TokenTableModel model = (TokenTableModel) ui.tokenTable.getModel();
            
            // Obtener los valores básicos
            int linea = simbolo.left + 1;
            int columna = simbolo.right + 1;
            
            // Obtener el nombre del token usando terminalNames
            String nombreToken = sym.terminalNames[simbolo.sym];
            
            // El lexema es el valor del símbolo (si existe)
            String lexema = simbolo.value != null ? simbolo.value.toString() : "";
            
            // Combinar lexema con su valor numérico
            String lexemaValor = lexema + " / " + simbolo.sym;
            
            // Agregar la fila
            model.addRow(new Object[]{
                linea,           // Línea
                columna,         // Columna
                nombreToken,     // TIPO_TOKEN (nombre del token)
                lexemaValor     // LEXEMA/VALOR (lexema / número)
            });
        });
    }

    // Clase interna FileNode

    static class FileNode {

        File file;

        public FileNode(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return file.getName();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            FileNode fileNode = (FileNode) obj;
            return file != null ? file.equals(fileNode.file) : fileNode.file == null;
        }

        @Override
        public int hashCode() {
            return file != null ? file.hashCode() : 0;
        }
    }
}
