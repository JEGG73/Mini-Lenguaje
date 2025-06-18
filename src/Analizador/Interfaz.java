package Analizador;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Interfaz extends JFrame {

    JTree projectsTree;
    JTabbedPane editorTabbedPane;
    JTabbedPane outputTabbedPane;
    JTextArea lexicalOutputArea;
    JTextArea syntaxOutputArea;
    JTextArea generalOutputArea;
    JLabel statusLabel;
    JLabel lineLabel;

    // Barra de Menú y sus elementos 
    JMenuBar menuBar;
    JMenuItem newProjectItem, newFileItem, openItem, saveItem, saveAsItem, exitItem;
    JMenuItem undoItem, redoItem, cutItem, copyItem, pasteItem;
    JMenuItem compileItem, compileAndRunItem;
    JMenuItem userManualItem, aboutItem;

    DefaultMutableTreeNode projectRoot;
    DefaultTreeModel treeModel;

    private InterfazLogic logic;

    // Agregar este campo en la clase Interfaz
    JTable tokenTable;
    private TokenTableModel tokenTableModel;

    public Interfaz() {
        setTitle("Mini-Aplicacion IDE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        logic = new InterfazLogic(this); // Crear la lógica, pasándole esta instancia de Interfaz

        createMainArea();        // Crea los paneles principales y componentes
        logic.createActions();   // La lógica crea las Actions (que contienen los listeners y atajos)
        createMenuBar();         // Crea la barra de menú y ASIGNA las Actions de la lógica a los JMenuItems
        createStatusBar();       // Crea la barra de estado

        logic.setupInitialState(); // La lógica configura el estado inicial (ej. placeholder tab)

        // Aplicar el tema FlatLaf para un obtener un mejor diseño
        try {
            FlatMaterialLighterIJTheme.setup();
        } catch (Exception ex) {
            System.err.println("Error al cargar FlatLaf: " + ex.getMessage());
        }

        setVisible(true);
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // --- Menú Archivo ---
        JMenu fileMenu = new JMenu("Archivo");
        JMenu newMenu = new JMenu("Nuevo");

        newProjectItem = new JMenuItem("Nuevo Proyecto");
        newProjectItem.addActionListener(e -> logic.handleNewProject());

        newFileItem = new JMenuItem("Nuevo Archivo");
        newFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
        newFileItem.addActionListener(e -> logic.handleNewFile());

        newMenu.add(newProjectItem);
        newMenu.add(newFileItem);

        openItem = new JMenuItem("Abrir"); // Para abrir proyecto
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
        openItem.addActionListener(e -> logic.handleOpenProject()); // Llama a openProject

        saveItem = new JMenuItem("Guardar");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
        saveItem.addActionListener(e -> logic.handleSaveFile(false));

        saveAsItem = new JMenuItem("Guardar como...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, shortcutMask)); // Para elegir ruta de guardado
        saveAsItem.addActionListener(e -> logic.handleSaveFile(true));

        exitItem = new JMenuItem("Salir");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcutMask));
        exitItem.addActionListener(e -> logic.handleExit());

        fileMenu.add(newMenu);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- Menú Editar ---
        JMenu editMenu = new JMenu("Editar");
        
        undoItem = new JMenuItem(logic.getUndoAction()); // Asigna la Action directamente
        redoItem = new JMenuItem(logic.getRedoAction());
        cutItem = new JMenuItem(logic.getCutAction());
        copyItem = new JMenuItem(logic.getCopyAction());
        pasteItem = new JMenuItem(logic.getPasteAction());

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);

        // --- Menú Ejecutar ---
        JMenu runMenu = new JMenu("Ejecutar");
        compileItem = new JMenuItem(logic.getCompileAction());
        runMenu.add(compileItem);
        if (logic.getCompileAndExecuteAction() != null) { // Si la acción existe en la lógica
            compileAndRunItem = new JMenuItem(logic.getCompileAndExecuteAction());
            runMenu.add(compileAndRunItem);
        }

        // --- Menú Ayuda ---
        JMenu helpMenu = new JMenu("Ayuda");
        userManualItem = new JMenuItem("Manual de usuario");
        userManualItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        userManualItem.addActionListener(e -> logic.handleShowUserManual()); // Usado para abrir el manual de usuario

        aboutItem = new JMenuItem("Acerca de");
        aboutItem.addActionListener(e -> logic.handleShowAboutDialog());

        helpMenu.add(userManualItem);
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(runMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void createMainArea() {
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Panel de proyectos
        JPanel projectsPanel = new JPanel(new BorderLayout());
        projectsPanel.setBorder(BorderFactory.createTitledBorder("Explorador de Proyectos"));
        projectRoot = new DefaultMutableTreeNode("Ningún proyecto"); 
        treeModel = new DefaultTreeModel(projectRoot);
        projectsTree = new JTree(treeModel);
        projectsTree.setRootVisible(true);
        projectsTree.setShowsRootHandles(true);

        projectsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) { // Doble clic
                    logic.handleProjectTreeDoubleClick(e);
                }
            }
        });
        projectsPanel.add(new JScrollPane(projectsTree), BorderLayout.CENTER);

        editorTabbedPane = new JTabbedPane();

        outputTabbedPane = new JTabbedPane();

        // Panel para análisis léxico que contendrá tanto el área de texto como la tabla
        JPanel lexicalPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Configurar el área de texto del análisis léxico
        lexicalOutputArea = new JTextArea();
        lexicalOutputArea.setEditable(false);
        lexicalOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Configurar la tabla de tokens
        tokenTableModel = new TokenTableModel();
        tokenTable = new JTable(tokenTableModel);
        configureTokenTable();
        
        // Agregar área de texto y tabla en un JSplitPane horizontal
        JSplitPane lexicalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        lexicalSplitPane.setLeftComponent(new JScrollPane(lexicalOutputArea));
        
        JPanel tokenTablePanel = new JPanel(new BorderLayout());
        tokenTablePanel.setBorder(BorderFactory.createTitledBorder("Tokens"));
        tokenTablePanel.add(new JScrollPane(tokenTable), BorderLayout.CENTER);
        lexicalSplitPane.setRightComponent(tokenTablePanel);
        lexicalSplitPane.setResizeWeight(0.5);
        
        // Agregar el JSplitPane al panel de análisis léxico
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        lexicalPanel.add(lexicalSplitPane, gbc);
        
        // Agregar las pestañas al outputTabbedPane
        outputTabbedPane.addTab("Análisis Léxico", lexicalPanel);
        
        syntaxOutputArea = new JTextArea();
        syntaxOutputArea.setEditable(false);
        syntaxOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputTabbedPane.addTab("Análisis Sintáctico", new JScrollPane(syntaxOutputArea));

        generalOutputArea = new JTextArea();
        generalOutputArea.setEditable(false);
        generalOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputTabbedPane.addTab("Salida", new JScrollPane(generalOutputArea));

        // Configurar el split vertical principal
        JSplitPane rightVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightVerticalSplit.setTopComponent(editorTabbedPane);
        rightVerticalSplit.setBottomComponent(outputTabbedPane);
        rightVerticalSplit.setResizeWeight(0.75);

        // Configurar el split principal
        mainSplitPane.setLeftComponent(projectsPanel);
        mainSplitPane.setRightComponent(rightVerticalSplit);
        mainSplitPane.setResizeWeight(0.2);
        mainSplitPane.setDividerLocation(200);

        add(mainSplitPane, BorderLayout.CENTER);

        editorTabbedPane.addChangeListener(e -> logic.handleTabChange());
    }

    // Modificar el método configureTokenTable para ajustar las columnas según lo requerido
    private void configureTokenTable() {
        tokenTable.setFillsViewportHeight(true);
        tokenTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tokenTable.getTableHeader().setReorderingAllowed(false);
        
        // Crear un renderizador centrado para todas las columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                    ((JLabel) c).setToolTipText(value != null ? value.toString() : "");
                }
                return c;
            }
        };
        
        // Centrar los encabezados de las columnas
        ((DefaultTableCellRenderer)tokenTable.getTableHeader().getDefaultRenderer())
            .setHorizontalAlignment(JLabel.CENTER);
        
        // Ajustar el ancho y aplicar el renderizador centrado a cada columna
        for (int i = 0; i < tokenTable.getColumnCount(); i++) {
            tokenTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            switch (i) {
                case 0: // Línea
                    tokenTable.getColumnModel().getColumn(i).setPreferredWidth(60);
                    break;
                case 1: // Columna
                    tokenTable.getColumnModel().getColumn(i).setPreferredWidth(60);
                    break;
                case 2: // TIPO_TOKEN
                    tokenTable.getColumnModel().getColumn(i).setPreferredWidth(150);
                    break;
                case 3: // LEXEMA/VALOR
                    tokenTable.getColumnModel().getColumn(i).setPreferredWidth(200);
                    break;
            }
        }
        
        // Configurar colores y fuente
        tokenTable.setShowGrid(true);
        tokenTable.setGridColor(new Color(100, 100, 100));
        tokenTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Configurar colores alternados para las filas (opcional)
        tokenTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? table.getBackground() : new Color(240, 240, 240));
                }
                
                if (c instanceof JLabel) {
                    ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                }
                
                return c;
            }
        });
    }

    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel("Listo");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        lineLabel = new JLabel("Ln 1, Col 1");
        lineLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(lineLabel, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    // Método para que InterfazLogic añada una pestaña de editor construida
    public void addEditorTabUI(String title, Component editorComponent) {
        if (editorTabbedPane.getTabCount() == 1) {
            Component firstTabComp = editorTabbedPane.getComponentAt(0);
            String firstTabTitle = editorTabbedPane.getTitleAt(0);
            // Identifica el placeholder por nombre o título.
            if ("Bienvenido".equals(firstTabTitle) || (firstTabComp.getName() != null && "PlaceholderPanel".equals(firstTabComp.getName()))) {
                if (logic.isEditorFileMapEmpty()) { 
                    editorTabbedPane.removeTabAt(0);
                }
            }
        }
        editorTabbedPane.addTab(title, editorComponent);
        editorTabbedPane.setSelectedComponent(editorComponent);
    }

    public void createInitialPlaceholderTabUI() {
        if (editorTabbedPane.getTabCount() == 0) {
            JPanel initialPanel = new JPanel(new GridBagLayout());
            initialPanel.setName("PlaceholderPanel"); // Para identificación
            JLabel placeholderLabel = new JLabel("Cree o abra un archivo para empezar.");
            placeholderLabel.setForeground(Color.GRAY);
            initialPanel.add(placeholderLabel);
            editorTabbedPane.addTab("Bienvenido", initialPanel);
            editorTabbedPane.setEnabledAt(0, false); // Deshabilitar la pestaña placeholder
        }
    }

    public static void main(String[] args) {
        try {
            FlatArcDarkOrangeIJTheme.setup();
        } catch (Exception ex) {
            System.err.println("Error al cargar FlatLaf: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(Interfaz::new);
    }
}
