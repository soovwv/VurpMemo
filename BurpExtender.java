// soovwv
package burp;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class BurpExtender implements IBurpExtender, ITab {

    private IBurpExtenderCallbacks callbacks;
    private JPanel mainPanel;
    private static final int MAX_ITEMS = 1000; // List maximum items
    private javax.swing.Timer debounceTimer;

    private JTextField todoField, memoField1, memoField2, memoField3;
    private DefaultListModel<String> todoListModel, memoListModel1, memoListModel2, memoListModel3;
    private JList<String> todoList, memoList1, memoList2, memoList3;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        callbacks.setExtensionName("*VurpMemo");

        SwingUtilities.invokeLater(() -> {
            mainPanel = new JPanel(new BorderLayout());

            // Top: Input panel
            JPanel inputPanel = new JPanel(new GridLayout(1, 4, 10, 10));
            inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            todoField = createInputField("[To-Do]", inputPanel, "todo");
            memoField1 = createInputField("Memo1", inputPanel, "memo1"); // 메모1 => Memo1
            memoField2 = createInputField("Memo2", inputPanel, "memo2"); // 메모2 => Memo2
            memoField3 = createInputField("Memo3", inputPanel, "memo3"); // 메모3 => Memo3

            // Bottom: List panel
            JPanel listPanel = new JPanel(new GridLayout(1, 4, 10, 10));
            listPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            todoList = createList("To-Do", listPanel, todoListModel = new DefaultListModel<>()); // To-Do 목록 => To-Do List
            memoList1 = createList("List", listPanel, memoListModel1 = new DefaultListModel<>()); // 목록 => List
            memoList2 = createList("List", listPanel, memoListModel2 = new DefaultListModel<>()); // 목록 => List
            memoList3 = createList("List", listPanel, memoListModel3 = new DefaultListModel<>()); // 목록 => List

            // Bottom: Button panel
            JPanel buttonPanel = new JPanel(new BorderLayout());
            JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton openFolderButton = new JButton("Save Folder"); // 저장 폴더 => Save Folder
            openFolderButton.setBackground(new Color(173, 216, 230));
            openFolderButton.addActionListener(e -> openSaveFolder());
            rightButtonPanel.add(openFolderButton);

            JButton loadButton = new JButton("Load"); // 불러오기 => Load
            loadButton.setBackground(new Color(255, 182, 193));
            loadButton.addActionListener(e -> loadMemos());
            rightButtonPanel.add(loadButton);

            JButton saveButton = new JButton("Save Memo"); // 메모 저장 => Save Memo
            saveButton.setBackground(new Color(144, 238, 144));
            saveButton.addActionListener(e -> saveMemos());
            rightButtonPanel.add(saveButton);

            buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

            mainPanel.add(inputPanel, BorderLayout.NORTH);
            mainPanel.add(listPanel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            callbacks.customizeUiComponent(mainPanel);
            callbacks.addSuiteTab(this);
            callbacks.printOutput("soovwv~~~~~~~~~"); // Extension loaded successfully
        });
    }

    private JTextField createInputField(String label, JPanel panel, String type) {
        JPanel fieldPanel = new JPanel(new BorderLayout(5, 0));
        JTextField field = new JTextField(15);
        JButton addButton = new JButton("Add"); // 입력 => Add
        addButton.setBackground(new Color(140, 136, 136));

        fieldPanel.add(new JLabel(label + ": "), BorderLayout.WEST);
        fieldPanel.add(field, BorderLayout.CENTER);
        fieldPanel.add(addButton, BorderLayout.EAST);
        panel.add(fieldPanel);

        addButton.addActionListener(e -> {
            if (debounceTimer != null) debounceTimer.stop();
            debounceTimer = new javax.swing.Timer(300, ev -> {
                String text = field.getText().trim();
                if (!text.isEmpty()) {
                    switch (type) {
                        case "todo": addToList(todoListModel, text); break;
                        case "memo1": addToList(memoListModel1, text); break;
                        case "memo2": addToList(memoListModel2, text); break;
                        case "memo3": addToList(memoListModel3, text); break;
                    }
                    field.setText("");
                }
            });
            debounceTimer.setRepeats(false);
            debounceTimer.start();
        });

        return field;
    }

    private void addToList(DefaultListModel<String> model, String text) {
        if (model.size() >= MAX_ITEMS) {
            model.remove(0);
        }
        model.addElement(text);
    }

    private JList<String> createList(String title, JPanel panel, DefaultListModel<String> listModel) {
        JList<String> list = new JList<>(listModel);
        list.setVisibleRowCount(10);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(scrollPane);

        JPopupMenu popup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete"); // 삭제
        deleteItem.addActionListener(e -> {
            int index = list.getSelectedIndex();
            if (index != -1) {
                listModel.remove(index);
            }
        });
        JMenuItem editItem = new JMenuItem("Edit"); // 편집
        editItem.addActionListener(e -> {
            int index = list.getSelectedIndex();
            if (index != -1) {
                String current = listModel.get(index);
                String newText = JOptionPane.showInputDialog(mainPanel, "Edit content:", current); // 수정할 내용: => Edit content:
                if (newText != null && !newText.trim().isEmpty()) {
                    listModel.set(index, newText);
                }
            }
        });
        popup.add(deleteItem);
        popup.add(editItem);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index != -1) {
                    list.setSelectedIndex(index);
                    popup.show(list, e.getX(), e.getY());
                }
            }
        });

        return list;
    }

    private void openSaveFolder() {
        try {
            String userHome = System.getProperty("user.home");
            File burpDir = new File(userHome, ".burp");
            if (!burpDir.exists()) {
                burpDir.mkdirs();
            }
            Desktop.getDesktop().open(burpDir);
            callbacks.printOutput("Save folder opened: " + burpDir.getAbsolutePath()); // 저장 폴더 열림: => Save folder opened:
        } catch (IOException e) {
            callbacks.printError("Failed to open folder: " + e.getMessage()); // 폴더 열기 실패: => Failed to open folder:
            JOptionPane.showMessageDialog(mainPanel, "Failed to open folder: " + e.getMessage(), // 폴더 열기 실패: => Failed to open folder:
                    "Error", JOptionPane.ERROR_MESSAGE); // 오류 => Error
        }
    }

    private void saveMemos() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String userHome = System.getProperty("user.home");
                File burpDir = new File(userHome, ".burp");
                if (!burpDir.exists()) burpDir.mkdirs();
                File memoFile = new File(burpDir, "burp_memo.txt");

                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(memoFile), 8192))) {
                    writer.println("To-Do:");
                    for (int i = 0; i < todoListModel.size(); i++) writer.println("  - " + todoListModel.get(i));
                    writer.println("Memo1:"); // 메모1: => Memo1:
                    for (int i = 0; i < memoListModel1.size(); i++) writer.println("  - " + memoListModel1.get(i));
                    writer.println("Memo2:"); // 메모2: => Memo2:
                    for (int i = 0; i < memoListModel2.size(); i++) writer.println("  - " + memoListModel2.get(i));
                    writer.println("Memo3:"); // 메모3: => Memo3:
                    for (int i = 0; i < memoListModel3.size(); i++) writer.println("  - " + memoListModel3.get(i));
                    writer.println("Saved at: " + new java.util.Date()); // 저장 시각: => Saved at:
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    callbacks.printOutput("Memo saved successfully."); // 메모가 저장되었습니다. => Memo saved successfully.
                    JOptionPane.showMessageDialog(mainPanel, "Memo saved successfully.", "Save Complete", JOptionPane.INFORMATION_MESSAGE); // 메모가 저장되었습니다. => Memo saved successfully. / 저장 완료 => Save Complete
                } catch (Exception e) {
                    callbacks.printError("Error saving memo: " + e.getMessage()); // 메모 저장 중 오류: => Error saving memo:
                    JOptionPane.showMessageDialog(mainPanel, "Failed to save memo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // 저장 실패: => Failed to save memo: / 오류 => Error
                }
            }
        };
        worker.execute();
    }

    private void loadMemos() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String userHome = System.getProperty("user.home");
                File memoFile = new File(userHome + "/.burp/burp_memo.txt");
                if (!memoFile.exists()) {
                    callbacks.printOutput("No saved memo file found: " + memoFile.getAbsolutePath()); // 저장된 메모 파일이 없습니다: => No saved memo file found:
                    return null;
                }

                todoListModel.clear();
                memoListModel1.clear();
                memoListModel2.clear();
                memoListModel3.clear();

                try (BufferedReader reader = new BufferedReader(new FileReader(memoFile), 8192)) {
                    String line;
                    String currentSection = null;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("To-Do:")) {
                            currentSection = "todo";
                        } else if (line.startsWith("Memo1:")) { // 메모1: => Memo1:
                            currentSection = "memo1";
                        } else if (line.startsWith("Memo2:")) { // 메모2: => Memo2:
                            currentSection = "memo2";
                        } else if (line.startsWith("Memo3:")) { // 메모3: => Memo3:
                            currentSection = "memo3";
                        } else if (line.startsWith("Saved at:")) { // 저장 시각: => Saved at:
                            break;
                        } else if (line.startsWith("- ") && currentSection != null) {
                            String item = line.substring(2).trim();
                            if (!item.isEmpty()) {
                                switch (currentSection) {
                                    case "todo": addToList(todoListModel, item); break;
                                    case "memo1": addToList(memoListModel1, item); break;
                                    case "memo2": addToList(memoListModel2, item); break;
                                    case "memo3": addToList(memoListModel3, item); break;
                                }
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    callbacks.printOutput("Memo loaded successfully."); // 메모가 불러와졌습니다. => Memo loaded successfully.
                    JOptionPane.showMessageDialog(mainPanel, "Memo loaded successfully.", "Load Complete", JOptionPane.INFORMATION_MESSAGE); // 메모가 불러와졌습니다. => Memo loaded successfully. / 불러오기 완료 => Load Complete
                } catch (Exception e) {
                    callbacks.printError("Error loading memo: " + e.getMessage()); // 메모 불러오기 실패: => Error loading memo:
                    JOptionPane.showMessageDialog(mainPanel, "Failed to load memo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // 메모 불러오기 실패: => Failed to load memo: / 오류 => Error
                }
            }
        };
        worker.execute();
    }

    @Override
    public String getTabCaption() {
        return "*VurpMemo";
    }

    @Override
    public Component getUiComponent() {
        return mainPanel;
    }
}
