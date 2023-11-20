import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class App {
    private JFrame frame;
    private JTextField nameEntry;
    private JTextField descEntry;
    private JList<String> spellList;
    private DefaultListModel<String> spellListModel;
    private JTextArea selectedSpellInfo;

    private Connection conn;

    public App() {
        frame = new JFrame("Harry Potter Spell Book");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new FlowLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        frame.add(mainPanel);
        
        nameEntry = new JTextField(20);
        descEntry = new JTextField(20);
        
        JLabel nameLabel = new JLabel("Spell Name:");
        nameLabel.setForeground(Color.WHITE);
        mainPanel.add(nameLabel);
        mainPanel.add(nameEntry);
        
        JLabel descLabel = new JLabel("Spell Description:");
        descLabel.setForeground(Color.WHITE);
        mainPanel.add(descLabel);
        mainPanel.add(descEntry);
        
        JButton addButton = new JButton("Add Spell");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSpell();
            }
        });
        mainPanel.add(addButton);
        
        spellListModel = new DefaultListModel<>();
        spellList = new JList<>(spellListModel);
        spellList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spellList.addListSelectionListener(e -> onSpellSelected());
        spellList.setBackground(Color.orange);
        JScrollPane scrollPane = new JScrollPane(spellList);
        mainPanel.add(scrollPane);
        
        JLabel infoLabel = new JLabel("Spell Info:");
        infoLabel.setForeground(Color.WHITE);
        mainPanel.add(infoLabel);
        
        selectedSpellInfo = new JTextArea(5, 20);
        selectedSpellInfo.setEditable(false);
        selectedSpellInfo.setBackground(Color.orange);
        mainPanel.add(selectedSpellInfo);

        JButton modifyButton = new JButton("Modify Spell");
        modifyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openModifySpellDialog();
            }
        });
        mainPanel.add(modifyButton);

        JButton deleteButton = new JButton("Delete Spell");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSpell();
            }
        });
        mainPanel.add(deleteButton);

        connectToDatabase();
        loadSpells();
        
        mainPanel.setBackground(Color.RED);
        frame.setBackground(Color.RED);

        frame.setVisible(true);
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/spellbook","root","1234");
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS spells (id INTEGER PRIMARY KEY, name TEXT, description TEXT)");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void addSpell() {
        String name = nameEntry.getText();
        String description = descEntry.getText();

        if (!name.isEmpty() && !description.isEmpty()) {
            try {
                PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO spells (name, description) VALUES (?, ?)");
                statement.setString(1, name);
                statement.setString(2, description);
                statement.executeUpdate();

                nameEntry.setText("");
                descEntry.setText("");
                loadSpells();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Both name and description are required.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadSpells() {
        spellListModel.clear();
        try {
            PreparedStatement statement = conn.prepareStatement("SELECT id, name FROM spells");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                spellListModel.addElement(id + ". " + name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openModifySpellDialog() {
        String selectedValue = spellList.getSelectedValue();
        if (selectedValue != null) {
            int selectedSpellId = Integer.parseInt(selectedValue.split("\\.")[0]);
            try {
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT name, description FROM spells WHERE id = ?");
                statement.setInt(1, selectedSpellId);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String description = resultSet.getString("description");

                    // Create a dialog for modifying the spell
                    JDialog modifyDialog = new JDialog(frame, "Modify Spell", true);
                    modifyDialog.setLayout(new FlowLayout());

                    JTextField newNameEntry = new JTextField(name, 20);
                    JTextField newDescEntry = new JTextField(description, 20);

                    modifyDialog.add(new JLabel("New Spell Name:"));
                    modifyDialog.add(newNameEntry);

                    modifyDialog.add(new JLabel("New Spell Description:"));
                    modifyDialog.add(newDescEntry);

                    JButton updateButton = new JButton("Update");
                    updateButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            String newName = newNameEntry.getText();
                            String newDescription = newDescEntry.getText();

                            if (!newName.isEmpty() && !newDescription.isEmpty()) {
                                try {
                                    PreparedStatement updateStatement = conn.prepareStatement(
                                            "UPDATE spells SET name = ?, description = ? WHERE id = ?");
                                    updateStatement.setString(1, newName);
                                    updateStatement.setString(2, newDescription);
                                    updateStatement.setInt(3, selectedSpellId);
                                    updateStatement.executeUpdate();

                                    modifyDialog.dispose();
                                    loadSpells();
                                    selectedSpellInfo.setText("Name: " + newName + "\nDescription: " + newDescription);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                JOptionPane.showMessageDialog(modifyDialog, "Both name and description are required.",
                                        "Warning", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    });
                    modifyDialog.add(updateButton);

                    modifyDialog.setSize(300, 150);
                    modifyDialog.setVisible(true);
                } else {
                    selectedSpellInfo.setText("No spell selected");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            selectedSpellInfo.setText("No spell selected");
        }
    }

    private void deleteSpell() {
        String selectedValue = spellList.getSelectedValue();
        if (selectedValue != null) {
            int selectedSpellId = Integer.parseInt(selectedValue.split("\\.")[0]);

            int dialogResult = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this spell?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                try {
                    PreparedStatement deleteStatement = conn.prepareStatement(
                            "DELETE FROM spells WHERE id = ?");
                    deleteStatement.setInt(1, selectedSpellId);
                    deleteStatement.executeUpdate();

                    loadSpells();
                    selectedSpellInfo.setText("No spell selected");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            selectedSpellInfo.setText("No spell selected");
        }
    }

    private void onSpellSelected() {
        String selectedValue = spellList.getSelectedValue();
        if (selectedValue != null) {
            int selectedSpellId = Integer.parseInt(selectedValue.split("\\.")[0]);
            try {
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT name, description FROM spells WHERE id = ?");
                statement.setInt(1, selectedSpellId);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String description = resultSet.getString("description");
                    selectedSpellInfo.setText("Name: " + name + "\nDescription: " + description);
                } else {
                    selectedSpellInfo.setText("No spell selected");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            selectedSpellInfo.setText("No spell selected");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App());
    }
}
